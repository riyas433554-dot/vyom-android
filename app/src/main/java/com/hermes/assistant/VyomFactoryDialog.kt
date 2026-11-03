package com.hermes.assistant

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.InputType
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.os.Handler
import android.os.Looper
import java.util.UUID
import java.util.concurrent.Executors

/** Live Factory workspace. All visible task data is read from GitHub Issues, never demo fixtures. */
internal class VyomFactoryDialog(
    private val activity: Activity,
    private val token: String,
    private val onOpenSettings: () -> Unit
) {
    private val density = activity.resources.displayMetrics.density
    private fun dp(value: Int) = (value * density).toInt()
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val client by lazy { VyomFactoryGitHubClient(token) }
    private lateinit var dialog: Dialog
    private lateinit var status: TextView
    private lateinit var taskList: LinearLayout
    private lateinit var titleInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var createButton: Button
    private var selectedPriority = "P2"
    private var pendingSourceId: String? = null
    private var busy = false

    fun show() {
        dialog = Dialog(activity)
        val accent = VyomTheme.accent(activity, activity.getSharedPreferences("vyom_settings", Activity.MODE_PRIVATE)
            .getString("theme_accent", "cyan") ?: "cyan")
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(14))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xF5121F35.toInt(), 0xF8070B14.toInt())).apply {
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), Color.argb(160, Color.red(accent), Color.green(accent), Color.blue(accent)))
            }
        }
        val header = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val heading = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        heading.addView(TextView(activity).apply {
            text = "VYOM FACTORY"; textSize = 21f; letterSpacing = 0.14f
            typeface = Typeface.create("sans-serif", Typeface.BOLD); setTextColor(VyomDesignSpec.TEXT_PRIMARY)
        })
        heading.addView(TextView(activity).apply {
            text = "LIVE TASK REGISTRY · GITHUB ISSUES"; textSize = 9f
            typeface = Typeface.MONOSPACE; setTextColor(VyomDesignSpec.TEXT_SECONDARY)
        })
        header.addView(heading, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(TextView(activity).apply {
            text = "↻"; textSize = 23f; gravity = Gravity.CENTER; setTextColor(accent)
            contentDescription = "Refresh Factory tasks"
            setOnClickListener { refresh() }
        }, LinearLayout.LayoutParams(dp(42), dp(42)))
        header.addView(TextView(activity).apply {
            text = "✕"; textSize = 20f; gravity = Gravity.CENTER; setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            setOnClickListener { dialog.dismiss() }
        }, LinearLayout.LayoutParams(dp(38), dp(42)))
        root.addView(header)

        status = TextView(activity).apply {
            text = "Checking GitHub connection…"; textSize = 11f
            setTextColor(VyomDesignSpec.TEXT_SECONDARY); setPadding(0, dp(8), 0, dp(10))
        }
        root.addView(status)

        val composer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply { setColor(0x33213A54); cornerRadius = dp(16).toFloat() }
        }
        composer.addView(TextView(activity).apply {
            text = "REGISTER A COMPLAINT / TASK"; textSize = 10f
            typeface = Typeface.MONOSPACE; setTextColor(accent)
        })
        titleInput = EditText(activity).apply {
            hint = "Short problem title"; setSingleLine(true); textSize = 14f
            setTextColor(Color.WHITE); setHintTextColor(0xFF8B9BB0.toInt())
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        composer.addView(titleInput, LinearLayout.LayoutParams(-1, dp(46)))
        descriptionInput = EditText(activity).apply {
            hint = "What is broken, expected result, steps and evidence…"
            minLines = 2; maxLines = 4; textSize = 13f
            setTextColor(Color.WHITE); setHintTextColor(0xFF8B9BB0.toInt())
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        composer.addView(descriptionInput, LinearLayout.LayoutParams(-1, dp(88)))
        val actions = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val priorityButton = Button(activity).apply {
            text = "PRIORITY · $selectedPriority"; textSize = 10f
            setOnClickListener {
                val options = arrayOf("P0 · Critical", "P1 · High", "P2 · Normal", "P3 · Low")
                AlertDialog.Builder(activity).setTitle("Complaint priority")
                    .setSingleChoiceItems(options, selectedPriority.drop(1).toInt(), null)
                    .setPositiveButton("SET") { d, _ ->
                        val index = (d as AlertDialog).listView.checkedItemPosition
                        selectedPriority = "P" + index
                        text = "PRIORITY · $selectedPriority"
                    }.setNegativeButton("CANCEL", null).show()
            }
        }
        actions.addView(priorityButton, LinearLayout.LayoutParams(0, dp(42), 1f))
        createButton = Button(activity).apply {
            text = "REGISTER TASK"; textSize = 10f
            setOnClickListener { createTask() }
        }
        actions.addView(createButton, LinearLayout.LayoutParams(0, dp(42), 1f).apply { leftMargin = dp(8) })
        composer.addView(actions)
        root.addView(composer, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        root.addView(TextView(activity).apply {
            text = "TASKS · LIVE FROM REPOSITORY"; textSize = 10f
            letterSpacing = 0.1f; typeface = Typeface.MONOSPACE
            setTextColor(VyomDesignSpec.TEXT_SECONDARY); setPadding(0, dp(8), 0, dp(6))
        })
        val scroll = ScrollView(activity).apply { clipToPadding = false }
        taskList = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(taskList, android.widget.FrameLayout.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val footer = TextView(activity).apply {
            text = "Changes are saved to GitHub Issues. ChatGPT and Termux can use the same issue IDs."
            textSize = 9f; setTextColor(VyomDesignSpec.TEXT_SECONDARY)
            setPadding(dp(2), dp(8), dp(2), 0)
        }
        root.addView(footer)

        dialog.setContentView(root)
        dialog.setOnDismissListener { worker.shutdownNow() }
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.68f)
        }
        dialog.show()
        dialog.window?.setLayout(dp(560).coerceAtMost(activity.resources.displayMetrics.widthPixels - dp(24)),
            activity.resources.displayMetrics.heightPixels - dp(48))
        if (token.isBlank()) {
            status.text = "GitHub token missing. Open Settings and save a token with repository Issues read/write permission."
            taskList.addView(message("No GitHub connection. Tasks are not stored locally as a substitute."))
            taskList.addView(Button(activity).apply {
                text = "OPEN SETTINGS"
                setOnClickListener { dialog.dismiss(); onOpenSettings() }
            })
        } else {
            refresh()
        }
    }

    private fun refresh() {
        if (busy || token.isBlank() || !::status.isInitialized) return
        busy = true
        status.text = "Refreshing live tasks from GitHub…"
        worker.execute {
            try {
                val tasks = client.listTasks()
                main.post {
                    if (!dialog.isShowing) return@post
                    status.text = "GitHub Issues · ${tasks.size} Factory tasks loaded"
                    render(tasks)
                    busy = false
                }
            } catch (error: Exception) {
                main.post {
                    if (!dialog.isShowing) return@post
                    status.text = errorMessage(error)
                    taskList.removeAllViews()
                    taskList.addView(message("Could not load the shared registry. No local fake tasks were substituted."))
                    busy = false
                }
            }
        }
    }

    private fun createTask() {
        if (busy) return
        val title = titleInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()
        if (title.length < 4 || description.isBlank()) {
            status.text = "Enter a title (at least 4 characters) and a description."
            return
        }
        busy = true
        createButton.isEnabled = false
        status.text = "Registering complaint in GitHub Issues…"
        val prefs = activity.getSharedPreferences("vyom_factory", Activity.MODE_PRIVATE)
        val sourceId = pendingSourceId ?: prefs.getString("pending_draft_id", null) ?: UUID.randomUUID().toString()
        pendingSourceId = sourceId
        prefs.edit().putString("pending_draft_id", sourceId).apply()
        worker.execute {
            try {
                val task = client.createTask(title, description, selectedPriority, "factory", sourceId)
                main.post {
                    if (!dialog.isShowing) return@post
                    titleInput.setText("")
                    descriptionInput.setText("")
                    pendingSourceId = null
                    prefs.edit().remove("pending_draft_id").apply()
                    status.text = "Registered as #${task.number} · saved to shared registry"
                    createButton.isEnabled = true
                    busy = false
                    refresh()
                }
            } catch (error: Exception) {
                main.post {
                    if (!dialog.isShowing) return@post
                    status.text = errorMessage(error)
                    createButton.isEnabled = true
                    busy = false
                }
            }
        }
    }

    private fun render(tasks: List<VyomFactoryGitHubClient.Task>) {
        taskList.removeAllViews()
        if (tasks.isEmpty()) {
            taskList.addView(message("No Factory complaints registered yet. Use the form above to create the first shared task."))
            return
        }
        tasks.forEach { task ->
            val card = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = GradientDrawable().apply {
                    setColor(0x3316263B); cornerRadius = dp(14).toFloat()
                    setStroke(dp(1), 0x333A7891)
                }
            }
            card.addView(TextView(activity).apply {
                text = "#${task.number} · ${task.title}"; textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setTextColor(VyomDesignSpec.TEXT_PRIMARY)
            })
            card.addView(TextView(activity).apply {
                text = "${task.metadata.priority} · ${task.metadata.status.uppercase()} · UPDATED ${task.updatedAt}"
                textSize = 9f; typeface = Typeface.MONOSPACE
                setTextColor(VyomDesignSpec.TEXT_SECONDARY); setPadding(0, dp(4), 0, dp(4))
            })
            val description = task.body.substringBefore("<!-- VYOM_FACTORY_META:").trim()
            if (description.isNotBlank()) card.addView(TextView(activity).apply {
                text = description; textSize = 11f; setTextColor(VyomDesignSpec.TEXT_SECONDARY)
                maxLines = 3; setPadding(0, 0, 0, dp(6))
            })
            val controls = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            fun action(label: String, next: String) {
                controls.addView(Button(activity).apply {
                    text = label; textSize = 8f; isAllCaps = true
                    setOnClickListener { update(task, next, task.metadata.priority) }
                }, LinearLayout.LayoutParams(0, dp(36), 1f).apply { rightMargin = dp(3) })
            }
            when (task.metadata.status) {
                "open" -> { action("START", "in_progress"); action("PAUSE", "paused"); action("BLOCK", "blocked") }
                "in_progress" -> { action("PAUSE", "paused"); action("BLOCK", "blocked"); action("DONE", "completed") }
                "paused" -> { action("RESUME", "in_progress"); action("CANCEL", "cancelled") }
                "blocked" -> { action("REOPEN", "open"); action("CANCEL", "cancelled") }
                else -> Unit
            }
            val priority = Button(activity).apply {
                text = "PRIORITY"; textSize = 8f
                setOnClickListener { choosePriority(task) }
            }
            controls.addView(priority, LinearLayout.LayoutParams(dp(78), dp(36)))
            controls.addView(Button(activity).apply {
                text = "EDIT"; textSize = 8f
                setOnClickListener { editTask(task) }
            }, LinearLayout.LayoutParams(dp(64), dp(36)).apply { leftMargin = dp(3) })
            card.addView(controls)
            card.setOnClickListener {
                AlertDialog.Builder(activity).setTitle("Factory task #${task.number}")
                    .setMessage(description.ifBlank { "No description supplied." } + "\\n\\nIssue: " + task.url)
                    .setPositiveButton("OPEN GITHUB") { _, _ ->
                        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(task.url)))
                    }.setNegativeButton("CLOSE", null).show()
            }
            taskList.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
    }

    private fun editTask(task: VyomFactoryGitHubClient.Task) {
        val form = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(4))
        }
        val title = EditText(activity).apply {
            setSingleLine(true)
            setText(task.title.replace(Regex("^\\[VYOM FACTORY\\]\\[[Pp][0-3]\\]\\s*"), ""))
            hint = "Task title"
        }
        val description = EditText(activity).apply {
            minLines = 3
            maxLines = 8
            setText(task.body.substringBefore("<!-- VYOM_FACTORY_META:").trim())
            hint = "Complaint details"
        }
        form.addView(title)
        form.addView(description)
        AlertDialog.Builder(activity).setTitle("Edit Factory task #${task.number}")
            .setView(form)
            .setPositiveButton("SAVE") { _, _ ->
                val newTitle = title.text.toString().trim()
                val newDescription = description.text.toString().trim()
                if (newTitle.length < 4 || newDescription.isBlank()) {
                    status.text = "Edit not saved: title and description are required."
                } else {
                    saveEdit(task, newTitle, newDescription)
                }
            }.setNegativeButton("CANCEL", null).show()
    }

    private fun saveEdit(task: VyomFactoryGitHubClient.Task, title: String, description: String) {
        if (busy) return
        busy = true
        status.text = "Saving edits to task #${task.number}…"
        worker.execute {
            try {
                client.updateTask(task, newTitle = title, newDescription = description)
                main.post {
                    if (!dialog.isShowing) return@post
                    status.text = "Task #${task.number} edited and saved."
                    busy = false
                    refresh()
                }
            } catch (error: Exception) {
                main.post {
                    if (!dialog.isShowing) return@post
                    status.text = errorMessage(error)
                    busy = false
                }
            }
        }
    }

    private fun choosePriority(task: VyomFactoryGitHubClient.Task) {
        val values = arrayOf("P0", "P1", "P2", "P3")
        AlertDialog.Builder(activity).setTitle("Set task priority")
            .setSingleChoiceItems(values, values.indexOf(task.metadata.priority), null)
            .setPositiveButton("SAVE") { d, _ ->
                val index = (d as AlertDialog).listView.checkedItemPosition
                if (index in values.indices) update(task, task.metadata.status, values[index])
            }.setNegativeButton("CANCEL", null).show()
    }

    private fun update(task: VyomFactoryGitHubClient.Task, nextStatus: String, priority: String) {
        if (busy) return
        busy = true
        status.text = "Saving task #${task.number}…"
        worker.execute {
            try {
                client.updateTask(task, newPriority = priority, newStatus = nextStatus)
                main.post {
                    if (!dialog.isShowing) return@post
                    status.text = "Task #${task.number} updated and saved."
                    busy = false
                    refresh()
                }
            } catch (error: Exception) {
                main.post {
                    if (!dialog.isShowing) return@post
                    status.text = errorMessage(error)
                    busy = false
                }
            }
        }
    }

    private fun message(value: String) = TextView(activity).apply {
        text = value; textSize = 12f; setTextColor(VyomDesignSpec.TEXT_SECONDARY)
        setPadding(dp(12), dp(18), dp(12), dp(18))
    }

    private fun errorMessage(error: Exception): String {
        val api = error as? VyomFactoryGitHubClient.FactoryApiException
        return when (api?.code) {
            401 -> "GitHub token rejected (401). Replace it in Settings."
            403 -> "GitHub denied access (403). Token needs Issues read/write permission for this repository."
            404 -> "Repository or issue endpoint not found (404). Check repository access."
            422 -> "GitHub rejected the task data (422). Check title, labels and issue permissions."
            else -> error.message?.take(180) ?: "Factory request failed."
        }
    }
}
