package com.rentcollection.data.model

import com.google.firebase.firestore.DocumentId

data class House(
    @DocumentId val houseId: String = "",
    val houseNumber: String = "",
    val rentAmount: Double = 0.0,
    val depositAmount: Double = 0.0,
    val floorId: String = "",
    val buildingId: String = "",
    val rentChanges: List<RentChange> = emptyList()
)
