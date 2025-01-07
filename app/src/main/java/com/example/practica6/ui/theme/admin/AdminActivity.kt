package com.example.practica6.ui.theme.admin


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.practica6.R
import com.example.practica6.data.models.User
import com.example.practica6.data.models.toRemote
import com.example.practica6.ui.theme.auth.AuthViewModel
import com.example.practica6.ui.theme.auth.EditUserActivity
import com.example.practica6.ui.theme.auth.LoginActivity
import com.example.practica6.ui.theme.auth.RegisterActivity


class AdminActivity : ComponentActivity() {
    private lateinit var viewModel: AuthViewModel
    private lateinit var userListView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_activity)

        viewModel = ViewModelProvider(this).get(AuthViewModel::class.java)

        userListView = findViewById(R.id.userListView)
        val addUserButton: Button = findViewById(R.id.btnAgregarUsuario)
        val logoutButton: Button = findViewById(R.id.btnCerrarSesion)

        loadUserList()

        addUserButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivityForResult(intent, REGISTER_USER_REQUEST)
        }

        userListView.setOnItemClickListener { _, _, position, _ ->
            val selectedUser = userListView.adapter.getItem(position) as User
            val intent = Intent(this, EditUserActivity::class.java).apply {
                putExtra("USERNAME", selectedUser.username)
            }
            startActivityForResult(intent, EDIT_USER_REQUEST)
        }

        userListView.setOnItemLongClickListener { _, _, position, _ ->
            val selectedUser = userListView.adapter.getItem(position) as User
            viewModel.deleteUser(selectedUser.username)
            true
        }

        viewModel.deleteStatus.observe(this) { status ->
            if (status == 204) {
                Toast.makeText(this, "Usuario eliminado correctamente", Toast.LENGTH_LONG).show()
                loadUserList() // Recarga la lista de usuarios después de la eliminación
            } else {
                Toast.makeText(this, "Error al eliminar usuario", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.updateStatus.observe(this) { status ->
            if (status == 200) {
                Toast.makeText(this, "Usuario actualizado correctamente", Toast.LENGTH_LONG).show()
                loadUserList() // Recarga la lista de usuarios después de la actualización
            } else {
                Toast.makeText(this, "Error al actualizar usuario", Toast.LENGTH_SHORT).show()
            }
        }

        logoutButton.setOnClickListener {
            // Realiza cualquier acción adicional de cierre de sesión aquí, como limpiar datos del usuario

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish() // Finaliza la actividad actual
        }
    }

    private fun loadUserList() {
        viewModel.localUsersLiveData.observe(this) { userList ->
            if (userList != null && userList.isNotEmpty()) {
                val adapter = UserListAdapter(this, userList.map { it.toRemote() }) { username ->
                    viewModel.deleteUser(username)
                }
                userListView.adapter = adapter
            } else {
                Toast.makeText(this, "No se encontraron usuarios para mostrar.", Toast.LENGTH_SHORT).show()
            }
        }

        // Llama al método para obtener usuarios locales desde ViewModel
        viewModel.getAllLocalUsers()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == REGISTER_USER_REQUEST || requestCode == EDIT_USER_REQUEST) && resultCode == RESULT_OK) {
            loadUserList()
        }else if (requestCode == EDIT_USER_REQUEST && resultCode == RESULT_OK) {
            loadUserList()
        }
    }

    companion object {
        const val REGISTER_USER_REQUEST = 1
        const val EDIT_USER_REQUEST = 2
    }
}
