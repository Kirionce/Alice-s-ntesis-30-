package com.example.data.firebase

import com.example.data.ai.GeminiModelOption
import com.example.data.local.ChatChannel
import com.example.data.local.MessageSpeaker
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreChatRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireSignedInUid(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("El usuario debe iniciar sesión con Google antes de acceder a Firestore.")
        check(uid == expectedUserId) { "La sesión activa cambió; operación cancelada por seguridad." }
        return uid
    }

    fun observeMessages(userId: String, channel: ChatChannel): Flow<List<CloudChatMessage>> {
        val path = "users/$userId/messages"
        return db.collection("users")
            .document(userId)
            .collection("messages")
            .whereEqualTo("channelId", channel.id)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CloudChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(
                    compareBy<CloudChatMessage> { it.sequenceOrder }
                        .thenBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val toLog = firestoreError ?: (error as? Exception)
                if (toLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(toLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observePreferences(userId: String): Flow<CloudUserPreferences?> {
        val path = "users/$userId"
        return db.collection("users")
            .document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        CloudUserPreferences::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val toLog = firestoreError ?: (error as? Exception)
                if (toLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(toLog, OperationType.GET, path)
                }
                throw error
            }
    }

    suspend fun ensurePreferencesInitialized(
        userId: String,
        defaultAffectionate: Boolean,
        defaultAutoVoice: Boolean,
        defaultModel: GeminiModelOption
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireSignedInUid(userId)
            val docRef = db.collection("users").document(userId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = (auth.currentUser?.displayName?.trim().orEmpty())
                    .ifEmpty { "Administrador Kirionce" }
                    .take(80)
                val payload = mapOf(
                    "userId" to userId,
                    "displayName" to safeName,
                    "affectionateMode" to defaultAffectionate,
                    "autoVoiceReply" to defaultAutoVoice,
                    "selectedModel" to defaultModel.modelId,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePreferences(
        userId: String,
        affectionateMode: Boolean? = null,
        autoVoiceReply: Boolean? = null,
        selectedModel: GeminiModelOption? = null
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireSignedInUid(userId)
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (affectionateMode != null) updates["affectionateMode"] = affectionateMode
            if (autoVoiceReply != null) updates["autoVoiceReply"] = autoVoiceReply
            if (selectedModel != null) updates["selectedModel"] = selectedModel.modelId

            db.collection("users").document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun getRecentMessages(
        userId: String,
        channel: ChatChannel,
        limit: Int = 16
    ): Result<List<CloudChatMessage>> {
        val path = "users/$userId/messages"
        return try {
            requireSignedInUid(userId)
            val snap = db.collection("users")
                .document(userId)
                .collection("messages")
                .whereEqualTo("channelId", channel.id)
                .get()
                .await()
            val sorted = snap.toObjects(
                CloudChatMessage::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedBy { it.sequenceOrder }
            Result.success(sorted.takeLast(limit))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun addMessage(
        userId: String,
        channel: ChatChannel,
        speaker: MessageSpeaker,
        content: String,
        errorFlag: Boolean = false,
        sequenceOrder: Long = System.currentTimeMillis()
    ): Result<String> {
        var path = "users/$userId/messages"
        return try {
            requireSignedInUid(userId)
            val safeContent = content.trim().take(12000)
            require(safeContent.isNotEmpty()) { "El mensaje no puede estar vacío." }
            val docRef = db.collection("users")
                .document(userId)
                .collection("messages")
                .document()
            path = docRef.path
            val payload = mapOf(
                "userId" to userId,
                "channelId" to channel.id,
                "speaker" to speaker.id,
                "content" to safeContent,
                "errorFlag" to errorFlag,
                "sequenceOrder" to sequenceOrder.coerceAtLeast(0L),
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun clearChannel(userId: String, channel: ChatChannel): Result<Unit> {
        val path = "users/$userId/messages"
        return try {
            requireSignedInUid(userId)
            val snapshot = db.collection("users")
                .document(userId)
                .collection("messages")
                .whereEqualTo("channelId", channel.id)
                .get()
                .await()
            if (!snapshot.isEmpty) {
                val batch = db.batch()
                snapshot.documents.forEach { doc -> batch.delete(doc.reference) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
