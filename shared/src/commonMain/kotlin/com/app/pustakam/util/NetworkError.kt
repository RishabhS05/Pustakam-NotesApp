package com.app.pustakam.util

enum class NetworkError : Error {

    CONNECTION_FAILED {
        override fun getError(): String {
            return "Server Connection Falied"
        }
    },
    NOT_FOUND {
        override fun getError(): String {
            return "No Record found"
        }
              },
    REQUEST_TIMEOUT {
        override fun getError(): String {
            return "Request Timeout, Please try again later."
        }
    },
    UNAUTHORIZED {
        override fun getError(): String {
            return " Login failed, Please check your credentials and try again."
        }
    },
    CONFLICT {
        override fun getError(): String {
            return ""
        }
    },
    TOO_MANY_REQUESTS {
        override fun getError(): String {
            return "Too many requests"
        }
    },
    NO_INTERNET {
        override fun getError(): String {
            return "Please check your internet connection."
        }
    },
    PAYLOAD_TOO_LARGE {
        override fun getError(): String {
            return ""
        }
    },
    SERVER_ERROR {
        override fun getError(): String {
            return "Something went wrong!!"
        }
    },
    SERIALIZATION {
        override fun getError(): String {
            return "Serialization"
        }
    },
    UNKNOWN {
        override fun getError(): String {
            return ""
        }

    };

    abstract fun getError(): String
}

enum class ValidationError : Error {

    NAME {
        override fun getError(): String = "Enter a your name"
    },
    EMAIL {
        override fun getError(): String = "Enter a valid email"
    },
    PHONE {
        override fun getError(): String = "Enter a valid phone number."
    },
    PASSWORD {
        override fun getError(): String = "Enter a strong password."
    },
    PASSWORD_NOT_MATCHED {
        override fun getError(): String ="Password did not matched"
    },
    NONE {
        override fun getError(): String ="SuccessFul"
    };
    abstract fun getError(): String
}