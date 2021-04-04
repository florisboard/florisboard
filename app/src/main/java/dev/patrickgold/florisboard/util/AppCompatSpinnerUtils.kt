package dev.patrickgold.florisboard.util

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatSpinner

fun AppCompatSpinner.initItems(labels: List<String>, keys: List<String>, defaultSelectedKey: String) {
    this.initItems(labels, keys.indexOf(defaultSelectedKey).coerceAtLeast(0))
}

fun AppCompatSpinner.initItems(labels: List<String>, initSelection: Int) {
    this.initItems(labels)
    this.setSelection(initSelection)
}

fun AppCompatSpinner.initItems(labels: List<String>) {
    val adapter = ArrayAdapter(
        this.context,
        android.R.layout.simple_spinner_dropdown_item,
        labels
    )
    this.adapter = adapter
}

fun AppCompatSpinner.setOnSelectedListener(callback: (Int) -> Unit) {
    this.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            callback(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            // Stub
        }
    }
}
