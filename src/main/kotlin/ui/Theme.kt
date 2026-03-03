package org.education.ui

import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.border.Border

internal object Theme {
    val buttonsPanelColor = Color(244, 231, 207)
    val savedNotesPanelColor = Color(244, 231, 207)
    val savedNotesListColor = Color(244, 231, 207)
    val textAreaColor = Color(255, 247, 232)
    val selectBackground: Color = Color.WHITE
    val selectForeground: Color = Color.BLACK

    val savedNotesLabelFont = Font("Arial", Font.PLAIN, 18)
    val saveButtonFont = Font("Arial", Font.PLAIN, 18)
    val loadButtonFont = Font("Arial", Font.PLAIN, 18)
    val removeButtonFont = Font("Arial", Font.PLAIN, 18)
    val savedNotesListFont = Font("Arial", Font.PLAIN, 14)
    val noteHeaderFont = Font("Arial", Font.BOLD, 20)

    const val FRAME_WIDTH = 1280
    const val FRAME_HEIGHT = 820
    const val MENU_ICON_HEIGHT = 20
    const val MENU_ICON_WIDTH = 20
    val leftPanelSize = Dimension(300, 800)
    val saveButtonSize = Dimension(90, 50)
    val loadButtonSize = Dimension(90, 50)
    val removeButtonSize = Dimension(90, 50)
    val buttonsPadding: Border = BorderFactory.createEmptyBorder(40, 10, 20, 10)
    val statusPadding: Border = BorderFactory.createEmptyBorder(0, 10, 0, 0)
}