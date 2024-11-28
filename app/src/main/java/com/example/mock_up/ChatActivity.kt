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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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

@kotlinx.serialization.Serializable
data class TaskRequest(
    val title: String,
    val description: String,
    val creatorUserId: String
)

@kotlinx.serialization.Serializable
data class Message (
    val user_id: String,
    val name: String,
    val message_time: String,
    val message:String?,
    val description:String?,
    val title:String?
    )

//@kotlinx.serialization.Serializable
//data class TextMessage(
//    val message: String,
//    override val user_id: String,
//    override val name: String,
//    override val message_time: String
//) : Message()
//
//@kotlinx.serialization.Serializable
//data class TaskMessage(
//    val title: String?,
//    val description: String?,
//    override val user_id: String,
//    override val name: String,
//    override val message_time: String
//) : Message()


@kotlinx.serialization.Serializable
data class SendMessageRequest(
    val message: String,
    val name: String,
    val user_id: String,
    val chatroom_id: Int
)

var USER_NAME="$userName"
var USER_ID="$userId"

@OptIn(ExperimentalMaterial3Api::class)
class ChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chatroomId = intent.getIntExtra("chatroom_id", -1)
        val chatroomName = intent.getStringExtra("chatroom_name") ?: "Chatroom"  // 默认值为 "Chatroom"
        setContent {
            ChatScreen(chatroomId,chatroomName)
        }
    }



    //@Preview(showBackground = true)
    @Composable
    fun ChatScreen(chatroomId: Int,chatroomName: String) {
        var messageText by remember { mutableStateOf("") }
        val messages = remember { mutableStateListOf<Message>() }
        val coroutineScope = rememberCoroutineScope()
        val apiClient = ApiClient()
        var showDialog by remember { mutableStateOf(false) }
        var taskTitle by remember { mutableStateOf("") }
        var taskDescription by remember { mutableStateOf("") }

        fun loadMessages() {
            coroutineScope.launch(Dispatchers.IO) {
                val jsonResponse = apiClient.GET("http://192.168.0.51:5722/get_messages?chatroom_id=$chatroomId")
                if (jsonResponse.isNotEmpty()) {
                    val json = Json {
                        ignoreUnknownKeys = true
                    }
                    val response = json.decodeFromString<MessageResponse>(jsonResponse)
                    withContext(Dispatchers.Main) {
                        messages.clear()
                        messages.addAll(response.data.messages.sortedByDescending { message ->
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(message.message_time)
                        })
                    }
//                    messages.clear()
//                    messages.addAll()
                }
            }
        }

//fun loadMessages() {
//    coroutineScope.launch(Dispatchers.IO) {
//        val jsonResponse = apiClient.GET("http://192.168.0.51:5722/get_messages?chatroom_id=$chatroomId")
//        if (jsonResponse.isNotEmpty()) {
//            val json = Json {
//                ignoreUnknownKeys = true
//            }
//
//            try {
//                val jsonElement = json.parseToJsonElement(jsonResponse)
//                val messagesArray = jsonElement.jsonObject["data"]?.jsonObject?.get("messages")?.jsonArray
//
//                withContext(Dispatchers.Main) {
//                    messages.clear()
//
//                    if (messagesArray != null) {
//                        for (item in messagesArray) {
//                            val message: Message = if (item.jsonObject["title"]?.jsonPrimitive?.contentOrNull != null) {
//                                json.decodeFromJsonElement(TaskMessage.serializer(), item)
//                            } else {
//                                json.decodeFromJsonElement(TextMessage.serializer(), item)
//                            }
//                            messages.add(message)
//                        }
//                    }
//
//                    // 按时间顺序从新到老排序消息
//                    messages.sortByDescending {
//                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(it.message_time)
//                    }
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                Log.e("ChatApp", "Error parsing response: ${e.message}")
//            }
//        }
//    }
//}


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
                            startActivity(Intent(this@ChatActivity, MainActivity::class.java))
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
                                val postResponse = apiClient.POST("http://192.168.0.51:5722/send_message",
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
                Button(onClick = { showDialog = true }) {
                    Text("Initiate Task")
                }
            }
                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text("Initiate Task") },
                        text = {
                            Column {
                                TextField(
                                    value = taskTitle,
                                    onValueChange = { taskTitle = it },
                                    label = { Text("Title") }
                                )
                                TextField(
                                    value = taskDescription,
                                    onValueChange = { taskDescription = it },
                                    label = { Text("Description") }
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val taskRequest = TaskRequest(
                                        title = taskTitle,
                                        description = taskDescription,
                                        creatorUserId = USER_ID
                                    )
                                    taskTitle=""
                                    taskDescription=""
                                    coroutineScope.launch(Dispatchers.IO) {
                                        try {
                                            val postResponse = apiClient.POST("http://192.168.0.51:5722/task/create",
                                                Json.encodeToString(taskRequest))
                                            withContext(Dispatchers.Main) {
                                                if (postResponse.contains("ERROR")) {
                                                    Log.e("POST Request", "Failed to send message: $postResponse")
                                                } else {
                                                    loadMessages()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e("POST Request", "Exception: ", e)
                                        }
                                    }
                                    showDialog = false
                                }
                            ) {
                                Text("Send")
                            }
                        },
                        dismissButton = {
                            Button(
                                onClick = { showDialog = false }
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
    @Composable
    fun MessageItem(message: Message) {
        Row(
            horizontalArrangement = if (message.user_id==USER_ID) Arrangement.End else Arrangement.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
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
                        Text(text = "${message.name} Task: ${message.title}")
                        Text(text = "Description: ${message.description}")
                    }
                    Text(
                        text = message.message_time,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
//@Composable
//fun MessageItem(message: Message) {
//    Row(
//        horizontalArrangement = if (message.user_id == USER_ID) Arrangement.End else Arrangement.Start,
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(8.dp)
//    ) {
//        Box(
//            modifier = Modifier
//                .background(
//                    if (message.user_id == USER_ID) Color(0xFFD8F0D8) else Color(0xFF9B30FF)  // 绿色和紫色背景
//                )
//                .padding(12.dp)
//        ) {
//            Column {
//                when (message) {
//                    is TextMessage -> {
//                        Text(text = "${message.name}: ${message.message}")
//                    }
//                    is TaskMessage -> {
//                        Text(text = "${message.name} Task: ${message.title}")
//                        Text(text = "Description: ${message.description}")
//                    }
//                }
//                Text(
//                    text = message.message_time,
//                    style = MaterialTheme.typography.bodySmall
//                )
//            }
//        }
//    }
//}

    @kotlinx.serialization.Serializable
    data class MessageResponse(val data: MessageData)

    @kotlinx.serialization.Serializable
    data class MessageData(val messages: List<Message>)
}