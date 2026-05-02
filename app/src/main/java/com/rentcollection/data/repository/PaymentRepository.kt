package com.rentcollection.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.rentcollection.data.model.Payment
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    /** All payments — sorted client-side to avoid composite Firestore indexes. */
    fun getAllPaymentsFlow(): Flow<List<Payment>> = callbackFlow<List<Payment>> {
        val listener = firestore.collection("payments")
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val sorted = (snap?.documents?.mapNotNull { doc ->
                    doc.toObject(Payment::class.java)?.copy(paymentId = doc.id)
                } ?: emptyList())
                    .sortedWith(compareByDescending<Payment> { it.year }.thenByDescending { it.month })
                trySend(sorted)
            }
        awaitClose { listener.remove() }
    }.onStart { emit(emptyList()) }

    fun getPaymentsForMonthFlow(month: Int, year: Int): Flow<List<Payment>> = callbackFlow<List<Payment>> {
        val listener = firestore.collection("payments")
            .whereEqualTo("month", month)
            .whereEqualTo("year", year)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                trySend(snap?.documents?.mapNotNull { doc ->
                    doc.toObject(Payment::class.java)?.copy(paymentId = doc.id)
                } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }.onStart { emit(emptyList()) }

    fun getPaymentsForBuildingMonthFlow(buildingId: String, month: Int, year: Int): Flow<List<Payment>> = callbackFlow<List<Payment>> {
        val listener = firestore.collection("payments")
            .whereEqualTo("buildingId", buildingId)
            .whereEqualTo("month", month)
            .whereEqualTo("year", year)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                trySend(snap?.documents?.mapNotNull { doc ->
                    doc.toObject(Payment::class.java)?.copy(paymentId = doc.id)
                } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }.onStart { emit(emptyList()) }

    fun getPaymentsForHouseFlow(houseId: String): Flow<List<Payment>> = callbackFlow<List<Payment>> {
        val listener = firestore.collection("payments")
            .whereEqualTo("houseId", houseId)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val sorted = (snap?.documents?.mapNotNull { doc ->
                    doc.toObject(Payment::class.java)?.copy(paymentId = doc.id)
                } ?: emptyList())
                    .sortedWith(compareByDescending<Payment> { it.year }.thenByDescending { it.month })
                trySend(sorted)
            }
        awaitClose { listener.remove() }
    }.onStart { emit(emptyList()) }

    fun getPaymentForHouseMonthFlow(houseId: String, month: Int, year: Int): Flow<Payment?> = callbackFlow<Payment?> {
        val listener = firestore.collection("payments")
            .whereEqualTo("houseId", houseId)
            .whereEqualTo("month", month)
            .whereEqualTo("year", year)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(null); return@addSnapshotListener }
                val doc = snap?.documents?.firstOrNull()
                trySend(doc?.toObject(Payment::class.java)?.copy(paymentId = doc.id))
            }
        awaitClose { listener.remove() }
    }.onStart { emit(null) }

    /** Upsert: if a payment already exists for that house/month/year, update it. */
    suspend fun recordPayment(payment: Payment): String {
        val existing = firestore.collection("payments")
            .whereEqualTo("houseId", payment.houseId)
            .whereEqualTo("month", payment.month)
            .whereEqualTo("year", payment.year)
            .get().await()
            .documents.firstOrNull()

        return if (existing != null) {
            existing.reference.set(payment.copy(paymentId = existing.id)).await()
            existing.id
        } else {
            val ref = firestore.collection("payments").document()
            ref.set(payment.copy(paymentId = ref.id)).await()
            ref.id
        }
    }

    suspend fun markAsReceived(paymentId: String, paidDate: String) {
        firestore.collection("payments").document(paymentId)
            .update(mapOf("isReceived" to true, "paidDate" to paidDate))
            .await()
    }

    suspend fun updatePayment(payment: Payment) {
        firestore.collection("payments").document(payment.paymentId).set(payment).await()
    }

    suspend fun undoMarkAsReceived(paymentId: String) {
        firestore.collection("payments").document(paymentId)
            .update(mapOf("isReceived" to false, "paidDate" to ""))
            .await()
    }

    suspend fun deletePayment(paymentId: String) {
        firestore.collection("payments").document(paymentId).delete().await()
    }

    fun getPaymentsForYearsFlow(years: List<Int>): Flow<List<Payment>> = callbackFlow<List<Payment>> {
        val safeYears = years.distinct().take(10)
        if (safeYears.isEmpty()) { trySend(emptyList()); awaitClose(); return@callbackFlow }
        val listener = firestore.collection("payments")
            .whereIn("year", safeYears)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                trySend(snap?.documents?.mapNotNull { doc ->
                    doc.toObject(Payment::class.java)?.copy(paymentId = doc.id)
                } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }.onStart { emit(emptyList()) }
}
