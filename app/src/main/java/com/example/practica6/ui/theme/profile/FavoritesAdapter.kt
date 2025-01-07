package com.example.practica6.ui.theme.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.practica6.R
import com.example.practica6.data.models.FavoriteRemote

class FavoritesAdapter(private val favoriteList: List<FavoriteRemote>) :
    RecyclerView.Adapter<FavoritesAdapter.FavoritesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoritesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false)
        return FavoritesViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoritesViewHolder, position: Int) {
        val favorite = favoriteList[position]
        holder.titleView.text = favorite.title ?: "Título desconocido"
        holder.authorView.text = favorite.author ?: "Autor desconocido"
        holder.yearView.text = favorite.publishYear?.toString() ?: "Año desconocido"
    }

    override fun getItemCount(): Int = favoriteList.size

    class FavoritesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleView: TextView = view.findViewById(R.id.tvBookTitle)
        val authorView: TextView = view.findViewById(R.id.tvBookAuthor)
        val yearView: TextView = view.findViewById(R.id.tvBookYear)
    }
}
