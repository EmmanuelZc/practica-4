package com.example.practica6

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModelProvider
import com.example.practica6.data.api.RetrofitClient
import com.example.practica6.data.models.RolLocal
import com.example.practica6.data.models.UserLocal
import com.example.practica6.data.models.UserRoleCrossRef
import com.example.practica6.sync.UserRepository
import com.example.practica6.ui.theme.auth.AuthViewModel
import com.example.practica6.ui.theme.auth.AuthViewModelFactory
import com.example.practica6.ui.theme.profile.ProfileActivity
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.appevents.AppEventsLogger
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.delay
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    private lateinit var callbackManager: CallbackManager
    private lateinit var sessionManager: UserSessionManager
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = UserSessionManager(applicationContext)

        val apiService = RetrofitClient.instance
        val userDao = AppDatabase.getInstance(applicationContext).userDao()
        val userRepository = UserRepository(apiService, userDao, applicationContext)

        authViewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(userRepository, sessionManager)
        ).get(AuthViewModel::class.java)

        observeViewModel()
        synchronizeData()
        setupSocialLogins()

        setContent {
            AppContent()
        }
    }

    private fun observeViewModel() {
        authViewModel.errorMessage.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        }

        authViewModel.syncStatus.observe(this) { isSynced ->
            if (isSynced) {
                Toast.makeText(this, "Sincronización completada.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Sin conexión. Se usaron datos locales.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun synchronizeData() {
        authViewModel.syncUsersFromServer()
        authViewModel.syncPendingUsers()
        authViewModel.getAllLocalUsers()
        authViewModel.synchronizeData()
        authViewModel.syncFavorites()
    }

    private fun setupSocialLogins() {
        // Facebook Login Setup
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(application)
        callbackManager = CallbackManager.Factory.create()

        LoginManager.getInstance().registerCallback(callbackManager, object :
            FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                val accessToken = result.accessToken.token

                val request = GraphRequest.newMeRequest(result.accessToken) { jsonObject, _ ->
                    try {
                        // Obtener datos del usuario
                        val id = jsonObject?.optString("id") ?: UUID.randomUUID().toString()
                        val name = jsonObject?.optString("name", "Usuario de Facebook")
                        val email = jsonObject?.optString("email", "No email")

                        // Crear un objeto UserLocal
                        val userLocal = UserLocal(
                            id = id.hashCode(),
                            nombre = name ?: "Usuario de Facebook",
                            apaterno = "",
                            amaterno = "",
                            cumple = "",
                            username = email ?: "No email",
                            password = "", // No se almacena contraseña para Facebook
                            enabled = true
                        )

                        // Inserción en la base de datos local
                        val rolesLocal = listOf(RolLocal(2, "ROLE_USER"))
                        val crossRefs = rolesLocal.map { role -> UserRoleCrossRef(userLocal.id, role.id) }
                        authViewModel.insertUserWithRoles(userLocal, rolesLocal, crossRefs)

                        // Guardar la sesión del usuario
                        sessionManager.saveSession(
                            username = name ?: "Usuario de Facebook",
                            userId = id.hashCode(),
                            authMethod = "facebook"
                        )

                        // Navegar al perfil
                        navigateToProfile()

                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Error al procesar datos de Facebook: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                // Configuración de parámetros para obtener datos del usuario
                val parameters = Bundle()
                parameters.putString("fields", "id,name,email")
                request.parameters = parameters
                request.executeAsync()
            }

            override fun onCancel() {
                Toast.makeText(this@MainActivity, "Inicio de sesión cancelado", Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: FacebookException) {
                Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Google Sign-In Setup
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("742083522944-a536fac6ut8ge78uaudpt5e50q34o12u.apps.googleusercontent.com") // Cambia TU_CLIENTE_WEB_ID por tu ID de cliente web
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                handleGoogleSignInResult(account)
            } catch (e: ApiException) {
                Toast.makeText(this, "Error en Google Sign-In: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Composable
    fun AppContent() {
        val isUserLoggedIn = remember { mutableStateOf(sessionManager.getUsername() != null) }
        var showSplash by rememberSaveable { mutableStateOf(true) }

        if (showSplash) {
            SplashScreen {
                showSplash = false
            }
        } else {
            if (isUserLoggedIn.value) {
                NavigateToProfile()
            } else {
                LoginScreen(
                    onGoogleSignInClick = { signInWithGoogle() },
                    onFacebookSignInClick = { signInWithFacebook() },
                    onLoginSuccess = { username, userId ->
                        sessionManager.saveSession(username, userId as? Int ?: -1)
                        isUserLoggedIn.value = true
                    }
                )
            }
        }
    }

    @Composable
    fun NavigateToProfile() {
        LaunchedEffect(Unit) {
            val userId = sessionManager.getUserId()
            if (userId != -1) {
                val intent = Intent(this@MainActivity, ProfileActivity::class.java).apply {
                    putExtra("USER_ID", userId)
                }
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this@MainActivity, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Composable
    fun SplashScreen(onTimeout: () -> Unit) {
        var startAnimation by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            startAnimation = true
            delay(2000)
            onTimeout()
        }

        val scale by animateFloatAsState(
            targetValue = if (startAnimation) 5f else 0.5f,
            animationSpec = tween(durationMillis = 1000)
        )
        val alpha by animateFloatAsState(
            targetValue = if (startAnimation) 1f else 0f,
            animationSpec = tween(durationMillis = 1000)
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.portatil),
                contentDescription = "Splash Logo",
                modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
            )
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleGoogleSignInResult(account: GoogleSignInAccount?) {
        account?.let {
            val userId = it.id ?: UUID.randomUUID().toString()
            val displayName = it.displayName ?: "GoogleUser"
            val idToken = it.idToken

            if (!idToken.isNullOrEmpty()) {
                sessionManager.saveSession(
                    username = displayName,
                    userId = userId.hashCode(),
                    authMethod = "google"
                )
                Toast.makeText(this, "Inicio de sesión exitoso: $displayName", Toast.LENGTH_SHORT).show()
                navigateToProfile()
            } else {
                Toast.makeText(this, "Error: Token de ID no encontrado", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Error al iniciar sesión con Google.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInWithFacebook() {
        LoginManager.getInstance().logInWithReadPermissions(
            this,
            listOf("public_profile", "email")
        )
    }

    private fun navigateToProfile() {
        val intent = Intent(this, ProfileActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }
}
