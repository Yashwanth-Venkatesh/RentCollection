package com.rentcollection.data.model

import com.google.firebase.firestore.DocumentId

data class Tenant(
    @DocumentId val tenantId: String = "",
    val houseId: String = "",
    val buildingId: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val alternatePhone: String = "",
    val moveInDate: String = "",
    val moveOutDate: String = "",
    @get:JvmName("getIsActive") var isActive: Boolean = true,
    val notes: String = ""
)
