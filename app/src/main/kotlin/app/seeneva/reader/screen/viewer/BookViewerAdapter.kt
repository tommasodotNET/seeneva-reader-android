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

package app.seeneva.reader.screen.viewer

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import app.seeneva.reader.logic.entity.ComicBookPage
import app.seeneva.reader.screen.viewer.entity.PageSpread
import app.seeneva.reader.screen.viewer.entity.intoPageSpreads
import app.seeneva.reader.screen.viewer.entity.pagePositionOfSpread
import app.seeneva.reader.screen.viewer.entity.spreadIndexOfPage
import app.seeneva.reader.screen.viewer.page.BookViewerSpreadFragment

/**
 * Adapter which displays comic book pages as Android Fragments.
 *
 * In portrait orientation (or when [paired] is `false`) every original page is displayed alone.
 * In landscape orientation (when [paired] is `true`) pages are grouped into two-page
 * [PageSpread]s: the first original page is a single cover, then pages are paired 1+2, 3+4, etc.
 * A trailing odd page (if any) stays single. See [intoPageSpreads]
 */
class BookViewerAdapter(
    activity: FragmentActivity,
    private val paired: Boolean = false,
    initPages: List<ComicBookPage>? = null,
) : FragmentStateAdapter(activity) {
    private val differ = AsyncListDiffer(this, PageSpreadDiffCallback())

    /**
     * Spreads ids
     */
    private val ids = hashSetOf<Long>()

    private var positionOverrideFun: ((count: Int, pos: Int) -> Int)? = null

    init {
        setPages(initPages)
    }

    override fun getItemCount() = differ.currentList.size

    override fun createFragment(position: Int): Fragment {
        val spread = getSpread(position)

        // adapter position mapping (RTL/LTR) is handled by getSpread/positionOverrideFun above.
        // Here we only need to decide the *intra-spread* visual order: in RTL mode the earlier
        // (lower index) page of a spread should be displayed on the right, so we reverse it.
        // We reuse the very same "is RTL" signal (positionOverrideFun being set) that already
        // drives the inter-spread reversal, instead of tracking a separate direction field
        val orderedPages = if (positionOverrideFun != null) spread.pages.asReversed() else spread.pages

        return BookViewerSpreadFragment.newInstance(
            LongArray(orderedPages.size) { orderedPages[it].id },
            getItemId(position)
        )
    }

    override fun getItemId(position: Int) =
        if (position in differ.currentList.indices) {
            getSpread(position).directionAwareId
        } else {
            RecyclerView.NO_ID
        }


    //need to be implemented because I use custom getItemId. See description
    override fun containsItem(itemId: Long) = ids.contains(itemId)

    fun setPages(pages: List<ComicBookPage>?) {
        // keep null (as opposed to an empty list) when there are no pages yet, same as before -
        // AsyncListDiffer treats a null->non null submission as its fast, synchronous path,
        // which matters for the very first real pages submission after construction
        val spreads = pages?.intoPageSpreads(paired)

        ids.clear()

        spreads?.forEach { ids += it.directionAwareId }

        differ.submitList(spreads)
    }

    /**
     * Set function which will override adapter position
     */
    fun setPositionOverrideFun(f: ((count: Int, pos: Int) -> Int)?) {
        positionOverrideFun = f
        ids.clear()
        differ.currentList.forEach { ids += it.directionAwareId }
        notifyDataSetChanged()
    }

    private val PageSpread.directionAwareId
        get() = if (positionOverrideFun == null) id else id xor Long.MIN_VALUE

    /**
     * @return spread of pages located at [pos] raw adapter position
     */
    fun getSpread(pos: Int): PageSpread =
        differ.currentList[positionOverrideFun?.invoke(itemCount, pos) ?: pos]

    /**
     * @param pagePosition original page position (index in the list which was provided to [setPages])
     * @return index of the spread (in natural, non reversed order) which contains that page
     */
    fun spreadIndexOfPage(pagePosition: Int) = differ.currentList.spreadIndexOfPage(pagePosition)

    /**
     * @param spreadIndex spread index (in natural, non reversed order)
     * @return original position of the first (lowest indexed) page inside that spread
     */
    fun pagePositionOfSpread(spreadIndex: Int) = differ.currentList.pagePositionOfSpread(spreadIndex)

    /**
     * @param spreadIndex spread index (in natural, non reversed order)
     * @return how many original pages are grouped into that spread (1 or 2)
     */
    fun spreadPageCount(spreadIndex: Int) = differ.currentList.getOrNull(spreadIndex)?.pages?.size ?: 1
}