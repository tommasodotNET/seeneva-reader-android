/*
 * This file is part of Seeneva Android Reader
 * Copyright (C) 2023 Sergei Solodovnikov
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

package app.seeneva.reader.screen.viewer.entity

import app.seeneva.reader.logic.entity.ComicBookPage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PageSpreadTest {
    private fun pages(count: Int) =
        (0 until count).map { ComicBookPage(id = it.toLong(), position = it.toLong(), width = 100, height = 150) }

    @Test
    fun `not paired keeps every page single`() {
        val spreads = pages(5).intoPageSpreads(paired = false)

        assertEquals(5, spreads.size)
        assertTrue(spreads.all { it.pages.size == 1 })
    }

    @Test
    fun `paired groups first page alone then pairs, with a trailing single odd page`() {
        // 6 pages: cover(0), pair(1,2), pair(3,4), single(5)
        val spreads = pages(6).intoPageSpreads(paired = true)

        assertEquals(4, spreads.size)

        assertEquals(listOf(0L), spreads[0].pages.map { it.id })
        assertEquals(listOf(1L, 2L), spreads[1].pages.map { it.id })
        assertEquals(listOf(3L, 4L), spreads[2].pages.map { it.id })
        assertEquals(listOf(5L), spreads[3].pages.map { it.id })
    }

    @Test
    fun `paired with even remaining pages has no trailing odd page`() {
        // 5 pages: cover(0), pair(1,2), pair(3,4)
        val spreads = pages(5).intoPageSpreads(paired = true)

        assertEquals(3, spreads.size)

        assertEquals(listOf(0L), spreads[0].pages.map { it.id })
        assertEquals(listOf(1L, 2L), spreads[1].pages.map { it.id })
        assertEquals(listOf(3L, 4L), spreads[2].pages.map { it.id })
    }

    @Test
    fun `single page book stays single even when paired`() {
        val spreads = pages(1).intoPageSpreads(paired = true)

        assertEquals(1, spreads.size)
        assertEquals(listOf(0L), spreads[0].pages.map { it.id })
    }

    @Test
    fun `two page book keeps cover alone and the single leftover page alone`() {
        val spreads = pages(2).intoPageSpreads(paired = true)

        assertEquals(2, spreads.size)
        assertEquals(listOf(0L), spreads[0].pages.map { it.id })
        assertEquals(listOf(1L), spreads[1].pages.map { it.id })
    }

    @Test
    fun `empty pages produce empty spreads`() {
        assertTrue(emptyList<ComicBookPage>().intoPageSpreads(paired = true).isEmpty())
        assertTrue(emptyList<ComicBookPage>().intoPageSpreads(paired = false).isEmpty())
    }

    @Test
    fun `spread requires at least one page`() {
        assertFailsWith<IllegalArgumentException> { PageSpread(emptyList()) }
    }

    @Test
    fun `spread requires no more than two pages`() {
        assertFailsWith<IllegalArgumentException> { PageSpread(pages(3)) }
    }

    @Test
    fun `spread id is backed by the first page id`() {
        val spread = PageSpread(pages(2))

        assertEquals(0L, spread.id)
    }

    @Test
    fun `spread contains checks both pages`() {
        val spread = PageSpread(pages(2))

        assertTrue(spread.contains(0L))
        assertTrue(spread.contains(1L))
        assertFalse(spread.contains(2L))
    }

    @Test
    fun `spreadIndexOfPage and pagePositionOfSpread are inverse mappings`() {
        val spreads = pages(6).intoPageSpreads(paired = true)

        // spreads: [0], [1,2], [3,4], [5]
        assertEquals(0, spreads.spreadIndexOfPage(0))
        assertEquals(1, spreads.spreadIndexOfPage(1))
        assertEquals(1, spreads.spreadIndexOfPage(2))
        assertEquals(2, spreads.spreadIndexOfPage(3))
        assertEquals(2, spreads.spreadIndexOfPage(4))
        assertEquals(3, spreads.spreadIndexOfPage(5))

        assertEquals(0, spreads.pagePositionOfSpread(0))
        assertEquals(1, spreads.pagePositionOfSpread(1))
        assertEquals(3, spreads.pagePositionOfSpread(2))
        assertEquals(5, spreads.pagePositionOfSpread(3))

        // round trip: pagePositionOfSpread(spreadIndexOfPage(x)) should be <= x and point to the
        // start of the same spread
        for (page in 0 until 6) {
            val spreadIndex = spreads.spreadIndexOfPage(page)
            assertTrue(spreads.pagePositionOfSpread(spreadIndex) <= page)
        }
    }
}
