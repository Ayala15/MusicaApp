package com.example.musicapp.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Patterns
import com.example.musicapp.player.PlayerManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class User(val name: String, val username: String, val email: String)

data class SavedCredentials(val identifier: String, val password: String, val username: String, val email: String)

private data class StoredUser(
    val name: String,
    val username: String,
    val email: String,
    val salt: String,
    val hash: String
)

/**
 * Cuentas guardadas SOLO en este dispositivo (SharedPreferences).
 * Altamente optimizado para ejecutarse en segundo plano sin bloquear la interfaz (ANR).
 */
object AuthRepository {
    private var prefs: SharedPreferences? = null
    private val gson = Gson()

    private var cachedUsers: List<StoredUser>? = null
    private var cachedCredentials: SavedCredentials? = null
    private var isCredentialsLoaded = false

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val sessionKey = prefs?.getString("session_username", null)
            ?: prefs?.getString("session_email", null)
        val stored = sessionKey?.let { k ->
            readUsers().firstOrNull {
                it.username.equals(k, ignoreCase = true) || it.email.equals(k, ignoreCase = true)
            }
        }
        setUser(stored?.let { User(it.name, it.username, it.email) })
    }

    /** Devuelve las credenciales guardadas para auto-completado e inicio de sesión rápido. */
    fun getSavedCredentials(): SavedCredentials? {
        if (isCredentialsLoaded) return cachedCredentials
        val id = prefs?.getString("saved_identifier", null)
        val p = prefs?.getString("saved_password", null)
        val u = prefs?.getString("saved_username", id ?: "")
        val e = prefs?.getString("saved_email", "") ?: ""

        cachedCredentials = if (id != null && p != null && u != null) {
            SavedCredentials(id, p, u, e)
        } else null
        isCredentialsLoaded = true
        return cachedCredentials
    }

    fun saveCredentials(identifier: String, password: String, username: String, email: String) {
        cachedCredentials = SavedCredentials(identifier, password, username, email)
        isCredentialsLoaded = true
        prefs?.edit()?.apply {
            putString("saved_identifier", identifier)
            putString("saved_password", password)
            putString("saved_username", username)
            putString("saved_email", email)
        }?.apply()
    }

    fun clearSavedCredentials() {
        cachedCredentials = null
        isCredentialsLoaded = true
        prefs?.edit()?.apply {
            remove("saved_identifier")
            remove("saved_password")
            remove("saved_username")
            remove("saved_email")
        }?.apply()
    }

    /** Devuelve un mensaje de error, o null si todo salió bien. Ejecutado en segundo plano. */
    suspend fun register(username: String, email: String, password: String): String? = withContext(Dispatchers.Default) {
        val u = username.trim()
        val e = email.trim().lowercase()
        if (u.isEmpty()) return@withContext "Escribe tu nombre de usuario"
        if (u.contains(" ")) return@withContext "El usuario no debe contener espacios"
        if (u.length < 3) return@withContext "El usuario debe tener al menos 3 caracteres"
        if (!Patterns.EMAIL_ADDRESS.matcher(e).matches()) return@withContext "Correo no válido"
        if (password.length < 6) return@withContext "La contraseña debe tener al menos 6 caracteres"

        val users = readUsers().toMutableList()
        if (users.any { it.username.equals(u, ignoreCase = true) }) {
            return@withContext "Ese nombre de usuario ya está registrado"
        }
        if (users.any { it.email.equals(e, ignoreCase = true) }) {
            return@withContext "Ese correo ya tiene una cuenta registrada"
        }

        val saltBytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val salt = Base64.encodeToString(saltBytes, Base64.NO_WRAP)
        val hashedPassword = hash(password, salt)

        val newUser = StoredUser(u, u, e, salt, hashedPassword)
        users.add(newUser)
        writeUsers(users)

        saveCredentials(u, password, u, e)
        withContext(Dispatchers.Main) {
            setUser(User(u, u, e))
        }
        return@withContext null
    }

    suspend fun login(identifier: String, password: String, remember: Boolean = true): String? = withContext(Dispatchers.Default) {
        val id = identifier.trim().lowercase()
        if (id.isEmpty() || password.isEmpty()) return@withContext "Completa tu usuario/correo y contraseña"

        val userList = readUsers()
        val found = userList.firstOrNull {
            it.username.equals(id, ignoreCase = true) || it.email.equals(id, ignoreCase = true)
        }

        if (found == null || found.hash != hash(password, found.salt)) {
            return@withContext "Usuario o contraseña incorrectos"
        }

        if (remember) {
            saveCredentials(found.username, password, found.username, found.email)
        } else {
            clearSavedCredentials()
        }

        withContext(Dispatchers.Main) {
            setUser(User(found.name, found.username, found.email))
        }
        return@withContext null
    }

    fun logout() {
        PlayerManager.stop()
        setUser(null)
    }

    private fun setUser(u: User?) {
        prefs?.edit()?.apply {
            if (u == null) remove("session_username") else putString("session_username", u.username)
        }?.apply()
        _user.value = u
        FavoritesRepository.load(u?.username)
    }

    private fun hash(password: String, salt: String): String {
        val spec = PBEKeySpec(password.toCharArray(), Base64.decode(salt, Base64.NO_WRAP), 1000, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun readUsers(): List<StoredUser> {
        cachedUsers?.let { return it }
        return try {
            val json = prefs?.getString("users", null) ?: return emptyList()
            val list = gson.fromJson<List<Map<String, Any>>>(json, object : TypeToken<List<Map<String, Any>>>() {}.type)
            val parsed = list?.map { map ->
                val name = map["name"] as? String ?: ""
                val username = (map["username"] as? String) ?: (map["name"] as? String ?: "")
                val email = map["email"] as? String ?: ""
                val salt = map["salt"] as? String ?: ""
                val hash = map["hash"] as? String ?: ""
                StoredUser(if (name.isNotEmpty()) name else username, username, email, salt, hash)
            } ?: emptyList()
            cachedUsers = parsed
            parsed
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun writeUsers(users: List<StoredUser>) {
        cachedUsers = users
        prefs?.edit()?.putString("users", gson.toJson(users))?.apply()
    }
}
