package com.nabla.notes

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class — required for Hilt dependency injection.
 */
@HiltAndroidApp
class NotepadApp : Application()
