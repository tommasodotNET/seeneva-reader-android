/*
 * This file is part of Seeneva Android Reader
 * Copyright (C) 2021-2023 Sergei Solodovnikov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.seeneva.reader.widget

import android.content.Context
import android.util.AttributeSet
import app.seeneva.reader.logic.image.entity.DrawablePalette
import app.seeneva.reader.logic.image.target.ImageLoaderTarget

class CollectionCoverView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ComicCoverView(context, attrs, defStyleAttr), ImageLoaderTarget<DrawablePalette> {
    var loadState: ImageLoaderTarget.State<DrawablePalette> = ImageLoaderTarget.State.Clear
        private set

    override fun onImageLoadStateChanged(state: ImageLoaderTarget.State<DrawablePalette>) {
        if (state !is ImageLoaderTarget.State.Clear) {
            loadState = state
        }

        setImageDrawable(
            when (state) {
                is ImageLoaderTarget.State.Success -> state.result.drawable
                is ImageLoaderTarget.State.Error -> state.placeholder
                is ImageLoaderTarget.State.Loading -> state.placeholder
                // Binding installs the next placeholder before loading. Keep it when
                // a recycled request sends a delayed clear callback.
                ImageLoaderTarget.State.Clear -> drawable
            }
        )
    }
}
