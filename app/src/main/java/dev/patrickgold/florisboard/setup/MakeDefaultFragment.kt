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

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dev.patrickgold.florisboard.databinding.SetupFragmentMakeDefaultBinding
import dev.patrickgold.florisboard.ime.core.FlorisBoard

class MakeDefaultFragment : Fragment(), SetupActivity.EventListener {
    private lateinit var binding: SetupFragmentMakeDefaultBinding
    private var osHandler: Handler? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = SetupFragmentMakeDefaultBinding.inflate(inflater, container, false)
        binding.switchKeyboardButton.setOnClickListener {
            (activity as SetupActivity).imm.showInputMethodPicker()
        }

        return binding.root
    }

    private fun updateState(): Boolean {
        return if (FlorisBoard.checkIfImeIsSelected(requireContext())) {
            (activity as SetupActivity).changePositiveButtonState(true)
            binding.textAfterEnabled.visibility = View.VISIBLE
            true
        } else {
            (activity as SetupActivity).changePositiveButtonState(false)
            binding.textAfterEnabled.visibility = View.INVISIBLE
            false
        }
    }

    override fun onResume() {
        super.onResume()
        updateState()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus && context != null) {
            updateState()
            if (osHandler == null) {
                osHandler = Handler()
            }
            osHandler?.postDelayed({ updateState() }, 250)
            osHandler?.postDelayed({ updateState() }, 500)
        }
    }
}
