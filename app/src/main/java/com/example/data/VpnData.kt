package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "vpn_configs")
data class VpnConfig(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val server: String,
    val port: Int,
    val type: String, // VLESS, VMess, Shadowsocks, Trojan, SSH
    val uuid: String = "",
    val password: String = "",
    val isActive: Boolean = false,
    val ping: Int = -1
)

@Entity(tableName = "vpn_logs")
data class VpnLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val level: String = "INFO" // INFO, DEBUG, WARN, ERROR
)

@Dao
interface VpnDao {
    @Query("SELECT * FROM vpn_configs ORDER BY id DESC")
    fun getAllConfigs(): Flow<List<VpnConfig>>

    @Query("SELECT * FROM vpn_configs WHERE isActive = 1 LIMIT 1")
    fun getActiveConfig(): Flow<VpnConfig?>

    @Query("SELECT * FROM vpn_configs WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveConfigSync(): VpnConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: VpnConfig)

    @Update
    suspend fun updateConfig(config: VpnConfig)

    @Query("UPDATE vpn_configs SET isActive = 0")
    suspend fun deactivateAll()

    @Transaction
    suspend fun setActiveConfig(configId: Int) {
        deactivateAll()
        updateActiveStatus(configId, true)
    }

    @Query("UPDATE vpn_configs SET isActive = :isActive WHERE id = :configId")
    suspend fun updateActiveStatus(configId: Int, isActive: Boolean)

    @Query("UPDATE vpn_configs SET ping = :ping WHERE id = :configId")
    suspend fun updatePing(configId: Int, ping: Int)

    @Delete
    suspend fun deleteConfig(config: VpnConfig)

    @Query("SELECT * FROM vpn_logs ORDER BY timestamp DESC LIMIT 200")
    fun getRecentLogs(): Flow<List<VpnLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: VpnLog)

    @Query("DELETE FROM vpn_logs")
    suspend fun clearLogs()
}

@Database(entities = [VpnConfig::class, VpnLog::class], version = 1, exportSchema = false)
abstract class VpnDatabase : RoomDatabase() {
    abstract fun vpnDao(): VpnDao

    companion object {
        @Volatile
        private var INSTANCE: VpnDatabase? = null

        fun getDatabase(context: Context): VpnDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VpnDatabase::class.java,
                    "pyaephyohan_vpn_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
