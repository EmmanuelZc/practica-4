package com.example.practica6.ui.theme.admin


import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.practica6.R
import com.example.practica6.data.models.User
import com.example.practica6.ui.theme.auth.EditUserActivity

class UserListAdapter(
    private val context: Context,
    private val users: List<User>,
    private val deleteUserCallback: (String) -> Unit
) : BaseAdapter() {

    override fun getCount(): Int {
        return users.size
    }

    override fun getItem(position: Int): Any {
        return users[position]
    }

    override fun getItemId(position: Int): Long {
        return users[position].id.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.user_list_item, parent, false)
        val user = getItem(position) as User
        val usernameView = view.findViewById<TextView>(R.id.item_username)
        val modifyButton = view.findViewById<Button>(R.id.btnModificarUsuario)
        val deleteButton = view.findViewById<Button>(R.id.btnEliminarUsuario)
        usernameView.text = user.username

        modifyButton.setOnClickListener {
            val intent = Intent(context, EditUserActivity::class.java).apply {
                putExtra("USERNAME", user.username)
            }
            context.startActivity(intent)
        }

        deleteButton.setOnClickListener { // Mostrar alerta de confirmación
            AlertDialog.Builder(context)
                .setTitle("Confirmar eliminación")
                .setMessage("¿Estás seguro de que quieres eliminar al usuario ${user.username}?")
                .setPositiveButton("Eliminar") 	{ _, _ ->
                    deleteUserCallback(user.username) }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        return view
    }
}
