package com.phusy2001.nativedevicemonitordemo.nativecore.network

import android.util.Log
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Url
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit

private const val TAG = "ApiClient"
private const val CONNECT_TIMEOUT_SEC = 10L
private const val READ_TIMEOUT_SEC = 10L
// Giới hạn tổng thời gian của cả request (giống timeoutIntervalForResource bên iOS).
// Không có nó, OkHttp thử lần lượt từng IP của domain, mỗi IP chờ đủ CONNECT_TIMEOUT
private const val CALL_TIMEOUT_SEC = 10L

// --- Response model (theo đặc tả docs) ---

data class ApiResponse(
    val statusCode: Int,
    val body: String?,
    val isSuccessful: Boolean,
    val errorMessage: String? = null,
    // Thời gian từ lúc gửi request đến lúc nhận xong response (hoặc gặp lỗi)
    val durationMs: Long = 0,
    // true khi request bị huỷ do vượt quá connect/read timeout
    val isTimeout: Boolean = false
)

// --- Retrofit service interface ---
// Dùng @Url để hỗ trợ URL động (không bị giới hạn bởi baseUrl).
// ScalarsConverterFactory cho phép Retrofit trả về String thay vì phải parse JSON.

private interface RetrofitApiService {
    @GET
    suspend fun get(
        @Url url: String,
        @HeaderMap headers: Map<String, String>
    ): Response<String>

    @POST
    suspend fun post(
        @Url url: String,
        @Body body: String,
        @HeaderMap headers: Map<String, String>
    ): Response<String>
}

// --- Public interface ---

interface ApiClient {
    suspend fun get(url: String, headers: Map<String, String>? = null): ApiResponse
    suspend fun post(url: String, jsonBody: String, headers: Map<String, String>? = null): ApiResponse
}

// --- Implementation ---

class ApiClientImpl : ApiClient {

    private val service: RetrofitApiService by lazy {
        val logging = HttpLoggingInterceptor { message ->
            Log.d(TAG, message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SEC, TimeUnit.SECONDS)
            .addInterceptor(logging)
            // Tự động thêm Accept: application/json vào mọi request
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()

        // baseUrl là bắt buộc với Retrofit nhưng bị bỏ qua khi dùng @Url
        Retrofit.Builder()
            .baseUrl("https://placeholder.invalid/")
            .addConverterFactory(ScalarsConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(RetrofitApiService::class.java)
    }

    override suspend fun get(url: String, headers: Map<String, String>?): ApiResponse {
        Log.d(TAG, "GET $url")
        return execute("GET") { service.get(url, headers ?: emptyMap()) }
    }

    override suspend fun post(url: String, jsonBody: String, headers: Map<String, String>?): ApiResponse {
        Log.d(TAG, "POST $url | body: $jsonBody")
        val mergedHeaders = buildMap {
            put("Content-Type", "application/json")
            headers?.forEach { (k, v) -> put(k, v) }
        }
        return execute("POST") { service.post(url, jsonBody, mergedHeaders) }
    }

    private suspend fun execute(method: String, call: suspend () -> Response<String>): ApiResponse {
        val startNs = System.nanoTime()
        return try {
            mapResponse(call(), elapsedMs(startNs))
        } catch (e: CancellationException) {
            // Coroutine bị huỷ (vd: rời màn hình) -> phải ném lại để coroutine dừng đúng cách
            throw e
        } catch (e: Exception) {
            // Connect/read timeout ném SocketTimeoutException, call timeout ném InterruptedIOException("timeout").
            // SocketTimeoutException là lớp con của InterruptedIOException nên kiểm tra 1 lần là đủ
            val isTimeout = e is InterruptedIOException
            val message = e.localizedMessage ?: e.javaClass.simpleName
            Log.e(TAG, "$method error: $message")
            ApiResponse(
                statusCode = 0,
                body = null,
                isSuccessful = false,
                errorMessage = if (isTimeout) "Request timeout: $message" else message,
                durationMs = elapsedMs(startNs),
                isTimeout = isTimeout
            )
        }
    }

    private fun mapResponse(response: Response<String>, durationMs: Long): ApiResponse {
        val body = response.body() ?: response.errorBody()?.string()
        return ApiResponse(
            statusCode = response.code(),
            body = body,
            isSuccessful = response.isSuccessful,
            errorMessage = if (response.isSuccessful) null else "HTTP ${response.code()}",
            durationMs = durationMs
        )
    }

    private fun elapsedMs(startNs: Long): Long = (System.nanoTime() - startNs) / 1_000_000
}
