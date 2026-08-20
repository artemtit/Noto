package com.noto.app

import com.noto.app.domain.model.Priority
import org.junit.Assert.assertEquals
import org.junit.Test

class PriorityTest {

    @Test fun `fromString maps known values`() {
        assertEquals(Priority.LOW, Priority.fromString("low"))
        assertEquals(Priority.MEDIUM, Priority.fromString("medium"))
        assertEquals(Priority.HIGH, Priority.fromString("high"))
    }

    @Test fun `fromString case insensitive`() {
        assertEquals(Priority.HIGH, Priority.fromString("HIGH"))
        assertEquals(Priority.LOW, Priority.fromString("Low"))
    }

    @Test fun `fromString defaults to medium on unknown or null`() {
        assertEquals(Priority.MEDIUM, Priority.fromString(null))
        assertEquals(Priority.MEDIUM, Priority.fromString(""))
        assertEquals(Priority.MEDIUM, Priority.fromString("urgent"))
    }
}
