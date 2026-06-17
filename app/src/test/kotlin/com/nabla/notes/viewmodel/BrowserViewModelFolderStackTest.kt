package com.nabla.notes.viewmodel

import android.content.Context
import com.nabla.notes.auth.MsalManager
import com.nabla.notes.model.AppSettings
import com.nabla.notes.repository.OneDriveRepository
import com.nabla.notes.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrowserViewModelFolderStackTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var msalManager: MsalManager
    private lateinit var oneDriveRepository: OneDriveRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = mockk(relaxed = true)
        msalManager = mockk(relaxed = true)
        oneDriveRepository = mockk(relaxed = true)
        context = mockk(relaxed = true)

        // settings is a Flow property, not a suspend function
        every { settingsRepository.settings } returns flowOf(AppSettings())
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init restores non-empty folder stack from repository`() = runTest {
        val savedStack = listOf(Pair("abc", "Projects"), Pair("def", "2026"))
        coEvery { settingsRepository.loadLastFolderStack() } returns savedStack

        val vm = BrowserViewModel(context, msalManager, oneDriveRepository, settingsRepository)
        advanceUntilIdle()

        assertEquals(savedStack, vm.folderStack.value)
    }

    @Test
    fun `init keeps empty stack when repository returns empty`() = runTest {
        coEvery { settingsRepository.loadLastFolderStack() } returns emptyList()

        val vm = BrowserViewModel(context, msalManager, oneDriveRepository, settingsRepository)
        advanceUntilIdle()

        assertTrue(vm.folderStack.value.isEmpty())
    }

    @Test
    fun `init collector persists stack at least once`() = runTest {
        coEvery { settingsRepository.loadLastFolderStack() } returns emptyList()

        val vm = BrowserViewModel(context, msalManager, oneDriveRepository, settingsRepository)
        advanceUntilIdle()

        // Collector fires on initial value emission
        coVerify(atLeast = 1) { settingsRepository.saveLastFolderStack(any()) }
    }
}
