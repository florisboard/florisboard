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

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import dev.patrickgold.florisboard.databinding.SetupFragmentMakeDefaultBinding
import dev.patrickgold.florisboard.util.checkIfImeIsSelected

class MakeDefaultFragment : Fragment(), SetupActivity.EventListener {
    private lateinit var binding: SetupFragmentMakeDefaultBinding

    private var isChangeReceiverRegistered = false
    private val imeChangedReceiver = InputMethodChangedReceiver {
        unregisterImeChangeReceiver()
        updateState()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SetupFragmentMakeDefaultBinding.inflate(inflater, container, false)
        binding.switchKeyboardButton.setOnClickListener {
            registerImeChangeReceiver()
            (activity as SetupActivity).imm.showInputMethodPicker()
        }

        return binding.root
    }

    private fun updateState() {
        val isImeSelected = checkIfImeIsSelected(requireContext())

        (activity as SetupActivity).changePositiveButtonState(isImeSelected)
        binding.textAfterEnabled.isVisible = isImeSelected
    }

    override fun onResume() {
        super.onResume()
        updateState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unregisterImeChangeReceiver()
    }

    private fun unregisterImeChangeReceiver() {
        if (isChangeReceiverRegistered) {
            isChangeReceiverRegistered = false
            activity?.unregisterReceiver(imeChangedReceiver)
        }
    }

    private fun registerImeChangeReceiver() {
        if (!isChangeReceiverRegistered) {
            isChangeReceiverRegistered = true
            activity?.registerReceiver(
                imeChangedReceiver,
                IntentFilter(Intent.ACTION_INPUT_METHOD_CHANGED)
            )
        }
    }
}
