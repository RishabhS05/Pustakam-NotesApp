package com.app.pustakam.android.screen.bookUIView
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.pustakam.android.screen.notebookReader.BookPage

import com.app.pustakam.android.theme.CoverColor
import com.app.pustakam.android.theme.PaperColor
import com.app.pustakam.android.theme.PaperInk
import com.app.pustakam.android.theme.typography
import com.app.pustakam.android.widgets.zoom.zoomable


@Composable
fun BookPageContent(page: BookPage) {
    when (page) {
        is BookPage.Cover -> PaperPage(background = CoverColor) {
            Column(
                Modifier.fillMaxSize().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    page.title, style = typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
                    color = PaperColor, textAlign = TextAlign.Center
                )
                Box(Modifier.width(60.dp).height(2.dp).background(PaperColor.copy(alpha = .6f)))
                Text(page.subtitle, style = typography.titleSmall, color = PaperColor.copy(alpha = .8f))
            }
        }

        is BookPage.TextPage -> PaperPage {
            Column(Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 30.dp)) {
                Text(
                    page.text,
                    style = typography.bodyLarge.copy(fontFamily = FontFamily.Serif, lineHeight = 26.sp),
                    color = PaperInk,
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                )
                if (page.chunkCount > 1) Text(
                    "· ${page.chunkIndex} of ${page.chunkCount} ·", style = typography.labelSmall,
                    color = PaperInk.copy(alpha = .5f),
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
                )
            }
        }

        is BookPage.ImagePage -> PaperPage {
            Column(Modifier.fillMaxSize().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = page.path, contentDescription = page.title,
                    contentScale = ContentScale.Fit,   // never fills/crops — full image visible
                    // 🔧 19-Jul-2026: pinch/double-tap zoom on image pages
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(4.dp).zoomable()
                )
                if (page.title.isNotBlank()) Text(
                    page.title, style = typography.labelMedium.copy(fontStyle = FontStyle.Italic),
                    color = PaperInk.copy(alpha = .7f), maxLines = 1
                )
            }
        }

        is BookPage.PdfSheet -> PaperPage(background = Color.White) { PdfBookPage(page) }

        is BookPage.MediaPage -> PaperPage { MediaBookPage(page.media) }

        is BookPage.DocFilePage -> PaperPage { DocFileBookPage(page.media) }

        is BookPage.LinkPage -> PaperPage {
            val context = LocalContext.current
            Column(
                Modifier.fillMaxSize().padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                Text("A link lives on this page", style = typography.titleMedium, color = PaperInk)
                Spacer(Modifier.height(8.dp))
                Text(page.url, style = typography.bodyMedium, color = Color(0xFF1A5276), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(page.url)))
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Open link") }
            }
        }

        is BookPage.LocationPage -> PaperPage {
            val context = LocalContext.current
            Column(
                Modifier.fillMaxSize().padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.LocationOn, null, tint = CoverColor, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    page.address ?: "%.5f, %.5f".format(page.latitude, page.longitude),
                    style = typography.titleSmall, color = PaperInk, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    val geo = Uri.parse("geo:${page.latitude},${page.longitude}?q=${page.latitude},${page.longitude}")
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, geo)) }
                    catch (_: ActivityNotFoundException) { Toast.makeText(context, "No maps app found", Toast.LENGTH_SHORT).show() }
                }) { Text("Open in Maps") }
            }
        }
    }
}