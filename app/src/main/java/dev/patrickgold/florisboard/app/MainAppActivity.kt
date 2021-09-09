/*
 * Copyright (C) 2021 Patrick Goldinger
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

package dev.patrickgold.florisboard.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.patrickgold.florisboard.app.ui.settings.AboutScreen
import dev.patrickgold.florisboard.app.ui.theme.FlorisAppTheme

class MainAppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlorisAppTheme {
                Surface(color = MaterialTheme.colors.background) {
                    AppContent()
                }
            }
        }
    }

    @Composable
    fun AppContent() {
        val navController = rememberNavController()
        Column {
            TopAppBar(
                title = { Text(text = "FlorisBoard") },
                backgroundColor = Color.Transparent
            )
            NavHost(navController = navController, startDestination = "home") {
                composable("home") { AboutScreen() }
            }
        }
    }
}
