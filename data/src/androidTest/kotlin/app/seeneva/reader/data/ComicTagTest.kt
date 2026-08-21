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

package app.seeneva.reader.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.seeneva.reader.data.entity.ComicTag
import app.seeneva.reader.data.source.local.db.ComicDatabase
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSize
import org.junit.AfterClass
import org.junit.runner.RunWith
import java.util.concurrent.Executors
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class ComicTagTest {
    private val database = ComicDatabase.instance(
        ApplicationProvider.getApplicationContext(),
        Executors.newSingleThreadExecutor(),
        true
    )

    @AfterTest
    fun after() {
        database.clearAllTables()
    }

    companion object {
        private const val USER_TAG_TYPE = 0
        private const val OTHER_TAG_TYPE = 1

        @AfterClass
        @JvmStatic
        fun afterAll() {
            ComicDatabase.closeInstance()
        }
    }

    @Test
    fun testFindAllByType() {
        runBlocking {
            val tagSource = database.comicTagSource()

            tagSource.insertOrReplace(
                ComicTag(0, "zeta", USER_TAG_TYPE),
                ComicTag(0, "Alpha", USER_TAG_TYPE),
                ComicTag(0, "beta", USER_TAG_TYPE),
                ComicTag(0, "Hardcoded", OTHER_TAG_TYPE)
            ) shouldHaveSize 4

            //only tags of the requested type sorted case insensitively
            tagSource.findAllByType(USER_TAG_TYPE)
                .map { it.name } shouldBeEqualTo listOf("Alpha", "beta", "zeta")
        }
    }

    @Test
    fun testFindByTypeAndName() {
        runBlocking {
            val tagSource = database.comicTagSource()

            tagSource.insertOrReplace(
                ComicTag(0, "Marvel", USER_TAG_TYPE),
                ComicTag(0, "Marvel", OTHER_TAG_TYPE)
            ) shouldHaveSize 2

            //name comparison should be case insensitive
            assertNotNull(tagSource.findByTypeAndName(USER_TAG_TYPE, "mArVeL"))
                .type shouldBeEqualTo USER_TAG_TYPE

            tagSource.findByTypeAndName(USER_TAG_TYPE, "DC").shouldBeNull()
        }
    }

    @Test
    fun testDeleteById() {
        runBlocking {
            val tagSource = database.comicTagSource()

            val tagId = tagSource.insertOrReplace(ComicTag(0, "Marvel", USER_TAG_TYPE)).first()

            tagSource.deleteById(tagId) shouldBeEqualTo 1

            tagSource.findAllByType(USER_TAG_TYPE).shouldHaveSize(0)
        }
    }
}
