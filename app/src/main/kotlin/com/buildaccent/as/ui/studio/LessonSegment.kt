package com.buildaccent.`as`.ui.studio

data class LessonSegment(
    val startTimeMs: Long,
    val text: String
)

object SegmentParser {
    fun parse(rawText: String): List<LessonSegment> {
        val segments = mutableListOf<LessonSegment>()
        // Regex to match [mm:ss] or [m:ss]
        val regex = Regex("""\[(\d{1,2}):(\d{2})\](.*)""")
        
        val lines = rawText.lines()
        var currentTextAccumulator = StringBuilder()
        var currentStartTime = 0L
        var foundAnyTimestamp = false

        for (line in lines) {
            val match = regex.find(line.trim())
            if (match != null) {
                foundAnyTimestamp = true
                // If we have accumulated text for a previous segment, add it
                if (currentTextAccumulator.isNotEmpty()) {
                    segments.add(LessonSegment(currentStartTime, currentTextAccumulator.toString().trim()))
                    currentTextAccumulator.clear()
                }
                
                val minutes = match.groupValues[1].toLong()
                val seconds = match.groupValues[2].toLong()
                val content = match.groupValues[3].trim()
                
                currentStartTime = (minutes * 60 + seconds) * 1000
                currentTextAccumulator.append(content)
            } else {
                // Continuation of previous segment or plain text
                if (currentTextAccumulator.isNotEmpty()) {
                    currentTextAccumulator.append(" ").append(line.trim())
                } else {
                    currentTextAccumulator.append(line.trim())
                }
            }
        }
        
        // Add the final segment
        if (currentTextAccumulator.isNotEmpty()) {
            segments.add(LessonSegment(currentStartTime, currentTextAccumulator.toString().trim()))
        }

        // Fallback for non-timestamped text
        if (!foundAnyTimestamp && segments.size <= 1) {
            return listOf(LessonSegment(0, rawText))
        }

        return segments
    }
}
