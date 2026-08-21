/*
 * This file is part of Seeneva Android Reader
 * Copyright (C) 2021 Sergei Solodovnikov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package app.seeneva.reader.logic.entity.configuration

import android.view.Window
import android.view.WindowManager
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Configuration for comic book viewer
 * @param keepScreenOn should keep screen ON
 * @param brightness viewer screen brightness
 * @param tts is text-to-speech enabled
 * @param assistedBubbleZoomScale how much assisted reading balloons/bubbles should be zoomed in.
 * Dimensionless scalar which is added on top of the page minimal scale.
 * New field with a default value, so old serialized [ViewerConfig] JSON stays fully compatible
 */
@Serializable
data class ViewerConfig(
    @SerialName("keep_screen_on")
    val keepScreenOn: Boolean = true,
    @SerialName("brightness")
    val brightness: Float = SYSTEM_BRIGHTNESS,
    @SerialName("tts")
    val tts: Boolean = true,
    @SerialName("assisted_bubble_zoom_scale")
    val assistedBubbleZoomScale: Float = DEFAULT_ASSISTED_BUBBLE_ZOOM_SCALE
) {
    val systemBrightness
        get() = brightness == SYSTEM_BRIGHTNESS

    companion object {
        const val SYSTEM_BRIGHTNESS = -1.0f

        /**
         * Default assisted reading balloon/bubble zoom scale.
         * Smaller than the legacy hardcoded value (previously 0.5f) to avoid over zooming small bubbles
         */
        const val DEFAULT_ASSISTED_BUBBLE_ZOOM_SCALE = 0.30f

        /**
         * Minimal allowed [ViewerConfig.assistedBubbleZoomScale] value
         */
        const val ASSISTED_BUBBLE_ZOOM_SCALE_MIN = 0.1f

        /**
         * Maximum allowed [ViewerConfig.assistedBubbleZoomScale] value
         */
        const val ASSISTED_BUBBLE_ZOOM_SCALE_MAX = 1.0f

        /**
         * Step which should be used by any UI control which changes [ViewerConfig.assistedBubbleZoomScale]
         */
        const val ASSISTED_BUBBLE_ZOOM_SCALE_STEP = 0.05f
    }
}

/**
 * Apply viewer settings to Android Window
 */
fun ViewerConfig.applyToWindow(window: Window) {
    if (keepScreenOn) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    window.attributes = window.attributes.also { attrs ->
        attrs.screenBrightness = if (systemBrightness) {
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        } else {
            brightness.coerceIn(.0f, 1.0f)
        }
    }
}