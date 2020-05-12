package com.saintmarina.recordingsystem

private const val SEC_IN_NANO: Long = 1_000_000_000

object Util {
    @JvmStatic fun formatAudioDuration(totalSeconds: Int): String {
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / (60 * 60)

        return if (hours > 0)
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        else
            String.format("%02d:%02d", minutes, seconds)
    }

    @JvmStatic fun nanosToSec(time: Long): Int {
        return (time / SEC_IN_NANO).toInt()
    }
}
//https://drive.google.com/drive/folders/1Doloxa7z3FozwBdBXUDykTZu_B1AUnHB?usp=sharing


//1)
//hello.txt
//"Hello world"

  //      2) set metadata "audio/wave"