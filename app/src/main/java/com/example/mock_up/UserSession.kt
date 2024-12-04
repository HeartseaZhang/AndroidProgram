package com.example.mock_up

import com.example.mock_up.UserSession.userEmail
import com.example.mock_up.UserSession.userId
import com.example.mock_up.UserSession.userName

object UserSession {
    var userId: String? = null
    var userName:String?=null
    var userEmail:String?=null
    fun clearSession() {
        userId = ""
        userName = ""
        userEmail = ""
    }
}
