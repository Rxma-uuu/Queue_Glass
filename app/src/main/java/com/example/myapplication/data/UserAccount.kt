package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Account entity for QUEUEGLASS user authentication.
 * Passwords are never stored as plaintext; they are hashed using salted SHA-256.
 */
@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey val email: String,
    val passwordHash: String,
    val salt: String,
    val fullName: String = "",
    val createdAtMs: Long = System.currentTimeMillis(),
    val lastLoginMs: Long = System.currentTimeMillis()
)
