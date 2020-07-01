package com.saintmarina.recordingsystem.ui

import com.saintmarina.recordingsystem.service.RecordingService
import org.ocpsoft.prettytime.PrettyTime
import java.util.*

private val INSPIRATIONS = arrayOf(
    "You have great things to say",
    "You are an inspiration to others",
    "Speak from the heart",
    "Raise the frequency of the planet",
    "Your words reaches thousands"
)

private const val MILLIS_IN_MINUTE: Long = 60000
private const val FORGET_LAST_RECORDING_MINS: Long = 2*60

object StatusMessage {
    private var prettyTime = PrettyTime()

    class Content(val message: String, val isError: Boolean)

    fun fromState(state: RecordingService.State): Content {
        val isError: Boolean
        var message: String

        when {
            state.audioError != null -> {
                isError = true
                message = "Error: Contact Nico\nAudio failure: ${state.audioError}"
            }
            !state.micPlugged -> {
                isError = true
                message = "Error: Microphone seems disconnected\nCheck cable connections"
            }
            !state.internetAvailable && state.recorderState != RecordingService.RecorderState.RECORDING -> {
                isError = true
                message = "Warning: Internet connection lost\nRecording is operational\nUploading is delayed"
            }
            !state.powerAvailable -> {
                isError = true
                message = "Warning: Power outage detected\nThe system is running on battery"
            }
            state.fileSyncStatus != null && state.fileSyncStatus!!.error -> {
                isError = true
                message = state.fileSyncStatus!!.message
            }
            else -> {
                isError = false
                message = generateSuccessMessage(state)
            }
        }

        message = message.replace(".wav", "")
        return Content(message, isError)
    }

    private fun generateSuccessMessage(state: RecordingService.State): String {
        return when (state.recorderState) {
            RecordingService.RecorderState.IDLE -> {
                // IDLE
                state.fileSyncStatus?.let { fileSyncStatus ->
                    val ageOfStatusMessageMins = fileSyncStatus.date?.time?.let {
                        (Date().time - it) / MILLIS_IN_MINUTE
                    }
                    when (ageOfStatusMessageMins) {
                        null, 0L -> {
                            // Upload percent progress
                            fileSyncStatus.message
                        }
                        in 1..FORGET_LAST_RECORDING_MINS -> {
                            "${fileSyncStatus.message} ${prettyTime.format(fileSyncStatus.date)}"
                        }
                        else -> {
                            // It's been a while, display the message to get ready
                            null
                        }
                    }
                } ?: "Get ready to record\nDon't forget your water"
            }
            else -> {
                // RECORDING, PAUSED
                val quoteIndex = ((state.timeWhenStarted?.time ?: 0) % INSPIRATIONS.size).toInt()
                INSPIRATIONS[quoteIndex]
            }
        }
    }
}
