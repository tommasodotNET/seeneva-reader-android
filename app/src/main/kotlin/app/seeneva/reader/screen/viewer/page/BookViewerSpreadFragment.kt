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

package app.seeneva.reader.screen.viewer.page

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.recyclerview.widget.RecyclerView
import app.seeneva.reader.R
import app.seeneva.reader.binding.getValue
import app.seeneva.reader.binding.viewBinding
import app.seeneva.reader.databinding.FragmentViewerSpreadBinding
import app.seeneva.reader.logic.entity.Direction
import app.seeneva.reader.screen.viewer.BookViewerAdapter
import app.seeneva.reader.screen.viewer.page.BookViewerPageFragment.Companion.pageId
import app.seeneva.reader.screen.viewer.page.entity.PageObjectDirection

/**
 * Hosts one or two [BookViewerPageFragment]s side by side so a landscape two-page spread can be
 * displayed as a single [androidx.viewpager2.widget.ViewPager2] page.
 *
 * Also used to display a single page (portrait orientation, comic book cover page, or a leftover
 * odd page) so the rest of the viewer code can always work with the same "one adapter item -
 * one visible page slot" abstraction regardless of orientation
 */
class BookViewerSpreadFragment :
    Fragment(R.layout.fragment_viewer_spread),
    BookViewerPageFragment.Callback {
    private val viewBinding by viewBinding(FragmentViewerSpreadBinding::bind)

    private val callback
        get() = activity as? Callback

    private var activePageId: Long? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activePageId = savedInstanceState
            ?.takeIf { it.containsKey(STATE_ACTIVE_PAGE_ID) }
            ?.getLong(STATE_ACTIVE_PAGE_ID)

        val ids = pageIds

        // a weighted LinearLayout will automatically expand the start container to fill the
        // whole width once the end one is gone
        viewBinding.pageContainerEnd.isGone = ids.size < 2

        if (savedInstanceState == null) {
            childFragmentManager.commit {
                add(viewBinding.pageContainerStart.id, BookViewerPageFragment.newInstance(ids[0]))

                if (ids.size > 1) {
                    add(viewBinding.pageContainerEnd.id, BookViewerPageFragment.newInstance(ids[1]))
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        activePageId?.let { outState.putLong(STATE_ACTIVE_PAGE_ID, it) }
        super.onSaveInstanceState(outState)
    }

    override fun onPageObjectNavigationRequested(
        pageId: Long,
        direction: PageObjectDirection,
        readDirection: Direction
    ) {
        val visualPageIds = pageIds.asList()
        val forwardPageIds = when (readDirection) {
            Direction.LTR -> visualPageIds
            Direction.RTL -> visualPageIds.asReversed()
        }
        val traversal = when (direction) {
            PageObjectDirection.FORWARD -> forwardPageIds
            PageObjectDirection.BACKWARD -> forwardPageIds.asReversed()
        }

        val activeIndex = activePageId?.let(traversal::indexOf)?.takeIf { it >= 0 } ?: 0

        for (targetPageId in traversal.drop(activeIndex)) {
            val target = pageFragment(targetPageId) ?: continue

            if (target.showNextPageObject(direction)) {
                if (activePageId != targetPageId) {
                    resetPagesExcept(targetPageId)
                }
                activePageId = targetPageId
                return
            }
        }

        callback?.lastObjectViewed(pageId, direction)
    }

    override fun onPageObjectSelected(pageId: Long) {
        resetPagesExcept(pageId)
        activePageId = pageId
    }

    override fun isPageObjectDismissTap(pageId: Long, xFraction: Float): Boolean {
        val dismissWidth = ResourcesCompat.getFloat(
            resources,
            R.dimen.viewer_hide_page_object_x_percentage
        )
        val visualIndex = pageIds.indexOf(pageId)

        return when {
            pageIds.size == 1 -> {
                val halfWidth = dismissWidth * 0.5f
                xFraction in (0.5f - halfWidth)..(0.5f + halfWidth)
            }
            visualIndex == 0 -> xFraction >= 1f - dismissWidth
            visualIndex == 1 -> xFraction <= dismissWidth
            else -> false
        }
    }

    override fun onPageObjectDismissRequested() {
        childFragmentManager.fragments
            .filterIsInstance<BookViewerPageFragment>()
            .forEach(BookViewerPageFragment::hideCurrentPageObject)
    }

    override fun onToolbarRequested() {
        callback?.onToolbarRequested()
    }

    /**
     * @return `true` if this spread contains a page with the provided [pageId]
     */
    fun containsPage(pageId: Long) = pageId in pageIds

    /**
     * Reset read state of all pages inside this spread
     */
    fun reset() {
        childFragmentManager.fragments.forEach { fragment ->
            if (fragment is BookViewerPageFragment) {
                fragment.reset()
            }
        }
        activePageId = null
    }

    private fun pageFragment(pageId: Long) =
        childFragmentManager.fragments
            .filterIsInstance<BookViewerPageFragment>()
            .firstOrNull { it.pageId == pageId }

    private fun resetPagesExcept(pageId: Long) {
        childFragmentManager.fragments
            .filterIsInstance<BookViewerPageFragment>()
            .filterNot { it.pageId == pageId }
            .forEach(BookViewerPageFragment::reset)
    }

    interface Callback {
        fun lastObjectViewed(pageId: Long, direction: PageObjectDirection)

        fun onToolbarRequested()
    }

    companion object {
        private const val ARGS_PAGE_IDS = "page_ids"
        private const val ARGS_SPREAD_ID = "spread_id"
        private const val STATE_ACTIVE_PAGE_ID = "active_page_id"

        /**
         * Create new spread [Fragment]
         *
         * @param orderedPageIds one or two comic book page ids which should be open, already
         * ordered for visual left-to-right display according to the current read direction
         * @param spreadId direction-aware stable adapter item id
         */
        fun newInstance(orderedPageIds: LongArray, spreadId: Long) =
            BookViewerSpreadFragment()
                .apply {
                    arguments = bundleOf(
                        ARGS_PAGE_IDS to orderedPageIds,
                        ARGS_SPREAD_ID to spreadId
                    )
                }

        val BookViewerSpreadFragment.pageIds: LongArray
            get() = requireArguments().getLongArray(ARGS_PAGE_IDS)
                ?: throw IllegalStateException("Provide comic book page ids which should be open")

        /**
         * Direction-aware stable id of this spread, matching [BookViewerAdapter.getItemId].
         */
        val BookViewerSpreadFragment.spreadId: Long
            get() = requireArguments().getLong(ARGS_SPREAD_ID, RecyclerView.NO_ID)
                .takeUnless { it == RecyclerView.NO_ID }
                ?: throw IllegalStateException("Provide a stable viewer spread id")
    }
}
