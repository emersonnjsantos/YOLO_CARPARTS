package com.emerson.carpartsyolo.ui.main

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MainScreenViewModelTest {
  @Test
  fun uiState_initialValues() = runTest {
    val viewModel = MainScreenViewModel()
    val state = viewModel.uiState.first()

    assertEquals(AppMode.LIVE_CAMERA, state.mode)
    assertFalse(state.isModelLoaded)
    assertEquals(0.25f, state.confThreshold)
    assertEquals(0.45f, state.iouThreshold)
    assertTrue(state.showBoxes)
    assertTrue(state.showLabels)
    assertTrue(state.showMasks)
  }

  @Test
  fun uiState_thresholdUpdates() = runTest {
    val viewModel = MainScreenViewModel()
    viewModel.updateConfThreshold(0.50f)
    viewModel.updateIouThreshold(0.60f)

    val state = viewModel.uiState.value
    assertEquals(0.50f, state.confThreshold)
    assertEquals(0.60f, state.iouThreshold)
  }

  @Test
  fun uiState_modeSwitch() = runTest {
    val viewModel = MainScreenViewModel()
    viewModel.setMode(AppMode.GALLERY_IMAGE)
    assertEquals(AppMode.GALLERY_IMAGE, viewModel.uiState.value.mode)
  }
}
