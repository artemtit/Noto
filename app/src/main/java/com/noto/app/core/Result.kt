package com.noto.app.core

sealed interface AppResult<out T> {
    data class Ok<T>(val value: T) : AppResult<T>
    data class Err(val error: AppError) : AppResult<Nothing>
}

sealed class AppError(val messageKey: String) {
    data object NoNetwork : AppError("error_network")
    data object Timeout : AppError("error_timeout")
    data class Api(val code: Int, val body: String?) : AppError("error_api")
    data object BadResponse : AppError("error_bad_response")
    data object NoApiKey : AppError("error_no_api_key")
    data object EmptySpeech : AppError("error_empty_speech")
    data class Unknown(val cause: Throwable?) : AppError("error_api")
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Ok -> AppResult.Ok(transform(value))
    is AppResult.Err -> this
}
