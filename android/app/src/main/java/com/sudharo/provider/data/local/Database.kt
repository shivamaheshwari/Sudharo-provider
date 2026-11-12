package com.sudharo.provider.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "requirements")
data class RequirementEntity(
    @PrimaryKey val id: String,
    val title: String, val category: String, val location: String,
    val budgetMin: Int, val budgetMax: Int, val daysLeft: Int,
    val description: String, val postedAgo: String, val status: String = "open"
)

@Entity(tableName = "bids")
data class BidEntity(
    @PrimaryKey val id: String,
    val requirementId: String, val requirementTitle: String,
    val amount: Int, val days: Int, val notes: String, val status: String = "sent"
)

@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey val id: String,
    val title: String, val amount: Int, val days: Int,
    val status: String, val date: String
)

@Entity(tableName = "quotations")
data class QuotationEntity(
    @PrimaryKey val id: String,
    val jobId: String, val title: String,
    val labour: Int, val material: Int, val other: Int,
    val timelineDays: Int, val terms: String, val version: Int = 1,
    val status: String = "draft"
)

@Entity(tableName = "portfolio")
data class PortfolioEntity(
    @PrimaryKey val id: String,
    val title: String, val category: String, val imageUri: String
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String, val body: String, val timeAgo: String, val read: Boolean = false
)

@Entity(tableName = "provider_profile")
data class ProviderEntity(
    @PrimaryKey val id: String = "me",
    val name: String, val email: String, val phone: String = "",
    val city: String = "Mumbai", val categories: String = "",
    val experienceYears: Int = 0, val about: String = ""
)

@Dao
interface SudharoDao {
    @Query("SELECT * FROM requirements WHERE status='open' ORDER BY daysLeft ASC")
    fun requirements(): Flow<List<RequirementEntity>>
    @Query("SELECT * FROM requirements WHERE id=:id") suspend fun requirement(id: String): RequirementEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRequirements(items: List<RequirementEntity>)

    @Query("SELECT * FROM bids ORDER BY id DESC") fun bids(): Flow<List<BidEntity>>
    @Query("SELECT COUNT(*) FROM bids WHERE requirementId=:reqId") suspend fun bidCountFor(reqId: String): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBid(bid: BidEntity)

    @Query("SELECT * FROM jobs ORDER BY id DESC") fun jobs(): Flow<List<JobEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertJobs(items: List<JobEntity>)
    @Query("UPDATE jobs SET status='completed' WHERE id=:id") suspend fun completeJob(id: String)

    @Query("SELECT * FROM quotations ORDER BY id DESC") fun quotations(): Flow<List<QuotationEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertQuotation(q: QuotationEntity)
    @Query("UPDATE quotations SET status=:status WHERE id=:id") suspend fun setQuotationStatus(id: String, status: String)

    @Query("SELECT * FROM portfolio ORDER BY id DESC") fun portfolio(): Flow<List<PortfolioEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPortfolio(item: PortfolioEntity)

    @Query("SELECT * FROM notifications ORDER BY id DESC") fun notifications(): Flow<List<NotificationEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertNotifications(items: List<NotificationEntity>)
    @Query("UPDATE notifications SET read=1 WHERE id=:id") suspend fun markNotificationRead(id: String)

    @Query("SELECT * FROM provider_profile WHERE id='me'") suspend fun profile(): ProviderEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveProfile(p: ProviderEntity)
}

@Database(
    entities = [RequirementEntity::class, BidEntity::class, JobEntity::class,
        QuotationEntity::class, PortfolioEntity::class, NotificationEntity::class,
        ProviderEntity::class],
    version = 1, exportSchema = false
)
abstract class SudharoDatabase : RoomDatabase() {
    abstract fun dao(): SudharoDao

    companion object {
        @Volatile private var INSTANCE: SudharoDatabase? = null
        fun get(context: Context): SudharoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context, SudharoDatabase::class.java, "sudharo.db")
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
