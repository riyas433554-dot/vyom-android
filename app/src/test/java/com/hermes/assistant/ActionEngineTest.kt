package com.hermes.assistant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
class ActionEngineTest {
 @Test fun normalConversationBypassesEngine(){ assertNull(ActionEngine.classify("Explain why the sky is blue")); assertNull(ActionEngine.classify("Tell me a joke")); assertNull(ActionEngine.classify("How do I delete a file?")); assertNull(ActionEngine.classify("Send me a recipe")) }
 @Test fun settingsAndAppsAreSafe(){ assertEquals(ActionEngine.Risk.SAFE,ActionEngine.classify("open Wi-Fi settings")?.risk); assertEquals(ActionEngine.Operation.OPEN_SETTINGS,ActionEngine.classify("open Wi-Fi settings")?.operation); assertEquals("WhatsApp",ActionEngine.classify("open WhatsApp")?.target) }
 @Test fun mediaControlsAreSafe(){ assertEquals(1,ActionEngine.classify("volume up")?.value); assertEquals(-1,ActionEngine.classify("volume down")?.value); assertEquals(70,ActionEngine.classify("set volume to 70%")?.value); assertEquals(25,ActionEngine.classify("set brightness to 25%")?.value); assertEquals(ActionEngine.Operation.TORCH,ActionEngine.classify("turn on flashlight")?.operation) }
 @Test fun hinglishAndTypoTolerantControls() {
  assertEquals(ActionEngine.Operation.TORCH,ActionEngine.classify("torch chalu")?.operation)
  assertEquals(1,ActionEngine.classify("flashlight chalu karo")?.value)
  assertEquals(ActionEngine.Operation.TORCH,ActionEngine.classify("torch band")?.operation)
  assertEquals(0,ActionEngine.classify("torch band")?.value)
  assertEquals(ActionEngine.Operation.VOLUME_DELTA,ActionEngine.classify("volume badhao")?.operation)
  assertEquals(1,ActionEngine.classify("volume badhao")?.value)
  assertEquals(-1,ActionEngine.classify("volume kam karo")?.value)
  assertEquals(-1,ActionEngine.classify("volume down")?.value)
  assertEquals(50,ActionEngine.classify("volume 50%")?.value)
  assertEquals(ActionEngine.Operation.VOLUME_MUTE,ActionEngine.classify("awaaz band")?.operation)
  assertEquals(ActionEngine.Operation.VOLUME_UNMUTE,ActionEngine.classify("awaaz chalu")?.operation)
  assertEquals("YouTube",ActionEngine.classify("Open Yiutube")?.target)
  assertEquals("YouTube",ActionEngine.classify("Open Youtube")?.target)
 }
 @Test fun alarmTimerParse(){ assertEquals("07:30",ActionEngine.classify("set alarm for 7:30")?.target); assertEquals(300,ActionEngine.classify("set timer for 5 minutes")?.value) }
 @Test fun consequentialActionsNeedConfirmation(){ assertEquals(ActionEngine.Risk.CONFIRM,ActionEngine.classify("call +1 555 123 4567")?.risk); assertEquals(ActionEngine.Risk.CONFIRM,ActionEngine.classify("call my mom")?.risk); assertEquals(ActionEngine.Risk.CONFIRM,ActionEngine.classify("text my mom saying hello")?.risk); assertEquals(ActionEngine.Risk.CONFIRM,ActionEngine.classify("text +1 555 123 4567 saying hello")?.risk); assertEquals(ActionEngine.Risk.CONFIRM,ActionEngine.classify("send this to my friend")?.risk); assertEquals(ActionEngine.Risk.CONFIRM,ActionEngine.classify("delete old file")?.risk); assertEquals(ActionEngine.Risk.CONFIRM,ActionEngine.classify("install this app")?.risk) }
 @Test fun blockedPolicyWins(){ assertEquals(ActionEngine.Risk.BLOCKED,ActionEngine.classify("open my bank app")?.risk); assertEquals(ActionEngine.Risk.BLOCKED,ActionEngine.classify("read my OTP")?.risk); assertEquals(ActionEngine.Risk.BLOCKED,ActionEngine.classify("send my OTP to someone")?.risk); assertEquals(ActionEngine.Risk.BLOCKED,ActionEngine.classify("uninstall the app")?.risk); assertEquals(ActionEngine.Risk.BLOCKED,ActionEngine.classify("open security settings")?.risk) }
 @Test fun unknownDirectActionMustFallbackAndCannotExecuteDirectly(){ assertEquals(true,ActionEngine.looksLikeDeviceAction("please enable airplane mode")); assertNull(ActionEngine.classify("please enable airplane mode")) }
 @Test fun modelFallbackCannotChangeRequestedIntent(){ assertFalse(ActionEngine.fallbackMatchesIntent("please enable silent mode","open silent mode")); assertEquals(true,ActionEngine.fallbackMatchesIntent("increase sound","volume up")); assertEquals(true,ActionEngine.fallbackMatchesIntent("turn off bluetooth","open Bluetooth settings")) }
}
