package com.example.mock_up

import ApiClient
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class RegisterUser(
    val userId: String,
    val name: String,
    val email: String,
    val password: String,
    val token: String,
    val type: String,
)

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RegisterScreen()
        }
    }

    @Composable
    fun RegisterScreen() {
        var userId by remember { mutableStateOf("") }
        var userName by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var token by remember { mutableStateOf("") }
        var verificationCode by remember { mutableStateOf("") }
        var userIdExists by remember { mutableStateOf(false) }
        var showSuccessDialog by remember { mutableStateOf(false) }
        var cooldown by remember { mutableStateOf(0) }
        val coroutineScope = rememberCoroutineScope()
        val apiClient = ApiClient()

        // 获取 Firebase token
        LaunchedEffect(Unit) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    token = task.result ?: ""
                    Log.d("FCM Token", "Token: $token")
                } else {
                    Log.w("FCM Token", "Fetching FCM registration token failed", task.exception)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { finish() },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text("Back")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = userId,
                    onValueChange = {
                        userId = it
                        coroutineScope.launch {
                            userIdExists = checkUserIdExists(userId)
                        }
                    },
                    label = { Text("User ID") },
                    modifier = Modifier.weight(1f)
                )
                if (userIdExists) {
                    Text(
                        text = "ID already exists",
                        color = Color.Red,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    val emailRequest = email
                                   // println("qwe"+Json.encodeToString(emailRequest))

                                    val response = apiClient.POST(
                                        "http://149.248.20.141:80/user/sendEmail",
                                        Json.encodeToString(emailRequest)
                                    )

                                    if (response.contains("errorMsg")) {
                                        val json = Json {
                                            ignoreUnknownKeys = true // 忽略未知字段
                                        }
                                        val errorResponse = json.decodeFromString<ErrorResponse>(response)
                                        showErrorDialog(errorResponse.errorMsg)
                                    } else {
                                        cooldown = 60
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("POST Request", "Exception: ", e)
                            }
                        }
                    },
                    enabled = cooldown == 0
                ) {
                    if (cooldown > 0) {
                        Text("Wait $cooldown s")
                    } else {
                        Text("Send Code")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = verificationCode,
                onValueChange = { verificationCode = it },
                label = { Text("Verification Code") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                val user = RegisterUser(
                                    token = token,
                                    name = userName,
                                    userId = userId,
                                    email = email,
                                    password = password,
                                    type = "user"
                                )
                                val response = apiClient.POST(
                                    "http://149.248.20.141:80/user/create/$verificationCode",
                                    Json.encodeToString(user)
                                )
                                if (response.contains("errorMsg")) {
                                    val json = Json {
                                        ignoreUnknownKeys = true // 忽略未知字段
                                    }
                                    val errorResponse = json.decodeFromString<ErrorResponse>(response)
                                    showErrorDialog(errorResponse.errorMsg)
                                } else {
                                    withContext(Dispatchers.Main) {
                                        showSuccessDialog = true
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("POST Request", "Exception: ", e)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Register")
            }

            if (showSuccessDialog) {
                AlertDialog(
                    onDismissRequest = { showSuccessDialog = false },
                    title = { Text("Success") },
                    text = { Text("Registration successful!") },
                    confirmButton = {
                        Button(onClick = {
                            showSuccessDialog = false
                            startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                        }) {
                            Text("OK")
                        }
                    }
                )
            }
        }

        // 倒计时逻辑
        LaunchedEffect(cooldown) {
            if (cooldown > 0) {
                delay(1000L)
                cooldown--
            }
        }
    }

    private fun showErrorDialog(errorMessage: String) {
        runOnUiThread {
            android.app.AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(errorMessage)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    private suspend fun checkUserIdExists(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val apiClient = ApiClient()
                val response =
                    apiClient.GET("http://149.248.20.141:80/user/checkUserId?userId=$userId")
                if (response.contains("errorMsg")) {
                    return@withContext true
                }
                false
            } catch (e: Exception) {
                Log.e("LoginActivity", "Error during login", e)
                false
            }
        }
    }
}