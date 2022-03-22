package com.saintmarina.recordingsystem

import android.util.Log
import java.io.File

private const val TAG = "Destination"

private const val TEST_MODE = true
private const val TEST_MODE_DRIVE_ID = "XXX"

//Global list of destinations
val DESTINATIONS = arrayOf(
    Destination(
        cardName = "Lumeri",
        localDir = File("/sdcard/Recordings/Lumeri"),
        driveID = "XXX",
        imgPath = R.drawable.card_lumeri
    ),
    Destination(
        cardName = "Japan",
        localDir = File("/sdcard/Recordings/Japan"),
        driveID = "XXX",
        imgPath = R.drawable.card_japan
    ),
    Destination(
        cardName = "Personal",
        localDir = File("/sdcard/Recordings/Personal"),
        driveID = "XXX",
        imgPath = R.drawable.card_personal
    )
)

class Destination(
    val cardName: String,
    val localDir: File,
    var driveID: String,
    val imgPath: Int) {

    init {
        if (TEST_MODE)
            driveID = TEST_MODE_DRIVE_ID
    }
}