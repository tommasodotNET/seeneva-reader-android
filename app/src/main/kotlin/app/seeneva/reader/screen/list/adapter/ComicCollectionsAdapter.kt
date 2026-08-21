/*
 * This file is part of Seeneva Android Reader
 * Copyright (C) 2021-2023 Sergei Solodovnikov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.seeneva.reader.screen.list.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.content.res.AppCompatResources
import app.seeneva.reader.R
import app.seeneva.reader.databinding.VhComicCollectionGridBinding
import app.seeneva.reader.databinding.VhComicCollectionListBinding
import app.seeneva.reader.logic.ComicListViewType
import app.seeneva.reader.logic.entity.ComicCollection
import app.seeneva.reader.logic.image.ImageLoader
import app.seeneva.reader.logic.image.ImageLoadingTask
import app.seeneva.reader.logic.image.target.ImageLoaderTarget
import app.seeneva.reader.widget.CollectionCoverView

class ComicCollectionsAdapter(
    private var viewType: ComicListViewType,
    private val imageLoader: ImageLoader,
    private val inflater: LayoutInflater,
    private val onClick: (ComicCollection) -> Unit
) : ListAdapter<ComicCollection, ComicCollectionsAdapter.ViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int) = when (viewType) {
        ComicListViewType.GRID -> R.layout.vh_comic_collection_grid
        ComicListViewType.LIST -> R.layout.vh_comic_collection_list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        when (viewType) {
            R.layout.vh_comic_collection_grid -> {
                val binding = VhComicCollectionGridBinding.inflate(inflater, parent, false)
                ViewHolder(binding.root, binding.cover, binding.title, imageLoader, onClick)
            }
            R.layout.vh_comic_collection_list -> {
                val binding = VhComicCollectionListBinding.inflate(inflater, parent, false)
                ViewHolder(binding.root, binding.cover, binding.title, imageLoader, onClick)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.loadCover()
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.recycle()
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    fun setViewType(newViewType: ComicListViewType) {
        if (viewType == newViewType) return

        viewType = newViewType
        notifyItemRangeChanged(0, itemCount)
    }

    class ViewHolder(
        itemView: android.view.View,
        private val coverView: CollectionCoverView,
        private val title: android.widget.TextView,
        private val imageLoader: ImageLoader,
        private val onClick: (ComicCollection) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private lateinit var collection: ComicCollection
        private var imageLoadingTask: ImageLoadingTask? = null
        private var requestedCover: ComicCollection.Cover? = null
        private val placeholder by lazy {
            AppCompatResources.getDrawable(itemView.context, R.drawable.collection_cover_placeholder)
        }

        init {
            itemView.setOnClickListener { onClick(collection) }
        }

        fun bind(value: ComicCollection) {
            val coverChanged = !::collection.isInitialized || collection.cover != value.cover
            recycle()
            collection = value
            title.text = value.name

            if (coverChanged) {
                requestedCover = null
                coverView.setImageDrawable(placeholder)
            }

            if (itemView.isAttachedToWindow &&
                (coverChanged || coverView.loadState !is ImageLoaderTarget.State.Success)
            ) {
                loadCover()
            }
        }

        fun loadCover() {
            val cover = collection.cover
            if (cover == requestedCover &&
                coverView.loadState is ImageLoaderTarget.State.Success
            ) {
                return
            }

            recycle()
            if (cover == null) {
                requestedCover = null
                coverView.setImageDrawable(placeholder)
            } else {
                requestedCover = cover
                imageLoadingTask = imageLoader.pageThumbnail(
                    cover.path,
                    cover.position,
                    coverView,
                    placeholder
                )
            }
        }

        fun recycle() {
            imageLoadingTask?.dispose()
            imageLoadingTask = null
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ComicCollection>() {
        override fun areItemsTheSame(oldItem: ComicCollection, newItem: ComicCollection) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ComicCollection, newItem: ComicCollection) =
            oldItem == newItem
    }
}
