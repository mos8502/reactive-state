package hu.nemi.costate.util

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

fun View.toggleSoftKeyboard() {
    inputMethodManager.toggleSoftInputFromWindow(windowToken, InputMethodManager.SHOW_IMPLICIT, 0)
}

fun View.hideSoftKeyboard() {
    inputMethodManager.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_IMPLICIT_ONLY)
}

private val View.inputMethodManager: InputMethodManager
    get() = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

