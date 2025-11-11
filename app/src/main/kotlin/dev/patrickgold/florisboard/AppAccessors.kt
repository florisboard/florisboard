package dev.patrickgold.florisboard

import android.content.Context
import dev.patrickgold.florisboard.app.layoutbuilder.LayoutPackRepository

val Context.layoutPackRepository: LayoutPackRepository
    get() = (applicationContext as FlorisApplication).layoutPackRepository
