/*
 * This file is part of Seeneva Android Reader
 * Copyright (C) 2026
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

package app.seeneva.reader.logic.comic

import app.seeneva.reader.logic.entity.Direction
import kotlin.test.Test
import kotlin.test.assertEquals

class PageObjectHelperTest {
    @Test
    fun `staggered panels in the same row follow left to right reading order`() {
        val panels = listOf(
            "right" to PanelBounds(left = 510f, top = 20f, right = 990f, bottom = 420f),
            "left" to PanelBounds(left = 10f, top = 120f, right = 490f, bottom = 520f)
        )

        assertEquals(
            listOf("left", "right"),
            orderPanelsByRows(panels, Direction.LTR)
        )
    }

    @Test
    fun `staggered panels in the same row follow right to left reading order`() {
        val panels = listOf(
            "left" to PanelBounds(left = 10f, top = 20f, right = 490f, bottom = 420f),
            "right" to PanelBounds(left = 510f, top = 120f, right = 990f, bottom = 520f)
        )

        assertEquals(
            listOf("right", "left"),
            orderPanelsByRows(panels, Direction.RTL)
        )
    }

    @Test
    fun `panels with only a small vertical overlap remain in separate rows`() {
        val panels = listOf(
            "bottom" to PanelBounds(left = 10f, top = 390f, right = 990f, bottom = 790f),
            "top" to PanelBounds(left = 10f, top = 10f, right = 990f, bottom = 410f)
        )

        assertEquals(
            listOf("top", "bottom"),
            orderPanelsByRows(panels, Direction.RTL)
        )
    }

    @Test
    fun `multiple panel rows are ordered top to bottom then by reading direction`() {
        val panels = listOf(
            "bottom-left" to PanelBounds(left = 10f, top = 510f, right = 490f, bottom = 910f),
            "top-right" to PanelBounds(left = 510f, top = 10f, right = 990f, bottom = 410f),
            "bottom-right" to PanelBounds(left = 510f, top = 530f, right = 990f, bottom = 930f),
            "top-left" to PanelBounds(left = 10f, top = 30f, right = 490f, bottom = 430f)
        )

        assertEquals(
            listOf("top-left", "top-right", "bottom-left", "bottom-right"),
            orderPanelsByRows(panels, Direction.LTR)
        )
    }
}
