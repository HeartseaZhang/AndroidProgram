package com.example.mock_up

import ApiClient
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import java.text.SimpleDateFormat
import java.util.*
import com.example.mock_up.UserSession.userId
import com.example.mock_up.UserSession.userName
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayOutputStream
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign

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
data class UserDetail(
    val name: String,
    val email: String,
    val type: String,
)

@kotlinx.serialization.Serializable
data class CreatePrivateChatRequest(
    val user1Id: String,
    val user2Id: String
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
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(message.message_time)
                        })
                    }
//                    messages.clear()
//                    messages.addAll()
                }
            }
        }
        fun myConvertor(uri: Uri): Bitmap {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        }

        val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
            onResult = { uri ->
                uri?.let {
                    val bitmap = myConvertor(it)
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
                    val imageString = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)

                    val sendMessageRequest = SendTaskRequest(
                        description = imageString,
                        title = "image",
                        createUserId = USER_ID,
                        chatroom_id = chatroomId
                    )

                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val postResponse = apiClient.POST("http://149.248.20.141:80/send_task",
                                Json.encodeToString(sendMessageRequest))
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
                }
            }
        )

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
                        Button(onClick = { showDialog = true }) {
                            Text("Initiate Task")
                        }
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
                IconButton(onClick = {
                    // Launch photo picker
                    singlePhotoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Create Chatroom")
                }
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
                                val postResponse = apiClient.POST("http://149.248.20.141:80/askAi",
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
                    Text("Ask Ai")
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
        var showUserDetailDialog by remember { mutableStateOf(false) }
        var selectedUserDetail by remember { mutableStateOf<UserDetail?>(null) }
        var button by remember (message.title) { mutableStateOf(message.completeUser.isNullOrEmpty()) }

        var showSuccessDialog by remember { mutableStateOf(false) } // 添加状态变量
        Row(
            horizontalArrangement = if (message.user_id==USER_ID) Arrangement.End else Arrangement.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            if (message.user_id != USER_ID&&message.user_id!="null") {
                Button(
                    onClick = {

                        coroutineScope.launch {
                            val userDetails = fetchUserDetails(message.user_id)
                            selectedUserDetail = userDetails
                            showUserDetailDialog = true
                        }
                    },
                    modifier = Modifier
                        .size(52.2.dp)
                        .padding(2.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (message.user_id == USER_ID) Color(0xFFD8F0D8) else Color(
                            0xFF9B30FF
                        )
                    )
                )
                { Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ){
                    Text(
                        text = message.name.substring(0, 1)
                            .uppercase(), // Display first letter of name
                        color = Color.Black,
                        //fontSize =11.sp,
                        textAlign = TextAlign.Center,
                    )
                }}
            }
            Box(
                modifier = Modifier
                    .background(
                        if (message.user_id==USER_ID) Color(0xFFD8F0D8) else if(message.user_id=="null") Color(0xFFFFFFFF) else Color(0xFF9B30FF)  // 绿色和紫色背景
                    )
                    .padding(12.dp)
            ) {
                Column {
                    if (message.title.isNullOrEmpty()) {
                        Text(text = "${message.name}: ${message.message}")
                    } else if(message.title=="image"){
                        message.description?.let { imageString ->
                            val imageBytes = Base64.decode(imageString, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
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
            if (message.user_id == USER_ID) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val userDetails = fetchUserDetails(message.user_id)
                            selectedUserDetail = userDetails
                            showUserDetailDialog = true
                        }
                    },
                    modifier = Modifier
                        .size(52.2.dp)
                        .padding(2.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (message.user_id == USER_ID) Color(0xFFD8F0D8) else Color(
                            0xFF9B30FF
                        )
                    )
                )

                {
                    Text(
                        text = message.name.substring(0, 1)
                            .uppercase(), // Display first letter of name
                        color = Color.Black,
                        //fontSize =15.sp, // 设置字体大小
                        textAlign = TextAlign.Center,
                        //modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)
                        //.padding(0.dp),
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
        //新增部分
        // 显示用户详细信息对话框
        if (showUserDetailDialog && selectedUserDetail!= null) {
            AlertDialog(
                onDismissRequest = { showUserDetailDialog = false },
                title = { Text("User Details") },
                text = {
                    Column {
                        // 展示用户的姓名信息
                        Text("Name: ${selectedUserDetail!!.name}")
                        // 展示用户的邮箱信息
                        Text("Email: ${selectedUserDetail!!.email}")
                        // 展示用户的类型信息
                        Text("Type: ${selectedUserDetail!!.type}")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val response = createPrivateChatroom(USER_ID, message.user_id)
                                showUserDetailDialog = false
                                if (response.contains("chatroom_id")) {
                                    val json = Json { ignoreUnknownKeys = true }
                                    val jsonObject = json.parseToJsonElement(response).jsonObject
                                    val chatroomId = jsonObject["chatroom_id"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                                    val chatroomName = jsonObject["chatroom_name"]?.jsonPrimitive?.contentOrNull
                                    // 如果 chatroomId 存在，则导航到新的聊天房间
                                    if (chatroomId != null) {
                                        try {
                                            Log.d("ChatActivity", "Starting PrivateChatActivity with chatroomId: $chatroomId")
                                            val intent = Intent(this@ChatActivity, PrivateChatActivity::class.java)
                                            intent.putExtra("chatroom_id", chatroomId)
                                            intent.putExtra("chatroom_name", chatroomName)
                                            startActivity(intent)
                                        } catch (e: Exception) {
                                            Log.e("ChatActivity", "Error starting PrivateChatActivity", e)
                                        }
                                    } else {
                                        Log.e("ChatActivity", "Failed to parse chatroom_id from response: $response")
                                    }
                                } else {
                                    Log.e("ChatActivity", "Error creating private chatroom: $response")
                                }
                            }
                        }
                    ) {
                        Text("Send message")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showUserDetailDialog = false }
                    ) {
                        Text("Close")
                    }
                }
            )
        }
        //新增部分结束

    }
    private suspend fun fetchUserDetails(userId: String): UserDetail? {
        val apiClient = ApiClient()
        val json = Json { ignoreUnknownKeys = true}
        return withContext(Dispatchers.IO) {
            try {
                val jsonResponse = apiClient.GET("http://61.238.214.170:8090/api/user/$userId")
                Log.d("ChatActivity", "Received response: $jsonResponse")
                if (jsonResponse.isNotEmpty()) {
                    json.decodeFromString<UserDetail>(jsonResponse)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error fetching user details", e)
                null
            }
        }
    }

    private suspend fun createPrivateChatroom(user1Id: String, user2Id: String): String {
        val apiClient = ApiClient()
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ChatActivity", "Creating private chatroom for users: $user1Id and $user2Id")

                val request = CreatePrivateChatRequest(user1Id, user2Id)
                Log.d("ChatActivity", "Sending request: ${Json.encodeToString(request)}")

                val response = apiClient.POST(
                    "http://61.238.214.170:8090/api/create_private_chat",
                    Json.encodeToString(request)
                )

                Log.d("ChatActivity", "Received response: $response")

                if (response.contains("chatroom_id")) {
                    Log.d("ChatActivity", "Private chatroom created successfully")
                } else {
                    Log.e("ChatActivity", "Error creating private chatroom: $response")
                }

                response
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error creating private chatroom", e)
                "Error: ${e.message}"
            }
        }
    }

    @kotlinx.serialization.Serializable
    data class MessageResponse(val data: MessageData)

    @kotlinx.serialization.Serializable
    data class MessageData(val messages: List<Message>)

}




