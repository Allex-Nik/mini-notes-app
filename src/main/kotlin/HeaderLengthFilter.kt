package org.education

import java.awt.Toolkit
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DocumentFilter

const val MAX_CHARACTERS = 60

class HeaderLengthFilter : DocumentFilter() {
    @Throws(BadLocationException::class)
    override fun insertString(fb: FilterBypass, offs: Int, str: String, a: AttributeSet?) {
        if ((fb.document.length + str.length) <= MAX_CHARACTERS) {
            super.insertString(fb, offs, str, a)
        } else Toolkit.getDefaultToolkit().beep()
    }

    @Throws(BadLocationException::class)
    override fun replace(
        fb: FilterBypass, offs: Int,
        length: Int,
        str: String, a: AttributeSet?
    ) {
        if ((fb.document.length + str.length - length) <= MAX_CHARACTERS) {
            super.replace(fb, offs, length, str, a)
        } else Toolkit.getDefaultToolkit().beep()
    }
}