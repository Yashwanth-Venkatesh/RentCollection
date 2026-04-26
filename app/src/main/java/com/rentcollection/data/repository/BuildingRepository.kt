package com.rentcollection.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.rentcollection.data.model.Building
import com.rentcollection.data.model.Floor
import com.rentcollection.data.model.House
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildingRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    // ── Buildings ──────────────────────────────────────────────────────────────

    fun getBuildingsFlow(): Flow<List<Building>> = callbackFlow {
        val listener = firestore.collection("buildings")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.toObjects(Building::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun getBuilding(buildingId: String): Building? = try {
        firestore.collection("buildings").document(buildingId).get().await()
            .toObject(Building::class.java)
    } catch (_: Exception) { null }

    suspend fun addBuilding(building: Building): String {
        val ref = firestore.collection("buildings").document()
        ref.set(building).await()
        return ref.id
    }

    suspend fun updateBuilding(building: Building) {
        firestore.collection("buildings").document(building.buildingId).set(building).await()
    }

    suspend fun deleteBuilding(buildingId: String) {
        firestore.collection("buildings").document(buildingId).delete().await()
    }

fun getAllHousesFlow(): Flow<List<House>> = callbackFlow {
        val listener = firestore.collectionGroup("houses")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.toObjects(House::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun getAllFloorsFlow(): Flow<List<Floor>> = callbackFlow {
        val listener = firestore.collectionGroup("floors")
            .addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.toObjects(Floor::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    // ── Floors ─────────────────────────────────────────────────────────────────

    fun getFloorsFlow(buildingId: String): Flow<List<Floor>> = callbackFlow {
        val listener = firestore.collection("buildings").document(buildingId)
            .collection("floors")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val floors = (snap?.toObjects(Floor::class.java) ?: emptyList())
                    .sortedBy { it.floorNumber }
                trySend(floors)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getFloorsOnce(buildingId: String): List<Floor> = try {
        firestore.collection("buildings").document(buildingId)
            .collection("floors").get().await()
            .toObjects(Floor::class.java)
            .sortedBy { it.floorNumber }
    } catch (_: Exception) { emptyList() }

    suspend fun addFloor(buildingId: String, floor: Floor): String {
        val ref = firestore.collection("buildings").document(buildingId)
            .collection("floors").document()
        ref.set(floor.copy(buildingId = buildingId)).await()
        return ref.id
    }

    suspend fun updateFloor(buildingId: String, floor: Floor) {
        firestore.collection("buildings").document(buildingId)
            .collection("floors").document(floor.floorId).set(floor).await()
    }

    suspend fun deleteFloor(buildingId: String, floorId: String) {
        firestore.collection("buildings").document(buildingId)
            .collection("floors").document(floorId).delete().await()
    }

    suspend fun getFloor(buildingId: String, floorId: String): Floor? = try {
        firestore.collection("buildings").document(buildingId)
            .collection("floors").document(floorId).get().await()
            .toObject(Floor::class.java)
    } catch (_: Exception) { null }

    // ── Houses ─────────────────────────────────────────────────────────────────

    fun getHousesFlow(buildingId: String, floorId: String): Flow<List<House>> = callbackFlow {
        val listener = firestore.collection("buildings").document(buildingId)
            .collection("floors").document(floorId)
            .collection("houses")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val houses = (snap?.toObjects(House::class.java) ?: emptyList())
                    .sortedBy { it.houseNumber }
                trySend(houses)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getHousesOnce(buildingId: String, floorId: String): List<House> = try {
        firestore.collection("buildings").document(buildingId)
            .collection("floors").document(floorId)
            .collection("houses").get().await()
            .toObjects(House::class.java)
            .sortedBy { it.houseNumber }
    } catch (_: Exception) { emptyList() }

    suspend fun getHouse(buildingId: String, floorId: String, houseId: String): House? = try {
        firestore.collection("buildings").document(buildingId)
            .collection("floors").document(floorId)
            .collection("houses").document(houseId).get().await()
            .toObject(House::class.java)
    } catch (_: Exception) { null }

    suspend fun addHouse(buildingId: String, floorId: String, house: House): String {
        val ref = firestore.collection("buildings").document(buildingId)
            .collection("floors").document(floorId)
            .collection("houses").document()
        ref.set(house.copy(buildingId = buildingId, floorId = floorId)).await()
        return ref.id
    }

    suspend fun updateHouse(buildingId: String, floorId: String, house: House) {
        firestore.collection("buildings").document(buildingId)
            .collection("floors").document(floorId)
            .collection("houses").document(house.houseId).set(house).await()
    }

    suspend fun deleteHouse(buildingId: String, floorId: String, houseId: String) {
        firestore.collection("buildings").document(buildingId)
            .collection("floors").document(floorId)
            .collection("houses").document(houseId).delete().await()
    }
}
