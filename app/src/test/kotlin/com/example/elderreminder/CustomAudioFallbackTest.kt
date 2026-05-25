package com.example.elderreminder

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CustomAudioFallbackTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var filesDir: File

    @Before
    fun setUp() {
        filesDir = tempFolder.newFolder("files")
    }

    @Test
    fun testGetCustomAudioFile_Curfew() {
        // When curfew is true, it should always return custom_reminder_curfew.3gp
        val file = SpeechReminder.getCustomAudioFile(filesDir, isCurfew = true)
        assertThat(file.name).isEqualTo("custom_reminder_curfew.3gp")
        assertThat(file.parentFile).isEqualTo(filesDir)
    }

    @Test
    fun testGetCustomAudioFile_Regular_EmptyDir() {
        // When curfew is false and directory is empty, it should return custom_reminder_regular.3gp
        val file = SpeechReminder.getCustomAudioFile(filesDir, isCurfew = false)
        assertThat(file.name).isEqualTo("custom_reminder_regular.3gp")
    }

    @Test
    fun testGetCustomAudioFile_Regular_WithOldFile() {
        // When curfew is false and custom_reminder_regular.3gp doesn't exist,
        // but old custom_reminder.3gp exists, it should return custom_reminder.3gp
        val oldFile = File(filesDir, "custom_reminder.3gp")
        oldFile.createNewFile()

        val file = SpeechReminder.getCustomAudioFile(filesDir, isCurfew = false)
        assertThat(file.name).isEqualTo("custom_reminder.3gp")
    }

    @Test
    fun testGetCustomAudioFile_Regular_WithBothFiles() {
        // When curfew is false, if custom_reminder_regular.3gp exists,
        // even if custom_reminder.3gp exists, it should prefer custom_reminder_regular.3gp
        val oldFile = File(filesDir, "custom_reminder.3gp")
        oldFile.createNewFile()
        val regularFile = File(filesDir, "custom_reminder_regular.3gp")
        regularFile.createNewFile()

        val file = SpeechReminder.getCustomAudioFile(filesDir, isCurfew = false)
        assertThat(file.name).isEqualTo("custom_reminder_regular.3gp")
    }
}
