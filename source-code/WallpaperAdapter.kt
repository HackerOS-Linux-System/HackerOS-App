package com.hackeros.hackeros-apl

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import pl.hackeros.app.databinding.ItemWallpaperBinding

class WallpaperAdapter(
    private val urls: List<String>,
    private val activity: MainActivity
) : RecyclerView.Adapter<WallpaperAdapter.WallpaperViewHolder>() {

    class WallpaperViewHolder(val binding: ItemWallpaperBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WallpaperViewHolder {
        val binding = ItemWallpaperBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WallpaperViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WallpaperViewHolder, position: Int) {
        val url = urls[position]

        Picasso.get()
            .load(url)
            .fit()
            .centerCrop()
            .into(holder.binding.imageWallpaper)

        holder.binding.btnDownload.setOnClickListener {
            activity.downloadImage(url)
        }
    }

    override fun getItemCount(): Int = urls.size
}
