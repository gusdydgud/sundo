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

    private fun isLocationInRestrictedArea(lat: Double, long: Double): Boolean {
        for (polygon in polygonList) {
            val polygonBounds = polygon.points
            val point = LatLng(lat, long)

            // 주어진 좌표가 폴리곤 내에 있는지 확인
            if (containsLocation(point, polygonBounds)) {
                return true
            }
        }
        return false
    }
    // LatLng가 폴리곤 안에 있는지 확인하는 함수
    private fun containsLocation(point: LatLng, polygon: List<LatLng>): Boolean {
        var intersectCount = 0
        for (j in polygon.indices) {
            val vertex1 = polygon[j]
            val vertex2 = polygon[(j + 1) % polygon.size]
            if (rayCastIntersect(point, vertex1, vertex2)) {
                intersectCount++
            }
        }
        return (intersectCount % 2 == 1) // 홀수이면 폴리곤 안에 있음
    }
    // ray-casting 알고리즘 사용하여 포인트가 폴리곤 내부에 있는지 확인
    private fun rayCastIntersect(point: LatLng, vertex1: LatLng, vertex2: LatLng): Boolean {
        val pointLat = point.latitude
        val pointLng = point.longitude
        val vertex1Lat = vertex1.latitude
        val vertex1Lng = vertex1.longitude
        val vertex2Lat = vertex2.latitude
        val vertex2Lng = vertex2.longitude

        if ((vertex1Lng > pointLng) != (vertex2Lng > pointLng)) {
            val intersectLat = (vertex2Lat - vertex1Lat) * (pointLng - vertex1Lng) / (vertex2Lng - vertex1Lng) + vertex1Lat
            if (pointLat < intersectLat) {
                return true
            }
        }
        return false
    }

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
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
            )
        } else {
            apiClient.connect()
        }

        // 규제구역 버튼 처리
        val controlLineButton = findViewById<ImageButton>(R.id.controllLine)
        controlLineButton.setOnClickListener {
            if (isRestrictedAreaVisible) {
                hideRestrictedAreas() // 규제구역 숨기기
            } else {
                loadDevelopmentRestrictedAreas() // 규제구역 표시
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
        val selectLocationButton = findViewById<TextView>(R.id.selectLocationTextView)
        selectLocationButton.setOnClickListener {
            val currentCenter = googleMap?.cameraPosition?.target

            if (currentCenter != null) {
                // 규제구역 내에 있는지 확인
                if (isLocationInRestrictedArea(currentCenter.latitude, currentCenter.longitude)) {
                    Toast.makeText(this, "규제구역입니다. 마커를 추가할 수 없습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    // 중심 좌표에 마커 추가
                    addMarkerAtLocation(currentCenter.latitude, currentCenter.longitude, "선택된 위치")
                    Toast.makeText(this, "마커가 추가되었습니다: ${currentCenter.latitude}, ${currentCenter.longitude}", Toast.LENGTH_SHORT).show()
                }
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
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
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
            moveToCurrentLocation()
        } else {
            moveMap(lat, long)
        }
    }

    override fun onConnectionSuspended(p0: Int) {
        Log.d("GisActivity", "Google API 연결 일시 중단됨.")
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
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
        // 여기서 원래 사용하고 있던 API의 URL을 설정합니다.
        val apiKey = "05C26CB0-9905-39AC-8E59-423EE652CA06"  // 사용자의 API 키 입력
        val url = "https://api.vworld.kr/req/data?service=data&request=GetFeature&data=LT_C_UD801&key=$apiKey&geomFilter=BOX($long,$lat,${long + 1},${lat + 1})&format=json&size=100"

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
                val response = jsonObject.getJSONObject("response")
                val status = response.getString("status")
                if (status != "OK") {
                    Toast.makeText(this@GisActivity, "API 요청 실패: $status", Toast.LENGTH_SHORT).show()
                    return
                }

                val featureCollection = response.getJSONObject("result").getJSONObject("featureCollection")
                val features = featureCollection.getJSONArray("features")

                for (i in 0 until features.length()) {
                    val feature = features.getJSONObject(i)
                    val geometry = feature.getJSONObject("geometry")
                    val coordinates = geometry.getJSONArray("coordinates")

                    val polygonOptions = PolygonOptions()
                    val outerBoundary = coordinates.getJSONArray(0).getJSONArray(0)

                    for (j in 0 until outerBoundary.length()) {
                        val point = outerBoundary.getJSONArray(j)
                        val lon = point.getDouble(0)
                        val lat = point.getDouble(1)
                        polygonOptions.add(LatLng(lat, lon))
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