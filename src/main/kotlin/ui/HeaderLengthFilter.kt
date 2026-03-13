package org.education.ui

import org.education.model.NOTE_TITLE_MAX_LENGTH
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DocumentFilter

internal class HeaderLengthFilter(
    private val onLimitExceeded: () -> Unit = {}
) : DocumentFilter() {
    @Throws(BadLocationException::class)
    override fun insertString(fb: FilterBypass, offs: Int, str: String?, a: AttributeSet?) {
        if (str == null) return
        if (exceedsLimit(fb, str)) {
            onLimitExceeded.invoke()
            return
        }
        super.insertString(fb, offs, str, a)
    }

    @Throws(BadLocationException::class)
    override fun replace(
        fb: FilterBypass,
        offs: Int,
        length: Int,
        str: String?,
        a: AttributeSet?
    ) {
        if (str == null) return
        if (exceedsLimit(fb, str)) {
            onLimitExceeded()
            return
        }
        super.replace(fb, offs, length, str, a)
    }

    private fun exceedsLimit(fb: FilterBypass, str: String) =
        (fb.document.length + str.length) > NOTE_TITLE_MAX_LENGTH
}