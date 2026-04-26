package com.rentcollection.data.model

import com.google.firebase.firestore.DocumentId

data class Floor(
    @DocumentId val floorId: String = "",
    val floorNumber: Int = 0,
    val floorName: String = "",
    val buildingId: String = ""
)
