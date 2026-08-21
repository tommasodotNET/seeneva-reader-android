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
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.getSystemService
import androidx.fragment.app.DialogFragment
import app.seeneva.reader.R
import app.seeneva.reader.di.parentFragmentScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout

/**
 * Allow user to type a name of a new comic book collection.
 *
 * Collection with a case insensitively equal name will be reused instead of creating a duplicate
 */
class NewCollectionDialog : DialogFragment() {
    @Suppress("InflateParams")
    private val nameInput by lazy {
        layoutInflater.inflate(R.layout.dialog_comic_collection_name, null) as TextInputLayout
    }

    private val nameEditText
        get() = nameInput.editText!!

    private val canFinish: Boolean
        get() = dialog.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled

    private val imm by lazy { requireContext().getSystemService<InputMethodManager>()!! }

    private val callback by lazy { parentFragmentScope?.getOrNull<Callback>() }

    private val bookIds by lazy {
        requireNotNull(requireArguments().getLongArray(KEY_BOOK_IDS)) { "Use newInstance!" }
            .toHashSet()
    }

    private val clickListener = DialogInterface.OnClickListener { _, which ->
        when (which) {
            DialogInterface.BUTTON_POSITIVE -> onFinish()
            DialogInterface.BUTTON_NEGATIVE -> dialog.cancel()
        }
    }

    private val textChangeListener = object : TextWatcher {
        override fun afterTextChanged(s: Editable) {
            onNameChanged(s)
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        nameEditText.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    onFinish()
                    true
                }

                else -> false
            }
        }

        savedInstanceState?.getCharSequence(STATE_NAME)?.also { nameEditText.setText(it) }

        nameEditText.post {
            if (nameEditText.requestFocus()) {
                nameEditText.setSelection(nameEditText.length())
                imm.showSoftInput(nameEditText, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.comic_list_collection_new_title)
            .setPositiveButton(R.string.all_ok, clickListener)
            .setNegativeButton(R.string.all_cancel, clickListener)
            .setView(nameInput)
            .setCancelable(false)
            .create()
            .apply {
                setCanceledOnTouchOutside(false)

                setOnShowListener {
                    onNameChanged(nameEditText.text)

                    nameEditText.addTextChangedListener(textChangeListener)
                }
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putCharSequence(STATE_NAME, nameEditText.text)
    }

    override fun getDialog(): AlertDialog = super.getDialog() as AlertDialog

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        callback?.onNewCollectionCanceled()
    }

    private fun onFinish() {
        if (canFinish) {
            callback?.onNewCollectionNamed(bookIds, nameEditText.text.toString())
            dismiss()
        }
    }

    private fun onNameChanged(name: CharSequence) {
        if (name.isBlank()) {
            nameInput.error = getString(R.string.comic_list_collection_err_empty_name)
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = false
        } else {
            nameInput.error = null
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = true
        }
    }

    interface Callback {
        /**
         * User typed a name of a new collection
         * @param bookIds comic book ids which should be added into the collection
         * @param name typed collection name
         */
        fun onNewCollectionNamed(bookIds: Set<Long>, name: String)

        /**
         * User closed the dialog without typing a name
         */
        fun onNewCollectionCanceled()
    }

    companion object {
        private const val STATE_NAME = "collection_name"
        private const val KEY_BOOK_IDS = "book_ids"

        /**
         * @param bookIds comic book ids which should be added into a new collection
         */
        fun newInstance(bookIds: Set<Long>) =
            NewCollectionDialog().apply {
                arguments = Bundle().apply { putLongArray(KEY_BOOK_IDS, bookIds.toLongArray()) }
            }
    }
}
