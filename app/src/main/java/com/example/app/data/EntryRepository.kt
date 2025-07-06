package com.example.app.data

import android.net.Uri
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.example.app.model.Entry
import javax.inject.Inject

class EntryRepository @Inject constructor(
    private val ref: DatabaseReference,
    private val storage: FirebaseStorage
) {
    suspend fun add(uid: String, e: Entry, photo: Uri?, audio: Uri?): Result<Unit> = try {
        val key = ref.child("entries").child(uid).push().key!!
        val photoUrl = photo?.let { uploadPhoto(key, it) }
        val audioUrl = audio?.let { uploadAudio(key, it) }

        val entry = e.copy(
            id = key,
            photoUrl = photoUrl,
            audioUrl = audioUrl
        )

        ref.child("entries/$uid/$key").setValue(entry).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun uploadPhoto(key: String, uri: Uri): String {
        val ref = storage.reference.child("photos/$key.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    private suspend fun uploadAudio(key: String, uri: Uri): String {
        val ref = storage.reference.child("audio/$key.aac")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    fun flow(uid: String): Flow<List<Entry>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val entries = snapshot.children.mapNotNull {
                    it.getValue(Entry::class.java)
                }.sortedByDescending { it.timestamp }
                trySend(entries)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        val entriesRef = ref.child("entries/$uid")
        entriesRef.addValueEventListener(listener)

        awaitClose {
            entriesRef.removeEventListener(listener)
        }
    }

    suspend fun delete(uid: String, entryId: String) {
        ref.child("entries/$uid/$entryId").removeValue().await()
    }
}
