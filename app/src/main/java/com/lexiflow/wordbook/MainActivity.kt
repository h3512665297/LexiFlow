package com.lexiflow.wordbook

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.lexiflow.wordbook.R
import com.lexiflow.wordbook.data.WordEntity
import com.lexiflow.wordbook.databinding.ActivityMainBinding
import com.lexiflow.wordbook.ui.HomeFragment
import com.lexiflow.wordbook.ui.AppViewModel
import com.lexiflow.wordbook.ui.LibraryFragment
import com.lexiflow.wordbook.ui.StatsFragment
import com.lexiflow.wordbook.ui.StudyFragment
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var binding: ActivityMainBinding
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var selected = Tab.HOME

    val viewModel: AppViewModel by viewModels {
        AppViewModel.Factory((application as LexiFlowApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        tts = TextToSpeech(this, this)

        binding.navHome.setOnClickListener { navigate(Tab.HOME) }
        binding.navLearn.setOnClickListener { navigate(Tab.STUDY) }
        binding.navStats.setOnClickListener { navigate(Tab.STATS) }
        binding.navLibrary.setOnClickListener { navigate(Tab.LIBRARY) }
        if (savedInstanceState == null) {
            navigate(Tab.HOME)
        } else {
            selected = savedInstanceState.getString(KEY_TAB)
                ?.let { runCatching { Tab.valueOf(it) }.getOrNull() }
                ?: Tab.HOME
            updateNavigation()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_TAB, selected.name)
        super.onSaveInstanceState(outState)
    }

    fun openStudy() = navigate(Tab.STUDY)

    fun speak(word: WordEntity) = speak(word.text)

    fun speak(text: String) {
        val engine = tts
        if (engine == null || !ttsReady) {
            Toast.makeText(this, getString(R.string.tts_unavailable), Toast.LENGTH_SHORT).show()
            return
        }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "word_$text")
    }

    private fun navigate(tab: Tab) {
        selected = tab
        val fragment: Fragment = when (tab) {
            Tab.HOME -> HomeFragment()
            Tab.STUDY -> StudyFragment()
            Tab.STATS -> StatsFragment()
            Tab.LIBRARY -> LibraryFragment()
        }
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.content, fragment)
            .commit()
        updateNavigation()
    }

    private fun updateNavigation() {
        val forest = ContextCompat.getColor(this, R.color.forest)
        val muted = ContextCompat.getColor(this, R.color.muted)
        val mint = ContextCompat.getColor(this, R.color.mint)
        listOf(
            binding.navHome to Tab.HOME,
            binding.navLearn to Tab.STUDY,
            binding.navStats to Tab.STATS,
            binding.navLibrary to Tab.LIBRARY
        )
            .forEach { (button, tab) ->
                val active = tab == selected
                button.setTextColor(if (active) forest else muted)
                button.backgroundTintList = ColorStateList.valueOf(if (active) mint else Color.TRANSPARENT)
            }
    }

    override fun onInit(status: Int) {
        if (isFinishing || isDestroyed) return
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = tts?.setLanguage(Locale.US) != TextToSpeech.LANG_MISSING_DATA
            tts?.setSpeechRate(0.82f)
        } else {
            Toast.makeText(this, getString(R.string.tts_not_found), Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        tts?.shutdown()
        tts = null
        ttsReady = false
        super.onDestroy()
    }

    private enum class Tab { HOME, STUDY, STATS, LIBRARY }

    companion object {
        private const val KEY_TAB = "selected_tab"
    }
}
