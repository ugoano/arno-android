package network.arno.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String = "New Chat",
    val preview: String = "",
    val messageCount: Int = 0,
    val lastActivity: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
)
