package com.example.liststart

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log

import android.view.LayoutInflater
import android.view.MotionEvent

import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TabHost
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.liststart.adapter.BusinessAdapter
import com.example.liststart.datasource.DataSourceProvider
import com.example.liststart.model.Business
import com.example.liststart.util.Constants
import com.example.liststart.view.TAG
import com.example.liststart.viewmodel.BusinessViewModel
import com.example.liststart.viewmodel.MarkerViewModel
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.tasks.OnSuccessListener
import com.unity3d.player.UnityPlayerActivity
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class GisActivity : AppCompatActivity(), OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    // Google Map 관련 변수
    private var googleMap: GoogleMap? = null
    private var currentCenter: LatLng? = null // 현재 지도의 중심 좌표

    // UI 요소
    private lateinit var centerMarkerPreview: ImageView // 화면 가운데 미리보기 마커
    private lateinit var selectLocationTextView: TextView // 위치 선택 안내 텍스트뷰
    private lateinit var centerEditText: EditText // 사업 제목 수정용 EditText
    private lateinit var rightButton: ImageButton // 수정 적용 버튼
    private lateinit var leftButton: ImageButton // GPS 위치 이동 버튼
    private lateinit var recyclerLayout: LinearLayout // RecyclerView가 포함된 레이아웃
    private lateinit var getListButton: LinearLayout // 리스트 버튼 (사업 목록 표시 버튼)
    private lateinit var recyclerView: RecyclerView // 사업 목록 RecyclerView

    // 상태 관리 변수
    private var isMarkerPreviewVisible = false // 미리보기 마커의 가시성 상태 추적
    private var isRestrictedAreaVisible = false // 규제 구역 표시 여부
    private var isRecyclerViewVisible = false // RecyclerView 가시성 상태
    private var markerCounter = 1 // 마커의 카운트
    private var titleCounter: String = "" // 제목 카운트용 문자열
    private var data: Business? = null// 선택된 사업
    private var title: String? = null// 선택된 사업
    private var backPressedTime: Long = 0 // 뒤로가기 버튼을 마지막으로 누른 시간

    // 지도 상의 마커와 폴리곤 관리
    private var markersList: MutableList<Marker> = mutableListOf() // 지도에 추가된 마커 리스트
    private var polygonList: MutableList<Polygon> = mutableListOf() // 지도에 추가된 폴리곤 리스트

    // 위치 관련 변수
    private var lat: Double = 0.0 // 현재 GPS로 얻은 위도
    private var long: Double = 0.0 // 현재 GPS로 얻은 경도
    private lateinit var apiClient: GoogleApiClient // Google API 클라이언트
    private lateinit var providerClient: com.google.android.gms.location.FusedLocationProviderClient // 위치 제공 클라이언트

    // ViewModel 및 Adapter 관련 변수
    private lateinit var businessAdapter: BusinessAdapter // 비즈니스 데이터용 RecyclerView 어댑터
    private lateinit var businessViewModel: BusinessViewModel // 비즈니스 ViewModel
    private lateinit var markerViewModel: MarkerViewModel // 마커 ViewModel

    // 규제구역 내에 있는지 확인하는 함수
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

        //현용 뒤로가기2번눌러서 앱종료
        // onBackPressedDispatcher를 통한 뒤로가기 콜백 등록
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 두 번 눌러야 앱이 종료되도록 처리
                if (System.currentTimeMillis() > backPressedTime + 2000) {
                    backPressedTime = System.currentTimeMillis()
                    Toast.makeText(this@GisActivity, "뒤로가기를 한 번 더 누르면 앱이 종료됩니다", Toast.LENGTH_SHORT).show()
                } else {
                    finishAffinity() // 현재 액티비티를 포함한 모든 액티비티 종료
                    System.exit(0) // 앱 프로세스 종료
                }
            }
        })
        //현용 뒤로가기2번눌러서 앱종료

        // 인텐트로 전달된 제목 데이터 받기
        data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 (Tiramisu) 이상
            intent?.getParcelableExtra("data", Business::class.java)
        } else {
            // Android 13 미만
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra<Business>("data")
        }

        title = data?.title ?: "이름없음"

    // 수민
        // DataSourceProvider에서 싱글톤 인스턴스를 가져옴
        val businessViewModelFactory = DataSourceProvider.businessViewModelFactory
        businessViewModel = ViewModelProvider(this, businessViewModelFactory).get(BusinessViewModel::class.java)
        val markerViewModelFactory = DataSourceProvider.markerViewModelFactory
        markerViewModel = ViewModelProvider(this, markerViewModelFactory).get(MarkerViewModel::class.java)

        // RecyclerView 설정
        recyclerLayout = findViewById(R.id.recyclerLayout)
        recyclerView = findViewById(R.id.recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // RecyclerView 높이를 미리 화면 아래로 이동시킵니다.
        recyclerLayout.post {
            recyclerLayout.translationY = recyclerLayout.height.toFloat()
        }

        // 어댑터 초기화
        businessAdapter = BusinessAdapter(isVisible = false) { item -> handleClick(item) }
        recyclerView.adapter = businessAdapter

        // BusinessViewModel의 데이터를 관찰하여 RecyclerView 업데이트
        businessViewModel.businessList.observe(this) { businessList ->
            businessAdapter.updateList(businessList)
        }

        // MarkerViewModel의 데이터를 관찰하여 마커 리스트 업데이트 및 첫 번째 마커로 지도 이동
        markerViewModel.markerList.observe(this) { markerList ->
            if (markerList.isNotEmpty()) {
                val firstMarker = markerList.first()

                // 첫 번째 마커의 좌표로 지도 중심을 이동
                val firstLatLng = LatLng(firstMarker.latitude, firstMarker.longitude)
                googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLatLng, 15f))

                // 마커를 지도에 표시
                for (markerData in markerList) {
                    val markerOptions = MarkerOptions()
                        .position(LatLng(markerData.latitude, markerData.longitude)) // 마커 좌표
                        .title(markerData.title)  // 마커 제목 설정
                    googleMap?.addMarker(markerOptions)
                }
            }
        }

        // ViewModel의 에러 메시지를 관찰하여 토스트로 표시
        businessViewModel.error.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        markerViewModel.error.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // 네트워크 상태 확인 후 데이터 로딩
        if (Constants.isNetworkAvailable(this)) {
            businessViewModel.loadBusinessList()

            // bno가 null이 아닌 경우에만 마커 로딩
            data?.bno?.let { bno ->
                markerViewModel.loadMarkerList(bno)
            } ?: run {
                Toast.makeText(this, "사업 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "네트워크 연결이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    // 수민

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
            if (isRecyclerViewVisible) {
                // 애니메이션을 통해 사업지 목록을 먼저 내립니다.
                animateRecyclerView(false) // 사업지 목록 내리기

                // 애니메이션이 끝난 후에 좌표 선택 동작을 실행합니다.
                val listener = object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {}
                    override fun onAnimationEnd(animation: Animator) {
                        // 좌표 선택 동작 실행
                        isMarkerPreviewVisible = !isMarkerPreviewVisible
                        centerMarkerPreview.visibility = if (isMarkerPreviewVisible) View.VISIBLE else View.GONE
                        selectLocationTextView.visibility = if (isMarkerPreviewVisible) View.VISIBLE else View.GONE
                    }

                    override fun onAnimationCancel(animation: Animator) {}
                    override fun onAnimationRepeat(animation: Animator) {}
                }

                // 애니메이션에 리스너를 추가하여 끝난 후에 실행될 동작을 정의합니다.
                ObjectAnimator.ofFloat(recyclerLayout, "translationY", recyclerLayout.height.toFloat()).apply {
                    duration = 500 // 애니메이션 지속 시간 (ms)
                    addListener(listener)
                }.start()

            } else {
                // 사업지 목록이 보이지 않을 때는 바로 좌표 선택 동작을 실행합니다.
                isMarkerPreviewVisible = !isMarkerPreviewVisible
                centerMarkerPreview.visibility = if (isMarkerPreviewVisible) View.VISIBLE else View.GONE
                selectLocationTextView.visibility = if (isMarkerPreviewVisible) View.VISIBLE else View.GONE
            }
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
                    val marker = addMarkerAtLocation(currentCenter.latitude, currentCenter.longitude, title + " " + markerCounter)
                    Toast.makeText(this, "마커가 추가되었습니다: ${currentCenter.latitude}, ${currentCenter.longitude}", Toast.LENGTH_SHORT).show()
                    titleCounter = title + " " + markerCounter
                    // 마커 클릭 시 다이얼로그 호출
                    googleMap?.setOnMarkerClickListener {
                        showCustomDialog(it)
                        true
                    }
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

    // 수민
        // 사업지 목록 버튼 이벤트
        getListButton = findViewById(R.id.getListButton)
        getListButton.setOnClickListener {
            if (!isRecyclerViewVisible) {
                // 슬라이드 업 애니메이션 실행
                animateRecyclerView(true)
            } else {
                // 슬라이드 다운 애니메이션 실행
                animateRecyclerView(false)
            }
        }
    // 수민

        //AR camera
        val cameraBtn = findViewById<LinearLayout>(R.id.cameraBtn)
        cameraBtn.setOnClickListener{
            val intent = Intent(this, UnityPlayerActivity::class.java)
            intent.putExtra("unity", "some_value")  // 여기에 문자열 값을 명시적으로 전달
            startActivity(intent)
        }
    }

    //현용
    override fun onMapReady(map: GoogleMap?) {
        googleMap = map

        // 커스텀 InfoWindow 어댑터 설정
        googleMap?.setInfoWindowAdapter(CustomInfoWindowAdapter(this))

        // 지도 이동: 전달된 사업 좌표로 이동
        if (lat != 0.0 && long != 0.0) {
            val location = LatLng(lat, long)
            val zoomLevel = 15f // 원하는 줌 레벨 설정
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, zoomLevel))

            // 마커 추가
            googleMap?.addMarker(
                MarkerOptions().position(location).title("사업 위치")
            )?.showInfoWindow() // 마커를 추가할 때 InfoWindow를 바로 표시
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
    //현용
    private fun addMarkerAtLocation(
        latitude: Double,
        longitude: Double,
        title: String = "사업지명 $markerCounter",
        markerColor: Float = BitmapDescriptorFactory.HUE_RED
    ): Marker {
        val latLng = LatLng(latitude, longitude)
        val markerOption = MarkerOptions()
            .position(latLng)
            .title(title)  // 마커 제목 설정
            .icon(BitmapDescriptorFactory.defaultMarker(markerColor))

        val marker = googleMap?.addMarker(markerOption)

        // 마커가 성공적으로 추가되면 리스트에 저장하고 카운터를 증가시킵니다.
        if (marker != null) {
            markersList.add(marker)
            markerCounter++ // 마커 추가 후 카운터 증가
            marker.showInfoWindow() // InfoWindow를 바로 표시
        }

        return marker ?: throw IllegalStateException("Marker could not be added")
    }


    class CustomInfoWindowAdapter(private val context: Context) : GoogleMap.InfoWindowAdapter {

        override fun getInfoWindow(marker: Marker): View? {
            // 기본 InfoWindow 사용
            return null
        }

        override fun getInfoContents(marker: Marker): View {
            // 커스텀 뷰 생성
            val view = LayoutInflater.from(context).inflate(R.layout.custom_info_window, null)

            // TextView에 마커의 제목 설정
            val titleTextView = view.findViewById<TextView>(R.id.title)
            titleTextView.text = marker.title

            return view
        }
    }
//현용

    override fun onConnected(p0: Bundle?) {
        val lat = intent.getDoubleExtra("lat", 0.0)
        val long = intent.getDoubleExtra("long", 0.0)

        // 마커 리스트가 존재하지 않을 때만 현재 위치로 이동
        if (lat == 0.0 && long == 0.0 || (lat == 91.0 || long == 181.0)) {
            // 마커가 없을 경우에만 현재 위치로 이동
            markerViewModel.markerList.observe(this) { markerList ->
                if (markerList.isEmpty()) {
                    moveToCurrentLocation()
                }
            }
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

    private fun showCustomDialog(marker: Marker) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_markinfo, null)

        // 위도, 경도 설정
        val latitudeEditText = dialogView.findViewById<EditText>(R.id.edit_latitude)
        val longitudeEditText = dialogView.findViewById<EditText>(R.id.edit_longitude)

        latitudeEditText.setText(marker.position.latitude.toString())
        longitudeEditText.setText(marker.position.longitude.toString())

        // 도분초 값 변환
        val (latDegrees, latMinutes, latSeconds) = decimalToDMS(marker.position.latitude)
        val (longDegrees, longMinutes, longSeconds) = decimalToDMS(marker.position.longitude)

        // 도분초 값 설정
        dialogView.findViewById<EditText>(R.id.degrees_lat).setText(latDegrees.toString())
        dialogView.findViewById<EditText>(R.id.minutes_lat).setText(latMinutes.toString())
        dialogView.findViewById<EditText>(R.id.seconds_lat).setText(latSeconds.toString())

        dialogView.findViewById<EditText>(R.id.degrees_long).setText(longDegrees.toString())
        dialogView.findViewById<EditText>(R.id.minutes_long).setText(longMinutes.toString())
        dialogView.findViewById<EditText>(R.id.seconds_long).setText(longSeconds.toString())

        // 동적으로 제목 설정
        val titleTextView = dialogView.findViewById<TextView>(R.id.title_text)
        titleTextView.text = "$titleCounter"

        // 모델 지정 Spinner 설정
        val modelSpinner = dialogView.findViewById<Spinner>(R.id.spinner_model)
        val modelImageView = dialogView.findViewById<ImageView>(R.id.model_image)

        // 모델 목록
        val models = arrayOf("모델 1", "모델 2", "모델 3")
        val modelImages = arrayOf(R.drawable.fan, R.drawable.fan, R.drawable.fan)

        // Spinner 어댑터 설정
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, models)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modelSpinner.adapter = adapter

        // Spinner 선택 이벤트 처리
        modelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                modelImageView.setImageResource(modelImages[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // 선택되지 않았을 때 처리
            }
        }

        // Directions Spinner 설정
        val directionLatSpinner = dialogView.findViewById<Spinner>(R.id.direction_lat_spinner)
        val directionLongSpinner = dialogView.findViewById<Spinner>(R.id.direction_long_spinner)

        val directionLatAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.latitude_directions,
            android.R.layout.simple_spinner_item
        )
        directionLatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        directionLatSpinner.adapter = directionLatAdapter

        val directionLongAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.longitude_directions,
            android.R.layout.simple_spinner_item
        )
        directionLongAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        directionLongSpinner.adapter = directionLongAdapter

        // TabHost 설정
        val tabHost = dialogView.findViewById<TabHost>(R.id.tabHost)
        tabHost.setup()

        val spec1 = tabHost.newTabSpec("Model").setIndicator("모델 지정").setContent(R.id.tab1)
        tabHost.addTab(spec1)

        val spec2 = tabHost.newTabSpec("Coordinates").setIndicator("좌표 지정").setContent(R.id.tab2)
        tabHost.addTab(spec2)

        // 다이얼로그 빌더 생성
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setView(dialogView)
            .setCancelable(true)

        val alertDialog = dialogBuilder.create()
        alertDialog.show()

        // 저장하기 클릭 이벤트
        dialogView.findViewById<TextView>(R.id.tv_target).apply {
            text = "저장하기"
            setOnClickListener {
                // 도분초 및 위도, 경도 값 모두 받기
                val degreesLat = dialogView.findViewById<EditText>(R.id.degrees_lat).text.toString().toDoubleOrNull()
                val minutesLat = dialogView.findViewById<EditText>(R.id.minutes_lat).text.toString().toDoubleOrNull()
                val secondsLat = dialogView.findViewById<EditText>(R.id.seconds_lat).text.toString().toDoubleOrNull()

                val degreesLong = dialogView.findViewById<EditText>(R.id.degrees_long).text.toString().toDoubleOrNull()
                val minutesLong = dialogView.findViewById<EditText>(R.id.minutes_long).text.toString().toDoubleOrNull()
                val secondsLong = dialogView.findViewById<EditText>(R.id.seconds_long).text.toString().toDoubleOrNull()

                val newLatitude = latitudeEditText.text.toString().toDoubleOrNull()
                val newLongitude = longitudeEditText.text.toString().toDoubleOrNull()

                // 도분초 및 위도/경도 값이 모두 입력되었는지 확인
                if (degreesLat != null && minutesLat != null && secondsLat != null &&
                    degreesLong != null && minutesLong != null && secondsLong != null) {
                    // 도분초 값을 위도 경도 값으로 변환
                    val latDecimal = dmsToDecimal(degreesLat, minutesLat, secondsLat)
                    val longDecimal = dmsToDecimal(degreesLong, minutesLong, secondsLong)

                    // 위도/경도 필드에도 값 반영
                    latitudeEditText.setText(latDecimal.toString())
                    longitudeEditText.setText(longDecimal.toString())

                    // 마커의 위치를 새로운 위도/경도로 업데이트
                    marker.position = LatLng(latDecimal, longDecimal)

                    marker.showInfoWindow()
                    Toast.makeText(this@GisActivity, "마커 위치가 업데이트되었습니다.", Toast.LENGTH_SHORT).show()
                    alertDialog.dismiss()

                } else if (newLatitude != null && newLongitude != null) {
                    // 위도와 경도 값만 입력된 경우, 도분초 값으로 변환
                    val (convertedLatDegrees, convertedLatMinutes, convertedLatSeconds) = decimalToDMS(newLatitude)
                    val (convertedLongDegrees, convertedLongMinutes, convertedLongSeconds) = decimalToDMS(newLongitude)

                    dialogView.findViewById<EditText>(R.id.degrees_lat).setText(convertedLatDegrees.toString())
                    dialogView.findViewById<EditText>(R.id.minutes_lat).setText(convertedLatMinutes.toString())
                    dialogView.findViewById<EditText>(R.id.seconds_lat).setText(convertedLatSeconds.toString())

                    dialogView.findViewById<EditText>(R.id.degrees_long).setText(convertedLongDegrees.toString())
                    dialogView.findViewById<EditText>(R.id.minutes_long).setText(convertedLongMinutes.toString())
                    dialogView.findViewById<EditText>(R.id.seconds_long).setText(convertedLongSeconds.toString())

                    marker.position = LatLng(newLatitude, newLongitude)
                    marker.showInfoWindow()
                    Toast.makeText(this@GisActivity, "마커 위치가 업데이트되었습니다.", Toast.LENGTH_SHORT).show()
                    alertDialog.dismiss()
                } else {
                    Toast.makeText(this@GisActivity, "올바른 값을 입력하세요.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 삭제하기 클릭 이벤트
        dialogView.findViewById<TextView>(R.id.tv_delete).setOnClickListener {
            marker.remove()
            Toast.makeText(this, "마커가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            alertDialog.dismiss()
        }
    }

    // 규제구역 로드 함수 수정: 마커 위치를 확인하고 규제구역 내에 있는 마커는 삭제

    private fun loadDevelopmentRestrictedAreas() {
        // 여기서 원래 사용하고 있던 API의 URL을 설정합니다.
        val apiKey = "05C26CB0-9905-39AC-8E59-423EE652CA06"  // 사용자의 API 키 입력
        val url = "https://api.vworld.kr/req/data?service=data&request=GetFeature&data=LT_C_UD801&key=$apiKey&geomFilter=BOX(${long - 1},${lat - 1},${long + 1},${lat + 1})&format=json&size=100"

        DownloadTask().execute(url)

        // 규제구역 로드 후, 마커 검사 및 제거
        for (marker in markersList) {
            val markerPosition = marker.position
            if (isLocationInRestrictedArea(markerPosition.latitude, markerPosition.longitude)) {
                marker.remove() // 마커 제거
                Toast.makeText(this, "마커가 규제구역 내에 있어 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 리스트에서 제거된 마커들 갱신
        markersList.removeAll { isLocationInRestrictedArea(it.position.latitude, it.position.longitude) }
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

                // 기존 폴리곤 삭제
                hideRestrictedAreas()

                // 새로 로드된 폴리곤 추가
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

                    polygonOptions.fillColor(0x7FFF0000)  // 규제구역 색상 및 투명도
                    polygonOptions.strokeColor(Color.RED)
                    polygonOptions.strokeWidth(2f)

                    val polygon = googleMap?.addPolygon(polygonOptions)
                    polygon?.let { polygonList.add(it) }
                }

                // 새로 로드된 규제구역을 기준으로 마커 삭제
                removeMarkersInRestrictedAreas()

                isRestrictedAreaVisible = true
            }
        }
    }


    // 규제구역 내의 마커를 제거하는 함수
    private fun removeMarkersInRestrictedAreas() {
        val markersToRemove = markersList.filter { marker ->
            val markerPosition = marker.position
            isLocationInRestrictedArea(markerPosition.latitude, markerPosition.longitude)
        }

        markersToRemove.forEach { marker ->
            marker.remove()
            Toast.makeText(this, "마커가 규제구역 내에 있어 삭제되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // 리스트에서 제거된 마커들 갱신
        markersList.removeAll(markersToRemove)

    }
    //위도경도 도분초 변환 공식
    private fun dmsToDecimal(degrees: Double, minutes: Double, seconds: Double): Double {
        return degrees + (minutes / 60) + (seconds / 3600)
    }
    private fun decimalToDMS(decimal: Double): Triple<Double, Double, Double> {
        val degrees = decimal.toInt()
        val minutes = ((decimal - degrees) * 60).toInt()
        val seconds = (((decimal - degrees) * 60) - minutes) * 60
        return Triple(degrees.toDouble(), minutes.toDouble(), seconds)
    }

// 수민
    // 애니메이션 설정 함수
    private fun animateRecyclerView(show: Boolean) {
        // 목표 translationY 값 설정
        val targetY = if (show) 0f else recyclerLayout.height.toFloat()

        // ObjectAnimator를 사용하여 recyclerLayout의 translationY를 애니메이션
        val animator = ObjectAnimator.ofFloat(recyclerLayout, "translationY", targetY)
        animator.duration = 500 // 애니메이션 지속 시간 (ms)

        // 애니메이션 시작 전과 끝 후에 터치 이벤트를 처리
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                if (show) {
                    // 리사이클러뷰를 보여줄 때, 터치 이벤트를 차단하고 레이아웃을 표시
                    recyclerLayout.setOnTouchListener { _, _ -> true }
                    recyclerLayout.visibility = View.VISIBLE
                    recyclerView.isEnabled = true
                    businessAdapter.updateVisibility(true)
                    // 지도 터치 비활성화
                    googleMap?.uiSettings?.isScrollGesturesEnabled = false
                    googleMap?.uiSettings?.isZoomGesturesEnabled = false
                } else {
                    // 숨길 때 터치 이벤트를 차단합니다.
                    recyclerLayout.setOnTouchListener { _, _ -> true }
                }
            }

            override fun onAnimationEnd(animation: Animator) {
                if (!show) {
                    // 애니메이션이 끝난 후, 레이아웃을 완전히 숨기고 터치 이벤트를 해제
                    recyclerLayout.visibility = View.GONE
                    recyclerView.isEnabled = false
                    businessAdapter.updateVisibility(false)
                    recyclerLayout.setOnTouchListener(null)
                    // 지도 터치 활성화
                    googleMap?.uiSettings?.isScrollGesturesEnabled = true
                    googleMap?.uiSettings?.isZoomGesturesEnabled = true
                }
                // 리사이클러뷰 가시성 상태 업데이트
                isRecyclerViewVisible = show
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })

        animator.start() // 애니메이션 시작
    }
    // 목록 클릭 이벤트
    private fun handleClick(data: Business) {
        Log.d(TAG, "Clicked item: ${data.title}")

        // 선택한 사업지의 좌표로 이동
        val latLng = LatLng(91.0, 181.0)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

        // 선택한 위치에 마커 추가
        //addMarkerAtLocation(data.lat, data.long, data.title)
    }
    // 수민

}