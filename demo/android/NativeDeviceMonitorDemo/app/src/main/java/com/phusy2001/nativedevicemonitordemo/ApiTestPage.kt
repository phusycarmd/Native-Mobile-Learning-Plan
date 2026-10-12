package com.phusy2001.nativedevicemonitordemo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phusy2001.nativedevicemonitordemo.nativecore.network.ApiClient
import com.phusy2001.nativedevicemonitordemo.nativecore.network.ApiClientImpl
import com.phusy2001.nativedevicemonitordemo.nativecore.network.ApiResponse
import com.phusy2001.nativedevicemonitordemo.ui.theme.NativeDeviceMonitorDemoTheme
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private enum class HttpMethod { GET, POST }

// URL mẫu để test nhanh đủ các trường hợp: thành công, lỗi HTTP và timeout (client timeout là 10s).
// Timeout: cổng 81 bị firewall bỏ qua nên kết nối treo đến khi hết giờ.
// Không dùng httpbin.org/delay vì httpbin giới hạn delay tối đa 10s, trùng với timeout của client
private val QUICK_URLS = listOf(
    "GET 200" to "https://httpbin.org/get",
    "POST 200" to "https://httpbin.org/post",
    "404" to "https://httpbin.org/status/404",
    "500" to "https://httpbin.org/status/500",
    "Timeout" to "https://example.com:81"
)

@Composable
fun ApiTestPage(
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {}
) {
    // Colors matching HomePage.kt
    val primaryBlue = Color(0xFF2563EB)
    val lightBlueBg = Color(0xFFEFF4FF)
    val lightBlueBorder = Color(0xFFDBE4FF)
    val textPrimary = Color(0xFF1F2937)
    val textSecondary = Color(0xFF4B5563)
    val textLabel = Color(0xFF374151)
    val borderColor = Color(0xFFE5E7EB)
    val terminalBg = Color(0xFF0F172A)
    val terminalText = Color(0xFF4ADE80)
    val terminalLabel = Color(0xFF94A3B8)
    val terminalError = Color(0xFFF87171)
    val statusGreen = Color(0xFF10B981)
    val statusAmber = Color(0xFFF59E0B)
    val stopRed = Color(0xFFDC2626)

    val apiClient: ApiClient = remember { ApiClientImpl() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var url by rememberSaveable { mutableStateOf(QUICK_URLS.first().second) }
    var selectedMethod by remember { mutableStateOf(HttpMethod.GET) }
    var isLoading by remember { mutableStateOf(false) }
    var response by remember { mutableStateOf<ApiResponse?>(null) }

    fun send(method: HttpMethod) {
        if (isLoading) return
        focusManager.clearFocus()
        selectedMethod = method
        isLoading = true
        val target = url.trim()
        // Retrofit suspend tự chạy request trên thread nền, kết quả quay về main thread để cập nhật UI
        scope.launch {
            response = when (method) {
                HttpMethod.GET -> apiClient.get(target)
                HttpMethod.POST -> apiClient.post(target, buildSamplePostBody())
            }
            isLoading = false
        }
    }

    val currentResponse = response
    val responseText = when {
        isLoading -> "Đang gửi ${selectedMethod.name} request..."
        currentResponse == null -> "Bấm GET hoặc POST để gửi request."
        !currentResponse.body.isNullOrBlank() -> prettyJson(currentResponse.body)
        else -> currentResponse.errorMessage ?: "(Không có nội dung)"
    }
    val responseColor = when {
        isLoading || currentResponse == null -> terminalLabel
        currentResponse.statusCode == 0 -> terminalError
        else -> terminalText
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        topBar = { AppTopBar(title = "API Test", onMenuClick = onMenuClick) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // --- Section: API URL ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "API URL",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = textLabel
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    placeholder = {
                        Text(
                            text = "https://...",
                            color = Color(0xFF9CA3AF),
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = textPrimary
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(onGo = { send(selectedMethod) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryBlue,
                        unfocusedBorderColor = borderColor,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = primaryBlue
                    )
                )

                // URL mẫu
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QUICK_URLS.forEach { (label, presetUrl) ->
                        val isSelected = url == presetUrl
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) primaryBlue else textSecondary,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) lightBlueBg else Color.White)
                                .border(1.dp, if (isSelected) primaryBlue else borderColor, CircleShape)
                                .clickable { url = presetUrl }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // --- Buttons: GET / POST ---
            // Nút của method vừa chọn được tô đậm, giống mockup
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HttpMethod.entries.forEach { method ->
                    val isPrimary = method == selectedMethod
                    val containerColor = if (isPrimary) primaryBlue else lightBlueBg
                    val contentColor = if (isPrimary) Color.White else primaryBlue
                    Button(
                        onClick = { send(method) },
                        enabled = !isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = if (isPrimary) null else BorderStroke(1.dp, lightBlueBorder),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = containerColor,
                            contentColor = contentColor,
                            disabledContainerColor = containerColor.copy(alpha = 0.7f),
                            disabledContentColor = contentColor.copy(alpha = 0.8f)
                        )
                    ) {
                        if (isLoading && isPrimary) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = contentColor,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(text = method.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }

            // --- Section: Response ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Response",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = textLabel
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(terminalBg)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // Cho phép bôi đen / copy nội dung response
                    SelectionContainer {
                        Text(
                            text = responseText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = responseColor
                        )
                    }
                }

                // --- Status: ● 200 OK · 128 ms ---
                if (currentResponse != null && !isLoading) {
                    val dotColor = when {
                        currentResponse.isSuccessful -> statusGreen
                        currentResponse.statusCode in 300..499 -> statusAmber
                        else -> stopRed
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Text(
                            text = currentResponse.statusLine(),
                            fontSize = 14.sp,
                            color = textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(name = "API Test", showBackground = true)
@Composable
fun ApiTestPagePreview() {
    NativeDeviceMonitorDemoTheme {
        ApiTestPage()
    }
}

private fun ApiResponse.statusLine(): String {
    val label = when {
        statusCode > 0 -> "$statusCode ${httpStatusText(statusCode)}".trimEnd()
        isTimeout -> "Timeout"
        else -> "Network error"
    }
    return "$label · $durationMs ms"
}

// Tự map mã -> chữ: response.message() của OkHttp rỗng khi server dùng HTTP/2
private fun httpStatusText(code: Int): String = when (code) {
    200 -> "OK"
    201 -> "Created"
    202 -> "Accepted"
    204 -> "No Content"
    301 -> "Moved Permanently"
    302 -> "Found"
    304 -> "Not Modified"
    400 -> "Bad Request"
    401 -> "Unauthorized"
    403 -> "Forbidden"
    404 -> "Not Found"
    405 -> "Method Not Allowed"
    408 -> "Request Timeout"
    429 -> "Too Many Requests"
    500 -> "Internal Server Error"
    502 -> "Bad Gateway"
    503 -> "Service Unavailable"
    504 -> "Gateway Timeout"
    else -> ""
}

// Format JSON thụt lề 2 khoảng trắng; không phải JSON thì giữ nguyên
private fun prettyJson(raw: String): String {
    val text = raw.trim()
    return try {
        val pretty = when {
            text.startsWith("{") -> JSONObject(text).toString(2)
            text.startsWith("[") -> JSONArray(text).toString(2)
            else -> return raw
        }
        // org.json của Android escape "/" thành "\/", trả lại cho dễ đọc
        pretty.replace("\\/", "/")
    } catch (e: JSONException) {
        raw
    }
}

// Body mẫu theo mockup. Battery đang là giá trị giả, Tuần 3 sẽ lấy từ BatteryProvider
private fun buildSamplePostBody(): String {
    val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return JSONObject().apply {
        put("status", "ok")
        put("battery", 82)
        put("ts", isoFormatter.format(Date()))
    }.toString()
}
