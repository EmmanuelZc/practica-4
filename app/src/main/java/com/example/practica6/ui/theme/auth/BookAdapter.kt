package com.example.practica6.ui.theme.auth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.practica6.R
import com.example.practica6.data.api.Book

class BookAdapter(
    private val books: List<Book>,
    private val favoriteBooks: MutableList<String>,
    private val onFavoriteToggle: (Book) -> Unit
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    inner class BookViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvBookTitle)
        val author: TextView = view.findViewById(R.id.tvBookAuthor)
        val year: TextView = view.findViewById(R.id.tvBookYear)
        val favoriteIcon: ImageView = view.findViewById(R.id.ivFavorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]
        holder.title.text = book.title ?: "Sin título"
        holder.author.text = book.author_name?.joinToString(", ") ?: "Desconocido"
        holder.year.text = book.first_publish_year?.toString() ?: "N/A"

        // Cambiar el icono según si es favorito o no
        val isFavorite = favoriteBooks.contains(book.key)
        holder.favoriteIcon.setImageResource(
            if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite
        )

        // Manejar clic en el ícono de favorito
        holder.favoriteIcon.setOnClickListener {
            onFavoriteToggle(book)
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = books.size
}
