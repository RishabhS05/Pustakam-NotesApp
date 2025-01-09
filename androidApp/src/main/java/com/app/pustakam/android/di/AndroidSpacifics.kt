package com.app.pustakam.android.di

import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.app.pustakam.android.hardware.audio.AudioRecorder
import com.app.pustakam.android.hardware.audio.IAudioRecorder
import com.app.pustakam.android.notification.AppNotificationManager
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext

import org.koin.core.module.Module
import org.koin.dsl.module

@OptIn(UnstableApi::class)
fun getAndroidSpecifics(): Module =
   module {
       single<ExoPlayer> {
           ExoPlayer.Builder(androidApplication())
           .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus= */ true)
           .setHandleAudioBecomingNoisy(true)
           .setTrackSelector(DefaultTrackSelector(androidContext()))
           .build()
       }
       single <AppNotificationManager> { AppNotificationManager(get()) }
       single <IAudioRecorder>{ AudioRecorder(get())  }
   }
