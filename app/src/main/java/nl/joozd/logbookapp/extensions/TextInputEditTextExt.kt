package nl.joozd.logbookapp.extensions

import android.view.View
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

val TextInputEditText.textInputLayout: TextInputLayout?
    get() {
        var p: View = this
        while (p !is TextInputLayout && p.parent is View)
            p = p.parent as View
        return p.takeIf{ it is TextInputLayout } as TextInputLayout?
    }