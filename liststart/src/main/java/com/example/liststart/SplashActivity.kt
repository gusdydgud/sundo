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


            proceedToNextScreenWithDelay()

    }

    // 필요한 권한이 모두 허용되었는지 확인하는 함수
    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
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
