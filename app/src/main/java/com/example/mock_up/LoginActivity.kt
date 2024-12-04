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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import android.app.AlertDialog
import androidx.compose.ui.Alignment
import kotlinx.serialization.Serializable

@kotlinx.serialization.Serializable
data class LoginRequest(
    val userId: String,
    val password: String,
)
@kotlinx.serialization.Serializable
data class LoginResponse(val data: User)
@kotlinx.serialization.Serializable
data class User(
    val userId: String,
    val name: String,
    val email:String,
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
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally // 居中对齐

        ) {
            Text(
                text = "TaskTalk",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = MaterialTheme.typography.headlineLarge.fontSize * 2), // 使用大号字体
                modifier = Modifier.padding(bottom = 50.dp)
            )
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
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val success = login(userId, password)
                            if (success) {
                                UserSession.userId = userId
                                UserSession.userName = UserName
                                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                finish()
                            } else {
                                Log.e("LoginScreen", "Login failed")
                            }
                        }
                    }
                ) {
                    Text("Login")
                }
                Button(
                    onClick = {
                        startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
                    }
                ) {
                    Text("Register")
                }
            }
        }
    }
    private suspend fun login(userId: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val loginRequest = LoginRequest(
                    password = password,
                    userId = userId,
                )
                val json = Json {
                    ignoreUnknownKeys = true // 忽略未知字段
                }
                val apiClient = ApiClient()
                val response = apiClient.POST("http://149.248.20.141:80/user/login", Json.encodeToString(loginRequest))


                if (response.contains("errorMsg")) {
                    withContext(Dispatchers.Main) {
                        val errorResponse = json.decodeFromString<ErrorResponse>(response)
                        showErrorDialog(errorResponse.errorMsg)
                    }
                    return@withContext false
                } else {
                    val loginResponse = json.decodeFromString<LoginResponse>(response)
                    UserName = loginResponse.data.name
                    UserSession.userEmail = loginResponse.data.email
true
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Error during login", e)
                false
            }
        }
    }

    private fun showErrorDialog(errorMessage: String) {
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(errorMessage)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
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
@Serializable
data class ErrorResponse(
    val success:Boolean,
    val errorMsg:String,
)
