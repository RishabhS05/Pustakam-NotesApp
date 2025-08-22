package com.app.pustakam.android.screen

interface TaskCode
enum class AUTH : TaskCode{
    LOGIN, SIGNUP
}
enum class NOTES_CODES  : TaskCode {
    GET_NOTES, INSERT, DELETE, UPDATE, READ
}
enum class BOOKS : TaskCode{
    GET_BOOKS, ADD_BOOK, UPDATE_BOOK, DELETE_BOOK,
}
enum class PROFILE : TaskCode{
    USER_PROFILE, UPDATE, DELETE , PROFILE_IMAGE
}
