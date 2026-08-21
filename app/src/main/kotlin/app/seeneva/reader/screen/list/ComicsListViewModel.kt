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
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import app.seeneva.reader.common.coroutines.Dispatchers
import app.seeneva.reader.logic.ComicsSettings
import app.seeneva.reader.logic.comic.AddComicBookMode
import app.seeneva.reader.logic.comic.Library
import app.seeneva.reader.logic.entity.ComicAddResult
import app.seeneva.reader.logic.entity.ComicCollection
import app.seeneva.reader.logic.entity.ComicListItem
import app.seeneva.reader.logic.entity.query.QueryParams
import app.seeneva.reader.logic.usecase.ComicListUseCase
import app.seeneva.reader.logic.usecase.RenameComicBookUseCase
import app.seeneva.reader.logic.usecase.tags.ComicCollectionsUseCase
import app.seeneva.reader.logic.usecase.tags.ComicCompletedTagUseCase
import app.seeneva.reader.logic.usecase.tags.ComicRemovedTagUseCase
import app.seeneva.reader.service.add.AddComicBookServiceConnector
import app.seeneva.reader.viewmodel.CoroutineViewModel
import app.seeneva.reader.viewmodel.EventSender
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface ListEvents

/**
 * Comic books marked as removed, but not deleted yet
 * @param ids ids of marked comic books
 */
data class ComicsMarkedAsRemoved(val ids: Set<Long>) : ListEvents

data class ComicsOpened(val result: ComicAddResult) : ListEvents

/**
 * Comic books were added into a collection
 * @param count count of added comic books
 * @param collectionName name of the target collection
 */
data class ComicsAddedToCollection(val count: Int, val collectionName: String) : ListEvents

/**
 * Comic books were removed from a collection
 * @param count count of removed comic books
 * @param collectionName name of the target collection
 */
data class ComicsRemovedFromCollection(val count: Int, val collectionName: String) : ListEvents

/**
 * User typed invalid collection name
 */
object InvalidCollectionName : ListEvents

sealed interface ComicsPagingState {
    object Idle : ComicsPagingState

    object Loading : ComicsPagingState

    /**
     * Loaded comic book page
     * @param pagingData data pf the page
     * @param totalCount total count of comic books
     */
    data class Loaded(
        val pagingData: PagingData<ComicListItem>,
        val totalCount: Long
    ) : ComicsPagingState
}

interface ComicsListViewModel {
    val pagingState: StateFlow<ComicsPagingState>

    val eventsFlow: Flow<ListEvents>

    val libraryState: StateFlow<Library.State>

    var queryParams: QueryParams

    /**
     * All known user collections sorted by name (case insensitive)
     */
    val collections: StateFlow<List<ComicCollection>>

    /**
     * Currently active collection or null if all comic books should be showed
     */
    val activeCollection: StateFlow<ComicCollection?>

    /**
     * Load all user collections into [collections]
     */
    fun loadCollections()

    /**
     * Load and return all user collections sorted by name (case insensitive)
     */
    suspend fun awaitCollections(): List<ComicCollection>

    /**
     * Set currently active collection filter
     * @param collectionId id of the collection or null to show all comic books
     */
    fun setActiveCollection(collectionId: Long?)

    /**
     * Add comic books into an existed collection
     * @param ids comic book ids to add
     * @param collectionId target collection id
     */
    fun addToCollection(ids: Set<Long>, collectionId: Long)

    /**
     * Add comic books into a collection with provided [name].
     * Collection will be created if it doesn't exist yet
     * @param ids comic book ids to add
     * @param name name of the target collection
     */
    fun addToNewCollection(ids: Set<Long>, name: String)

    /**
     * Remove comic books from a collection
     * @param ids comic book ids to remove
     * @param collectionId target collection id
     */
    fun removeFromCollection(ids: Set<Long>, collectionId: Long)

    /**
     * Start comics list page loading
     * @param startIndex init page position
     * @param pageSize page size
     */
    fun loadComicsPagingData(startIndex: Int, pageSize: Int)

    /**
     * Synchronize library
     */
    fun sync()

    /**
     * Add comic book into user library
     * @param path comic book path
     */
    fun add(path: Uri, addComicBookMode: AddComicBookMode, openFlags: Int) {
        add(listOf(path), addComicBookMode, openFlags)
    }

    /**
     * Add comic books into user library
     * @param paths comic books paths
     */
    fun add(paths: List<Uri>, addComicBookMode: AddComicBookMode, openFlags: Int)

    /**
     * Change comic books removed state
     * @param ids comic book ids to set as removed
     * @param removed removed or not
     */
    fun setRemovedState(ids: Set<Long>, removed: Boolean)

    /**
     * Delete comic books
     * @param ids comic book ids to delete
     */
    fun permanentRemove(ids: Set<Long>)

    fun rename(id: Long, title: String)

    fun toggleCompletedMark(id: Long)

    fun setComicsCompletedMark(ids: Set<Long>, completed: Boolean)
}

class ComicsListViewModelImpl(
    dispatchers: Dispatchers,
    private val settings: ComicsSettings,
    private val addComicBookServiceConnector: AddComicBookServiceConnector,
    private val comicListUseCase: ComicListUseCase,
    private val collectionsUseCase: ComicCollectionsUseCase,
    private val removeStateUseCase: ComicRemovedTagUseCase,
    private val renameUseCase: RenameComicBookUseCase,
    private val markComicCompletedUseCase: ComicCompletedTagUseCase,
    private val library: Library,
    job: Job //need to set as parent in the [AddComicBookServiceConnector]
) : CoroutineViewModel(dispatchers, job), ComicsListViewModel {
    override val pagingState = MutableStateFlow<ComicsPagingState>(ComicsPagingState.Idle)

    override val libraryState
        get() = library.state

    override var queryParams
        get() = queryParamsFlow.value
        set(value) {
            queryParamsFlow.value = value
        }

    private val queryParamsFlow = MutableStateFlow(settings.getComicListQueryParams())

    override val collections = MutableStateFlow<List<ComicCollection>>(emptyList())

    // Active collection is intentionally NOT persisted between app launches.
    // Collection ids are dynamic (they are database ids) and the persisted query params
    // should not be affected by them. So the library always starts from the 'All books' state
    private val activeCollectionFlow = MutableStateFlow<ComicCollection?>(null)

    override val activeCollection: StateFlow<ComicCollection?>
        get() = activeCollectionFlow

    private val eventsSender = EventSender<ListEvents>()

    override val eventsFlow
        get() = eventsSender.eventState

    private var listLoadJob: ListLoadingJob? = null

    init {
        vmScope.launch {
            queryParamsFlow.collectLatest {
                settings.saveComicListQueryParams(it)
            }
        }
    }

    override fun loadComicsPagingData(startIndex: Int, pageSize: Int) {
        listLoadJob?.also {
            val sameRequest = it.isActive && it.pageSize == pageSize && it.startIndex == startIndex

            if (sameRequest) {
                return
            }
        }

        val prevListLoadJob = listLoadJob

        listLoadJob = ListLoadingJob(pageSize,
            startIndex,
            vmScope.launch {
                prevListLoadJob?.cancelAndJoin()

                val pagingDataFlow = queryParamsFlow
                    .combine(activeCollectionFlow) { params, collection -> params to collection?.id }
                    .flatMapLatest { (params, collectionId) ->
                        comicListUseCase.getPagingData(
                            PagingConfig(pageSize),
                            params,
                            startIndex,
                            collectionId
                        )
                    }.cachedIn(this)
                    .mapLatest {
                        val totalCount = comicListUseCase.totalCount(queryParams)

                        ComicsPagingState.Loaded(it, totalCount)
                    }.onStart<ComicsPagingState> { emit(ComicsPagingState.Loading) }
                    .onCompletion { emit(ComicsPagingState.Idle) }

                pagingState.emitAll(pagingDataFlow)
            })
    }

    override fun sync() {
        //TODO I think it should be moved to a Service...
        vmScope.launch { library.sync() }
    }

    override fun add(paths: List<Uri>, addComicBookMode: AddComicBookMode, openFlags: Int) {
        if (paths.isEmpty()) {
            return
        }

        vmScope.launch {
            eventsSender.sendAll(
                addComicBookServiceConnector.add(
                    paths,
                    addComicBookMode,
                    openFlags
                ).map(::ComicsOpened)
            )
        }
    }

    override fun setRemovedState(ids: Set<Long>, removed: Boolean) {
        vmScope.launch {
            removeStateUseCase.change(ids, removed)

            if (removed) {
                eventsSender.send(ComicsMarkedAsRemoved(ids))
            }
        }
    }

    override fun permanentRemove(ids: Set<Long>) {
        vmScope.launch {
            library.delete(ids)
        }
    }

    override fun rename(id: Long, title: String) {
        vmScope.launch { renameUseCase.byComicBookId(id, title) }
    }

    override fun toggleCompletedMark(id: Long) {
        vmScope.launch { markComicCompletedUseCase.toggle(id) }
    }

    override fun setComicsCompletedMark(ids: Set<Long>, completed: Boolean) {
        vmScope.launch { markComicCompletedUseCase.change(ids, completed) }
    }

    override fun loadCollections() {
        vmScope.launch { refreshCollections() }
    }

    override suspend fun awaitCollections() = refreshCollections()

    override fun setActiveCollection(collectionId: Long?) {
        if (activeCollectionFlow.value?.id == collectionId) {
            return
        }

        if (collectionId == null) {
            activeCollectionFlow.value = null
        } else {
            vmScope.launch {
                //collections can be not loaded yet (e.g. after a process death)
                val collection = collections.value.firstOrNull { it.id == collectionId }
                    ?: refreshCollections().firstOrNull { it.id == collectionId }

                activeCollectionFlow.value = collection
            }
        }
    }

    override fun addToCollection(ids: Set<Long>, collectionId: Long) {
        if (ids.isEmpty()) {
            return
        }

        vmScope.launch {
            collectionsUseCase.assignToCollection(ids, collectionId)

            val collectionName = refreshCollections()
                .firstOrNull { it.id == collectionId }
                ?.name ?: return@launch

            eventsSender.send(ComicsAddedToCollection(ids.size, collectionName))
        }
    }

    override fun addToNewCollection(ids: Set<Long>, name: String) {
        if (ids.isEmpty()) {
            return
        }

        vmScope.launch {
            val collection = try {
                collectionsUseCase.createOrGetCollection(name)
            } catch (t: IllegalArgumentException) {
                eventsSender.send(InvalidCollectionName)
                return@launch
            }

            collectionsUseCase.assignToCollection(ids, collection.id)

            refreshCollections()

            eventsSender.send(ComicsAddedToCollection(ids.size, collection.name))
        }
    }

    override fun removeFromCollection(ids: Set<Long>, collectionId: Long) {
        if (ids.isEmpty()) {
            return
        }

        vmScope.launch {
            val collectionName = collections.value.firstOrNull { it.id == collectionId }?.name

            collectionsUseCase.removeFromCollection(ids, collectionId)

            if (collectionName != null) {
                eventsSender.send(ComicsRemovedFromCollection(ids.size, collectionName))
            }
        }
    }

    /**
     * Load all user collections and update [collections] state
     * @return loaded collections
     */
    private suspend fun refreshCollections(): List<ComicCollection> =
        collectionsUseCase.getCollections()
            .also { loaded ->
                collections.value = loaded

                //active collection could be deleted or renamed somewhere else
                activeCollectionFlow.value = activeCollectionFlow.value
                    ?.let { active -> loaded.firstOrNull { it.id == active.id } }
            }

    private data class ListLoadingJob(
        val pageSize: Int,
        val startIndex: Int,
        private val job: Job
    ) : Job by job
}