package com.app.pustakam.android.di

import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

import com.app.pustakam.android.hardware.audio.recorder.AudioRecorder
import com.app.pustakam.android.hardware.audio.recorder.IAudioRecorder
import com.app.pustakam.android.services.mediaSessionService.MediaNotificationManager
import com.app.pustakam.android.services.mediaSessionService.MediaServiceListener


import org.koin.core.module.Module
import org.koin.dsl.module

@OptIn(UnstableApi::class)
fun getAndroidSpecifics(): Module =
   module {
       single<ExoPlayer> {
           val audioAttributes = AudioAttributes.Builder()
               .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
               .setUsage(C.USAGE_MEDIA)
               .build()
           ExoPlayer.Builder(get())
           .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
           .setHandleAudioBecomingNoisy(true)
           .setTrackSelector(DefaultTrackSelector(get()))
           .build()
       }
       single <IAudioRecorder>{ AudioRecorder(get()) }
       single <MediaNotificationManager> {  MediaNotificationManager(
           context =  get(),
           exoPlayer = get()
       )}
       single <MediaServiceListener>{ MediaServiceListener(get())  }
   }
