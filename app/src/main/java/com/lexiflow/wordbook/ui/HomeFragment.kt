package com.lexiflow.wordbook.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.lexiflow.wordbook.MainActivity
import com.lexiflow.wordbook.R
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeFragment : Fragment() {
    private val activity get() = requireActivity() as MainActivity
    private val viewModel get() = activity.viewModel
    private lateinit var content: LinearLayout
    private lateinit var ui: UiKit

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        ui = UiKit(requireContext())
        content = ui.vertical()
        return ScrollView(requireContext()).apply {
            isFillViewport = true
            addView(content)
        }
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dashboard.collect { render(it) }
            }
        }
        viewModel.refresh()
    }

    private fun render(state: DashboardState) = with(ui) {
        content.removeAllViews()
        content.setPadding(dp(20), dp(24), dp(20), dp(28))
        if (state.loading) {
            content.addView(text(str(R.string.home_loading), 16, muted))
            return
        }
        if (state.error != null) {
            content.addView(text(str(R.string.home_error_title), 18, coral, true))
            content.gap(8)
            content.addView(text(state.error, 14, muted))
            content.gap(16)
            content.addView(primaryButton(str(R.string.home_retry)) { viewModel.refresh() }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54)))
            return
        }
        val stats = state.stats
        val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..10 -> "早上好"
            in 11..13 -> "中午好"
            in 14..18 -> "下午好"
            else -> "晚上好"
        }
        content.addView(text(str(R.string.home_app_title) + " · " + (state.selectedBook?.name ?: "LEXIFLOW"), 13, forest, true))
        content.addView(text(str(R.string.home_greeting, greeting), 30, ink, true))
        content.addView(text(str(R.string.home_subtitle), 15, muted))
        content.gap(24)
        content.addView(card(mint) {
            setPadding(dp(22), dp(20), dp(22), dp(20))
            addView(horizontal {
                addView(vertical {
                    addView(text(str(R.string.home_today_plan), 14, forest, true))
                    addView(text("${stats.completedToday} / ${stats.dailyGoal}", 32, ink, true))
                    addView(text(str(R.string.home_today_new, stats.newToday) + " · " + str(R.string.home_today_review, stats.reviewedToday), 13, muted))
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(text(str(R.string.home_streak, stats.streak), 16, coral, true, Gravity.CENTER))
            })
            gap(15)
            addView(progress(stats.dailyGoal, stats.completedToday))
            gap(16)
            addView(primaryButton(if (stats.completedToday == 0) str(R.string.home_start_learning) else str(R.string.home_continue_learning)) {
                activity.openStudy()
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54)))
        })
        content.gap(22)
        val word = state.featuredWord
        if (word != null) {
            content.addView(horizontal {
                addView(text(str(R.string.home_word_of_day), 19, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
                addView(softButton("🎲", tint = android.graphics.Color.TRANSPARENT, textColor = forest) { viewModel.randomizeFeatured() })
            })
            content.gap(10)
            val wordCard = card {
                setPadding(dp(22), dp(22), dp(22), dp(22))
                addView(horizontal {
                    addView(vertical {
                        addView(text(word.text, 25, ink, true))
                        addView(text(word.phonetic, 14, muted))
                    }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                    addView(softButton(str(R.string.home_speak), action = { activity.speak(word) }))
                })
            gap(14)
            addView(text(word.meaning, 16))
            }
            wordCard.setOnClickListener {
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle(word.text)
                    .setMessage(buildString {
                        append("${word.phonetic}\n\n${word.meaning}")
                        if (word.example.isNotBlank()) append("\n\n${word.example}\n${word.exampleTranslation}")
                        if (word.wordRoot.isNotBlank()) append("\n\n词根：${word.wordRoot}")
                        if (word.synonyms.isNotBlank()) append("\n近义词：${word.synonyms}")
                    })
                    .setPositiveButton(str(R.string.library_detail_close), null)
                    .setNeutralButton(str(R.string.library_detail_speak)) { _, _ -> activity.speak(word) }
                    .show()
            }
            content.addView(wordCard)
        }
        content.gap(22)
        content.addView(text(str(R.string.home_library_title), 19, ink, true))
        content.gap(10)
        content.addView(card {
            setPadding(dp(18), dp(18), dp(18), dp(18))
            addView(horizontal {
                listOf(
                    stats.learned.toString() to str(R.string.home_learned),
                    stats.mastered.toString() to str(R.string.home_mastered),
                    stats.total.toString() to str(R.string.home_total)
                ).forEach { (value, label) ->
                    addView(vertical {
                        gravity = Gravity.CENTER
                        addView(text(value, 25, ink, true, Gravity.CENTER))
                        addView(text(label, 13, muted, gravity = Gravity.CENTER))
                    }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                }
            })
        })
    }
}
