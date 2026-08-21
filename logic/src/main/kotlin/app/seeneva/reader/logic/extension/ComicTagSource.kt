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

package app.seeneva.reader.logic.extension

import android.content.Context
import app.seeneva.reader.data.entity.ComicTag
import app.seeneva.reader.data.source.local.db.dao.ComicTagSource
import app.seeneva.reader.logic.entity.TagType

/**
 * Check is Tag type hardcoded. Throw exception otherwise
 */
private fun TagType.requireHardcoded() {
    require(hardcoded) { "Incorrect hardcoded tag type: $this" }
}

/**
 * @return hardcoded tag id or null if it wasn't created
 */
internal suspend fun ComicTagSource.getHardcodedTagId(type: TagType): Long? {
    type.requireHardcoded()

    return findByType(type.ordinal)?.id
}

/**
 * @return hardcoded tag or null if it wasn't created
 */
internal suspend fun ComicTagSource.getHardcodedTag(context: Context, type: TagType): ComicTag? {
    type.requireHardcoded()

    //fix tag name for hardcoded types. It allow user to change device locale
    return findByType(type.ordinal)?.let {
        it.copy(name = it.humanName(context))
    }
}

/**
 * Get or create hardcoded tag id by it [type]
 * @param type type of the hardcoded comic book tag
 * @return tag id
 */
internal suspend fun ComicTagSource.getOrCreateHardcodedTagId(type: TagType): Long {
    return getHardcodedTagId(type) ?: insertOrReplace(type.newHardcodedTag()).first()
}

/**
 * Get or create hardcoded tag by it [type]
 * @param type type of the hardcoded comic book tag
 * @return tag
 */
internal suspend fun ComicTagSource.getOrCreateHardcodedTag(
    context: Context,
    type: TagType
): ComicTag {
    return getHardcodedTag(context, type)
        ?: type.newHardcodedTag().let { it.copy(id = insertOrReplace(it).first()) }
}

/**
 * @return all user created tags sorted by name (case insensitive)
 */
internal suspend fun ComicTagSource.getUserTags(): List<ComicTag> =
    findAllByType(TagType.TYPE_USER.ordinal)

/**
 * Try to find a user tag by it [name]. Name comparison is case insensitive
 * @param name name of the user tag
 * @return user tag or null if it doesn't exist
 */
internal suspend fun ComicTagSource.findUserTagByName(name: String): ComicTag? =
    findByTypeAndName(TagType.TYPE_USER.ordinal, name)

/**
 * Get user tag by it [name] or create a new one. Name comparison is case insensitive,
 * so it is not possible to have two user tags with case insensitively equal names
 * @param name name of the user tag. Should not be blank
 * @return existed or newly created user tag
 */
internal suspend fun ComicTagSource.getOrCreateUserTag(name: String): ComicTag {
    require(name.isNotBlank()) { "User tag name cannot be blank" }

    return findUserTagByName(name)
        ?: ComicTag(0, name, TagType.TYPE_USER.ordinal)
            .let { it.copy(id = insertOrReplace(it).first()) }
}