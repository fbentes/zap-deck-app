package br.com.facbentes.zapdeck.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val primaryPhone: String,
    val secondaryPhone: String,
    val landlinePhone: String = "",
    val email: String = "",
    val address: String,
    val observations: String,
    val imageBase64: String, // Storing base64 JPEG of front business card image
    val backImageBase64: String = "", // Storing base64 JPEG of back business card image
    val createdAt: Long = System.currentTimeMillis(),
    val instagram: String = "",
    val useWhatsAppBusiness: Boolean = false,
    val instagramFollowed: Boolean = false
)
