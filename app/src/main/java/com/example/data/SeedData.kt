package com.example.data

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch

object SeedData {

    suspend fun populateIfEmpty(
        db: FirebaseFirestore,
        workspaceId: String,
        ownerUid: String,
        awaitCommit: suspend (WriteBatch) -> Unit
    ) {
        val wsRef = db.collection("workspaces").document(workspaceId)
        val now = System.currentTimeMillis()

        val ceoAdmin = EmployeeEntity(
            uid = "emp-ceo-000",
            ownerUid = ownerUid,
            workspaceId = workspaceId,
            employeeId = "3KB-000",
            fullName = "Daniel Reyes",
            role = "CEO / Administrator",
            pin = "0000",
            active = true,
            admin = true,
            createdAtMillis = now
        )

        val ops = listOf<Pair<DocumentReference, Map<String, Any>>>(
            wsRef.collection("settings").document("devotional_privacy") to mapOf(
                "ownerUid" to ownerUid,
                "workspaceId" to workspaceId,
                "policy" to DevotionalPrivacyPolicy.OPT_IN_ONLY.name,
                "updatedByUid" to ownerUid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            wsRef.collection("employees").document(ceoAdmin.uid) to mapOf(
                "ownerUid" to ownerUid,
                "workspaceId" to workspaceId,
                "employeeId" to ceoAdmin.employeeId,
                "fullName" to ceoAdmin.fullName,
                "role" to ceoAdmin.role,
                "pin" to ceoAdmin.pin,
                "active" to ceoAdmin.active,
                "admin" to ceoAdmin.admin,
                "createdAtMillis" to ceoAdmin.createdAtMillis,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        val batch = db.batch()
        ops.forEach { (ref, payload) -> batch.set(ref, payload) }
        awaitCommit(batch)
    }
}
