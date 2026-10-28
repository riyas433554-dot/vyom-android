package com.hermes.assistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Command vocabulary comes from bundled JSON; authorization and safety remain compiled into ActionEngine. */
object DeviceCommandConfig {
    data class AppEntry(val name: String, val packageName: String, val aliases: List<String>)
    data class Snapshot(val apps: List<AppEntry>, val openVerbs: List<String>, val commandPhrases: Map<String,List<String>>, val commandPatterns: Map<String,Regex>)
    @Volatile private var current: Snapshot = fallbackSnapshot()

    fun initialize(context: Context) {
        current = try {
            val root=JSONObject(context.assets.open("device_commands.json").bufferedReader().use { it.readText() })
            val appArray=root.optJSONArray("apps") ?: JSONArray()
            val apps=(0 until appArray.length()).mapNotNull { i ->
                val o=appArray.optJSONObject(i) ?: return@mapNotNull null
                AppEntry(o.optString("name"),o.optString("package"),o.optJSONArray("aliases").strings()+o.optString("name"))
            }
            val verbs=root.optJSONArray("openVerbs").strings().ifEmpty { fallbackSnapshot().openVerbs }
            val po=root.optJSONObject("commandPhrases") ?: JSONObject()
            val phrases=po.keys().asSequence().associateWith { po.optJSONArray(it).strings() }
            val ro=root.optJSONObject("commandPatterns") ?: JSONObject()
            val patterns=ro.keys().asSequence().mapNotNull { key -> try { key to Regex(ro.optString(key),RegexOption.IGNORE_CASE) } catch(_:Exception) { null } }.toMap()
            Snapshot(apps,verbs,phrases,patterns)
        } catch(_:Exception) { fallbackSnapshot() }
    }
    fun matchCommand(input:String):String? {
        val s=normalize(input)
        return current.commandPhrases.entries.firstOrNull { (_,phrases)->phrases.any { phraseMatches(s,normalize(it)) } }?.key
    }
    fun percentValue(input:String,key:String):Int?=current.commandPatterns[key]?.find(input)?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0..100 }
    fun extractAppTarget(input:String):String? {
        val raw=input.trim().removeSuffix("?").trim()
        val words=raw.split(Regex("\\s+")).filter { it.isNotBlank() }
        if(words.size<2)return null
        val verb=current.openVerbs.sortedByDescending { it.length }.firstOrNull { raw.startsWith(it,ignoreCase=true) }
            ?: words.firstOrNull()?.takeIf { first->current.openVerbs.any { closeWord(first,it) } } ?: return null
        var target=if(raw.startsWith(verb,ignoreCase=true))raw.substring(verb.length).trim() else words.drop(1).joinToString(" ")
        target=target.replace(Regex("^(?i:the|app|application)\\s+"),"").replace(Regex("(?i)\\s+(app|application)$"),"").trim()
        if(target.isBlank()||target.length>50)return null
        return bestAppName(target) ?: target
    }
    fun packageForApp(name:String):String?=current.apps.map { it to bestAliasScore(name,it) }.filter { it.second>=minimumAppMatchScore(name) }.maxByOrNull { it.second }?.first?.packageName?.takeIf { it.isNotBlank() }
    fun displayNameForPackage(pkg:String):String?=current.apps.firstOrNull { it.packageName==pkg }?.name
    fun similarityScore(input:String,label:String):Int=similarity(normalize(input),normalize(label))
    fun minimumAppMatchScore(input:String):Int=if(normalize(input).length<=4)75 else 62
    private fun bestAppName(input:String):String?=current.apps.map { it to bestAliasScore(input,it) }.filter { it.second>=minimumAppMatchScore(input) }.maxByOrNull { it.second }?.first?.name
    private fun bestAliasScore(input:String,app:AppEntry):Int=(app.aliases+app.name).maxOfOrNull { similarity(normalize(input),normalize(it)) } ?: 0

    fun resolveLearnedCommand(context:Context,phrase:String):String? {
        val raw=context.getSharedPreferences("vyom_taught_commands",Context.MODE_PRIVATE).getString("aliases_json",null) ?: return null
        val json=try { JSONObject(raw) } catch(_:Exception) { return null }
        val key=normalize(phrase)
        val best=json.keys().asSequence().map { it to json.optString(it) }.maxByOrNull { (learned,_)->similarity(key,learned) } ?: return null
        return best.second.takeIf { similarity(key,best.first)>=if(key.length<=5)80 else 68 }
    }
    fun saveTeaching(context:Context,phrase:String,canonicalCommand:String) {
        val key=normalize(phrase).take(100); val canonical=canonicalCommand.trim().take(120)
        require(key.isNotBlank()&&canonical.isNotBlank())
        val prefs=context.getSharedPreferences("vyom_taught_commands",Context.MODE_PRIVATE)
        val json=try { JSONObject(prefs.getString("aliases_json","{}") ?: "{}") } catch(_:Exception) { JSONObject() }
        json.put(key,canonical); prefs.edit().putString("aliases_json",json.toString()).apply()
    }
    private fun phraseMatches(input:String,phrase:String):Boolean {
        if(input==phrase||input.contains(" $phrase ")||input.startsWith("$phrase ")||input.endsWith(" $phrase"))return true
        val words=input.split(' ').filter { it.isNotBlank() }; val wanted=phrase.split(' ').filter { it.isNotBlank() }
        if(wanted.isEmpty()||words.size<wanted.size)return false
        for(start in 0..(words.size-wanted.size)) {
            var ok=true
            for(i in wanted.indices)if(!closeWord(words[start+i],wanted[i])){ok=false;break}
            if(ok)return true
        }
        return similarity(input,phrase)>=(if(phrase.length<10)78 else 66)
    }
    private fun closeWord(a:String,b:String):Boolean {
        if(a==b)return true
        if(a.length<4||b.length<4)return false
        return editDistance(a,b)<=(if(maxOf(a.length,b.length)>=8)2 else 1)
    }
    private fun normalize(s:String):String=s.lowercase(Locale.ROOT).replace('ā','a').replace('ī','i').replace(Regex("[^a-z0-9% ]")," ").replace(Regex("\\s+")," ").trim()
    private fun similarity(a:String,b:String):Int {
        if(a.isEmpty()||b.isEmpty())return if(a==b)100 else 0
        if(a==b)return 100
        return ((maxOf(a.length,b.length)-editDistance(a,b)).coerceAtLeast(0)*100)/maxOf(a.length,b.length)
    }
    private fun editDistance(a:String,b:String):Int {
        var prev=IntArray(b.length+1){it}
        for(i in a.indices){val next=IntArray(b.length+1);next[0]=i+1
            for(j in b.indices)next[j+1]=minOf(next[j]+1,prev[j+1]+1,prev[j]+if(a[i]==b[j])0 else 1)
            prev=next
        };return prev[b.length]
    }
    private fun JSONArray?.strings():List<String> = if(this==null)emptyList() else (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }
    private fun fallbackSnapshot()=Snapshot(
        listOf(AppEntry("YouTube","com.google.android.youtube",listOf("youtube","you tube","yt")),AppEntry("Chrome","com.android.chrome",listOf("chrome","google chrome","browser")),AppEntry("WhatsApp","com.whatsapp",listOf("whatsapp","whats app")),AppEntry("Maps","com.google.android.apps.maps",listOf("maps","google maps")),AppEntry("Instagram","com.instagram.android",listOf("instagram","insta")),AppEntry("Spotify","com.spotify.music",listOf("spotify"))),
        listOf("open","launch","start","khol","kholo","kholna","chalao","chala"),
        mapOf("torch_on" to listOf("turn on torch","torch on","flashlight on","turn on flashlight","torch chalu","torch chalu karo","flashlight chalu"),"torch_off" to listOf("turn off torch","torch off","flashlight off","turn off flashlight","torch band","torch band karo","flashlight band"),"volume_up" to listOf("volume up","increase volume","volume badhao","awaaz badhao","sound badhao"),"volume_down" to listOf("volume down","decrease volume","volume kam karo","awaaz kam karo","sound kam karo"),"volume_mute" to listOf("mute","volume mute","awaaz band","sound band"),"volume_unmute" to listOf("unmute","volume unmute","awaaz chalu","sound chalu")),
        mapOf("volume_set" to Regex("""\b(?:set\s+(?:the\s+)?volume\s+to\s+|volume\s+)(\d{1,3})\s*%?\b""",RegexOption.IGNORE_CASE),"brightness_set" to Regex("""\b(?:set|change)\s+(?:the\s+)?brightness\s+(?:to\s+)?(\d{1,3})\s*%?\b""",RegexOption.IGNORE_CASE))
    )
}