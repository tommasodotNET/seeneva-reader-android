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

package app.seeneva.reader.screen.list.entity

import app.seeneva.reader.logic.entity.query.filter.FilterGroup

/**
 * Label of an applied comic book list filter
 * @param id id of the label
 * @param title human readable label title
 */
data class FilterLabel(val id: Id, val title: String) {
    /**
     * Id of a [FilterLabel]
     */
    sealed interface Id {
        /**
         * Label of a filter from a filter group
         * @param groupId id of the filter group
         */
        data class Group(val groupId: FilterGroup.ID) : Id

        /**
         * Label of the currently active comic book collection
         */
        object Collection : Id
    }
}