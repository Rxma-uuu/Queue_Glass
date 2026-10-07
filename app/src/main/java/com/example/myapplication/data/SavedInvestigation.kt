package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A saved investigation: the complete evidence behind one experiment run so it
 * can be inspected later. Everything is actual engine output, never fabricated.
 */
@Entity(tableName = "investigations")
data class SavedInvestigation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val savedAtMs: Long,
    val seed: Long,
    /** Chart interval in ms used when the experiment ran. */
    val chartIntervalMs: Int,
    /** Events stepped into the engine before the experiment. */
    val eventsStepped: Int,
    /** Experiment input parameters. */
    val side: String,
    val limitPriceTicks: Long,
    val qtyUnits: Long,
    val startSeq: Long,
    val endSeq: Long,
    /** Actual experiment outputs from the C++ engine. */
    val filledQty: Long,
    val unfilledQty: Long,
    val avgFillPrice: Double,
    val totalNotional: Double,
    val fees: Double,
    val slippageVsArrivalMid: Double,
    val arrivalMidTicks: Long,
    val levelsConsumed: Int,
    val gapsDetected: Long,
    /** Number of fills retained in evidence (capped for storage). */
    val fillCount: Int,
    /** Measured engine perf at experiment time. */
    val replayNs: Long,
    val experimentNs: Long,
    /** User note. */
    val note: String,
)
