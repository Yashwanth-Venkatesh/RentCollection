package com.rentcollection.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Payment(
    val paymentId: String = "",
    val houseId: String = "",
    val buildingId: String = "",
    val tenantId: String = "",
    val amount: Double = 0.0,
    val month: Int = 0,
    val year: Int = 0,
    val paidDate: String = "",
    val paymentMode: String = "Cash", // Cash / UPI / Bank Transfer / Cheque
    val notes: String = "",
    @get:JvmName("getIsReceived") var isReceived: Boolean = false,
    val recordedBy: String = "",       // user display name
    val recordedByEmail: String = "",  // user email for audit
    @ServerTimestamp val recordedAt: Date? = null,
    // Denormalized fields for display in history without extra lookups
    val houseNumber: String = "",
    val buildingName: String = "",
    val tenantName: String = "",
    val tenantPhone: String = "",
    val floorId: String = "",
    val floorName: String = ""
)
