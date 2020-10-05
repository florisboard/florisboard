/*
 * Copyright (C) 2020 Patrick Goldinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.setup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.databinding.SetupActivityBinding
import dev.patrickgold.florisboard.ime.core.PrefHelper
import dev.patrickgold.florisboard.settings.SettingsMainActivity

class SetupActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_SHOW_SINGLE_STEP = "EXTRA_SHOW_SINGLE_STEP"
    }

    private lateinit var adapter: ViewPagerAdapter
    private lateinit var binding: SetupActivityBinding
    lateinit var imm: InputMethodManager
    lateinit var prefs: PrefHelper
    private var shouldFinish: Boolean = false
    private var shouldLaunchSettings: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = PrefHelper(this)
        prefs.initDefaultPreferences()
        imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        val mode = when (prefs.advanced.settingsTheme) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            "auto" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
        }
        AppCompatDelegate.setDefaultNightMode(mode)

        super.onCreate(savedInstanceState)
        binding = SetupActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // NOTE: using findViewById() instead of view binding because the binding does not include
        //       a reference to the included layout...
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        adapter = ViewPagerAdapter(this)
        adapter.addFragment(WelcomeFragment(), resources.getString(R.string.setup__welcome__title))
        adapter.addFragment(EnableImeFragment(), resources.getString(R.string.setup__enable_ime__title))
        adapter.addFragment(MakeDefaultFragment(), resources.getString(R.string.setup__make_default__title))
        adapter.addFragment(FinishFragment(), resources.getString(R.string.setup__finish__title))
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.adapter = adapter
        binding.progressBar.max = adapter.itemCount - 1

        binding.prevButton.setOnClickListener {
            loadPage(binding.viewPager.currentItem - 1)
        }
        binding.cancelButton.setOnClickListener {
            finish()
        }
        binding.nextButton.setOnClickListener {
            loadPage(binding.viewPager.currentItem + 1)
        }
        binding.finishButton.setOnClickListener {
            prefs.internal.isImeSetUp = true
            launchSettingsAndSetFinishFlag()
        }
        binding.okButton.setOnClickListener {
            finish()
        }

        val extraShowSingleStep = intent.getIntExtra(EXTRA_SHOW_SINGLE_STEP, -1)
        if (extraShowSingleStep >= 0) {
            shouldLaunchSettings = false
            loadPage(extraShowSingleStep, true)
        } else {
            loadPage(Step.WELCOME)
        }
    }

    override fun onResume() {
        super.onResume()
        if (shouldFinish) {
            if (!isFinishing) {
                finish()
            }
            return
        }
        if (prefs.internal.isImeSetUp && shouldLaunchSettings) {
            launchSettingsAndSetFinishFlag()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        val fragment = adapter.createFragment(binding.viewPager.currentItem)
        if (fragment is EventListener) {
            fragment.onWindowFocusChanged(hasFocus)
        }
    }

    private fun launchSettingsAndSetFinishFlag() {
        Intent(this, SettingsMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(this)
        }
        shouldFinish = true
    }

    private fun loadPage(pageIndex: Int, isSingleStepOnly: Boolean = false) {
        binding.prevButton.isEnabled = pageIndex > 0 && !isSingleStepOnly
        binding.cancelButton.isEnabled = isSingleStepOnly
        binding.progressBar.progress = pageIndex
        binding.progressBar.visibility = if (isSingleStepOnly) { View.INVISIBLE } else { View.VISIBLE }
        val isLast = pageIndex + 1 == adapter.itemCount
        binding.negativeButtonViewFlipper.displayedChild =
            if (isSingleStepOnly) { 1 } else { 0 }
        binding.positiveButtonViewFlipper.displayedChild =
            if (isSingleStepOnly) { 2 } else { if (isLast) { 1 } else { 0 } }
        changePositiveButtonState(false)
        supportActionBar?.title = adapter.getPageTitle(pageIndex)
        binding.viewPager.currentItem = pageIndex
    }

    fun changePositiveButtonState(enabled: Boolean) {
        binding.nextButton.isEnabled = enabled
        binding.finishButton.isEnabled = enabled
        binding.okButton.isEnabled = enabled
    }

    object Step {
        const val WELCOME =         0
        const val ENABLE_IME =      1
        const val MAKE_DEFAULT =    2
        const val FINISH =          3
    }

    interface EventListener {
        fun onWindowFocusChanged(hasFocus: Boolean) {}
    }

    private class ViewPagerAdapter(fa: FragmentActivity) :
        FragmentStateAdapter(fa) {
        private val fragments: MutableList<Fragment> = mutableListOf()
        private val titles: MutableList<String> = mutableListOf()

        override fun getItemCount(): Int {
            return fragments.size
        }

        fun addFragment(fragment: Fragment, title: String) {
            fragments.add(fragment)
            titles.add(title)
        }

        override fun createFragment(position: Int): Fragment {
            return fragments[position]
        }

        fun getPageTitle(position: Int): CharSequence? {
            return titles[position]
        }
    }
}
