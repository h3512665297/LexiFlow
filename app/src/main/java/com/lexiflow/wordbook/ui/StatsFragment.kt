package com.lexiflow.wordbook.ui

import android.graphics.Color
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
import com.lexiflow.wordbook.domain.LearningStats
import kotlinx.coroutines.launch

class StatsFragment : Fragment() {
    private val viewModel get() = (requireActivity() as MainActivity).viewModel
    private lateinit var content: LinearLayout
    private lateinit var ui: UiKit

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        ui = UiKit(requireContext())
        content = ui.vertical()
        return ScrollView(requireContext()).apply { addView(content) }
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dashboard.collect { if (!it.loading) render(it.stats, it.moods) }
            }
        }
        viewModel.refresh()
    }

    private fun render(stats: LearningStats, moods: Map<String, String>) = with(ui) {
        content.removeAllViews()
        content.setPadding(dp(20), dp(24), dp(20), dp(28))
        content.addView(text(str(R.string.stats_title), 13, forest, true))
        content.addView(text(str(R.string.stats_subtitle), 29, ink, true))
        content.gap(24)
        content.addView(card(forest) {
            setPadding(dp(22), dp(24), dp(22), dp(24))
            addView(text(str(R.string.stats_streak_label), 14, Color.WHITE))
            addView(text(str(R.string.stats_streak_days, stats.streak), 38, Color.WHITE, true))
            addView(text(if (stats.streak == 0) str(R.string.stats_streak_empty) else str(R.string.stats_streak_active), 14, mint))
        })
        content.gap(18)
        content.addView(horizontal {
            addView(metric(stats.newToday.toString(), str(R.string.stats_new_today)), LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) })
            addView(metric(stats.reviewedToday.toString(), str(R.string.stats_review_today)), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(6) })
        })
        content.gap(18)
        content.addView(card {
            setPadding(dp(22), dp(22), dp(22), dp(22))
            addView(text(str(R.string.stats_accuracy), 18, ink, true))
            gap(10)
            addView(text("${stats.accuracy}%", 34, forest, true))
            addView(progress(100, stats.accuracy, coral))
            gap(8)
            addView(text(str(R.string.stats_accuracy_desc), 13, muted))
        })
        content.gap(18)
        content.addView(card {
            setPadding(dp(22), dp(22), dp(22), dp(22))
            addView(text("记忆健康度", 18, ink, true))
            val health = stats.memoryHealth
            if (health.learned == 0) {
                gap(12)
                addView(text("完成首次学习后，这里将展示记忆健康分布。", 14, muted))
            } else {
                gap(14)
                val colors = listOf(forest, Color.rgb(33, 150, 243), Color.rgb(255, 152, 0), muted)
                health.tiers.forEach { tier ->
                    addView(horizontal {
                        val color = colors[tier.tier]
                        addView(text(tier.label, 14, color, true))
                        gap(6)
                        addView(text("${tier.count} 词 · ${tier.count * 100 / health.learned}%", 13, muted))
                    })
                    gap(6)
                    addView(progress(health.learned, tier.count, colors[tier.tier]))
                    gap(12)
                }
            }
        })
        content.gap(18)
        content.addView(card(mint) {
            setPadding(dp(20), dp(20), dp(20), dp(20))
            addView(text(str(R.string.stats_weekly_title), 18, ink, true))
            gap(12)
            val map = stats.weeklyActivity.associate { it.dayKey to it.count }
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -6)
            addView(horizontal {
                repeat(7) {
                    val key = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(calendar.time)
                    val count = map[key] ?: 0
                    val emoji = moods[key]
                    val dayCell = vertical {
                        gravity = Gravity.CENTER
                        addView(text(java.text.SimpleDateFormat("E", java.util.Locale.CHINA).format(calendar.time), 12, muted, gravity = Gravity.CENTER))
                        gap(4)
                        addView(text(if (count > 0) count.toString() else "·", 16, if (count > 0) forest else muted, true, Gravity.CENTER))
                        if (emoji != null) {
                            gap(4)
                            addView(text(emoji, 16, gravity = Gravity.CENTER))
                        }
                    }
                    dayCell.setOnClickListener { showMoodPicker(key, emoji) }
                    addView(dayCell, LinearLayout.LayoutParams(0, if (emoji != null) dp(70) else dp(55), 1f))
                    calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
            })
            gap(10)
            val minutes = stats.durationTodayMs / 60_000
            addView(text(str(R.string.stats_duration, minutes.coerceAtLeast(if (stats.completedToday > 0) 1 else 0), stats.weeklyActivity.sumOf { it.count }), 14, muted))
        })
    }

    private fun metric(value: String, label: String) = ui.card {
        setPadding(ui.dp(18), ui.dp(20), ui.dp(18), ui.dp(20))
        gravity = Gravity.CENTER
        addView(ui.text(value, 30, ui.ink, true, Gravity.CENTER))
        addView(ui.text(label, 14, ui.muted, gravity = Gravity.CENTER))
    }

    private fun showMoodPicker(dayKey: String, current: String?) {
        val moodOptions = listOf(
            "😊" to "开心", "😄" to "很棒", "😐" to "一般", "😢" to "难过", "😡" to "生气",
            "❤️" to "喜欢", "🎉" to "兴奋", "🤔" to "思考", "😴" to "疲惫", "💪" to "加油"
        )
        val content = ui.vertical { setPadding(ui.dp(16), ui.dp(16), ui.dp(16), ui.dp(8)) }
        val builder = android.app.AlertDialog.Builder(requireContext())
            .setTitle("选择心情")
            .setView(content)
        if (current != null) {
            builder.setNegativeButton("清除") { _, _ -> viewModel.setMood(dayKey, "") }
        }
        builder.setPositiveButton("关闭", null)
        val dialog = builder.show()
        var row: LinearLayout? = null
        moodOptions.forEachIndexed { index, (emoji, label) ->
            if (index % 5 == 0) {
                row = ui.horizontal {}
                content.addView(row)
            }
            val cell = ui.vertical {
                gravity = Gravity.CENTER
                setPadding(ui.dp(8), ui.dp(8), ui.dp(8), ui.dp(8))
                addView(ui.text(emoji, 32, gravity = Gravity.CENTER))
                addView(ui.text(label, 11, ui.muted, gravity = Gravity.CENTER))
            }
            cell.setOnClickListener {
                viewModel.setMood(dayKey, emoji)
                dialog.dismiss()
            }
            row!!.addView(cell, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
    }
}
