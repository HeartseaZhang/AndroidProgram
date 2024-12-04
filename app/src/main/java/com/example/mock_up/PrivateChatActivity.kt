package com.example.mock_up
import ApiClient
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import com.example.mock_up.ChatActivity.MessageResponse
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import java.text.SimpleDateFormat
import java.util.*
import com.example.mock_up.UserSession.userId
import com.example.mock_up.UserSession.userName
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
class PrivateChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkUserSession()
        try {
            chatroomId = intent.getIntExtra("chatroom_id", -1)
            val chatroomName = intent.getStringExtra("chatroom_name") ?: "Chatroom"

            Log.d("PrivateChatActivity", "Starting with chatroomId: $chatroomId, chatroomName: $chatroomName")

            if (chatroomId == -1) {
                Log.e("PrivateChatActivity", "Invalid chatroom ID received")
                // 处理无效的chatroomId
                finish()
                return
            }

            setContent {
                PrivateChatScreen(chatroomId, chatroomName)
            }
        } catch (e: Exception) {
            Log.e("PrivateChatActivity", "Error in onCreate", e)
            finish()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PrivateChatScreen(chatroomId: Int,chatroomName: String) {

        var messageText by remember { mutableStateOf("") }
        val messages = remember { mutableStateListOf<Message>() }
        val coroutineScope = rememberCoroutineScope()
        val apiClient = ApiClient()
        var showDialog by remember { mutableStateOf(false) }
        var taskTitle by remember { mutableStateOf("") }
        var taskDescription by remember { mutableStateOf("") }

        fun loadMessages() {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    Log.d("PrivateChatActivity", "Starting to load messages for chatroom: $chatroomId")

                    val jsonResponse = apiClient.GET("http://149.248.20.141:80/get_messages?chatroom_id=$chatroomId")
                    Log.d("PrivateChatActivity", "Received response: $jsonResponse")

                    if (jsonResponse.isNotEmpty()) {
                        try {
                            val json = Json {
                                ignoreUnknownKeys = true
                            }
                            val response = json.decodeFromString<MessageResponse>(jsonResponse)

                            withContext(Dispatchers.Main) {
                                messages.clear()
                                messages.addAll(response.data.messages.sortedByDescending { message ->
                                    try {
                                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(message.message_time)
                                    } catch (e: Exception) {
                                        Log.e("PrivateChatActivity", "Error parsing date: ${message.message_time}", e)
                                        Date()
                                    }
                                })
                                Log.d("PrivateChatActivity", "Successfully loaded ${messages.size} messages")
                            }
                        } catch (e: Exception) {
                            Log.e("PrivateChatActivity", "Error parsing JSON response", e)
                        }
                    } else {
                        Log.w("PrivateChatActivity", "Received empty response from server")
                    }
                } catch (e: Exception) {
                    Log.e("PrivateChatActivity", "Error loading messages", e)
                }
            }
        }

        LaunchedEffect(chatroomId) {
            loadMessages()
        }


        Scaffold(
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = { Text(chatroomName) },
                        navigationIcon = {
                            Button(onClick = {
                                finish()
                            }) {
                                Text("Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                loadMessages()
                            }){
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }
                    )
                    HorizontalDivider(color = Color.Black, thickness = 1.dp)
                }

            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    reverseLayout = true
                ) {

                    items(messages) { message ->
                        MessageItem(message)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BasicTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f).padding(8.dp).background(Color.LightGray),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (messageText.isNotEmpty()) {
                                    messageText = ""
                                    loadMessages()
                                }
                            }
                        )
                    )
                    Button(onClick = {
                        if (messageText.isNotEmpty()) {

                            val sendMessageRequest = SendMessageRequest(
                                message = messageText,
                                name = USER_NAME,
                                user_id = USER_ID,
                                chatroom_id = chatroomId
                            )

                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val postResponse = apiClient.POST("http://149.248.20.141:80/send_message",
                                        Json.encodeToString(sendMessageRequest))
                                    withContext(Dispatchers.Main) {
                                        if (postResponse.contains("ERROR")) {
                                            Log.e("POST Request", "Failed to send message: $postResponse")
                                        } else {
                                            loadMessages()
                                            messageText = ""
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("POST Request", "Exception: ", e)
                                }
                            }
                        }
                    }) {
                        Text("Send")
                    }
                }

            }
        }
    }

    @Composable
    fun MessageItem(message: Message) {
//        val coroutineScope = rememberCoroutineScope()
//        val apiClient = ApiClient()
//        var button by remember (message.title) { mutableStateOf(message.completeUser.isNullOrEmpty()) }
//        var showSuccessDialog by remember { mutableStateOf(false) } // 添加状态变量
        Row(
            horizontalArrangement = if (message.user_id==USER_ID) Arrangement.End else Arrangement.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            if (message.user_id != USER_ID) {
            Button(
                onClick = {
                },
                modifier = Modifier
                    .size(40.dp)
                    .padding(4.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (message.user_id == USER_ID) Color(0xFFD8F0D8) else Color(0xFF9B30FF)
                )
            ) {
                Text(
                    text = message.name.substring(0, 1).uppercase(), // Display first letter of name
                    color = Color.White
                )
            }}
            Box(
                modifier = Modifier
                    .background(
                        if (message.user_id==USER_ID) Color(0xFFD8F0D8) else Color(0xFF9B30FF)  // 绿色和紫色背景
                    )
                    .padding(12.dp)
            ) {
                Column {
                    if (message.title.isNullOrEmpty()) {
                        Text(text = "${message.name}: ${message.message}")
                    } else {
                        Text(text = "${message.name}: ")
                        Text("${message.title}")
                        Text(text = "${message.description}")
                        //var button = false;
//                        if(message.completeUser.isNullOrEmpty()){
//                            button = true;
//                        }

                    }
                    Text(
                        text = message.message_time,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (message.user_id == USER_ID) {
                Button(
                    onClick = {
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .padding(4.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (message.user_id == USER_ID) Color(0xFFD8F0D8) else Color(0xFF9B30FF)
                    )
                ) {
                    Text(
                        text = message.name.substring(0, 1).uppercase(), // Display first letter of name
                        color = Color.White
                    )
                }}
        }
        }

    fun checkUserSession() {
        if (USER_ID.isEmpty() || USER_NAME.isEmpty()) {
            Log.e("PrivateChatActivity", "User session invalid. USER_ID: $USER_ID, USER_NAME: $USER_NAME")
            // 处理无效的用户会话
            finish()
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}


