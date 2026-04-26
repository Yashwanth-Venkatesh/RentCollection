package com.rentcollection

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// SETUP: Add google-services.json to /app folder before building.
// Enable Firebase Authentication (Email/Password) and Firestore in Firebase Console.
@HiltAndroidApp
class RentCollectionApp : Application()
