package com.example.liststart

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.tasks.OnSuccessListener
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class GisActivity : AppCompatActivity(), OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private var googleMap: GoogleMap? = null
    private var currentCenter: LatLng? = null
    private lateinit var centerMarkerPreview: ImageView // 화면 가운데 미리보기 마커
    private lateinit var selectLocationTextView: TextView // 위치 선택 텍스트뷰
    private var isMarkerPreviewVisible = false // 미리보기 마커 상태 추적
    private lateinit var centerEditText: EditText // 제목 수정용 EditText
    private lateinit var rightButton: ImageButton // 수정 적용 버튼
    private lateinit var leftButton: ImageButton // GPS 위치로 이동 버튼

    private var polygonList: MutableList<Polygon> = mutableListOf()
    private var isRestrictedAreaVisible = false // 규제구역 표시 여부
    private var lat: Double = 0.0
    private var long: Double = 0.0

    private lateinit var apiClient: GoogleApiClient
    private lateinit var providerClient: com.google.android.gms.location.FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gis)

        // 인텐트로 전달된 제목 데이터 받기
        val title = intent.getStringExtra("title") ?: "이름 없음"

        // UI 요소 초기화
        centerEditText = findViewById(R.id.centerEditText)
        centerEditText.setText(title)

        // 권한 요청 설정
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            if (it.all { permission -> permission.value == true }) {
                apiClient.connect()
            } else {
                Toast.makeText(this, "권한 거부", Toast.LENGTH_SHORT).show()
            }
        }

        // Intent로 전달된 사업 좌표를 받습니다.
        val bundle = intent.extras
        if (bundle != null) {
            lat = bundle.getDouble("lat", 0.0) // 위도값 받기, 기본값 0.0
            long = bundle.getDouble("long", 0.0) // 경도값 받기, 기본값 0.0
        }

        // 지도 프래그먼트 초기화
        (supportFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment).getMapAsync(this)

        providerClient = LocationServices.getFusedLocationProviderClient(this)
        apiClient = GoogleApiClient.Builder(this)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build()

        // 위치 권한 요청
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
            )
        } else {
            apiClient.connect()

            // 규제구역 버튼 처리
            val controlLineButton = findViewById<ImageButton>(R.id.controllLine)
            controlLineButton.setOnClickListener {
                if (isRestrictedAreaVisible) {
                    hideRestrictedAreas() // 규제구역 숨기기
                } else {
                    loadDevelopmentRestrictedAreas() // 규제구역 표시
                }
            }
        }

        // 중앙 미리보기 마커와 위치 선택 텍스트뷰 설정
        centerMarkerPreview = findViewById(R.id.centerMarkerPreview)
        selectLocationTextView = findViewById(R.id.selectLocationTextView)
        centerMarkerPreview.visibility = View.GONE // 초기에는 숨김
        selectLocationTextView.visibility = View.GONE // 초기에는 숨김

        // 좌표 선택 버튼 클릭 이벤트 설정
        val selctlotiLayout = findViewById<LinearLayout>(R.id.selctloti)
        selctlotiLayout.setOnClickListener {
            isMarkerPreviewVisible = !isMarkerPreviewVisible
            centerMarkerPreview.visibility = if (isMarkerPreviewVisible) View.VISIBLE else View.GONE
            selectLocationTextView.visibility = if (isMarkerPreviewVisible) View.VISIBLE else View.GONE
        }
        // '지정하기' 버튼 클릭 이벤트 설정
        val selectLocationButton = findViewById<TextView>(R.id.selectLocationTextView) // 지정하기 버튼의 ID로 대체
        selectLocationButton.setOnClickListener {
            // 현재 지도 중심 좌표 가져오기
            val currentCenter = googleMap?.cameraPosition?.target

            if (currentCenter != null) {
                // 중심 좌표에 마커 추가
                addMarkerAtLocation(currentCenter.latitude, currentCenter.longitude, "선택된 위치")

                // 좌표를 Toast 메시지로 출력
                Toast.makeText(this, "마커가 추가되었습니다: ${currentCenter.latitude}, ${currentCenter.longitude}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 수정 버튼 클릭 이벤트 설정
        rightButton = findViewById(R.id.rightButton)
        rightButton.setOnClickListener {
            val newTitle = centerEditText.text.toString()
            Toast.makeText(this, "수정된 제목: $newTitle", Toast.LENGTH_SHORT).show()
        }

        // GPS 위치로 이동 버튼 설정
        leftButton = findViewById(R.id.leftButton)
        leftButton.setOnClickListener {
            moveToCurrentLocation()
        }
    }

    override fun onMapReady(map: GoogleMap?) {
        googleMap = map

        // 지도 이동: 전달된 사업 좌표로 이동
        if (lat != 0.0 && long != 0.0) {
            val location = LatLng(lat, long)
            val zoomLevel = 15f // 원하는 줌 레벨 설정
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, zoomLevel))

            // 마커 추가
            googleMap?.addMarker(
                MarkerOptions().position(location).title("사업 위치")
            )
        }

        // 지도 중심 위치 업데이트 리스너
        googleMap?.setOnCameraIdleListener {
            currentCenter = googleMap?.cameraPosition?.target
        }
    }

    private fun moveToCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            providerClient.lastLocation.addOnSuccessListener(this, OnSuccessListener<Location> { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    moveMap(latitude, longitude)
                    addMarkerAtLocation(latitude, longitude, "현재 위치", BitmapDescriptorFactory.HUE_BLUE)
                } else {
                    Toast.makeText(this, "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveMap(latitude: Double, longitude: Double) {
        val latLng = LatLng(latitude, longitude)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    // 중심 좌표에 마커를 추가하는 함수
    private fun addMarkerAtLocation(latitude: Double, longitude: Double, title: String, markerColor: Float = BitmapDescriptorFactory.HUE_RED) {
        val latLng = LatLng(latitude, longitude)
        val markerOption = MarkerOptions()
            .position(latLng)
            .title(title)
            .icon(BitmapDescriptorFactory.defaultMarker(markerColor)) // 기본 마커 색상 설정
        googleMap?.addMarker(markerOption)
    }

    override fun onConnected(p0: Bundle?) {
        val lat = intent.getDoubleExtra("lat", 0.0)
        val long = intent.getDoubleExtra("long", 0.0)

        if (lat == 0.0 && long == 0.0) {
            // 현재 위치 사용
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                providerClient.lastLocation.addOnSuccessListener(this, OnSuccessListener<Location> { location ->
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        moveMap(latitude, longitude)
                    }
                })
            }
        } else {
            // 전달된 좌표로 지도 이동
            moveMap(lat, long)
        }
    }

    override fun onConnectionSuspended(p0: Int) {
        // 연결이 일시 중단되었을 때 처리
        Log.d("GisActivity", "Google API 연결 일시 중단됨.")
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        // 연결 실패 처리
        Toast.makeText(this, "Google API 연결 실패: ${connectionResult.errorMessage}", Toast.LENGTH_LONG).show()
    }

    // 터치 이벤트 처리 (EditText 외부 터치 시 키보드 숨김)
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = android.graphics.Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun loadDevelopmentRestrictedAreas() {
        val url = "https://openapi.gg.go.kr/DevelopRestrictionArea?KEY=apikey&Type=json&pIndex=1&pSize=100"
        DownloadTask().execute(url)
    }

    private fun hideRestrictedAreas() {
        polygonList.forEach { it.remove() }
        polygonList.clear()
        isRestrictedAreaVisible = false
    }

    inner class DownloadTask : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg urls: String?): String {
            val url = urls[0]
            val result = StringBuilder()

            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"

                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    result.append(line)
                }

                reader.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return result.toString()
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            result?.let {
                val jsonObject = JSONObject(it)
                val resultObject = jsonObject.getJSONObject("DevelopRestrictionArea")
                val rowArray = resultObject.getJSONArray("row")

                for (i in 0 until rowArray.length()) {
                    val row = rowArray.getJSONObject(i)
                    val coords = row.getString("COORDINATES")

                    val polygonOptions = PolygonOptions()
                    val coordPairs = coords.split(",")

                    for (coord in coordPairs) {
                        val latLng = coord.split(" ")
                        if (latLng.size == 2) {
                            val lat = latLng[0].toDoubleOrNull() ?: continue
                            val lon = latLng[1].toDoubleOrNull() ?: continue
                            polygonOptions.add(LatLng(lat, lon))
                        }
                    }
                    polygonOptions.fillColor(0x55FF0000)  // 반투명 빨간색
                    polygonOptions.strokeColor(0xFFFF0000.toInt())  // 빨간색 테두리
                    polygonOptions.strokeWidth(3f)  // 선 굵기

                    val polygon = googleMap?.addPolygon(polygonOptions)

                    if (polygon != null) {
                        polygonList.add(polygon)
                    }
                }

                isRestrictedAreaVisible = true
            }
        }
    }
}
