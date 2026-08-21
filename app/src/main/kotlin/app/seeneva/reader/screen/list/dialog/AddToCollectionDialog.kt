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

package app.seeneva.reader.screen.list.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import app.seeneva.reader.R
import app.seeneva.reader.di.parentFragmentScope
import app.seeneva.reader.logic.entity.ComicCollection
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Allow user to select a collection where selected comic books should be added.
 *
 * The first item always allows to create a new collection
 */
class AddToCollectionDialog : DialogFragment() {
    private val callback by lazy { parentFragmentScope?.getOrNull<Callback>() }

    private val bookIds by lazy {
        requireNotNull(requireArguments().getLongArray(KEY_BOOK_IDS)) { "Use newInstance!" }
            .toHashSet()
    }

    private val collectionIds by lazy {
        requireNotNull(requireArguments().getLongArray(KEY_COLLECTION_IDS)) { "Use newInstance!" }
    }

    private val collectionNames by lazy {
        requireNotNull(requireArguments().getStringArray(KEY_COLLECTION_NAMES)) { "Use newInstance!" }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val items = arrayOf(getString(R.string.comic_list_collection_new)) + collectionNames

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.comic_list_collection_add)
            .setItems(items) { _, which ->
                if (which == NEW_COLLECTION_POSITION) {
                    callback?.onNewCollectionRequested(bookIds)
                } else {
                    callback?.onCollectionSelectedToAdd(
                        bookIds,
                        collectionIds[which - 1]
                    )
                }

                dismiss()
            }
            .setNegativeButton(R.string.all_cancel) { dialog, _ -> dialog.cancel() }
            .create()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        callback?.onAddToCollectionCanceled()
    }

    interface Callback {
        /**
         * User selected an existed collection
         * @param bookIds comic book ids to add
         * @param collectionId id of the selected collection
         */
        fun onCollectionSelectedToAdd(bookIds: Set<Long>, collectionId: Long)

        /**
         * User wants to create a new collection
         * @param bookIds comic book ids to add
         */
        fun onNewCollectionRequested(bookIds: Set<Long>)

        /**
         * User closed the dialog without any selection
         */
        fun onAddToCollectionCanceled()
    }

    companion object {
        private const val NEW_COLLECTION_POSITION = 0

        private const val KEY_BOOK_IDS = "book_ids"
        private const val KEY_COLLECTION_IDS = "collection_ids"
        private const val KEY_COLLECTION_NAMES = "collection_names"

        /**
         * @param collections all user collections
         * @param bookIds comic book ids which should be added into a collection
         */
        fun newInstance(collections: List<ComicCollection>, bookIds: Set<Long>) =
            AddToCollectionDialog().apply {
                arguments = Bundle().apply {
                    putLongArray(KEY_BOOK_IDS, bookIds.toLongArray())
                    putLongArray(
                        KEY_COLLECTION_IDS,
                        LongArray(collections.size) { collections[it].id })
                    putStringArray(
                        KEY_COLLECTION_NAMES,
                        Array(collections.size) { collections[it].name })
                }
            }
    }
}
