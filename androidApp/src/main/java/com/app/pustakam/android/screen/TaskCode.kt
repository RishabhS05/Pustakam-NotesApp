package com.app.pustakam.android.screen

interface TaskCode
enum class AUTH : TaskCode{
    LOGIN, SIGNUP
}
enum class NOTES_CODES  : TaskCode {
    GET_NOTES, ADD, DELETE,UPDATE
}
enum class PROFILE : TaskCode{
    USER_PROFILE, UPDATE, DELETE , PROFILE_IMAGE
}