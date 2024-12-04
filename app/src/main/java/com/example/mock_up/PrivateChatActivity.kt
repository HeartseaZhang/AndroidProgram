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
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import com.example.mock_up.ChatActivity.MessageResponse
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import java.text.SimpleDateFormat
import java.util.*

import java.io.ByteArrayOutputStream

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
//        var showDialog by remember { mutableStateOf(false) }
//        var taskTitle by remember { mutableStateOf("") }
//        var taskDescription by remember { mutableStateOf("") }

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
                    onClick = {  },
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
                        text = message.name.substring(0, 1).uppercase(), // Display first letter of name
                        color = Color.Black,
                        //fontSize =15.sp, // 设置字体大小
                        textAlign = TextAlign.Center,
                    )
                }
            }
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
                    }else {
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
                    onClick = {  },
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
                        text = message.name.substring(0, 1).uppercase(), // Display first letter of name
                        color = Color.Black,
                        //fontSize =15.sp, // 设置字体大小
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    private fun checkUserSession() {
        if (USER_ID.isEmpty() || USER_NAME.isEmpty()) {
            Log.e("PrivateChatActivity", "User session invalid. USER_ID: $USER_ID, USER_NAME: $USER_NAME")
            // 处理无效的用户会话
            finish()
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}


