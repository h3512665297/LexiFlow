package com.lexiflow.wordbook.ui

import android.Manifest
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.switchmaterial.SwitchMaterial
import com.lexiflow.wordbook.LexiFlowApplication
import com.lexiflow.wordbook.MainActivity
import com.lexiflow.wordbook.R
import com.lexiflow.wordbook.ReminderScheduler
import com.lexiflow.wordbook.data.UserSettings
import com.lexiflow.wordbook.data.DictEntry
import com.lexiflow.wordbook.data.DictResult
import com.lexiflow.wordbook.data.DictionaryClient
import com.lexiflow.wordbook.data.WordEntity
import kotlinx.coroutines.launch

class LibraryFragment : Fragment() {
    private val activity get() = requireActivity() as MainActivity
    private val viewModel get() = activity.viewModel
    private lateinit var ui: UiKit
    private lateinit var content: LinearLayout
    private lateinit var scrollView: ScrollView
    private var pendingExport = ""

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            runCatching {
                val json = requireContext().contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error(getString(R.string.library_cannot_read))
                val isSimple = runCatching {
                    val first = org.json.JSONArray(json).getJSONObject(0)
                    !first.has("phonetic") && !first.has("example") && !first.has("synonyms")
                }.getOrDefault(false)
                if (isSimple) {
                    Toast.makeText(requireContext(), "正在通过 AI 生成词汇详情…", Toast.LENGTH_SHORT).show()
                    viewModel.importSimpleWords(json)
                } else {
                    viewModel.importWords(json)
                }
            }.onSuccess {
                Toast.makeText(requireContext(), getString(R.string.library_imported, it), Toast.LENGTH_SHORT).show()
                viewModel.refresh()
            }.onFailure {
                Toast.makeText(requireContext(), getString(R.string.library_import_failed, it.message), Toast.LENGTH_LONG).show()
            }
        }
    }

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@registerForActivityResult
        runCatching {
            requireContext().contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(pendingExport) }
        }.onSuccess { Toast.makeText(requireContext(), getString(R.string.library_export_done), Toast.LENGTH_SHORT).show() }
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(requireContext(), getString(R.string.library_no_perm), Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        ui = UiKit(requireContext())
        content = ui.vertical()
        scrollView = ScrollView(requireContext()).apply { addView(content) }
        return scrollView
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val child = scrollView.getChildAt(0) ?: return@setOnScrollChangeListener
            if (scrollY + scrollView.height >= child.height - ui.dp(200)) {
                viewModel.loadMore()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.library.collect { render(it) }
            }
        }
        viewModel.loadLibrary("search")
    }

    private fun render(state: LibraryState) = with(ui) {
        content.removeAllViews()
        content.setPadding(dp(20), dp(24), dp(20), dp(30))
        content.addView(text(str(R.string.library_title), 13, forest, true))
        content.addView(text(str(R.string.library_subtitle), 27, ink, true))
        content.gap(20)

        content.addView(text(str(R.string.library_current_book), 18, ink, true))
        content.gap(8)
        state.books.forEach { book ->
            content.addView(card(if (book.isSelected) mint else paper) {
                setPadding(dp(16), dp(14), dp(16), dp(14))
                addView(horizontal {
                    addView(vertical {
                        addView(text(book.name, 17, ink, true))
                        val count = state.bookSizes[book.id] ?: 0
                        addView(text("${book.description}  ·  $count 词", 13, muted))
                    }, LinearLayout.LayoutParams(0, -2, 1f))
                    addView(softButton(if (book.isSelected) str(R.string.library_learning) else str(R.string.library_select)) { viewModel.selectBook(book.id) })
                })
            })
            content.gap(8)
        }

        val search = EditText(requireContext()).apply {
            hint = str(R.string.library_search_hint)
            setSingleLine()
            setText(state.query)
            contentDescription = str(R.string.library_search_hint)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setOnEditorActionListener { _, _, _ ->
                viewModel.loadLibrary("search", text.toString())
                true
            }
        }
        content.gap(12)
        content.addView(search, LinearLayout.LayoutParams(-1, dp(52)))
        content.gap(8)
        content.addView(horizontal {
            addView(softButton(str(R.string.library_tab_all)) { viewModel.loadLibrary("search") }, LinearLayout.LayoutParams(0, dp(48), 1f))
            addView(softButton(str(R.string.library_tab_favorites)) { viewModel.loadLibrary("favorites") }, LinearLayout.LayoutParams(0, dp(48), 1f))
            addView(softButton(str(R.string.library_tab_mistakes)) { viewModel.loadLibrary("mistakes") }, LinearLayout.LayoutParams(0, dp(48), 1f))
        })
        content.gap(8)
        content.addView(softButton(str(R.string.library_dict_lookup), forest, android.graphics.Color.WHITE) { showLookupDialog() }, LinearLayout.LayoutParams(-1, dp(48)))
        content.gap(14)
        if (state.words.isEmpty() && !state.loadingMore) {
            content.addView(text(str(R.string.library_empty), 15, muted))
        } else {
            state.words.forEach { content.addView(wordCard(it)).also { content.gap(8) } }
            if (state.loadingMore) {
                content.gap(8)
                content.addView(text(str(R.string.library_load_more), 14, muted))
            }
        }

        content.gap(18)
        content.addView(text(str(R.string.library_settings), 18, ink, true))
        content.gap(8)
        content.addView(settingsCard(state.settings))
        content.gap(18)
        content.addView(text(str(R.string.library_tools), 18, ink, true))
        content.gap(8)
        content.addView(horizontal {
            addView(softButton(str(R.string.library_import_json)) { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                LinearLayout.LayoutParams(0, dp(50), 1f))
            addView(softButton(str(R.string.library_export_data)) {
                lifecycleScope.launch {
                    pendingExport = viewModel.exportJson()
                    exportLauncher.launch("lexiflow-backup.json")
                }
            }, LinearLayout.LayoutParams(0, dp(50), 1f))
        })
        content.gap(8)
        content.addView(softButton(str(R.string.library_clear_progress), coral, android.graphics.Color.WHITE) {
            AlertDialog.Builder(requireContext())
                .setTitle(str(R.string.library_clear_confirm))
                .setMessage(str(R.string.library_clear_message))
                .setNegativeButton(str(R.string.library_clear_cancel), null)
                .setPositiveButton(str(R.string.library_clear_ok)) { _, _ -> viewModel.clearData() }
                .show()
        }, LinearLayout.LayoutParams(-1, dp(50)))
    }

    private fun wordCard(word: WordEntity): View = with(ui) {
        card {
            setPadding(dp(16), dp(14), dp(12), dp(14))
            addView(horizontal {
                addView(vertical {
                    addView(text(word.text, 19, ink, true))
                    val shortMeaning = word.meaning.lineSequence().firstOrNull()?.take(80) ?: word.meaning.take(80)
                    addView(text(shortMeaning, 14, muted))
                    if (word.lapseCount > 0) addView(text(str(R.string.library_lapses, word.lapseCount), 12, coral))
                }, LinearLayout.LayoutParams(0, -2, 1f))
                addView(softButton(if (word.isFavorite) str(R.string.library_favorite_on) else str(R.string.library_favorite_off)) { viewModel.toggleFavorite(word.id) })
            })
            setOnClickListener { showWordDetail(word) }
            setOnLongClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle(word.text)
                    .setMessage("确定删除这个单词吗？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("删除") { _, _ ->
                        lifecycleScope.launch { viewModel.deleteWord(word.id) }
                    }
                    .show()
                true
            }
            contentDescription = "${word.text}，${word.meaning}"
        }
    }

    private fun settingsCard(current: UserSettings): View = with(ui) {
        card(mint) {
            setPadding(dp(18), dp(18), dp(18), dp(18))
            fun settingRow(label: String, value: String, change: () -> Unit) {
                addView(horizontal {
                    addView(text(label, 15, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
                    addView(softButton(value) { change() })
                })
                gap(8)
            }
            val wordsPerRoundOptions = intArrayOf(5, 10, 15, 20)
            settingRow(str(R.string.library_setting_daily_goal), str(R.string.library_setting_value_words, current.dailyGoal)) {
                showPicker(str(R.string.library_setting_daily_goal), wordsPerRoundOptions, current.dailyGoal) { selected ->
                    viewModel.saveSettings(current.copy(dailyGoal = selected))
                }
            }
            val reviewsPerRoundOptions = intArrayOf(10, 15, 20, 40, 100)
            settingRow(str(R.string.library_setting_new_limit), str(R.string.library_setting_value_words, current.newLimit)) {
                showPicker(str(R.string.library_setting_new_limit), reviewsPerRoundOptions, current.newLimit) { selected ->
                    viewModel.saveSettings(current.copy(newLimit = selected))
                }
            }
            val reminderHourOptions = IntArray(15) { it + 8 }
            settingRow(str(R.string.library_setting_reminder), str(R.string.library_setting_value_hour, current.reminderHour)) {
                showPicker(str(R.string.library_setting_reminder), reminderHourOptions, current.reminderHour) { selected ->
                    val updated = current.copy(reminderHour = selected)
                    viewModel.saveSettings(updated)
                    ReminderScheduler.schedule(requireContext(), updated.reminderEnabled, updated.reminderHour)
                }
            }
            val retentionOptions = intArrayOf(85, 90, 95)
            settingRow(str(R.string.library_setting_retention), str(R.string.library_setting_value_pct, current.desiredRetention)) {
                showPicker(str(R.string.library_setting_retention), retentionOptions, current.desiredRetention) { selected ->
                    viewModel.saveSettings(current.copy(desiredRetention = selected))
                }
            }
            val hasKey = viewModel.getApiKey().isNotBlank()
            settingRow(str(R.string.library_setting_api_key), if (hasKey) "已设置" else "未设置") {
                showApiKeyDialog()
            }
            addView(switchRow(str(R.string.library_setting_auto_play), current.autoPlay) {
                viewModel.saveSettings(current.copy(autoPlay = it))
            })
            addView(switchRow(str(R.string.library_setting_daily_reminder), current.reminderEnabled) {
                val updated = current.copy(reminderEnabled = it)
                viewModel.saveSettings(updated)
                if (it && Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                ReminderScheduler.schedule(requireContext(), it, current.reminderHour)
            })
            addView(switchRow(str(R.string.library_setting_dark_mode), current.darkMode) {
                viewModel.saveSettings(current.copy(darkMode = it))
                AppCompatDelegate.setDefaultNightMode(
                    if (it) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                )
            })
        }
    }

    private fun switchRow(label: String, checked: Boolean, action: (Boolean) -> Unit) =
        SwitchMaterial(requireContext()).apply {
            text = label
            isChecked = checked
            gravity = Gravity.CENTER_VERTICAL
            setOnCheckedChangeListener { _, value -> action(value) }
        }

    private fun showApiKeyDialog() {
        val input = EditText(requireContext()).apply {
            hint = "sk-..."
            isSingleLine = true
            setText(viewModel.getApiKey())
            setPadding(ui.dp(16), ui.dp(12), ui.dp(16), ui.dp(12))
        }
        AlertDialog.Builder(requireContext())
            .setTitle(ui.str(R.string.library_setting_api_key))
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                viewModel.setApiKey(input.text.toString().trim())
                viewModel.refresh()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showPicker(title: String, options: IntArray, current: Int, onSelect: (Int) -> Unit) {
        val items = options.map { it.toString() }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setSingleChoiceItems(items, options.indexOf(current).coerceAtLeast(0)) { dialog, which ->
                onSelect(options[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun showWordDetail(word: WordEntity) {
        val message = buildString {
            append("${word.phonetic}\n\n${word.meaning}")
            if (word.example.isNotBlank()) append("\n\n${word.example}\n${word.exampleTranslation}")
            if (word.wordRoot.isNotBlank()) append("\n\n词根联想：${word.wordRoot}")
            if (word.collocation.isNotBlank()) append("\n常见搭配：${word.collocation}")
            if (word.synonyms.isNotBlank()) append("\n近义词：${word.synonyms}")
        }
        AlertDialog.Builder(requireContext())
            .setTitle(word.text)
            .setMessage(message)
            .setNeutralButton(ui.str(R.string.library_detail_speak)) { _, _ -> activity.speak(word) }
            .setNegativeButton("删除") { _, _ ->
                lifecycleScope.launch { viewModel.deleteWord(word.id) }
            }
            .setPositiveButton(ui.str(R.string.library_detail_close), null)
            .show()
    }

    private fun showLookupDialog() {
        val input = EditText(requireContext()).apply {
            hint = ui.str(R.string.library_dict_hint)
            isSingleLine = true
            textSize = 16f
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setPadding(0, 0, 0, 0)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
        }
        val searchBar = with(ui) {
            vertical {
                setPadding(dp(12), dp(4), dp(12), dp(4))
                addView(horizontal {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(text("🔍", 18, muted, gravity = Gravity.CENTER))
                    gap(10)
                    addView(input, LinearLayout.LayoutParams(0, dp(44), 1f))
                })
            }
        }
        val card = with(ui) { card(paper) { addView(searchBar) } }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(card)
            .setPositiveButton(ui.str(R.string.library_dict_search), null)
            .setNegativeButton(ui.str(R.string.library_detail_close), null)
            .create()

        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performLookup(input.text.toString().trim(), input, dialog)
                true
            } else false
        }
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                setTextColor(ui.forest)
                setOnClickListener { performLookup(input.text.toString().trim(), input, dialog) }
            }
        }
        dialog.show()
    }

    private fun performLookup(word: String, input: View, dialog: AlertDialog) {
        if (word.isBlank()) return
        val parent = input.parent as? android.view.ViewGroup ?: return
        parent.removeAllViews()
        parent.addView(ui.text("$word — ${ui.str(R.string.library_dict_loading)}", 16, ui.muted, gravity = Gravity.CENTER),
            android.widget.LinearLayout.LayoutParams(-1, ui.dp(60)))

        lifecycleScope.launch {
            when (val result = DictionaryClient.lookup(word, viewModel.getApiKey())) {
                is DictResult.Found -> {
                    dialog.dismiss()
                    showWordResult(result.entry)
                }
                is DictResult.NotFound -> {
                    parent.removeAllViews()
                    parent.addView(ui.text(ui.str(R.string.library_dict_not_found), 15, ui.muted, gravity = Gravity.CENTER),
                        android.widget.LinearLayout.LayoutParams(-1, ui.dp(60)))
                    (input as? EditText)?.let {
                        it.text?.clear()
                        parent.addView(it, android.widget.LinearLayout.LayoutParams(-1, ui.dp(56)))
                    }
                }
                is DictResult.Error -> {
                    parent.removeAllViews()
                    parent.addView(ui.text(result.message, 15, ui.coral, gravity = Gravity.CENTER),
                        android.widget.LinearLayout.LayoutParams(-1, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT))
                }
            }
        }
    }

    private fun showWordResult(entry: DictEntry) = with(ui) {
        val titleBar = horizontal {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(12))
            addView(vertical {
                addView(text(entry.word, 26, ink, true))
                if (entry.phonetic.isNotBlank()) {
                    addView(text(entry.phonetic, 14, muted))
                }
            }, LinearLayout.LayoutParams(0, -2, 1f))
            gap(12)
            addView(softButton("🔊", tint = android.graphics.Color.TRANSPARENT, textColor = forest) { activity.speak(entry.word) })
        }

        val content = vertical {
            setPadding(dp(20), dp(0), dp(20), dp(16))

            entry.meanings.forEach { m ->
                val posLabel = m.partOfSpeech
                val defs = m.definitions.map { it.definition }
                addView(horizontal {
                    addView(text(posLabel, 14, forest, true, Gravity.TOP), LinearLayout.LayoutParams(ui.dp(42), -2))
                    addView(text(defs.joinToString("；"), 14, ink), LinearLayout.LayoutParams(0, -2, 1f))
                })
                gap(8)
            }

            if (entry.example.isNotBlank()) {
                gap(4)
                addDivider()
                gap(12)
                addView(text(entry.example, 14, muted))
                if (entry.exampleTranslation.isNotBlank()) {
                    gap(4)
                    addView(text(entry.exampleTranslation, 13, forest))
                }
            }

            if (entry.synonyms.isNotBlank()) {
                gap(8)
                addDivider()
                gap(12)
                addView(text(str(R.string.library_dict_synonyms), 12, muted, true))
                gap(6)
                val synWords = entry.synonyms.split(",", "，").map { it.trim() }.filter { it.isNotBlank() }.take(8)
                val synsContainer = vertical {}
                var row: LinearLayout? = null
                synWords.forEach { synonym ->
                    if (row == null || row!!.childCount >= 3) {
                        row = horizontal { setPadding(0, 0, 0, dp(2)) }
                        synsContainer.addView(row)
                    }
                    row?.addView(softButton(synonym, mint, ink) { showLookupForWord(synonym) })
                    row?.run { gap(6) }
                }
                addView(synsContainer)
            }

            if (entry.wordRoot.isNotBlank()) {
                gap(8)
                addDivider()
                gap(12)
                addView(text("词根：${entry.wordRoot}", 13, muted))
            }
        }

        val scroll = ScrollView(requireContext()).apply { addView(content) }
        val dialogLayout = vertical {
            addView(titleBar)
            addDivider()
            addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogLayout)
            .setPositiveButton(str(R.string.library_dict_add)) { _, _ ->
                lifecycleScope.launch {
                    viewModel.addWord(entry)
                    Toast.makeText(requireContext(), str(R.string.library_dict_added), Toast.LENGTH_SHORT).show()
                    viewModel.refresh()
                }
            }
            .setNegativeButton(str(R.string.library_detail_close), null)
            .show()
    }

    private fun showLookupForWord(word: String) {
        lifecycleScope.launch {
            when (val result = DictionaryClient.lookup(word, viewModel.getApiKey())) {
                is DictResult.Found -> showWordResult(result.entry)
                is DictResult.NotFound -> Toast.makeText(requireContext(), ui.str(R.string.library_dict_not_found), Toast.LENGTH_SHORT).show()
                is DictResult.Error -> Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
