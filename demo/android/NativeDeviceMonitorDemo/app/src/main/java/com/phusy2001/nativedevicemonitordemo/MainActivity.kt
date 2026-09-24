package com.phusy2001.nativedevicemonitordemo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.phusy2001.nativedevicemonitordemo.ui.theme.NativeDeviceMonitorDemoTheme

class MainActivity : ComponentActivity() {
    private val TAG = "LifecycleDemo"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NativeDeviceMonitorDemoTheme {
                HomePage()
            }
        }
        Log.d(TAG, "--> onCreate: Khởi tạo View và biến")
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "--> onStart: Màn hình bắt đầu hiển thị")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "--> onResume: Sẵn sàng tương tác với người dùng")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "--> onPause: Tạm dừng tương tác")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "--> onStop: Màn hình bị ẩn hoàn toàn")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "--> onDestroy: Giải phóng tài nguyên")
    }
}