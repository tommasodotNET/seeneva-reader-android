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

package app.seeneva.reader.screen.list

import android.net.Uri
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.whenStarted
import app.seeneva.reader.common.coroutines.Dispatchers
import app.seeneva.reader.extension.observe
import app.seeneva.reader.logic.ComicListViewType
import app.seeneva.reader.logic.ComicsSettings
import app.seeneva.reader.logic.comic.AddComicBookMode
import app.seeneva.reader.logic.comic.Library
import app.seeneva.reader.logic.entity.ComicCollection
import app.seeneva.reader.logic.entity.query.QueryParams
import app.seeneva.reader.logic.entity.query.QuerySort
import app.seeneva.reader.logic.entity.query.filter.Filter
import app.seeneva.reader.logic.entity.query.filter.FilterGroup
import app.seeneva.reader.presenter.BaseStatefulPresenter
import app.seeneva.reader.presenter.Presenter
import app.seeneva.reader.screen.list.entity.FilterLabel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.tinylog.kotlin.Logger

interface ComicsListPresenter : Presenter {
    val pagingState: StateFlow<ComicsPagingState>

    val collections: StateFlow<List<ComicCollection>>

    val currentSearchQuery: String?

    /**
     * Currently active comic book collection or null if all comic books are showed
     */
    val activeCollection: StateFlow<ComicCollection?>

    /**
     * Load comic book paging data
     * @param startIndex start loading position
     * @param pageSize size of loading page
     * @see pagingState
     */
    fun loadComicsPagingData(startIndex: Int = 0, pageSize: Int = COMIC_PAGE_SIZE)

    /**
     * User click on edit list filters button
     */
    fun onEditFilterClick()

    /**
     * User click on sync button
     */
    fun onSyncClick()

    /**
     * Delete or mark as deleted
     * @param ids ids of comic book
     * @param permanent true - delete, false - mark as removed
     */
    fun deleteComicBook(ids: Set<Long>, permanent: Boolean = false)

    /**
     * Undo mark as removed
     * @param ids ids of comic book
     */
    fun undoComicRemove(ids: Set<Long>)

    /**
     * On sort comic book list button click
     */
    fun onSortListClick()

    /**
     * User selected a new sort
     * @param selectedSort buildNew sort type
     */
    fun onSortSelected(selectedSort: QuerySort)

    fun onFiltersAccepted(acceptedFilters: Map<FilterGroup.ID, Filter>)

    fun removeFilter(groupId: FilterGroup.ID)

    /**
     * Rename comic book
     * @param id ikd of the comic book to rename
     * @param title buildNew title of the comic book
     */
    fun renameComicBook(id: Long, title: String)

    fun onSearchQuery(query: String?)

    fun toggleComicCompletedMark(id: Long)

    /**
     * Set completed mark to all comics with provided [ids]
     * @param ids comic books ids
     * @param completed which flag should be set
     */
    fun setComicsCompletedMark(ids: Set<Long>, completed: Boolean)

    fun onListTypeChanged(listType: ComicListViewType)

    /**
     * User click on collections filter button
     */
    fun onCollectionsClick()

    /**
     * User selected a collection to filter the comic book list
     * @param collectionId id of the selected collection or null to show all comic books
     */
    fun onCollectionSelected(collectionId: Long?)

    /**
     * User wants to add comic books into a collection
     * @param ids comic book ids to add
     */
    fun onAddToCollectionClick(ids: Set<Long>)

    /**
     * User selected an existed collection to add comic books into
     * @param ids comic book ids to add
     * @param collectionId target collection id
     */
    fun addToCollection(ids: Set<Long>, collectionId: Long)

    /**
     * User typed a name of a collection to add comic books into.
     * Collection will be created if it doesn't exist yet
     * @param ids comic book ids to add
     * @param name name of the target collection
     */
    fun addToNewCollection(ids: Set<Long>, name: String)

    /**
     * Remove comic books from the currently active collection
     * @param ids comic book ids to remove
     */
    fun removeFromActiveCollection(ids: Set<Long>)

    /**
     * Add provided comic books
     * @param mode adding mode
     * @param paths comic book paths
     * @param flags adding flags
     */
    fun addComicBooks(mode: AddComicBookMode, paths: List<Uri>, flags: Int)

    companion object {
        private const val COMIC_PAGE_SIZE = 15
    }
}

class ComicsListPresenterImpl(
    view: ComicsListView,
    dispatchers: Dispatchers,
    private val settings: ComicsSettings,
    private val viewModel: ComicsListViewModel
) : BaseStatefulPresenter<ComicsListView>(view, dispatchers), ComicsListPresenter {
    override val pagingState: StateFlow<ComicsPagingState>
        get() = viewModel.pagingState

    override val collections: StateFlow<List<ComicCollection>>
        get() = viewModel.collections

    private var queryParams: QueryParams
        get() = viewModel.queryParams
        set(newQueryParams) {
            if (queryParams.filters != newQueryParams.filters) {
                showFilters(newQueryParams)
            }

            viewModel.queryParams = newQueryParams
        }

    override val currentSearchQuery: String?
        get() = queryParams.titleQuery

    override val activeCollection: StateFlow<ComicCollection?>
        get() = viewModel.activeCollection

    init {
        viewModel.eventsFlow
            .observe(view) {
                when (val content = it) {
                    is ComicsMarkedAsRemoved -> view.onComicsMarkedRemoved(content.ids)
                    is ComicsOpened -> view.onComicAdded(content.result)
                    is ComicsAddedToCollection ->
                        view.onComicsAddedToCollection(content.count, content.collectionName)
                    is ComicsRemovedFromCollection ->
                        view.onComicsRemovedFromCollection(content.count, content.collectionName)
                    InvalidCollectionName -> view.onInvalidCollectionName()
                }
            }

        viewModel.libraryState
            .observe(view) {
                when (it) {
                    Library.State.IDLE -> ComicsListView.SyncState.IDLE
                    Library.State.SYNCING -> ComicsListView.SyncState.IN_PROGRESS
                    Library.State.CHANGING -> ComicsListView.SyncState.DISABLED
                }.also { state ->
                    Logger.debug("Set view sync state to: $state")

                    view.onSyncStateChanged(state)
                }
            }

        viewModel.activeCollection
            .observe(view) { showFilters(queryParams) }

        presenterScope.launch {
            view.whenStarted {
                showFilters(queryParams)
            }
        }

        viewModel.loadCollections()
    }

    override fun onCreate(state: Bundle?) {
        view.setComicListType(settings.getComicListType())

        if (state != null) {
            //restore titleQuery if needed
            onSearchQuery(state.getString(STATE_SEARCH_QUERY))
        }
    }

    override fun saveState() =
        bundleOf(STATE_SEARCH_QUERY to currentSearchQuery)

    override fun loadComicsPagingData(startIndex: Int, pageSize: Int) {
        viewModel.loadComicsPagingData(startIndex, pageSize)
    }

    override fun onEditFilterClick() {
        view.showFiltersEditor(queryParams.filters)
    }

    override fun onSyncClick() {
        viewModel.sync()
    }

    override fun deleteComicBook(ids: Set<Long>, permanent: Boolean) {
        if (permanent) {
            viewModel.permanentRemove(ids)
        } else {
            viewModel.setRemovedState(ids, true)
        }
    }

    override fun undoComicRemove(ids: Set<Long>) {
        viewModel.setRemovedState(ids, false)
    }

    override fun onSortListClick() {
        view.showComicSortTypes(queryParams.sort)
    }

    override fun onSortSelected(selectedSort: QuerySort) {
        if (queryParams.sort == selectedSort) {
            return
        }

        queryParams = queryParams.buildNew { sort = selectedSort }
    }

    override fun onFiltersAccepted(acceptedFilters: Map<FilterGroup.ID, Filter>) {
        queryParams = queryParams.buildNew {
            acceptedFilters.forEach { (id, filter) -> addFilter(id, filter) }
        }
    }

    override fun removeFilter(groupId: FilterGroup.ID) {
        if (!queryParams.filters.containsKey(groupId)) {
            return
        }

        queryParams = queryParams.buildNew { removeFilter(groupId) }
    }

    override fun renameComicBook(id: Long, title: String) {
        viewModel.rename(id, title)
    }

    override fun onSearchQuery(query: String?) {
        if (queryParams.titleQuery == query) {
            return
        }

        queryParams = queryParams.buildNew { titleQuery = query }
    }

    override fun toggleComicCompletedMark(id: Long) {
        viewModel.toggleCompletedMark(id)
    }

    override fun setComicsCompletedMark(ids: Set<Long>, completed: Boolean) {
        viewModel.setComicsCompletedMark(ids, completed)
    }

    override fun onListTypeChanged(listType: ComicListViewType) {
        settings.saveComicListType(listType)
    }

    override fun addComicBooks(mode: AddComicBookMode, paths: List<Uri>, flags: Int) {
        viewModel.add(paths, mode, flags)
    }

    override fun onCollectionsClick() {
        //always load fresh collections before showing a picker
        presenterScope.launch {
            view.showCollectionsPicker(viewModel.awaitCollections(), activeCollection.value?.id)
        }
    }

    override fun onCollectionSelected(collectionId: Long?) {
        viewModel.setActiveCollection(collectionId)
    }

    override fun onAddToCollectionClick(ids: Set<Long>) {
        if (ids.isEmpty()) {
            return
        }

        presenterScope.launch {
            view.showAddToCollection(viewModel.awaitCollections(), ids)
        }
    }

    override fun addToCollection(ids: Set<Long>, collectionId: Long) {
        viewModel.addToCollection(ids, collectionId)
    }

    override fun addToNewCollection(ids: Set<Long>, name: String) {
        viewModel.addToNewCollection(ids, name)
    }

    override fun removeFromActiveCollection(ids: Set<Long>) {
        val activeCollectionId = activeCollection.value?.id ?: return

        viewModel.removeFromCollection(ids, activeCollectionId)
    }

    private fun showFilters(queryParams: QueryParams) {
        val filterLabels = queryParams.filters.map { (groupId, filter) ->
            FilterLabel(FilterLabel.Id.Group(groupId), filter.title)
        }

        view.showFilters(
            when (val collection = activeCollection.value) {
                null -> filterLabels
                else -> filterLabels + FilterLabel(FilterLabel.Id.Collection, collection.name)
            }
        )
    }

    private companion object {
        private const val STATE_SEARCH_QUERY = "search_query"
    }
}