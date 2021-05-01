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
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dev.patrickgold.florisboard.databinding.SetupFragmentEnableImeBinding
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.util.checkIfImeIsEnabled

class EnableImeFragment : Fragment() {
    private lateinit var binding: SetupFragmentEnableImeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SetupFragmentEnableImeBinding.inflate(inflater, container, false)
        binding.languageAndInputButton.setOnClickListener {
            val intent = Intent()
            intent.action = Settings.ACTION_INPUT_METHOD_SETTINGS
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            startActivity(intent)
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (checkIfImeIsEnabled(requireContext())) {
            (activity as SetupActivity).changePositiveButtonState(true)
            binding.textAfterEnabled.visibility = View.VISIBLE
        } else {
            (activity as SetupActivity).changePositiveButtonState(false)
            binding.textAfterEnabled.visibility = View.INVISIBLE
        }
    }
}
