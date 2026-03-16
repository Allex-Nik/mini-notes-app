package org.education.ui

import java.awt.Taskbar
import javax.imageio.ImageIO

internal object Icons {
    const val APP_ICON = "/app_icon.png"
    const val NEW_NOTE_ICON = "/new_note.jpeg"
    const val SAVE_NOTE_ICON = "/save_note.jpeg"
    const val EXIT_ICON = "/exit.jpeg"

    fun installMacDockIcon() {
        runCatching {
            if (!Taskbar.isTaskbarSupported()) return
            val taskbar = Taskbar.getTaskbar()
            if (!taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) return

            val stream = Icons::class.java.getResourceAsStream("/app_icon.png") ?: return
            stream.use {
                val image = ImageIO.read(it) ?: return
                taskbar.iconImage = image
            }
        }
    }
}