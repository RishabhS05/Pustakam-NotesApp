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
enum class BOOK : TaskCode{
    GET_BOOK, ADD_BOOK, UPDATE_BOOK, DELETE_BOOK, READ_BOOK
}
enum class CATEGORIES : TaskCode{
    GET_CATEGORIES, ADD_CATEGORY, UPDATE_CATEGORY, DELETE_CATEGORY,
}
enum class SETTINGS : TaskCode{
    GET_SETTINGS, UPDATE_SETTINGS, DELETE_SETTINGS,
}

enum class PROFILE : TaskCode{
    USER_PROFILE, UPDATE, DELETE , PROFILE_IMAGE
}
