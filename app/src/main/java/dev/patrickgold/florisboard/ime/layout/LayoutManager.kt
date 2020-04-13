package dev.patrickgold.florisboard.ime.layout

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.patrickgold.florisboard.R

class LayoutManager {

    private val context: Context
    /*private val _characterLayout: LayoutData
        private set*/

    constructor(context: Context) {
        this.context = context
    }

    /*fun loadLayout(name: String): LayoutData {
        val jsonRaw = context.resources.openRawResource(R.raw.kbd_qwerty)
            .bufferedReader().use { it.readText() }
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val layoutAdapter = moshi.adapter(LayoutData::class.java)
        *//*loadedLayout = layoutAdapter.fromJson(jsonRaw) ?: throw Error("Provided JSON layout '$name' is invalid and cannot be parsed!")
        return loadedLayout*//*
    }*/
}