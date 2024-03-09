package com.tibagni.logviewer

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.File
import java.nio.charset.StandardCharsets

class LogsRepositoryTests {
  private var temporaryFiles: Array<File>? = null
  private lateinit var logsRepository: LogsRepository

  @Mock
  private lateinit var mockProgressReporter: ProgressReporter

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)
    logsRepository = LogsRepositoryImpl()
  }

  @After
  fun tearDown() {
    // Cleanup any temporary files created by the test case (if any)
    temporaryFiles?.forEach { it.delete() }
  }

  private fun createTempLogFiles(vararg names: String): Array<File> {
    val files = names.map {
      File.createTempFile(it, "txt").apply {
        writeText(
          "01-06 20:46:26.091 821-2168/? V/ThermalMonitor: Foreground Application Changed: com.voidcorporation.carimbaai\n" +
              "01-06 20:46:26.091 821-2168/? D/ThermalMonitor: Foreground Application Changed: com.voidcorporation.carimbaai\n" +
              "01-06 20:46:42.501 821-2810/? I/ActivityManager: Process com.voidcorporation.carimbaai (pid 25175) (adj 0) has died.\n" +
              "01-06 20:46:39.491 821-1054/? W/ActivityManager:   Force finishing activity com.voidcorporation.carimbaai/.UserProfileActivity\n" +
              "01-06 20:46:39.481 25175-25175/? E/AndroidRuntime: FATAL EXCEPTION: main"
        )
      }
    }

    temporaryFiles = files.toTypedArray()
    return temporaryFiles!!
  }

  private fun createTempLogEmptyFiles(vararg names: String): Array<File> {
    val files = names.map {
      File.createTempFile(it, "txt")
    }

    temporaryFiles = files.toTypedArray()
    return temporaryFiles!!
  }

  @Test
  fun testOpenSingleLogFile() {
    val temporaryLogFile = createTempLogFiles("log")
    logsRepository.openLogFiles(temporaryLogFile, StandardCharsets.UTF_8, mockProgressReporter)

    verify(mockProgressReporter, atLeastOnce()).onProgress(anyInt(), anyString())
    verify(mockProgressReporter, never()).failProgress()
    assertEquals(1, logsRepository.availableStreams.size)
    assertEquals(1, logsRepository.currentlyOpenedLogFiles.size)
    assertEquals(5, logsRepository.currentlyOpenedLogs.size)
  }

  @Test
  fun testOpenMultipleLogFilesOneStream() {
    val temporaryLogFiles = createTempLogFiles("log", "log2")

    logsRepository.openLogFiles(temporaryLogFiles, StandardCharsets.UTF_8, mockProgressReporter)

    verify(mockProgressReporter, atLeastOnce()).onProgress(anyInt(), anyString())
    verify(mockProgressReporter, never()).failProgress()
    assertEquals(1, logsRepository.availableStreams.size)
    assertEquals(2, logsRepository.currentlyOpenedLogFiles.size)
    assertEquals(10, logsRepository.currentlyOpenedLogs.size)
  }

  @Test
  fun testOpenMultipleLogFilesMultipleStreams() {
    val temporaryLogFiles = createTempLogFiles("main", "system")

    logsRepository.openLogFiles(temporaryLogFiles, StandardCharsets.UTF_8, mockProgressReporter)

    verify(mockProgressReporter, atLeastOnce()).onProgress(anyInt(), anyString())
    verify(mockProgressReporter, never()).failProgress()
    assertEquals(2, logsRepository.availableStreams.size)
    assertEquals(2, logsRepository.currentlyOpenedLogFiles.size)
    assertEquals(10, logsRepository.currentlyOpenedLogs.size)
  }

  @Test
  fun testOpenEmptyLogFiles() {
    val temporaryLogFiles = createTempLogEmptyFiles("main")

    logsRepository.openLogFiles(temporaryLogFiles, StandardCharsets.UTF_8, mockProgressReporter)

    verify(mockProgressReporter, atLeastOnce()).onProgress(anyInt(), anyString())
    verify(mockProgressReporter, never()).failProgress()
    assertEquals(0, logsRepository.currentlyOpenedLogFiles.size)
    assertEquals(0, logsRepository.currentlyOpenedLogs.size)
  }

  @Test(expected = OpenLogsException::class)
  fun testOpenLogFilesInvalidFiles() {
    val temporaryInvalidLogFile = File("invalid")

    logsRepository.openLogFiles(arrayOf(temporaryInvalidLogFile), StandardCharsets.UTF_8, mockProgressReporter)
  }

  @Test
  fun testVisibleLogsSublist() {
    testOpenSingleLogFile()
    logsRepository.firstVisibleLogIndex = 2
    assertEquals(3, logsRepository.currentlyOpenedLogs.size)

    logsRepository.lastVisibleLogIndex = 3
    assertEquals(2, logsRepository.currentlyOpenedLogs.size)
  }

  @Test
  fun testResetVisibleLogs() {
    testVisibleLogsSublist()
    logsRepository.firstVisibleLogIndex = -1
    assertEquals(0, logsRepository.firstVisibleLogIndex)
    assertEquals(4, logsRepository.currentlyOpenedLogs.size)

    logsRepository.lastVisibleLogIndex = -1
    assertEquals(4, logsRepository.lastVisibleLogIndex)
    assertEquals(5, logsRepository.currentlyOpenedLogs.size)
  }

  @Test
  fun testResetVisibleLogsOnLoad() {
    testVisibleLogsSublist()
    testOpenSingleLogFile()

    assertEquals(0, logsRepository.firstVisibleLogIndex)
    assertEquals(4, logsRepository.lastVisibleLogIndex)
    assertEquals(5, logsRepository.currentlyOpenedLogs.size)
  }
}