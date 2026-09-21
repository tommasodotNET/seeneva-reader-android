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
import kotlin.test.assertNull

class PageObjectHelperTest {
    @Test
    fun `tall left panel keeps both bubbles before the two right panels`() {
        val panels = listOf(
            "left" to PanelBounds(40f, 100f, 470f, 750f),
            "top-right" to PanelBounds(495f, 30f, 1150f, 425f),
            "bottom-right" to PanelBounds(495f, 450f, 1150f, 750f)
        )
        val bubbles = listOf(
            1 to PanelBounds(85f, 140f, 285f, 270f),
            2 to PanelBounds(850f, 160f, 1090f, 250f),
            3 to PanelBounds(915f, 245f, 1135f, 400f),
            4 to PanelBounds(195f, 325f, 415f, 455f),
            5 to PanelBounds(520f, 465f, 710f, 640f),
            6 to PanelBounds(635f, 620f, 785f, 715f)
        )
        val byPanel = bubbles.groupBy { (_, bounds) -> findParentPanel(bounds, panels) }
        val ordered = orderPanelsByRows(panels, Direction.LTR)
            .flatMap { byPanel.getValue(it) }
            .map { it.first }

        assertEquals(listOf(1, 4, 2, 3, 5, 6), ordered)
    }

    @Test
    fun `overlapping bubbles across a panel border retain different owners`() {
        val panels = listOf(
            "left" to PanelBounds(0f, 0f, 500f, 500f),
            "right" to PanelBounds(480f, 0f, 1000f, 500f)
        )
        val leftBubble = PanelBounds(400f, 100f, 510f, 220f)
        val rightBubble = PanelBounds(490f, 150f, 650f, 270f)

        assertEquals("left", findParentPanel(leftBubble, panels))
        assertEquals("right", findParentPanel(rightBubble, panels))
        assertEquals("left", findParentPanel(leftBubble, panels.reversed()))
        assertEquals("right", findParentPanel(rightBubble, panels.reversed()))
    }

    @Test
    fun `equally contained bubble belongs to the smaller detected panel`() {
        val panels = listOf(
            "page" to PanelBounds(0f, 0f, 1000f, 1000f),
            "panel" to PanelBounds(10f, 10f, 400f, 400f)
        )
        val bubble = PanelBounds(50f, 50f, 100f, 100f)

        assertEquals("panel", findParentPanel(bubble, panels))
        assertEquals("panel", findParentPanel(bubble, panels.reversed()))
    }

    @Test
    fun `undetected panels and edge-only contact leave bubbles unassigned`() {
        val bubble = PanelBounds(100f, 100f, 200f, 200f)

        assertNull(findParentPanel(bubble, emptyList<Pair<Long, PanelBounds>>()))
        assertNull(findParentPanel(bubble, listOf(1L to PanelBounds(0f, 0f, 100f, 100f))))
    }

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
