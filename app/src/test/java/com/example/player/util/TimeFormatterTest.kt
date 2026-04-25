package com.example.player.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatterTest {

    @Test
    fun `formats seconds correctly`() {
        assertEquals("00:00", 0L.formatDuration())
        assertEquals("00:01", 1_000L.formatDuration())
        assertEquals("01:00", 60_000L.formatDuration())
        assertEquals("01:23", 83_000L.formatDuration())
    }

    @Test
    fun `formats hours correctly`() {
        assertEquals("1:00:00", 3_600_000L.formatDuration())
        assertEquals("1:30:45", 5_445_000L.formatDuration())
        assertEquals("10:00:00", 36_000_000L.formatDuration())
    }

    @Test
    fun `pads minutes and seconds`() {
        assertEquals("01:02", 62_000L.formatDuration())
        assertEquals("10:05", 605_000L.formatDuration())
    }
}
