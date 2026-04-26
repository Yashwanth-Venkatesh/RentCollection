package com.rentcollection.data.model

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId val userId: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "viewer" // "owner" or "viewer"
)
