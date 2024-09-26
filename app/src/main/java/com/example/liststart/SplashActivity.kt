package com.example.liststart

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.liststart.view.MainActivity

class SplashActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 100

    // 필요한 권한 배열 (카메라와 위치 권한)
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val splashImage = findViewById<ImageView>(R.id.splash_image)

        // Glide를 사용해서 GIF 로드하기
        Glide.with(this)
            .asGif()
            .load(R.drawable.splash_background) // GIF 파일의 리소스 ID
            .into(splashImage)

        // 권한 확인 및 요청
        if (allPermissionsGranted()) {
            // 권한이 이미 허용되었을 경우 2초 후에 다음 화면으로 이동
            proceedToNextScreenWithDelay()
        } else {
            // 권한 요청
            requestPermissions()
        }
    }

    // 필요한 권한이 모두 허용되었는지 확인하는 함수
    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    // 카메라 및 위치 권한 요청
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE)
    }

    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // 모든 권한이 허용된 경우 2초 후 다음 화면으로 이동
                proceedToNextScreenWithDelay()
            } else {
                // 권한이 거부된 경우 앱 종료
                Toast.makeText(this, "카메라 및 위치 권한이 필요합니다. 앱을 종료합니다.", Toast.LENGTH_LONG).show()
                finish() // 앱 종료
            }
        }
    }

    // 2초 후에 다음 화면으로 이동
    private fun proceedToNextScreenWithDelay() {
        Handler(Looper.getMainLooper()).postDelayed({
            proceedToNextScreen()
        }, 2000) // 2초 대기
    }

    // 다음 화면으로 이동하는 함수
    private fun proceedToNextScreen() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // SplashActivity 종료
    }
}
