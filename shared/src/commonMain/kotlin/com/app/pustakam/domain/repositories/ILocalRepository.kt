package com.app.pustakam.domain.repositories

import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.Notes

interface ILocalRepository {
    fun insertUpdate(note: Note)
    fun deleteById(id : String)
    fun getNotes() : Notes
}