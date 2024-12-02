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
    val title:String?,
    val completeUser:String?
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
data class SendTaskRequest(
    val title: String,
    val description: String,
    val createUserId: String,
    val chatroom_id: Int
)

@kotlinx.serialization.Serializable
data class SendMessageRequest(
    val message: String,
    val name: String,
    val user_id: String,
    val chatroom_id: Int
)
@kotlinx.serialization.Serializable
data class AcceptTask(
    val title: String,
    val description: String,
    val userId: String,
    val chatroomId:Int
)

var USER_NAME="$userName"
var USER_ID="$userId"
var chatroomId=1
@OptIn(ExperimentalMaterial3Api::class)
class ChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        chatroomId = intent.getIntExtra("chatroom_id", -1)
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
                val jsonResponse = apiClient.GET("http://149.248.20.141:80/get_messages?chatroom_id=$chatroomId")
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
                                    val sendtaskRequest = SendTaskRequest(
                                        title = taskTitle,
                                        description = taskDescription,
                                        createUserId = USER_ID ,
                                        chatroom_id = chatroomId
                                    )
                                    taskTitle=""
                                    taskDescription=""
                                    coroutineScope.launch(Dispatchers.IO) {
                                        try {
                                            val postResponse = apiClient.POST("http://149.248.20.141:80/task/create",
                                                Json.encodeToString(taskRequest))
                                            apiClient.POST("http://149.248.20.141:80/send_task",Json.encodeToString(sendtaskRequest))
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
        val coroutineScope = rememberCoroutineScope()
        val apiClient = ApiClient()
        var button by remember (message.title) { mutableStateOf(message.completeUser.isNullOrEmpty()) }

        var showSuccessDialog by remember { mutableStateOf(false) } // 添加状态变量
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
                        Text(text = "${message.name}: ")
                        Text("${message.title}")
                        Text(text = "${message.description}")
                        //var button = false;
//                        if(message.completeUser.isNullOrEmpty()){
//                            button = true;
//                        }
                        Button(

                            enabled = button,
                            onClick = {
                                button = false;
                                val acceptTask = message.description?.let {
                                    AcceptTask(
                                        title = message.title,
                                        description = it,
                                        userId = USER_ID,
                                        chatroomId = chatroomId
                                    )
                                }
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val postResponse = apiClient.POST("http://149.248.20.141:80/task/accept",
                                            Json.encodeToString(acceptTask))
                                        withContext(Dispatchers.Main) {
                                            if (postResponse.contains("ERROR")) {
                                                Log.e("POST Request", "Failed to send message: $postResponse")
                                            }else{
                                                showSuccessDialog = true // 设置弹窗显示

                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("POST Request", "Exception: ", e)
                                    }
                                }

                            }
                        ) {
                            Text("Accept Task")
                        }

                    }
                    Text(
                        text = message.message_time,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false },
                title = { Text("Tips") },
                text = { Text("Accept Successfully") },
                confirmButton = {
                    Button(onClick = { showSuccessDialog = false }) {
                        Text("ok")
                    }
                }
            )
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