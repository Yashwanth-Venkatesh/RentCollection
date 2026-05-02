package com.rentcollection.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.rentcollection.data.model.Tenant
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TenantRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    // Query only by houseId (single-field index, no composite index needed).
    // Filter isActive in memory to avoid requiring a composite Firestore index.

    fun getAllActiveTenantsFlow(): Flow<List<Tenant>> = callbackFlow<List<Tenant>> {
        val listener = firestore.collection("tenants")
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val tenants = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(Tenant::class.java)?.copy(tenantId = doc.id)
                }?.filter { it.isActive } ?: emptyList()
                trySend(tenants)
            }
        awaitClose { listener.remove() }
    }.onStart { emit(emptyList()) }

    fun getActiveTenantForHouseFlow(houseId: String): Flow<Tenant?> = callbackFlow<Tenant?> {
        val listener = firestore.collection("tenants")
            .whereEqualTo("houseId", houseId)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val active = snap?.documents
                    ?.mapNotNull { doc -> doc.toObject(Tenant::class.java)?.copy(tenantId = doc.id) }
                    ?.firstOrNull { it.isActive }
                trySend(active)
            }
        awaitClose { listener.remove() }
    }.onStart { emit(null) }

    suspend fun getActiveTenantForHouseOnce(houseId: String): Tenant? = try {
        firestore.collection("tenants")
            .whereEqualTo("houseId", houseId)
            .get().await()
            .documents.mapNotNull { doc ->
                doc.toObject(Tenant::class.java)?.copy(tenantId = doc.id)
            }.firstOrNull { it.isActive }
    } catch (_: Exception) { null }

    suspend fun getTenant(tenantId: String): Tenant? = try {
        firestore.collection("tenants").document(tenantId).get().await()
            .let { doc -> doc.toObject(Tenant::class.java)?.copy(tenantId = doc.id) }
    } catch (_: Exception) { null }

    suspend fun addTenant(tenant: Tenant): String {
        val ref = firestore.collection("tenants").document()
        ref.set(tenant).await()
        return ref.id
    }

    suspend fun updateTenant(tenant: Tenant) {
        firestore.collection("tenants").document(tenant.tenantId).set(tenant).await()
    }

    suspend fun deactivateTenantForHouse(houseId: String) {
        val tenants = firestore.collection("tenants")
            .whereEqualTo("houseId", houseId)
            .get().await()
            .documents.mapNotNull { doc ->
                doc.toObject(Tenant::class.java)?.copy(tenantId = doc.id)
            }.filter { it.isActive }
        tenants.forEach { t ->
            firestore.collection("tenants")
                .document(t.tenantId)
                .update("isActive", false)
                .await()
        }
    }
}
