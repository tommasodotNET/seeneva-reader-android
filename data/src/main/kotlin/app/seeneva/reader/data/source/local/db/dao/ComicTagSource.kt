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

package app.seeneva.reader.data.source.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.seeneva.reader.data.entity.ComicTag

@Dao
interface ComicTagSource {
    /**
     * Try to find comic book tag by it [type]
     * @param type comic book tag type
     * @return comic book tag if it exists
     */
    @Query(
        """
        SELECT * FROM ${ComicTag.TABLE_NAME}
        WHERE ${ComicTag.COLUMN_TYPE} = :type
        LIMIT 1
    """
    )
    suspend fun findByType(type: Int): ComicTag?

    /**
     * Find all comic book tags by it [type]
     * @param type comic book tag type
     * @return all tags of the provided type sorted by name (case insensitive)
     */
    @Query(
        """
        SELECT * FROM ${ComicTag.TABLE_NAME}
        WHERE ${ComicTag.COLUMN_TYPE} = :type
        ORDER BY ${ComicTag.COLUMN_NAME} COLLATE NOCASE
    """
    )
    suspend fun findAllByType(type: Int): List<ComicTag>

    /**
     * Try to find a comic book tag by it [type] and [name]. Name comparison is case insensitive
     * @param type comic book tag type
     * @param name comic book tag name
     * @return comic book tag if it exists
     */
    @Query(
        """
        SELECT * FROM ${ComicTag.TABLE_NAME}
        WHERE ${ComicTag.COLUMN_TYPE} = :type AND ${ComicTag.COLUMN_NAME} = :name COLLATE NOCASE
        LIMIT 1
    """
    )
    suspend fun findByTypeAndName(type: Int, name: String): ComicTag?

    /**
     * Delete comic book tag by it id.
     *
     * Related rows in the join table will be removed by the `ON DELETE CASCADE` foreign key
     * @param tagId comic book tag id
     * @return count of deleted tags
     */
    @Query(
        """
        DELETE FROM ${ComicTag.TABLE_NAME}
        WHERE ${ComicTag.COLUMN_ID} = :tagId
    """
    )
    suspend fun deleteById(tagId: Long): Int

    /**
     * Add or replace existed comic book tags
     * @param tags tags to insert or replace
     * @return inserted tags ids
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(vararg tags: ComicTag): List<Long>
}