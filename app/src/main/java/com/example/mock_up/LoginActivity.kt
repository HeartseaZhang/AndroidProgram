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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.mock_up.ChatActivity.MessageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@kotlinx.serialization.Serializable
data class loginRequest(
    val userId: String,
    val password: String,
)
@kotlinx.serialization.Serializable
data class LoginResponse(val data: User)
@kotlinx.serialization.Serializable
data class User(
    val userId: String,
    val name: String,
)
var UserName: String? =null
class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginScreen()
        }
    }
    @Composable
    fun LoginScreen() {
        var userId by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        val coroutineScope = rememberCoroutineScope()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            TextField(
                value = userId,
                onValueChange = { userId = it },
                label = { Text("UserId") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        val success = login(userId, password)
                        if (success) {
                            UserSession.userId = userId
                            UserSession.userName= UserName
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        } else {
                            Log.e("LoginScreen", "Login failed")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }
        }
    }
    private suspend fun login(userId: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val loginRequest = loginRequest(
                    password = password,
                    userId = userId,
                )
                val json = Json {
                    ignoreUnknownKeys = true // 忽略未知字段
                }
                val apiClient = ApiClient()
                val response = apiClient.POST("http://192.168.0.51:5722/user/login", Json.encodeToString(loginRequest))
                UserName=json.decodeFromString<LoginResponse>(response).data.name
                println("asdjkl"+UserName)
                response.contains("success") // 假设成功响应中包含 "success"

            } catch (e: Exception) {
                Log.e("LoginActivity", "Error during login", e)
                false
            }
        }
    }
}

class PasswordVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val transformedText = buildString {
            if (text.isNotEmpty()) {
                for (i in 0 until text.length - 1) {
                    append('*')
                }
                append(text.last())
            }
        }
        return TransformedText(AnnotatedString(transformedText), OffsetMapping.Identity)
    }
}