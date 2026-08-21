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

/**
 * A group of one or two adjacent original comic book pages shown together as a single viewer
 * "page": either a two-page spread (landscape orientation) or a single page (portrait, cover
 * page, or a leftover odd page).
 *
 * Pages are always kept here in ascending original reading order, regardless of the comic
 * book's read direction (LTR/RTL). Visual left-to-right ordering on screen should be resolved
 * by callers depending on the current read direction (in RTL mode the earlier page of a spread
 * should be displayed on the right, so callers should reverse [pages] order in that case)
 *
 * @param pages one or two pages inside the spread, in ascending original page order
 */
data class PageSpread(val pages: List<ComicBookPage>) {
    init {
        require(pages.size in 1..2) { "A page spread should contain one or two pages, got ${pages.size}" }
    }

    /**
     * Stable unique id of this spread. Backed by the first (lowest indexed) page id, so it never
     * collides with any other spread's id and stays stable as long as this spread's first page
     * stays the same
     */
    val id: Long
        get() = pages.first().id

    /**
     * @return `true` if this spread contains a page with the provided [pageId]
     */
    operator fun contains(pageId: Long) = pages.any { it.id == pageId }
}

/**
 * Group comic book pages into [PageSpread]s.
 *
 * When [paired] is `false` every page becomes its own single page spread (e.g. portrait
 * orientation, or a book with a single page).
 *
 * When [paired] is `true` pages are grouped as landscape two-page spreads: the very first page
 * always stays a single cover page, then the rest of the pages are paired 1+2, 3+4 and so on.
 * A final leftover page (in case of an odd remaining pages count) stays single as well
 *
 * @param paired should adjacent pages be grouped into two-page spreads
 */
fun List<ComicBookPage>.intoPageSpreads(paired: Boolean): List<PageSpread> {
    if (!paired || size <= 1) {
        return map { PageSpread(listOf(it)) }
    }

    val spreads = ArrayList<PageSpread>(size / 2 + 1)

    // the very first page is always a single cover
    spreads += PageSpread(listOf(this[0]))

    var i = 1

    while (i < size) {
        if (i + 1 < size) {
            spreads += PageSpread(listOf(this[i], this[i + 1]))
            i += 2
        } else {
            spreads += PageSpread(listOf(this[i]))
            i += 1
        }
    }

    return spreads
}

/**
 * @param pagePosition original page position (an index into the source pages list which was used
 * to build these spreads)
 * @return index of the spread which contains the page at [pagePosition]
 */
fun List<PageSpread>.spreadIndexOfPage(pagePosition: Int): Int {
    var consumed = 0

    forEachIndexed { index, spread ->
        consumed += spread.pages.size

        if (pagePosition < consumed) {
            return index
        }
    }

    return lastIndex.coerceAtLeast(0)
}

/**
 * @param spreadIndex spread index
 * @return original position of the first (lowest indexed) page inside the spread at [spreadIndex]
 */
fun List<PageSpread>.pagePositionOfSpread(spreadIndex: Int): Int {
    var position = 0

    for (i in 0 until spreadIndex) {
        position += getOrNull(i)?.pages?.size ?: 0
    }

    return position
}
