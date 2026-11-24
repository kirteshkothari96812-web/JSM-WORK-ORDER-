package com.example.data.local

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
import androidx.room.Transaction
import com.example.data.model.OrderItem
import com.example.data.model.WorkOrder
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "work_orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val partyName: String,
    val orderDate: String,
    val deliveryDays: Int,
    val expectedDeliveryDate: String,
    val priority: String,
    val takenBy: String,
    val itemsJson: String,
    val totalAmount: Double,
    val status: String,
    val completionDate: String?,
    val creditDays: Int,
    val paymentDueDate: String?,
    val paidAmount: Double,
    val updatedAt: Long,
    val pendingSyncAction: String? = null
)

object OrderEntityMapper {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val itemsListType = Types.newParameterizedType(List::class.java, OrderItem::class.java)
    private val itemsAdapter = moshi.adapter<List<OrderItem>>(itemsListType)

    fun serializeItems(items: List<OrderItem>): String {
        return itemsAdapter.toJson(items)
    }

    fun deserializeItems(json: String): List<OrderItem> {
        return try {
            itemsAdapter.fromJson(json) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun toEntity(order: WorkOrder, pendingSyncAction: String? = null): OrderEntity {
        val normalizedItems = order.items.mapIndexed { idx, item ->
            item.copy(
                billingType = order.billingTypeEnum.label,
                taxPercent = order.taxPercent,
                paymentMode = if (idx == 0) order.paymentMode else null,
                paymentReceiver = if (idx == 0) order.paymentReceiver else null,
                paymentDate = if (idx == 0) order.paymentDate else null,
                paymentLog = if (idx == 0) order.paymentLog else null
            )
        }
        return OrderEntity(
            id = order.id,
            partyName = order.partyName,
            orderDate = order.orderDate,
            deliveryDays = order.deliveryDays,
            expectedDeliveryDate = order.expectedDeliveryDate,
            priority = order.priority,
            takenBy = order.takenBy,
            itemsJson = serializeItems(normalizedItems),
            totalAmount = order.totalAmount,
            status = order.status,
            completionDate = order.completionDate,
            creditDays = order.creditDays,
            paymentDueDate = order.paymentDueDate,
            paidAmount = order.paidAmount,
            updatedAt = order.updatedAt,
            pendingSyncAction = pendingSyncAction
        )
    }

    fun toDomain(entity: OrderEntity): WorkOrder {
        val parsedItems = deserializeItems(entity.itemsJson)
        val billingType = parsedItems.firstNotNullOfOrNull { it.billingType }
            ?: com.example.data.model.BillingType.NON_GST.label
        val taxPercent = parsedItems.firstNotNullOfOrNull { it.taxPercent } ?: 0.0
        val paymentMode = parsedItems.firstNotNullOfOrNull { it.paymentMode }
        val paymentReceiver = parsedItems.firstNotNullOfOrNull { it.paymentReceiver }
        val paymentDate = parsedItems.firstNotNullOfOrNull { it.paymentDate }
        val paymentLog = parsedItems.firstNotNullOfOrNull { it.paymentLog }
        return WorkOrder(
            id = entity.id,
            partyName = entity.partyName,
            orderDate = entity.orderDate,
            deliveryDays = entity.deliveryDays,
            expectedDeliveryDate = entity.expectedDeliveryDate,
            priority = entity.priority,
            takenBy = entity.takenBy,
            items = parsedItems,
            totalAmount = entity.totalAmount,
            status = entity.status,
            completionDate = entity.completionDate,
            creditDays = entity.creditDays,
            paymentDueDate = entity.paymentDueDate,
            paidAmount = entity.paidAmount,
            updatedAt = entity.updatedAt,
            billingType = billingType,
            taxPercent = taxPercent,
            itemsSummary = WorkOrder.buildItemsSummary(parsedItems, billingType, taxPercent, paymentLog),
            paymentMode = paymentMode,
            paymentReceiver = paymentReceiver,
            paymentDate = paymentDate,
            paymentLog = paymentLog
        )
    }
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM work_orders WHERE pendingSyncAction IS NULL OR pendingSyncAction != 'delete'")
    fun observeVisibleOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM work_orders WHERE pendingSyncAction IS NOT NULL")
    suspend fun getPendingSyncOrders(): List<OrderEntity>

    @Query("SELECT COUNT(*) FROM work_orders WHERE pendingSyncAction IS NOT NULL")
    suspend fun getPendingSyncCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrders(orders: List<OrderEntity>)

    @Query("DELETE FROM work_orders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM work_orders WHERE pendingSyncAction IS NULL")
    suspend fun deleteAllSynced()

    @Transaction
    suspend fun replaceSyncedOrders(remoteOrders: List<OrderEntity>) {
        val pendingLocal = getPendingSyncOrders().associateBy { it.id }
        deleteAllSynced()
        val toInsert = remoteOrders.filter { !pendingLocal.containsKey(it.id) }
        if (toInsert.isNotEmpty()) {
            upsertOrders(toInsert)
        }
    }
}

@Database(entities = [OrderEntity::class], version = 1, exportSchema = false)
abstract class JsmDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao

    companion object {
        @Volatile
        private var INSTANCE: JsmDatabase? = null

        fun getInstance(context: Context): JsmDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    JsmDatabase::class.java,
                    "jsm_industries_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
