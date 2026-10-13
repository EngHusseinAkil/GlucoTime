package com.sugartrack.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class ProfileRepository(private val db: SugarTrackDatabase) {
    val all: Flow<List<UserProfile>> = db.profiles().observeAll()
    suspend fun ensureDefault(name: String): UserProfile {
        val existing = db.profiles().get(1)
        if (existing == null) {
            db.profiles().insert(UserProfile(id = 1, name = name.ifBlank { "User" }))
            return db.profiles().get(1)!!
        }
        if (name.isNotBlank() && existing.name == "User") {
            val updated = existing.copy(name = name.trim(), updatedAt = System.currentTimeMillis())
            db.profiles().update(updated)
            return updated
        }
        return existing
    }
    suspend fun add(name: String): Long = db.profiles().insert(UserProfile(name = name.trim()))
    suspend fun update(profile: UserProfile) = db.profiles().update(profile.copy(name = profile.name.trim(), updatedAt = System.currentTimeMillis()))
    suspend fun deleteData(profileId: Long) = db.withTransaction {
        db.readings().deleteForProfile(profileId)
        db.notes().deleteForProfile(profileId)
        db.profiles().get(profileId)?.let { db.profiles().delete(it) }
    }
}
