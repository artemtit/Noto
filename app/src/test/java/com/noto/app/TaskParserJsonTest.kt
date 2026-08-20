package com.noto.app

import com.noto.app.ai.OpenAiTaskParser
import com.noto.app.data.prefs.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class TaskParserJsonTest {

    private val parser = OpenAiTaskParser(mock(SettingsRepository::class.java))

    @Test fun `parses valid tasks array`() {
        val json = """
            {"tasks":[
              {"title":"Do it","description":null,"dueDate":"2026-08-21","dueTime":"12:00","priority":"medium","project":null,"reminder":true},
              {"title":"Другое","description":"note","dueDate":null,"dueTime":null,"priority":"high","project":"Work","reminder":false}
            ]}
        """.trimIndent()
        val list = parser.parseTasksJson(json)
        assertEquals(2, list.size)
        assertEquals("Do it", list[0].title)
        assertEquals("2026-08-21", list[0].dueDate.toString())
        assertEquals("12:00", list[0].dueTime.toString())
        assertEquals("Другое", list[1].title)
        assertNull(list[1].dueDate)
        assertNull(list[1].dueTime)
        assertEquals("Work", list[1].projectName)
        assertTrue(!list[1].reminder)
    }

    @Test fun `ignores malformed items but keeps valid ones`() {
        val json = """{"tasks":[{"title":""},{"title":"OK"}]}"""
        val list = parser.parseTasksJson(json)
        assertEquals(1, list.size)
        assertEquals("OK", list[0].title)
    }

    @Test fun `empty tasks yields empty list`() {
        val list = parser.parseTasksJson("""{"tasks":[]}""")
        assertTrue(list.isEmpty())
    }
}
