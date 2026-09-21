package com.example.core.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeUtils {

    /**
     * Formats milliseconds to MM:SS or HH:MM:SS.
     */
    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0) return "00:00"

        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Formats milliseconds to MM:SS:ff (where ff is frames, assuming default 30fps)
     */
    fun formatTimecode(timeMs: Long, fps: Int = 30): String {
        val totalSeconds = timeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val remMs = timeMs % 1000
        val frame = ((remMs / 1000f) * fps).toInt()

        return String.format(Locale.getDefault(), "%02d:%02d:%02d", minutes, seconds, frame)
    }

    /**
     * Formats timestamp to a human-readable string like "Just now", "5m ago", "Yesterday", or "MMM dd, yyyy"
     */
    fun formatLastEdited(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        if (diff < 0) return "Just now"

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            days < 7 -> "${days}d ago"
            else -> {
                val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}
