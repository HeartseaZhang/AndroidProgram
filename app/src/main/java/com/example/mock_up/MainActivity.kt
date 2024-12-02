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
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.unit.sp
import com.example.mock_up.ChatActivity.MessageResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.encodeToString
import java.text.SimpleDateFormat
import java.util.Locale

@Serializable
data class ChatroomResponse(
    val data: List<Chatroom>,
    val success: Boolean,
)
@Serializable
data class TaskResponse(
    val data: List<Task>,
    val success: Boolean,
)
@Serializable
data class Task(
    val id: String,
    val title:String,
    val description:String,
    val creatorUserId: String,
    val completerUserId: String?,
    val createdAt: String,
    val completedAt: String?
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
@Composable
fun MainScreen(modifier: Modifier = Modifier)
{
    var selectedTab by remember { mutableStateOf(0) }
    Scaffold(
//        topBar = {
//            CenterAlignedTopAppBar(
//                title = {
//                    Box(
//                        modifier = Modifier.fillMaxWidth(),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Text("Chatroom")
//                    }
//                },
//                actions = {
//                    Row {
//                        Button(onClick = { /* Handle action */ }) {
//                            Text("Create Chatroom")
//                        }
//                        Button(onClick = { /* Handle action */ }) {
//                            Text("Tasklist")
//                        }
//                    }
//                }
//            )
//        },
        bottomBar = {
            BottomNavigation {
                BottomNavigationItem(
                    icon = { /* Icon for Chatroom */ },
                    label = { Text(text = "Chatroom",fontSize = 20.sp) },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                BottomNavigationItem(
                    icon = { /* Icon for Task */ },
                    label = { Text(text = "Task",fontSize = 20.sp) },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                BottomNavigationItem(
                    icon = { /* Icon for Me */ },
                    label = { Text(text = "Me",fontSize = 20.sp) },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> ChatroomScreen(modifier = Modifier.padding(innerPadding))
            1 -> TaskScreen(modifier = Modifier.padding(innerPadding))
            2 -> MeScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
    @Composable
    fun ChatroomScreen(modifier: Modifier = Modifier) {
        var chatrooms by remember { mutableStateOf(listOf<Chatroom>()) }
        val coroutineScope = rememberCoroutineScope()
        var showDialog by remember { mutableStateOf(false) }
        var chatroomName by remember { mutableStateOf("") }
        LaunchedEffect(Unit) {
            coroutineScope.launch(Dispatchers.IO) {
                val jsonResponse = apiClient.GET("http://149.248.20.141:80/get_chatrooms")
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
                            Text("Chatroom", fontSize = 20.sp)
                        },
                        navigationIcon = {
                            Text("$userName", fontSize = 20.sp, modifier = Modifier.padding(start = 16.dp))
                        },
                        actions = {
                            IconButton(onClick = { showDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Create Chatroom")
                            }
                        }
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
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Create Chatroom") },
                text = {
                    TextField(
                        value = chatroomName,
                        onValueChange = { chatroomName = it },
                        label = { Text(text = "Chatroom Name") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            val response = apiClient.POST("http://149.248.20.141:80/create_chatrooms?name=$chatroomName","{}")
                            if (response.isNotEmpty()) {
                                    showDialog = false
                                    chatroomName = ""
                                    val jsonResponse = apiClient.GET("http://149.248.20.141:80/get_chatrooms")
                                    if (jsonResponse.isNotEmpty()) {
                                        val newResponse = Json.decodeFromString<ChatroomResponse>(jsonResponse)
                                        withContext(Dispatchers.Main) {
                                        chatrooms = newResponse.data
                                    }
                                }
                            }
                        }
                    }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(modifier: Modifier = Modifier) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("My Created Tasks", "My Accepted Tasks")

    Scaffold(
        topBar = {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTabIndex) {
            0 -> MyCreatedTasksScreen(modifier = Modifier.padding(innerPadding))
            1 -> MyAcceptedTasksScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
fun MyCreatedTasksScreen(modifier: Modifier = Modifier) {
    // Display tasks created by the user
    // This part is left for further implementation
    val coroutineScope = rememberCoroutineScope()
    val apiClient = ApiClient()
    val tasks = remember { mutableStateListOf<Task>() }

    fun loadTasks() {
        coroutineScope.launch(Dispatchers.IO) {
            val jsonResponse = apiClient.GET("http://149.248.20.141:80/task/created/$USER_ID")
            if (jsonResponse.isNotEmpty()) {
                val json = Json {
                    ignoreUnknownKeys = true
                }
                val response = json.decodeFromString<TaskResponse>(jsonResponse)
                withContext(Dispatchers.Main) {
                    tasks.clear()
                    tasks.addAll(response.data)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadTasks()
    }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(tasks) { task ->
            TaskItem(task)
        }
    }
}

@Composable
fun MyAcceptedTasksScreen(modifier: Modifier = Modifier) {
    var showSuccessDialog by remember { mutableStateOf(false) } // 添加状态变量

    // Display tasks accepted by the user
    // This part is left for further implementation
    val coroutineScope = rememberCoroutineScope()
    val apiClient = ApiClient()
    val tasks = remember { mutableStateListOf<Task>() }
    fun loadTasks() {
        coroutineScope.launch(Dispatchers.IO) {
            val jsonResponse = apiClient.GET("http://149.248.20.141:80/task/completed/$USER_ID")
            if (jsonResponse.isNotEmpty()) {
                val json = Json {
                    ignoreUnknownKeys = true
                }
                val response = json.decodeFromString<TaskResponse>(jsonResponse)
                withContext(Dispatchers.Main) {
                    tasks.clear()
                    tasks.addAll(response.data)
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        loadTasks()
    }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(tasks) { task ->
            TaskItem(task)
        }
    }

}
@Composable
fun TaskItem(task: Task) {
    val coroutineScope = rememberCoroutineScope()
    val apiClient = ApiClient()
    var flag by remember (task.id) { mutableStateOf(task.completedAt.isNullOrEmpty()) }

    var showSuccessDialog by remember { mutableStateOf(false) } // 添加状态变量

    OutlinedButton(
        onClick = { /* Handle task click */ },
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.title, style = MaterialTheme.typography.bodyLarge)
                Text(text = task.description, style = MaterialTheme.typography.bodySmall)
            }
            //var flag = false
            if (task.completerUserId == USER_ID) {
//                if(task.completedAt.isNullOrEmpty()){
//                    flag=true
//                }
                Button(
                    enabled = flag,
                    onClick = {
                        flag = false

                        coroutineScope.launch(Dispatchers.IO) {
                            val jsonResponse = apiClient.POST(
                                "http://149.248.20.141:80/task/complete?taskId=${task.id}",
                                Json.encodeToString(task.id)
                            )
                            withContext(Dispatchers.Main) {
                                showSuccessDialog = true // 设置弹窗显示
                            }
                        }
                    }
                ) {
                    Text("Done")
                }
            }
        }
    }
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { androidx.compose.material3.Text("Tips") },
            text = { androidx.compose.material3.Text("Task done") },
            confirmButton = {
                Button(onClick = { showSuccessDialog = false }) {
                    androidx.compose.material3.Text("ok")
                }
            }
        )
    }
}
@Composable
fun MeScreen(modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()
    val apiClient = ApiClient()
    var userName by remember { mutableStateOf("$userName") }
    var userEmail by remember { mutableStateOf("${UserSession.userEmail}") }
    var showEditDialog by remember { mutableStateOf(false) }
    var editField by remember { mutableStateOf("") }
    var editType by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = "User ID: $userId", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Name: $userName", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                editType = "Name"
                editField = userName
                showEditDialog = true
            }) {
                Text("Edit")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Password: ******", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                editType = "Password"
                editField = ""
                showEditDialog = true
            }) {
                Text("Edit")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Email: $userEmail", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.width(8.dp))
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit $editType") },
            text = {
                TextField(
                    value = editField,
                    onValueChange = { editField = it },
                    label = { Text(text = "New $editType") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (editType == "Name") {
                        val modifyRequest = ModifyRequest(
                            userId= USER_ID,
                            userName=editField,
                            password=""
                        )
                        coroutineScope.launch(Dispatchers.IO){
                            apiClient.POST("http://149.248.20.141:80/user/modify",Json.encodeToString(modifyRequest))
                        }
                        userName = editField
                        UserSession.userName=userName
                    } else if (editType == "Password") {
                        val modifyRequest = ModifyRequest(
                            userId= USER_ID,
                            userName=userName,
                            password=editField
                        )
                        coroutineScope.launch(Dispatchers.IO){
                            apiClient.POST("http://149.248.20.141:80/user/modify",Json.encodeToString(modifyRequest))
                        }
                    }

                    showEditDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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

@Serializable
data class ModifyRequest(
    val userId:String,
    val userName:String?,
    val password:String?
)