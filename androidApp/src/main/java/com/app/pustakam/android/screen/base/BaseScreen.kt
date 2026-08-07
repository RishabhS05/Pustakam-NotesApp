package com.app.pustakam.android.screen.base

import androidx.compose.foundation.layout.PaddingValues

import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseScreen(
               fab: @Composable () -> Unit = {},
               topBar: @Composable () -> Unit = {},
               bottomBar: @Composable () -> Unit = {},
               content: @Composable (PaddingValues) -> Unit,
               ) {
    Scaffold(topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton =fab
    ) { padding ->
        content( padding)
    }
}