package com.app.pustakam.android.widgets.bookwidget

// 🔧 18-Jul-2026: NEW FEATURE (book widget) — home-screen widget styled as a small leather book;
//   tapping it deep-links straight into the page-flip reader for the last-read note.
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.app.pustakam.android.R

class BookWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id -> BookWidgetUpdater.render(context, appWidgetManager, id) }
    }
}

object BookWidgetUpdater {
    private const val PREFS = "pustakam_book_widget"
    private const val KEY_NOTE_ID = "last_note_id"
    private const val KEY_NOTE_TITLE = "last_note_title"

    // 🔧 18-Jul-2026: called by BookReaderScreen — remembers the book + refreshes every widget
    fun saveLastBook(context: Context, noteId: String, title: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_NOTE_ID, noteId)
            .putString(KEY_NOTE_TITLE, title.ifBlank { "Untitled note" })
            .apply()
        val manager = AppWidgetManager.getInstance(context)
        manager.getAppWidgetIds(ComponentName(context, BookWidgetProvider::class.java))
            .forEach { render(context, manager, it) }
    }

    // 🔧 18-Jul-2026: builds the RemoteViews — deep link when a book exists, app launch otherwise
    fun render(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val noteId = prefs.getString(KEY_NOTE_ID, null)
        val title = prefs.getString(KEY_NOTE_TITLE, null)

        val views = RemoteViews(context.packageName, R.layout.widget_book).apply {
            setTextViewText(R.id.widget_book_title, title ?: context.getString(R.string.book_widget_default_title))
            setTextViewText(R.id.widget_book_subtitle, context.getString(R.string.book_widget_open_hint))
        }
        val intent = if (noteId.isNullOrEmpty()) {
            context.packageManager.getLaunchIntentForPackage(context.packageName)
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse("pustakam://book/$noteId")).setPackage(context.packageName)
        }
        intent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val pending = PendingIntent.getActivity(
                context, widgetId, it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_book_root, pending)
        }
        manager.updateAppWidget(widgetId, views)
    }
}
