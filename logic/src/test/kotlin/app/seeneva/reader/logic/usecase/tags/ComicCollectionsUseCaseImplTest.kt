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
import app.seeneva.reader.data.source.local.db.dao.ComicBookCover
import app.seeneva.reader.data.source.local.db.dao.ComicBookSource
import app.seeneva.reader.data.source.local.db.dao.ComicTagSource
import app.seeneva.reader.logic.entity.ComicCollection
import app.seeneva.reader.logic.entity.TagType
import android.net.Uri
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ComicCollectionsUseCaseImplTest {
    @MockK
    private lateinit var comicBookSource: ComicBookSource

    @MockK
    private lateinit var comicTagSource: ComicTagSource

    @MockK
    private lateinit var coverPath: Uri

    private val transactionRunner = object : LocalTransactionRunner {
        override suspend fun <R> run(block: suspend () -> R): R = block()
    }

    private lateinit var useCase: ComicCollectionsUseCase

    private val userTagType: Int
        get() = TagType.TYPE_USER.ordinal

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)

        useCase = ComicCollectionsUseCaseImpl(comicBookSource, comicTagSource, transactionRunner)
    }

    @AfterTest
    fun after() {
        unmockkAll()
    }

    @Test
    fun `return only user tags as collections`() {
        val tags = listOf(
            ComicTag(1, "aaa", userTagType),
            ComicTag(2, "Bbb", userTagType)
        )

        coEvery { comicTagSource.findAllByType(userTagType) } returns tags
        coEvery { comicBookSource.findFirstCoverByTag(any()) } returns null

        runBlocking {
            assertEquals(
                listOf(ComicCollection(1, "aaa"), ComicCollection(2, "Bbb")),
                useCase.getCollections()
            )
        }

        coVerify(exactly = 1) { comicTagSource.findAllByType(userTagType) }
        coVerify(exactly = 2) { comicBookSource.findFirstCoverByTag(any()) }
    }

    @Test
    fun `use first collection book as cover`() {
        val tag = ComicTag(1, "aaa", userTagType)
        coEvery { comicTagSource.findAllByType(userTagType) } returns listOf(tag)
        coEvery { comicBookSource.findFirstCoverByTag(tag.id) } returns
                ComicBookCover(coverPath, 4)

        runBlocking {
            assertEquals(
                listOf(ComicCollection(1, "aaa", ComicCollection.Cover(coverPath, 4))),
                useCase.getCollections()
            )
        }
    }

    @Test
    fun `blank collection name is not allowed`() {
        runBlocking {
            assertFailsWith<IllegalArgumentException> { useCase.createOrGetCollection("   ") }
        }

        coVerify(exactly = 0) { comicTagSource.insertOrReplace(*anyVararg()) }
    }

    @Test
    fun `reuse case insensitively equal collection`() {
        val existed = ComicTag(5, "Marvel", userTagType)

        //name should be trimmed before any check
        coEvery { comicTagSource.findByTypeAndName(userTagType, "marvel") } returns existed

        runBlocking {
            assertEquals(ComicCollection(5, "Marvel"), useCase.createOrGetCollection("  marvel  "))
        }

        coVerify(exactly = 0) { comicTagSource.insertOrReplace(*anyVararg()) }
    }

    @Test
    fun `create a new collection`() {
        coEvery { comicTagSource.findByTypeAndName(userTagType, "DC") } returns null
        coEvery { comicTagSource.insertOrReplace(*anyVararg()) } returns listOf(7L)

        runBlocking {
            assertEquals(ComicCollection(7, "DC"), useCase.createOrGetCollection(" DC "))
        }

        coVerify(exactly = 1) {
            comicTagSource.insertOrReplace(ComicTag(0, "DC", userTagType))
        }
    }
}
