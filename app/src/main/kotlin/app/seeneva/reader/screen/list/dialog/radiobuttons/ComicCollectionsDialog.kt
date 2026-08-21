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

package app.seeneva.reader.screen.list.dialog.radiobuttons

import android.os.Bundle
import app.seeneva.reader.R
import app.seeneva.reader.di.parentFragmentScope
import app.seeneva.reader.logic.entity.ComicCollection

/**
 * Allow user to filter comic book list by a collection.
 *
 * Shows all user collections and a special 'All books' value
 */
class ComicCollectionsDialog : RadioButtonDialog<Long, ComicCollection>() {
    /**
     * 'All books' value is always the first one
     */
    override val values by lazy {
        val arguments = requireArguments()

        val ids = requireNotNull(arguments.getLongArray(KEY_COLLECTION_IDS)) { "Use newInstance!" }
        val names =
            requireNotNull(arguments.getStringArray(KEY_COLLECTION_NAMES)) { "Use newInstance!" }

        (arrayOf(ComicCollection(ALL_BOOKS_ID, getString(R.string.comic_list_collections_all))) +
                Array(ids.size) { ComicCollection(ids[it], names[it]) })
    }

    private val callback by lazy { parentFragmentScope?.getOrNull<Callback>() }

    override fun putKey(bundle: Bundle, key: Long) {
        bundle.putLong(KEY_SELECTED_COLLECTION, key)
    }

    override fun getKey(bundle: Bundle) = bundle.getLong(KEY_SELECTED_COLLECTION, ALL_BOOKS_ID)

    override fun valueKey(value: ComicCollection) = value.id

    override fun valueTitle(value: ComicCollection) = value.name

    override fun onValueCheck(value: ComicCollection) {
        callback?.onCollectionChecked(this, value.id.takeIf { it != ALL_BOOKS_ID })
    }

    interface Callback {
        /**
         * Comic book collection was checked
         * @param dialog
         * @param collectionId id of the checked collection or null if all comic books requested
         */
        fun onCollectionChecked(dialog: ComicCollectionsDialog, collectionId: Long?)
    }

    companion object {
        /**
         * Id of the special 'All books' value. Real collection ids are always positive
         */
        private const val ALL_BOOKS_ID = -1L

        private const val KEY_COLLECTION_IDS = "collection_ids"
        private const val KEY_COLLECTION_NAMES = "collection_names"
        private const val KEY_SELECTED_COLLECTION = "selected_collection"

        /**
         * @param collections all user collections
         * @param selectedId id of the currently active collection or null
         */
        fun newInstance(collections: List<ComicCollection>, selectedId: Long?) =
            ComicCollectionsDialog().apply {
                arguments = Bundle().apply {
                    putLongArray(
                        KEY_COLLECTION_IDS,
                        LongArray(collections.size) { collections[it].id })
                    putStringArray(
                        KEY_COLLECTION_NAMES,
                        Array(collections.size) { collections[it].name })
                    putLong(KEY_SELECTED_COLLECTION, selectedId ?: ALL_BOOKS_ID)
                }
            }
    }
}
