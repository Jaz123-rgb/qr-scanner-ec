package com.jp.scantwo

// EntryExitRequest.kt
data class EntryExitRequest(
    val uniqueIdPayment: String,
    val action: String
)

// EntryExitResponse.kt
data class EntryExitResponse(
    val message: String,
    val time: String,
    val user: User
)

data class User(
    val id: String,
    val createdAt: String,
    val updatedAt: String,
    val nombre: String,
    val correo: String,
    val nombreLider: String,
    val edad: Int,
    val genero: String,
    val rol: String,
    val pago: Double,
    val originStatus: String,
    val estado: String,
    val cburchOrigin: String,
    val password: String,
    val telefono: String,
    val history: List<History>,
    val historyCount: Int,
    val finalState: FinalState
)

data class History(
    val id: String,
    val updatedAt: String,
    val action: String,
    val total: Double,
    val added: Double,
    val note: String,
    val previousTotal: Double
)

data class FinalState(
    val id: String,
    val datee: String,
    val uniqueIdPayment: String,
    val status: String,
    val entryExitTime: String
)