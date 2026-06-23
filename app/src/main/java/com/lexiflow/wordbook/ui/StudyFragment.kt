package com.lexiflow.wordbook.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.lexiflow.wordbook.LexiFlowApplication
import com.lexiflow.wordbook.MainActivity
import com.lexiflow.wordbook.R
import com.lexiflow.wordbook.domain.RecallRating
import com.lexiflow.wordbook.domain.SpellingTolerance
import com.lexiflow.wordbook.domain.StudyMode
import com.lexiflow.wordbook.domain.StudyState
import com.lexiflow.wordbook.domain.TrainingCard
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudyFragment : Fragment() {
    private val activity get() = requireActivity() as MainActivity
    private val appViewModel get() = activity.viewModel
    private val viewModel: StudyViewModel by viewModels {
        StudyViewModel.Factory((requireActivity().application as LexiFlowApplication).repository)
    }
    private lateinit var host: FrameLayout
    private lateinit var modeBar: LinearLayout
    private lateinit var ui: UiKit

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        ui = UiKit(requireContext())
        return ui.vertical {
            setPadding(ui.dp(18), ui.dp(18), ui.dp(18), ui.dp(16))
            addView(ui.text(ui.str(R.string.study_title), 13, ui.forest, true))
            addView(ui.text(ui.str(R.string.study_subtitle), 24, ui.ink, true))
            ui.run { gap(10) }
            modeBar = ui.horizontal {}
            addView(modeBar)
            ui.run { gap(12) }
            host = FrameLayout(requireContext())
            addView(host, LinearLayout.LayoutParams(-1, 0, 1f))
        }
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { renderStudy(it) } }
                launch { viewModel.mode.collect { renderModes(it) } }
            }
        }
    }

    private fun renderModes(selected: StudyMode) = with(ui) {
        modeBar.removeAllViews()
        listOf(
            StudyMode.FLASHCARD to str(R.string.study_tab_flashcard),
            StudyMode.LISTENING to str(R.string.study_tab_listening),
            StudyMode.SPELLING to str(R.string.study_tab_spelling),
            StudyMode.CLOZE to str(R.string.study_tab_cloze)
        ).forEach { (mode, label) ->
            modeBar.addView(
                softButton(label, if (mode == selected) forest else mint,
                    if (mode == selected) android.graphics.Color.WHITE else ink) { viewModel.setMode(mode) },
                LinearLayout.LayoutParams(0, dp(44), 1f)
            )
        }
    }

    private fun renderStudy(state: StudyState) {
        host.removeAllViews()
        when (state) {
            StudyState.Loading -> host.addView(ui.text(ui.str(R.string.study_loading), 16, ui.muted, gravity = Gravity.CENTER), FrameLayout.LayoutParams(-1, -1))
            is StudyState.Ready -> {
                host.addView(trainingView(state.card, state.answering), FrameLayout.LayoutParams(-1, -1))
                val settings = (requireActivity().application as LexiFlowApplication).repository.settings()
                if (settings.autoPlay && state.card.mode != StudyMode.SPELLING && state.card.mode != StudyMode.CLOZE) activity.speak(state.card.studyCard.word)
            }
            is StudyState.Completed -> host.addView(completedView(state.nextReviewAt), FrameLayout.LayoutParams(-1, -1))
        }
    }

    private fun trainingView(card: TrainingCard, answering: Boolean): View = with(ui) {
        val word = card.studyCard.word
        if (card.mode == StudyMode.FLASHCARD) {
            return flashcardView(card)
        }
        card(paper) {
            setPadding(dp(20), dp(22), dp(20), dp(20))
            addView(text(modeTitle(card.mode), 13, coral, true, Gravity.CENTER))
            gap(18)
            when (card.mode) {
                StudyMode.FLASHCARD -> {} // handled by flashcardView
                StudyMode.LISTENING -> {
                    addView(text(str(R.string.study_listen_hint), 19, ink, true, Gravity.CENTER))
                    gap(16)
                    addView(primaryButton(str(R.string.study_listen_again)) { activity.speak(word) }, LinearLayout.LayoutParams(-1, dp(54)))
                    gap(14)
                    addInput(card, str(R.string.study_listen_input_hint))
                }
                StudyMode.SPELLING -> {
                    addView(text(word.meaning, 20, ink, true, Gravity.CENTER))
                    gap(12)
                    addView(text(word.phonetic, 15, muted, gravity = Gravity.CENTER).apply {
                        setOnClickListener { activity.speak(word) }
                    })
                    gap(18)
                    addInput(card, str(R.string.study_spell_input_hint))
                }
                StudyMode.CLOZE -> {
                    if (word.example.isNotBlank() && word.example.contains(word.text, ignoreCase = true)) {
                        val blank = word.example.replace(word.text, "______", ignoreCase = true)
                        addView(text(blank, 20, ink, true, Gravity.CENTER).apply {
                            setOnClickListener { activity.speak(word) }
                        })
                        gap(10)
                        addView(text(word.exampleTranslation, 14, muted, gravity = Gravity.CENTER))
                    } else {
                        addView(text(str(R.string.study_cloze_hint), 17, muted, true, Gravity.CENTER))
                        gap(10)
                        addView(text(word.meaning, 20, ink, true, Gravity.CENTER).apply {
                            setOnClickListener { activity.speak(word) }
                        })
                    }
                    gap(18)
                    addInput(card, str(R.string.study_cloze_input_hint))
                }
            }
            if (answering) {
                gap(10)
                addView(text(str(R.string.study_saving), 13, muted, gravity = Gravity.CENTER))
            }
        }
    }

    private fun flashcardView(card: TrainingCard): View = with(ui) {
        val word = card.studyCard.word
        var stage = 0
        var exampleReady = word.example.isNotBlank()
        var currentExample = word.example
        var currentExampleTrans = word.exampleTranslation

        lateinit var hintText: android.widget.TextView
        lateinit var exampleText: android.widget.TextView
        lateinit var exampleTrans: android.widget.TextView
        lateinit var meaningText: android.widget.TextView
        lateinit var ratingRow: LinearLayout

        val cardView = card(paper) {
            setPadding(dp(20), dp(22), dp(20), dp(20))
            addView(text(modeTitle(StudyMode.FLASHCARD), 13, coral, true, Gravity.CENTER))
            gap(18)
            addView(text(word.text, 34, ink, true, Gravity.CENTER).apply {
                setOnClickListener { activity.speak(word) }
            })
            addView(text(word.phonetic, 15, muted, gravity = Gravity.CENTER).apply {
                setOnClickListener { activity.speak(word) }
            })
            gap(20)

            hintText = text(str(R.string.study_tap_example), 14, muted, gravity = Gravity.CENTER)
            addView(hintText)

            exampleText = text(
                if (exampleReady) "${currentExample}" else "正在生成例句…",
                17, ink, false, Gravity.CENTER
            ).apply { visibility = View.GONE }
            addView(exampleText)
            gap(8)
            exampleTrans = text(currentExampleTrans, 14, muted, gravity = Gravity.CENTER).apply { visibility = View.GONE }
            addView(exampleTrans)
            gap(12)

            meaningText = text(word.meaning, 20, ink, true, Gravity.CENTER).apply { visibility = View.GONE }
            addView(meaningText)
            gap(24)

            ratingRow = horizontal {
                visibility = View.GONE
                gravity = Gravity.CENTER
                listOf(
                    str(R.string.study_rating_again) to RecallRating.AGAIN,
                    str(R.string.study_rating_hard) to RecallRating.HARD,
                    str(R.string.study_rating_good) to RecallRating.GOOD,
                    str(R.string.study_rating_easy) to RecallRating.EASY
                ).forEach { (label, rating) ->
                    addView(softButton(label) {
                        viewModel.answer(rating)
                        appViewModel.refresh()
                    }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(54)).apply { marginEnd = dp(8) })
                }
            }
            addView(ratingRow)
        }

        cardView.setOnClickListener {
            when (stage) {
                0 -> {
                    hintText.visibility = View.GONE
                    exampleText.visibility = View.VISIBLE
                    exampleTrans.visibility = View.VISIBLE
                    hintText.text = str(R.string.study_tap_meaning)
                    hintText.visibility = View.VISIBLE
                    stage = 1
                }
                1 -> {
                    hintText.visibility = View.GONE
                    exampleText.visibility = View.GONE
                    exampleTrans.visibility = View.GONE
                    meaningText.visibility = View.VISIBLE
                    ratingRow.visibility = View.VISIBLE
                    stage = 2
                }
            }
        }

        if (!exampleReady) {
            val repo = (requireActivity().application as LexiFlowApplication).repository
            lifecycleScope.launch {
                val key = appViewModel.getApiKey()
                val result = com.lexiflow.wordbook.data.DictionaryClient.generateExample(word.text, word.meaning, key)
                if (result != null && result.example.isNotBlank()) {
                    currentExample = result.example
                    currentExampleTrans = result.translation
                    exampleReady = true
                    exampleText.post {
                        exampleText.text = "${currentExample}"
                        exampleTrans.text = currentExampleTrans
                    }
                    runCatching { repo.updateExample(word.id, result.example, result.translation) }
                } else {
                    exampleText.post { exampleText.text = "轻触卡片查看释义" }
                }
            }
        }

        cardView
    }

    private fun LinearLayout.addInput(card: TrainingCard, hintText: String) = with(ui) {
        val input = EditText(requireContext()).apply {
            hint = hintText
            gravity = Gravity.CENTER
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_DONE
            contentDescription = hintText
        }
        addView(input, LinearLayout.LayoutParams(-1, dp(56)))
        gap(10)
        val check = { showFeedback(SpellingTolerance.matches(input.text.toString(), card.studyCard.word.text), card) }
        input.setOnEditorActionListener { _, _, _ -> check(); true }
        addView(primaryButton(str(R.string.study_check_answer)) { check() }, LinearLayout.LayoutParams(-1, dp(52)))
    }

    private fun LinearLayout.addReveal(card: TrainingCard) = with(ui) {
        addView(primaryButton(str(R.string.study_show_meaning)) { showAnswerDialog(card, null) }, LinearLayout.LayoutParams(-1, dp(54)))
    }

    private fun showFeedback(correct: Boolean, card: TrainingCard) {
        showAnswerDialog(card, correct)
    }

    private fun showAnswerDialog(card: TrainingCard, correct: Boolean?) {
        val word = card.studyCard.word
        val title = when (correct) {
            true -> ui.str(R.string.study_correct)
            false -> ui.str(R.string.study_wrong)
            null -> word.text
        }
        val actions = arrayOf(ui.str(R.string.study_rating_again), ui.str(R.string.study_rating_hard), ui.str(R.string.study_rating_good), ui.str(R.string.study_rating_easy))
        android.app.AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(buildString {
                append("${word.text} ${word.phonetic}\n${word.meaning}")
                if (word.example.isNotBlank()) append("\n\n${word.example}\n${word.exampleTranslation}")
            })
            .setItems(actions) { _, which ->
                val rating = listOf(RecallRating.AGAIN, RecallRating.HARD, RecallRating.GOOD, RecallRating.EASY)[which]
                viewModel.answer(if (correct == false && rating.value > 2) RecallRating.HARD else rating)
                appViewModel.refresh()
            }
            .show()
    }

    private fun completedView(nextReviewAt: Long?): View = with(ui) {
        card(mint) {
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(28), dp(28), dp(28))
            addView(text(str(R.string.study_completed), 28, ink, true, Gravity.CENTER))
            gap(10)
            addView(text(str(R.string.study_completed_desc), 15, muted, gravity = Gravity.CENTER))
            nextReviewAt?.let {
                gap(12)
                addView(text(str(R.string.study_next_review, SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINA).format(Date(it))), 14, forest, true, Gravity.CENTER))
            }
            gap(20)
            addView(primaryButton(str(R.string.study_free_review)) { viewModel.startFreeReview() }, LinearLayout.LayoutParams(-1, dp(52)))
            gap(12)
            addView(primaryButton(str(R.string.study_extra_learning)) { viewModel.startExtraLearning() }, LinearLayout.LayoutParams(-1, dp(52)))
        }
    }

    private fun modeTitle(mode: StudyMode) = when (mode) {
        StudyMode.FLASHCARD -> ui.str(R.string.study_mode_flashcard)
        StudyMode.LISTENING -> ui.str(R.string.study_mode_listening)
        StudyMode.SPELLING -> ui.str(R.string.study_mode_spelling)
        StudyMode.CLOZE -> ui.str(R.string.study_mode_cloze)
    }
}
