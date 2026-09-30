package dev.ajvanegasv.kontio.presentation.util

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope

@Composable
actual fun rememberGoogleDriveAuthLauncher(
    onAccountConnected: (email: String) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope("https://www.googleapis.com/auth/drive.appdata"))
        .build()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (data == null) {
            // Cancelado por el usuario
            return@rememberLauncherForActivityResult
        }

        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            val email = account?.email
            if (!email.isNullOrBlank()) {
                onAccountConnected(email)
            } else {
                onError("No se pudo obtener el correo de la cuenta de Google.")
            }
        } catch (e: ApiException) {
            if (e.statusCode == 12501) {
                // Selección cancelada por el usuario (toque fuera o botón atrás)
                return@rememberLauncherForActivityResult
            }
            val msg = when (e.statusCode) {
                10 -> "Error 10 (Developer error): Verifica que la huella SHA-1 y el paquete coincidan en GCP."
                7 -> "Error 7: Sin conexión a internet para conectar con Google."
                12500 -> "Error 12500: Fallo en Google Play Services."
                else -> "Error de Google (${e.statusCode}): ${e.message ?: "desconocido"}"
            }
            onError(msg)
        } catch (e: Exception) {
            onError(e.message ?: "Error al vincular cuenta de Google.")
        }
    }

    return {
        val client = GoogleSignIn.getClient(context, gso)
        // Cerrar sesión previa para forzar que el sistema muestre la lista de cuentas disponibles
        client.signOut().addOnCompleteListener {
            launcher.launch(client.signInIntent)
        }
    }
}
