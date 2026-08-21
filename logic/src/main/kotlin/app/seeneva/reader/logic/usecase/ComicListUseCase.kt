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

package app.seeneva.reader.logic.usecase

import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import app.seeneva.reader.common.coroutines.Dispatched
import app.seeneva.reader.common.coroutines.Dispatchers
import app.seeneva.reader.data.source.local.db.LocalTransactionRunner
import app.seeneva.reader.data.source.local.db.dao.ComicBookSource
import app.seeneva.reader.logic.ComicsPagingDataSource
import app.seeneva.reader.logic.entity.ComicListItem
import app.seeneva.reader.logic.entity.query.QueryParams
import app.seeneva.reader.logic.entity.query.QueryParamsResolver
import app.seeneva.reader.logic.entity.query.addDefaultFilters
import app.seeneva.reader.logic.entity.query.collectionFilters
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import org.tinylog.kotlin.Logger
import app.seeneva.reader.data.source.local.db.query.QueryParams as DataLayerQueryParams

interface ComicListUseCase {
    /**
     * Get total count of all not removed comic books
     * @return comic books count
     */
    suspend fun totalCount(params: QueryParams): Long

    /**
     * Get comic books paging data
     * @param config paging config
     * @param queryParams requested query params
     * @param initPageStartIndex initial requested page start index
     * @param collectionId id of the active comic book collection (user tag) or null to show all
     * comic books. It is not a part of [QueryParams] because collection ids are dynamic and should
     * never be serialized into persisted query params
     * @return flow of [PagingData]
     */
    fun getPagingData(
        config: PagingConfig,
        queryParams: QueryParams,
        initPageStartIndex: Int? = null,
        collectionId: Long? = null
    ): Flow<PagingData<ComicListItem>>
}

internal class ComicListUseCaseImpl(
    private val comicBookSource: ComicBookSource,
    private val queryParamsResolver: QueryParamsResolver,
    private val localTransactionRunner: LocalTransactionRunner,
    _pageUseCase: Lazy<ComicsPageUseCase>,
    override val dispatchers: Dispatchers,
) : ComicListUseCase, Dispatched {
    private val pageUseCase by _pageUseCase

    override suspend fun totalCount(params: QueryParams): Long {
        //remove any user added filters to get total comic book count
        //Maybe it should be changed when query by tags will be implemented?
        return comicBookSource.count(
            queryParamsResolver.resolveCount(
                params.buildNew {
                    clearFilters()
                    titleQuery = null
                }, QueryParamsResolver.FiltersEditor::addDefaultFilters
            )
        )
    }

    override fun getPagingData(
        config: PagingConfig,
        queryParams: QueryParams,
        initPageStartIndex: Int?,
        collectionId: Long?
    ) = flow {
        val sourceFactory = InvalidatingPagingSourceFactory {
            Logger.debug("Create new comics paging source")

            ComicsPagingDataSource(
                pageUseCase,
                localTransactionRunner,
                queryParams,
                config.pageSize,
                collectionId
            )
        }

        coroutineScope {
            // Subscribe to database changes and invalidate data source if any occur
            subscribeUpdates(queryParams, collectionId)
                .onEach {
                    Logger.debug("Invalidate paging data source")
                    sourceFactory.invalidate()
                }
                .launchIn(this)

            emitAll(Pager(config, initPageStartIndex, sourceFactory).flow)
        }
    }.flowOn(dispatchers.io)

    /**
     * Create [Flow] which will emit when comic books have been changed
     * @param params comic book query params
     * @param collectionId active comic book collection id if any
     */
    private fun subscribeUpdates(params: QueryParams, collectionId: Long?) =
        flow {
            emitAll(comicBookSource.subscribeSimpleWithTags(params.resolve(0, 0, collectionId))
                .drop(1) //we do not want to receive first emit
                .map { }
                .conflate()
                .flowOn(dispatchers.io))
        }

    private suspend fun QueryParams.resolve(
        start: Int,
        count: Int,
        collectionId: Long?
    ): DataLayerQueryParams =
        queryParamsResolver.resolve(
            this,
            start,
            count,
            edit = collectionFilters(collectionId)
        )
}