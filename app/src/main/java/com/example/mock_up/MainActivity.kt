package com.example.mock_up

import ApiClient
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import android.Manifest
import com.example.mock_up.UserSession.userId
import com.example.mock_up.UserSession.userName
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

@Serializable
data class ChatroomResponse(
    val data: List<Chatroom>,
    val success: Boolean,
    val errorMsg: String
)


@Serializable
data class Chatroom(val id: Int, val name: String)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val apiClient = ApiClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        requestNotificationPermission()
        super.onCreate(savedInstanceState)
        checkGooglePlayServices()
        setContent {
            MainScreen()
        }
    }

    override fun onResume() {
        super.onResume()
        requestNotificationPermission()
        checkGooglePlayServices()
    }

    @Preview(showBackground = true)
    @Composable
    fun MainScreen() {
        var chatrooms by remember { mutableStateOf(listOf<Chatroom>()) }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            coroutineScope.launch(Dispatchers.IO) {
                val jsonResponse = apiClient.GET("http://192.168.0.51:5722/get_chatrooms")
                if (jsonResponse.isNotEmpty()) {
                    val response = Json.decodeFromString<ChatroomResponse>(jsonResponse)
                    withContext(Dispatchers.Main) {
                        chatrooms = response.data
                    }
                } else {
                    Log.e("MainScreen", "No data received from server")
                }
            }
        }

        Scaffold(
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("$userName") // 显示用户ID
                                Text("Chatroom")
                                Button(onClick = {
                                    val intent = Intent(this@MainActivity, TaskListActivity::class.java)
                                    startActivity(intent)
                                }) {
                                    Text("Tasklist") // Tasklist按钮
                                }
                            }
                        },
                    )
                    HorizontalDivider(color = Color.Black, thickness = 1.dp)
                }
            }
        ){ innerPadding ->
            ChatRoomList(
                chatrooms = chatrooms,
                onChatroomClick = { chatroom ->
                    val intent = Intent(this@MainActivity, ChatActivity::class.java)
                    intent.putExtra("chatroom_id", chatroom.id)
                    intent.putExtra("chatroom_name", chatroom.name)
                    startActivity(intent)
                },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
    private fun checkGooglePlayServices(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, 9000)?.show()
            } else {
                Log.e("MainActivity", "This device is not supported.")
            }
            return false
        }
        return true
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    0
                )
            }
            // The code below is related to create notification channel for later use
//            val channel = NotificationChannel("MyNotification","MyNotification",
//                NotificationManager.IMPORTANCE_DEFAULT)
//            val manager = getSystemService(NotificationManager::class.java) as
//                    NotificationManager;
//            manager.createNotificationChannel(channel)

        }
    }

}


@Composable
fun ChatRoomList(
    chatrooms: List<Chatroom>,
    onChatroomClick: (Chatroom) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(chatrooms) { chatroom ->
            OutlinedButton(
                onClick = { onChatroomClick(chatroom) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .height(60.dp),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Black
                ),
                border = BorderStroke(1.dp, Color.Gray)
            ) {
                Text(text = chatroom.name, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}


