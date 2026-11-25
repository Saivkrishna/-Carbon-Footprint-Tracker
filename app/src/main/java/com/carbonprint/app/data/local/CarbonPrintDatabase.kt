package com.carbonprint.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.carbonprint.app.data.local.dao.BadgeDao
import com.carbonprint.app.data.local.dao.DailySummaryDao
import com.carbonprint.app.data.local.dao.GoalDao
import com.carbonprint.app.data.local.dao.TripDao
import com.carbonprint.app.data.local.dao.WaypointDao
import com.carbonprint.app.data.local.entity.BadgeEntity
import com.carbonprint.app.data.local.entity.DailySummaryEntity
import com.carbonprint.app.data.local.entity.GoalEntity
import com.carbonprint.app.data.local.entity.TripEntity
import com.carbonprint.app.data.local.entity.WaypointEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Main Room Database for CarbonPrint.
 */
@Database(
    entities = [
        TripEntity::class,
        WaypointEntity::class,
        GoalEntity::class,
        BadgeEntity::class,
        DailySummaryEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CarbonPrintDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao
    abstract fun waypointDao(): WaypointDao
    abstract fun goalDao(): GoalDao
    abstract fun badgeDao(): BadgeDao
    abstract fun dailySummaryDao(): DailySummaryDao

    companion object {
        @Volatile
        private var INSTANCE: CarbonPrintDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trips ADD COLUMN stepCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE daily_summaries ADD COLUMN runDistanceMeters REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE daily_summaries ADD COLUMN motorcycleDistanceMeters REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE daily_summaries ADD COLUMN carDistanceMeters REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE daily_summaries ADD COLUMN walkingSteps INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE daily_summaries ADD COLUMN runningSteps INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trips ADD COLUMN walkingSteps INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE trips ADD COLUMN runningSteps INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE trips ADD COLUMN walkingDurationSeconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE trips ADD COLUMN runningDurationSeconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE trips ADD COLUMN startBatteryPct INTEGER")
                db.execSQL("ALTER TABLE trips ADD COLUMN endBatteryPct INTEGER")
                db.execSQL("ALTER TABLE trips ADD COLUMN batteryDropPct INTEGER")
            }
        }

        fun getInstance(context: Context): CarbonPrintDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CarbonPrintDatabase::class.java,
                    "carbonprint_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedInitialBadges(database.badgeDao())
                    }
                }
            }

            private suspend fun seedInitialBadges(badgeDao: BadgeDao) {
                val defaultBadges = listOf(
                    BadgeEntity(
                        id = "first_green_mile",
                        title = "First Green Mile",
                        description = "Complete your first 1 km of walking or cycling",
                        category = "DISTANCE",
                        iconName = "directions_walk",
                        currentProgress = 0.0,
                        targetProgress = 1000.0,
                        isUnlocked = false
                    ),
                    BadgeEntity(
                        id = "zero_emission_day",
                        title = "Zero Emission Day",
                        description = "Complete all daily travel with zero carbon footprint",
                        category = "EMISSION",
                        iconName = "eco",
                        currentProgress = 0.0,
                        targetProgress = 1.0,
                        isUnlocked = false
                    ),
                    BadgeEntity(
                        id = "cycling_champion",
                        title = "Cycling Champion",
                        description = "Log over 25 km of bicycle rides",
                        category = "DISTANCE",
                        iconName = "directions_bike",
                        currentProgress = 0.0,
                        targetProgress = 25000.0,
                        isUnlocked = false
                    ),
                    BadgeEntity(
                        id = "century_walker",
                        title = "Century Walker",
                        description = "Walk a total distance of 50 km",
                        category = "DISTANCE",
                        iconName = "directions_walk",
                        currentProgress = 0.0,
                        targetProgress = 50000.0,
                        isUnlocked = false
                    ),
                    BadgeEntity(
                        id = "carbon_cutter",
                        title = "Carbon Cutter",
                        description = "Save 10 kg of CO2 compared to driving an average car",
                        category = "EMISSION",
                        iconName = "forest",
                        currentProgress = 0.0,
                        targetProgress = 10000.0,
                        isUnlocked = false
                    ),
                    BadgeEntity(
                        id = "goal_crusher",
                        title = "Goal Crusher",
                        description = "Successfully meet your weekly carbon reduction target",
                        category = "HABIT",
                        iconName = "emoji_events",
                        currentProgress = 0.0,
                        targetProgress = 1.0,
                        isUnlocked = false
                    )
                )
                badgeDao.insertBadges(defaultBadges)
            }
        }
    }
}
