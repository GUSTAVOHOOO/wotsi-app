package br.edu.utfpr.cc.setac.wotsiapp.extensions

import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.*
import kotlin.properties.Delegates

var clipToPadding by Delegates.notNull<Boolean>()

private data class InitialPadding(val left: Int, val top: Int, val right: Int, val bottom: Int)

private fun View.recordInitialPadding(): InitialPadding =
    InitialPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)

fun View.applyStatusBarPadding() {
    val initial = recordInitialPadding()
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        v.setPadding(initial.left, initial.top + top, initial.right, initial.bottom)
        insets
    }
    // pede um pass de insets imediatamente
    requestApplyInsetsWhenAttached()
}

fun View.applySideSystemBarsPadding() {
    val initial = recordInitialPadding()
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        v.setPadding(initial.left + sys.left, initial.top, initial.right + sys.right, initial.bottom)
        insets
    }
    requestApplyInsetsWhenAttached()
}

/** Usa o MAIOR entre IME (teclado) e navigation bar para não cobrir inputs/botões. */
fun View.applyBottomPaddingForNavOrIme() {
    clipToPadding = false // útil para RecyclerView/ScrollView exibir o último item acima do padding
    val initial = recordInitialPadding()
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        val bottom = maxOf(ime, nav)
        v.setPadding(initial.left, initial.top, initial.right, initial.bottom + bottom)
        insets
    }
    requestApplyInsetsWhenAttached()
}

fun View.applyNavigationBarPadding() {
    val initial = recordInitialPadding()
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        v.setPadding(initial.left, initial.top, initial.right, initial.bottom + nav)
        insets
    }
    requestApplyInsetsWhenAttached()
}

fun View.applyNavigationBarMargin() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        v.updateLayoutParams<MarginLayoutParams> {
            bottomMargin = nav + bottomMargin
        }
        insets
    }
    requestApplyInsetsWhenAttached()
}

private fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        requestApplyInsets()
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.removeOnAttachStateChangeListener(this)
                v.requestApplyInsets()
            }
            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
}


fun View.enableIfAllTrue(booleanList: Array<Boolean>) {
    this.isEnabled = booleanList.all { it }
}