package com.speekez.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import retrofit2.HttpException
import java.io.IOException

class NetworkUtilsTest {

    @Test
    fun `isOnline returns true when WiFi is connected`() {
        val context = mockk<Context>()
        val connectivityManager = mockk<ConnectivityManager>()
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        
        assertTrue(NetworkUtils.isOnline(context))
    }

    @Test
    fun `isOnline returns true when Cellular is connected`() {
        val context = mockk<Context>()
        val connectivityManager = mockk<ConnectivityManager>()
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
        every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true
        
        assertTrue(NetworkUtils.isOnline(context))
    }

    @Test
    fun `isOnline returns true when Ethernet is connected`() {
        val context = mockk<Context>()
        val connectivityManager = mockk<ConnectivityManager>()
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
        every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
        every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns true
        
        assertTrue(NetworkUtils.isOnline(context))
    }

    @Test
    fun `isOnline returns false when no network is active`() {
        val context = mockk<Context>()
        val connectivityManager = mockk<ConnectivityManager>()

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns null
        
        assertFalse(NetworkUtils.isOnline(context))
    }

    @Test
    fun `safeApiCall returns success on successful call`() = runTest {
        val result = NetworkUtils.safeApiCall { "success" }
        assertTrue(result.isSuccess)
        assertEquals("success", result.getOrNull())
    }

    @Test
    fun `safeApiCall retries on IOException`() = runTest {
        var attempts = 0
        val result = NetworkUtils.safeApiCall {
            attempts++
            if (attempts < 3) throw IOException("Network error")
            "success"
        }
        assertTrue(result.isSuccess)
        assertEquals(3, attempts)
        assertEquals("success", result.getOrNull())
    }

    @Test
    fun `safeApiCall returns failure after max retries for IOException`() = runTest {
        var attempts = 0
        val result = NetworkUtils.safeApiCall {
            attempts++
            throw IOException("Network error")
        }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals(4, attempts) // 1 initial + 3 retries
    }

    @Test
    fun `safeApiCall retries on HTTP 500`() = runTest {
        var attempts = 0
        val http500 = mockk<HttpException>()
        every { http500.code() } returns 500

        val result = NetworkUtils.safeApiCall {
            attempts++
            if (attempts < 2) throw http500
            "success"
        }
        assertTrue(result.isSuccess)
        assertEquals(2, attempts)
    }

    @Test
    fun `safeApiCall does not retry on HTTP 401`() = runTest {
        var attempts = 0
        val http401 = mockk<HttpException>()
        every { http401.code() } returns 401

        val result = NetworkUtils.safeApiCall {
            attempts++
            throw http401
        }
        assertTrue(result.isFailure)
        assertEquals(1, attempts)
    }

    @Test
    fun `safeApiCall does not retry on HTTP 402`() = runTest {
        var attempts = 0
        val http402 = mockk<HttpException>()
        every { http402.code() } returns 402

        val result = NetworkUtils.safeApiCall {
            attempts++
            throw http402
        }
        assertTrue(result.isFailure)
        assertEquals(1, attempts)
    }

    @Test
    fun `safeApiCall does not retry on HTTP 429`() = runTest {
        var attempts = 0
        val http429 = mockk<HttpException>()
        every { http429.code() } returns 429

        val result = NetworkUtils.safeApiCall {
            attempts++
            throw http429
        }
        assertTrue(result.isFailure)
        assertEquals(1, attempts)
    }

    @Test
    fun `safeApiCall rethrows CancellationException`() = runTest {
        assertThrows<CancellationException> {
            NetworkUtils.safeApiCall {
                throw CancellationException("Cancelled")
            }
        }
    }
}
