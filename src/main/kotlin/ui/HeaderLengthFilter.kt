package org.education.ui

import java.awt.Toolkit
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DocumentFilter

const val MAX_CHARACTERS = 60

/**
 * REVIEW:
 *
 * 1. MAX_CHARACTERS defined in UI layer.
 *    Explanation:
 *      This constant is used:
 *        - In service layer validation
 *        - In JPA entity (@Length)
 *        - In UI filter
 *
 *      Defining it in UI creates wrong dependency direction.
 *      Domain rules should not originate from presentation layer.
 *
 *
 * 2. Possible NPE risk for `str`.
 *    Explanation:
 *      DocumentFilter API allows `str` to be null.
 *      Current implementation calls str.length directly.
 *
 *      Defensive check should be added:
 *
 *          if (str == null) return
 *
 *
 * 3. Duplicate logic between insertString and replace.
 *    Explanation:
 *      Length calculation logic is duplicated.
 *      This increases maintenance cost.
 *
 *      Extract common private function:
 *
 *          private fun exceedsLimit(...)
 *
 *
 * 4. No early-return style.
 *    Explanation:
 *      Current pattern:
 *          if (condition) { super... } else beep()
 *
 *      Cleaner:
 *
 *          if (exceedsLimit) {
 *              Toolkit.getDefaultToolkit().beep()
 *              return
 *          }
 *          super.insertString(...)
 *
 *      Reduces nesting and improves clarity.
 *
 *
 * 5. Beep as hard-coded UI behavior.
 *    Explanation:
 *      Toolkit.getDefaultToolkit().beep() is a side effect.
 *      While acceptable in UI layer, this makes the filter
 *      non-reusable and tightly coupled to specific feedback.
 *
 *
 * 6. Visibility.
 *    Explanation:
 *      If this class is used only inside UI package,
 *      it should be marked internal.
 */
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