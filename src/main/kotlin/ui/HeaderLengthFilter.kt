package org.education.ui

import org.education.model.NOTE_TITLE_MAX_LENGTH
import java.awt.Toolkit
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DocumentFilter

internal class HeaderLengthFilter : DocumentFilter() {
    @Throws(BadLocationException::class)
    override fun insertString(fb: FilterBypass, offs: Int, str: String, a: AttributeSet?) {
        if ((fb.document.length + str.length) <= NOTE_TITLE_MAX_LENGTH) {
            super.insertString(fb, offs, str, a)
        } else Toolkit.getDefaultToolkit().beep()
    }

    @Throws(BadLocationException::class)
    override fun replace(
        fb: FilterBypass, offs: Int,
        length: Int,
        str: String, a: AttributeSet?
    ) {
        if ((fb.document.length + str.length - length) <= NOTE_TITLE_MAX_LENGTH) {
            super.replace(fb, offs, length, str, a)
        } else Toolkit.getDefaultToolkit().beep()
    }
}