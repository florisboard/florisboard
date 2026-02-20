package com.speekez.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException

object NetworkUtils {
    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    suspend fun <T> safeApiCall(call: suspend () -> T): Result<T> {
        var currentDelay = 1000L
        val maxRetries = 3

        for (attempt in 0..maxRetries) {
            try {
                return Result.success(call())
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                val isRetryable = when (e) {
                    is IOException -> true
                    is HttpException -> {
                        val code = e.code()
                        code in 500..599 && code != 401 && code != 402 && code != 429
                    }
                    else -> false
                }

                if (isRetryable && attempt < maxRetries) {
                    delay(currentDelay)
                    currentDelay *= 2
                } else {
                    return Result.failure(e)
                }
            }
        }
        return Result.failure(IllegalStateException("Unreachable"))
    }
}
