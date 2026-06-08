package com.jp.scantwo

import com.google.gson.annotations.SerializedName

// --- Auth ---
data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val status: Int,
    val message: String,
    val data: LoginData?,
    val error: String?
)

data class LoginData(
    val token: String,
    val user: AuthUser
)

data class AuthUser(
    val id: Long,
    val uuid: String,
    val username: String,
    val email: String,
    val role: String,
    @SerializedName("is_active") val isActive: Boolean
)

// --- Check-in ---
data class CheckInResponse(
    val status: Int,
    val message: String,
    val data: CheckInData?,
    val error: String?
)

data class CheckInData(
    val uuid: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("is_check_in") val isCheckIn: Boolean,
    @SerializedName("checked_in_at") val checkedInAt: String?
)
