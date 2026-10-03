package org.sangyan.shield.data.local
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Entity(tableName="history")
data class HistoryEntry(@PrimaryKey(autoGenerate=true) val id:Long=0, val timestamp:Long, val score:Int, val level:String, val preview:String, val resultJson:String)
@Dao interface HistoryDao {
 @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 100") fun observe():Flow<List<HistoryEntry>>
 @Insert suspend fun insert(entry:HistoryEntry)
 @Query("DELETE FROM history") suspend fun clear()
 @Query("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY timestamp DESC LIMIT 100)") suspend fun trim()
}
@Database(entities=[HistoryEntry::class],version=1,exportSchema=false)
abstract class ShieldDatabase:RoomDatabase() { abstract fun history():HistoryDao }
