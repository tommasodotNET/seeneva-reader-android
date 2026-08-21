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

package app.seeneva.reader.logic.usecase.tags

import app.seeneva.reader.data.entity.ComicTag
import app.seeneva.reader.data.source.local.db.LocalTransactionRunner
import app.seeneva.reader.data.source.local.db.dao.ComicBookSource
import app.seeneva.reader.data.source.local.db.dao.ComicTagSource
import app.seeneva.reader.logic.entity.ComicCollection
import app.seeneva.reader.logic.extension.getOrCreateUserTag
import app.seeneva.reader.logic.extension.getUserTags

/**
 * Manage user comic book collections.
 *
 * Any user comic book tag is a collection. It includes tags imported from a ComicRack metadata
 */
interface ComicCollectionsUseCase {
    /**
     * Get all user collections sorted by name (case insensitive)
     */
    suspend fun getCollections(): List<ComicCollection>

    /**
     * Get a collection by it [name] or create a new one.
     *
     * Name will be trimmed. Comparison is case insensitive, so an already existed collection
     * will be returned instead of creating a case insensitive duplicate
     *
     * @param name requested collection name
     * @throws IllegalArgumentException if [name] is blank
     */
    suspend fun createOrGetCollection(name: String): ComicCollection

    /**
     * Add comic books into a collection
     * @param bookIds comic book ids to add
     * @param collectionId target collection id
     */
    suspend fun assignToCollection(bookIds: Set<Long>, collectionId: Long)

    /**
     * Remove comic books from a collection
     * @param bookIds comic book ids to remove
     * @param collectionId target collection id
     */
    suspend fun removeFromCollection(bookIds: Set<Long>, collectionId: Long)

    /**
     * Delete a collection. Comic books will not be deleted
     * @param collectionId collection id to delete
     */
    suspend fun deleteCollection(collectionId: Long)
}

internal class ComicCollectionsUseCaseImpl(
    private val comicBookSource: ComicBookSource,
    private val comicTagSource: ComicTagSource,
    private val localTransactionRunner: LocalTransactionRunner,
) : ComicCollectionsUseCase {
    override suspend fun getCollections() =
        comicTagSource.getUserTags().map { tag ->
            val cover = comicBookSource.findFirstCoverByTag(tag.id)?.let {
                ComicCollection.Cover(it.filePath, it.coverPosition)
            }
            tag.intoCollection(cover)
        }

    override suspend fun createOrGetCollection(name: String): ComicCollection {
        val preparedName = name.trim()

        require(preparedName.isNotEmpty()) { "Collection name cannot be blank" }

        return localTransactionRunner.run {
            comicTagSource.getOrCreateUserTag(preparedName)
        }.intoCollection()
    }

    override suspend fun assignToCollection(bookIds: Set<Long>, collectionId: Long) {
        changeCollection(bookIds, collectionId, true)
    }

    override suspend fun removeFromCollection(bookIds: Set<Long>, collectionId: Long) {
        changeCollection(bookIds, collectionId, false)
    }

    override suspend fun deleteCollection(collectionId: Long) {
        //related rows in the join table will be removed by the foreign key `ON DELETE CASCADE`
        comicTagSource.deleteById(collectionId)
    }

    private suspend fun changeCollection(bookIds: Set<Long>, collectionId: Long, add: Boolean) {
        if (bookIds.isEmpty()) {
            return
        }

        localTransactionRunner.run {
            comicBookSource.changeTags(bookIds, setOf(collectionId), add)
        }
    }

    private companion object {
        fun ComicTag.intoCollection(cover: ComicCollection.Cover? = null) =
            ComicCollection(id, name, cover)
    }
}
