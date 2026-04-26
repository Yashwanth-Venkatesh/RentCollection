package com.rentcollection.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Building(
    @DocumentId val buildingId: String = "",
    val name: String = "",
    val address: String = "",
    val description: String = "",
    @ServerTimestamp val createdAt: Date? = null,
    val createdBy: String = ""
)
