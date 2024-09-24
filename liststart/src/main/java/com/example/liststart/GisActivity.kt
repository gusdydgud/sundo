package com.example.liststart

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
import com.google.maps.android.data.geojson.GeoJsonLayer
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TabHost
import android.widget.TextView
import android.widget.Toast
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
import com.google.maps.android.PolyUtil
import com.google.maps.android.data.geojson.GeoJsonMultiPolygon
import com.google.maps.android.data.geojson.GeoJsonPolygon
import com.google.maps.android.data.geojson.GeoJsonPolygonStyle
import com.google.maps.android.geometry.Point
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
//import com.unity3d.player.UnityPlayerActivity

class GisActivity : AppCompatActivity(), OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    // Google Map 관련 변수
    private var googleMap: GoogleMap? = null
    private var currentCenter: LatLng? = null // 현재 지도의 중심 좌표
    private var geoJsonLayer: GeoJsonLayer? = null

    private var isRegulatoryAreaVisible: Boolean = false
    private var geoJsonLayers = mutableMapOf<String, GeoJsonLayer>()

    //체크박스 규제구역
    private lateinit var checkBoxLayout: LinearLayout
    private var isCheckBoxVisible = false
    private var polygonOptionsList: MutableList<PolygonOptions> = mutableListOf()


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
    private var data: Business? = null// 선택된 사업
    private var title: String? = null// 선택된 사업

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

// 수민
    // 마커와 비즈니스 ID를 매핑할 Map 추가
    private val markerMap = mutableMapOf<Long, MutableList<Marker>>()
    // 마커 데이터를 캐싱할 Map 추가
    private val markerCache = mutableMapOf<Long, com.example.liststart.model.Marker>()
    private val markerColorMap = mutableMapOf<Long, Float>() // bno별로 색상 매핑
    private val availableColors = listOf(
        BitmapDescriptorFactory.HUE_RED,
        BitmapDescriptorFactory.HUE_BLUE,
        BitmapDescriptorFactory.HUE_GREEN,
        BitmapDescriptorFactory.HUE_ORANGE,
        BitmapDescriptorFactory.HUE_YELLOW,
        BitmapDescriptorFactory.HUE_VIOLET
    )

    private fun getMarkerColorForBno(bno: Long): Float {
        return markerColorMap.getOrPut(bno) {
            availableColors[markerColorMap.size % availableColors.size] // 색상을 순차적으로 할당
        }
    }
    // 지도 이동을 제어하는 변수 추가
    private var isInitialMarkerLoaded = false

    // 마커와 마커 데이터를 매핑할 Map 추가
    private val markerDataMap = mutableMapOf<Marker, com.example.liststart.model.Marker>()
// 수민

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
        val isInside = (intersectCount % 2 == 1)
        return isInside    }

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

        // 캐시 초기화
        markerCache.clear()

        // 인텐트로 전달된 제목 데이터 받기
        data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra("data", Business::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra<Business>("data")
        }

        title = data?.title ?: "이름없음"

        // ViewModel 설정
        val businessViewModelFactory = DataSourceProvider.businessViewModelFactory
        businessViewModel = ViewModelProvider(this, businessViewModelFactory).get(BusinessViewModel::class.java)
        val markerViewModelFactory = DataSourceProvider.markerViewModelFactory
        markerViewModel = ViewModelProvider(this, markerViewModelFactory).get(MarkerViewModel::class.java)

        // RecyclerView 설정
        recyclerLayout = findViewById(R.id.recyclerLayout)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // RecyclerView 높이를 미리 화면 아래로 이동
        recyclerLayout.post {
            recyclerLayout.translationY = recyclerLayout.height.toFloat()
        }

        // 어댑터 초기화
        businessAdapter = BusinessAdapter(
            true,
            isVisible = true,
            onItemClick = { item -> Log.d("rootClick", "onCreate: $item") },  // 아이템 클릭 이벤트 처리
            onCheckBoxClick = { item -> item.bno?.let { bno ->
                    if (item.isChecked) {
                        // 체크박스가 체크되었을 때 마커 추가
                        markerViewModel.loadMarkerList(bno)
                    } else {
                        // 체크박스가 해제되었을 때 마커 제거
                        removeMarkersForBusiness(bno)
                    }
                }
            }  // 체크박스 클릭 이벤트 처리
        )

        recyclerView.adapter = businessAdapter

        // 전달된 사업의 마커를 로드하고 지도에 표시
        data?.bno?.let { initialBno ->
            markerViewModel.loadMarkerList(initialBno)
        }

        markerViewModel.markerList.observe(this) { updatedMarkerList ->
            // 현재 지도에 표시된 마커 중 유지할 마커를 추적
            val markersToKeep = mutableListOf<Marker>()

            // 현재 선택된 사업지와 체크된 사업지의 bno를 리스트로 준비
            val selectedBno = data?.bno
            val checkedBnos = businessViewModel.businessList.value
                ?.filter { it.isChecked }
                ?.map { it.bno }
                ?.filterNotNull() // null 값을 제외하여 List<Long>으로 변환
                ?: emptyList()

            // 유지할 마커와 제거할 마커 분리
            markerMap.forEach { (bno, markers) ->
                markers.forEach { marker ->
                    val markerData = markerDataMap[marker]
                    if (markerData != null && (markerData.bno == selectedBno || checkedBnos.contains(markerData.bno))) {
                        markersToKeep.add(marker)
                    } else {
                        // 지도에서 제거하고 데이터 매핑에서 제거
                        marker.remove()
                        markerDataMap.remove(marker)
                    }
                }
            }

            // markerMap을 업데이트하여 유지할 마커만 남김
            markerMap.keys.forEach { bno ->
                markerMap[bno] = markerMap[bno]?.filter { markersToKeep.contains(it) }?.toMutableList() ?: mutableListOf()
            }

            // 업데이트된 마커 리스트에서 지도에 표시되지 않은 마커를 추가
            updatedMarkerList.forEach { markerData ->
                if (shouldDisplayMarker(markerData, selectedBno, checkedBnos)) {
                    // 마커가 이미 존재하는지 확인
                    val markerExists = markerMap[markerData.bno]?.any {
                        it.position.latitude == markerData.latitude && it.position.longitude == markerData.longitude
                    } == true

                    // 마커가 존재하지 않을 경우에만 추가
                    if (!markerExists) {
                        addMarkerToMap(markerData)
                    }
                }
            }

            // 첫 번째 마커를 지도 중심으로 이동
            moveToFirstMarkerIfNeeded(updatedMarkerList)
        }

        markerViewModel.updateResult.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // BusinessViewModel의 데이터를 관찰하여 RecyclerView 업데이트
        businessViewModel.businessList.observe(this) { businessList ->
            businessAdapter.updateList(businessList)
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
            data?.let { currentBusiness ->
                // 현재 선택된 사업지를 제외한 사업지 목록 로드
                businessViewModel.loadBusinessListExcluding(currentBusiness)
            }
            // bno가 null이 아닌 경우에만 마커 로딩
            data?.bno?.let { bno ->
                markerViewModel.loadMarkerList(bno)
            } ?: run {
                Toast.makeText(this, "사업 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "네트워크 연결이 필요합니다.", Toast.LENGTH_SHORT).show()
        }

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
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
            )
        } else {
            apiClient.connect()
        }

        //진석 체크박스
        checkBoxLayout = findViewById(R.id.checkBoxLayout)

        val saveButton = findViewById<LinearLayout>(R.id.saveButton) // 저장 하기 버튼
        saveButton.setOnClickListener {
            toggleCheckBoxLayout() // 저장하기 버튼을 클릭하면 체크박스를 표시/숨깁니다.
        }
        val checkBox1 = findViewById<CheckBox>(R.id.checkBox1)
        val checkBox2 = findViewById<CheckBox>(R.id.checkBox2)
        val checkBox3 = findViewById<CheckBox>(R.id.checkBox3)
        val checkBox4 = findViewById<CheckBox>(R.id.checkBox4)
        val checkBox5 = findViewById<CheckBox>(R.id.checkBox5)
        val checkBox6 = findViewById<CheckBox>(R.id.checkBox6)
        val checkBox7 = findViewById<CheckBox>(R.id.checkBox7)
        val checkBox8 = findViewById<CheckBox>(R.id.checkBox8)
        val checkBox9 = findViewById<CheckBox>(R.id.checkBox9)

        //개발제한구역
        checkBox1.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // 개발제한구역 표시
                loadDevelopmentRestrictedAreas()
            } else {
                // 개발제한구역 숨기기
                hideRestrictedAreas1()
            }
        }

        // 2. 보호대상 해양생물 체크박스
        checkBox2.setOnCheckedChangeListener { _, isChecked ->
            googleMap?.let { map ->
                if (isChecked) {
                    loadGeoJsonFile(map, this, R.raw.intb_anml_a, Color.parseColor("#80FF0000")) // 빨간색
                } else {
                    hideRestrictedAreas(Color.parseColor("#80FF0000"))
                }
            }
        }

// 3. 해양 생물 보호 대상 체크박스
        checkBox3.setOnCheckedChangeListener { _, isChecked ->
            googleMap?.let { map ->
                if (isChecked) {
                    loadGeoJsonFile(map, this, R.raw.mammalia,  Color.parseColor("#80FFA500")) // 주황색
                } else {
                    hideRestrictedAreas(Color.parseColor("#80FFA500"))
                }
            }
        }

// 4. 수면 보호구역 체크박스
        checkBox4.setOnCheckedChangeListener { _, isChecked ->
            googleMap?.let { map ->
                if (isChecked) {
                    loadGeoJsonFile(map, this, R.raw.prtwt_surface, Color.parseColor("#8000FF00")) // 녹색
                } else {
                    hideRestrictedAreas(Color.parseColor("#8000FF00"))
                }
            }
        }

// 5. 담수 보호구역 체크박스
        checkBox5.setOnCheckedChangeListener { _, isChecked ->
            googleMap?.let { map ->
                if (isChecked) {
                    loadGeoJsonFile(map, this, R.raw.pwtrs_a, Color.parseColor("#800000FF")) // 파란색
                } else {
                    hideRestrictedAreas(Color.parseColor("#800000FF"))
                }
            }
        }

// 6. 파충류 보호구역 체크박스
        checkBox6.setOnCheckedChangeListener { _, isChecked ->
            googleMap?.let { map ->
                if (isChecked) {
                    loadGeoJsonFile(map, this, R.raw.reptile, Color.parseColor("#80FFFF00")) // 노란색
                } else {
                    hideRestrictedAreas(Color.parseColor("#80FFFF00"))
                }
            }
        }

// 7. 해초류 보호구역 체크박스
        checkBox7.setOnCheckedChangeListener { _, isChecked ->
            googleMap?.let { map ->
                if (isChecked) {
                    loadGeoJsonFile(map, this, R.raw.seaweed, Color.parseColor("#80800080")) // 보라색
                } else {
                    hideRestrictedAreas(Color.parseColor("#80800080"))
                }
            }
        }

// 8. 해양 생태계 보호구역 체크박스
        checkBox8.setOnCheckedChangeListener { _, isChecked ->
            googleMap?.let { map ->
                if (isChecked) {
                    loadGeoJsonFile(map, this, R.raw.wld_lvb_pzn_a, Color.parseColor("#8000FFFF")) // 청록색
                } else {
                    hideRestrictedAreas(Color.parseColor("#8000FFFF"))
                }
            }
        }

// 9. 어류 보호구역 체크박스
        checkBox9.setOnCheckedChangeListener { _, isChecked ->
            googleMap?.let { map ->
                if (isChecked) {
                    loadGeoJsonFile(map, this, R.raw.fish, Color.parseColor("#80FF4500")) // 오렌지색
                } else {
                    hideRestrictedAreas(Color.parseColor("#80FF4500"))
                }
            }
        }

        // 중앙 미리보기 마커와 위치 선택 텍스트뷰 설정
        centerMarkerPreview = findViewById(R.id.centerMarkerPreview)
        selectLocationTextView = findViewById(R.id.selectLocationTextView)
        centerMarkerPreview.visibility = View.GONE
        selectLocationTextView.visibility = View.GONE

        // 좌표 선택 버튼 클릭 이벤트 설정
        val selctlotiLayout = findViewById<LinearLayout>(R.id.selctloti)
        selctlotiLayout.setOnClickListener {
            if (isRecyclerViewVisible) {
                animateRecyclerView(false)
                isRecyclerViewVisible = !isRecyclerViewVisible
            }
            if(isCheckBoxVisible) {
                toggleCheckBoxLayout()
            }
            isMarkerPreviewVisible = !isMarkerPreviewVisible
            centerMarkerPreview.visibility = if (isMarkerPreviewVisible) View.VISIBLE else View.GONE
            selectLocationTextView.visibility = if (isMarkerPreviewVisible) View.VISIBLE else View.GONE
        }

        // '지정하기' 버튼 클릭 이벤트 설정
        selectLocationTextView.setOnClickListener {
            val currentCenter = googleMap?.cameraPosition?.target
            if (currentCenter != null) {
                if (isLocationInRestrictedArea(currentCenter.latitude, currentCenter.longitude)) {
                    Toast.makeText(this, "규제구역입니다. 마커를 추가할 수 없습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    val marker = com.example.liststart.model.Marker(
                        mno = 0L,
                        regdate = "",
                        update = "",
                        degree = 0L,
                        latitude = currentCenter.latitude,
                        longitude = currentCenter.longitude,
                        bno = data?.bno ?: 0L,
                        model = "model1",
                        title = (title ?: "사업체명") + " $markerCounter"
                    )
                    saveMarkerToServer(marker)
                }
            } else {
                Toast.makeText(this, "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 수정 버튼 클릭 이벤트 설정
        rightButton = findViewById(R.id.rightButton)
        rightButton.setOnClickListener {
            val newTitle = centerEditText.text.toString()
            data!!.title = newTitle
            businessViewModel.updateBusiness(data!!)
            Toast.makeText(this, "수정된 제목: $newTitle", Toast.LENGTH_SHORT).show()
        }

        // GPS 위치로 이동 버튼 설정
        leftButton = findViewById(R.id.leftButton)
        leftButton.setOnClickListener {
            moveToCurrentLocation()
        }

        // 사업지 목록 버튼 이벤트
        getListButton = findViewById(R.id.getListButton)
        getListButton.setOnClickListener {
            if(isMarkerPreviewVisible) {
                isMarkerPreviewVisible = !isMarkerPreviewVisible
                centerMarkerPreview.visibility = if (isMarkerPreviewVisible) View.VISIBLE else View.GONE
                selectLocationTextView.visibility = if (isMarkerPreviewVisible) View.VISIBLE else View.GONE
            }
            if(isCheckBoxVisible) {
                toggleCheckBoxLayout()
            }
            if (!isRecyclerViewVisible) {
                animateRecyclerView(true)
            } else {
                animateRecyclerView(false)
            }
        }

        //AR camera
//    val cameraBtn = findViewById<LinearLayout>(R.id.cameraBtn)
//    cameraBtn.setOnClickListener{
//        val markerDataList = ArrayList<Marker>() // 마커 데이터를 담을 리스트
//
//        for (marker in markersList) {
//            val markerData = markerDataMap[marker] // 마커 데이터 매핑에서 해당 마커의 데이터를 가져옴
//            markerData?.let {
//                markerDataList.add(it) // Marker 객체를 리스트에 추가
//            }
//        }
//
//        // Unity로 넘길 인텐트를 생성
//        val intent = Intent(this, UnityPlayerActivity::class.java).apply {
//            putParcelableArrayListExtra("markerDataList", markerDataList) // Marker 객체 리스트를 담아서 보냄
//        }
//
//        startActivity(intent)
//    }
    }

    // 특정 마커가 표시되어야 하는지 확인
    private fun shouldDisplayMarker(markerData: com.example.liststart.model.Marker, selectedBno: Long?, checkedBnos: List<Long>): Boolean {
        return markerData.bno == selectedBno || checkedBnos.contains(markerData.bno)
    }

    // 마커를 지도에 추가하고 관련 맵에 업데이트
    private fun addMarkerToMap(markerData: com.example.liststart.model.Marker) {
        // 마커 색상 설정 (bno에 따라)
        val markerColor = getMarkerColorForBno(markerData.bno)

        val markerOptions = MarkerOptions()
            .position(LatLng(markerData.latitude, markerData.longitude))
            .title(markerData.title)
            .icon(BitmapDescriptorFactory.defaultMarker(markerColor))

        val marker = googleMap?.addMarker(markerOptions)
        marker?.tag = markerData.mno

        // 마커를 markerMap 및 markerDataMap에 추가
        if (marker != null) {
            markerMap.getOrPut(markerData.bno) { mutableListOf() }.add(marker)
            markerDataMap[marker] = markerData
            // markersList에 마커를 추가
            markersList.add(marker)
            Log.d("MarkerInitialization", "Marker added to markersList: ${marker.position}")

        }
    }

    // 첫 번째 마커로 이동
    private fun moveToFirstMarkerIfNeeded(markerList: List<com.example.liststart.model.Marker>) {
        if (markerList.isNotEmpty() && !isInitialMarkerLoaded) {
            val firstMarkerPosition = LatLng(markerList.first().latitude, markerList.first().longitude)
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(firstMarkerPosition, 15f))
            isInitialMarkerLoaded = true
        }
    }

    // 체크박스 해제 시 마커를 제거하는 함수
    private fun removeMarkersForBusiness(bno: Long) {
        markerMap[bno]?.forEach { marker ->
            marker.remove() // 지도에서 마커 제거
        }
        markerMap.remove(bno) // 해당 사업(bno)의 마커 리스트 제거
        markerDataMap.entries.removeIf { it.value.bno == bno } // 마커 데이터 맵에서 제거
    }

    //현용
    override fun onMapReady(map: GoogleMap?) {
        googleMap = map

        // 커스텀 InfoWindow 어댑터 설정
        googleMap?.setInfoWindowAdapter(CustomInfoWindowAdapter(this))

        // 마커 클릭 리스너 추가
        googleMap?.setOnMarkerClickListener { marker ->
            // ViewModel을 통해 마커 수정 다이얼로그를 표시
            showCustomDialog(marker)
            true
        }

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
    // 마커를 지도에 추가하고 tag에 mno 값을 설정 현용건들지마
    private fun addMarkerAtLocation(
        latitude: Double,
        longitude: Double,
        title: String = "사업지명 $markerCounter",
        markerColor: Float = BitmapDescriptorFactory.HUE_RED,
        markerData: com.example.liststart.model.Marker? = null // markerData를 전달받아 처리
    ): Marker {
        val latLng = LatLng(latitude, longitude)
        val markerOption = MarkerOptions()
            .position(latLng)
            .title(title)
            .icon(BitmapDescriptorFactory.defaultMarker(markerColor))

        val marker = googleMap?.addMarker(markerOption)

        // 새 마커인 경우 mno는 0L이므로, 서버에서 가져온 마커에 대해서만 태그에 mno 설정
        marker?.tag = markerData?.mno ?: 0L // 새로 추가한 마커는 기본 0L, 서버에서 가져온 마커는 실제 mno

        // 마커가 성공적으로 추가되면 리스트에 저장하고 카운터를 증가시킵니다.
        if (marker != null) {
            markersList.add(marker)
            Log.d("MarkerInitialization", "Marker added: ${marker.position}")

            markerCounter++
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
//현용 위에부분끝

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
    //진석
    private fun showCustomDialog(marker: Marker) {
        val markerMno = marker.tag as? Long

        // 마커의 mno가 null이거나 0L인 경우 오류 처리
        if (!isValidMarkerMno(markerMno)) return

        // 캐시에서 데이터 조회
        val cachedMarkerData = markerCache[markerMno]

        // 캐시에 데이터가 있으면 그 데이터를 사용, 없으면 서버 데이터를 사용
        val markerData = cachedMarkerData ?: markerDataMap[marker]

        // 현재 데이터의 bno와 마커의 bno가 일치하지 않으면 함수 종료
        if (!isValidMarkerData(markerData)) return

        // 마커의 bno와 data의 bno가 일치하는 경우에만 수정/삭제 다이얼로그를 표시
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_markinfo, null)

        // 다이얼로그 초기화
        initializeDialogView(dialogView, marker, markerData)

        // 다이얼로그 빌더 생성 및 표시
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setView(dialogView)
            .setCancelable(true)

        val alertDialog = dialogBuilder.create()
        alertDialog.show()

        // 저장하기 클릭 이벤트
        dialogView.findViewById<TextView>(R.id.tv_target).apply {
            text = "저장하기"
            setOnClickListener { onSaveClick(marker, dialogView, alertDialog) }
        }

        // 삭제 버튼 클릭 이벤트 처리
        dialogView.findViewById<TextView>(R.id.tv_delete).setOnClickListener {
            onDeleteClick(marker, alertDialog)
        }
    }

    // 마커의 mno가 유효한지 검사
    private fun isValidMarkerMno(mno: Long?): Boolean {
        if (mno == null || mno == 0L) {
            Toast.makeText(this, "마커 정보가 잘못되었습니다.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    // 마커 데이터가 유효한지 검사
    private fun isValidMarkerData(markerData: com.example.liststart.model.Marker?): Boolean {
        if (markerData == null || markerData.bno != data?.bno) {
            Toast.makeText(this, "이 마커는 수정 및 삭제가 불가능합니다.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    // 다이얼로그 초기화
    private fun initializeDialogView(dialogView: View, marker: Marker, markerData: com.example.liststart.model.Marker?) {
        // 위도, 경도 설정
        val latitudeEditText = dialogView.findViewById<EditText>(R.id.edit_latitude)
        val longitudeEditText = dialogView.findViewById<EditText>(R.id.edit_longitude)
        latitudeEditText.setText(marker.position.latitude.toString())
        longitudeEditText.setText(marker.position.longitude.toString())

        // 도분초 값 변환 및 설정
        val (latDegrees, latMinutes, latSeconds) = decimalToDMS(marker.position.latitude)
        val (longDegrees, longMinutes, longSeconds) = decimalToDMS(marker.position.longitude)
        setDmsValues(dialogView, latDegrees, latMinutes, latSeconds, longDegrees, longMinutes, longSeconds)

        // 제목 설정
        val titleTextView = dialogView.findViewById<TextView>(R.id.edit_title)

        // markerData가 존재한다면 마커의 제목을 설정하고, 그렇지 않다면 기본 제목을 설정합니다.
        val markerTitle = marker.title ?: "마커 정보"
        titleTextView.text = markerTitle

        // 모델 스피너 설정
        setupModelSpinner(dialogView, markerData?.model)

        // 각도 설정
        val degreeEditText = dialogView.findViewById<EditText>(R.id.edit_angle)
        degreeEditText.setText(markerData?.degree?.toString() ?: "0") // 저장된 각도 표시

        // 방향 스피너 설정
        setupDirectionSpinners(dialogView)

        // TabHost 설정
        setupTabHost(dialogView)
    }

    // 도분초 값 설정
    private fun setDmsValues(dialogView: View, latDegrees: Double, latMinutes: Double, latSeconds: Double,
                             longDegrees: Double, longMinutes: Double, longSeconds: Double) {
        dialogView.findViewById<EditText>(R.id.degrees_lat).setText(latDegrees.toString())
        dialogView.findViewById<EditText>(R.id.minutes_lat).setText(latMinutes.toString())
        dialogView.findViewById<EditText>(R.id.seconds_lat).setText(latSeconds.toString())

        dialogView.findViewById<EditText>(R.id.degrees_long).setText(longDegrees.toString())
        dialogView.findViewById<EditText>(R.id.minutes_long).setText(longMinutes.toString())
        dialogView.findViewById<EditText>(R.id.seconds_long).setText(longSeconds.toString())
    }

    // 모델 스피너 설정
    private fun setupModelSpinner(dialogView: View, selectedModel: String?) {
        val modelSpinner = dialogView.findViewById<Spinner>(R.id.spinner_model)
        val modelImageView = dialogView.findViewById<ImageView>(R.id.model_image)
        val models = arrayOf("모델 1", "모델 2", "모델 3")
        val modelImages = arrayOf(R.drawable.fan, R.drawable.fan, R.drawable.fan)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, models)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modelSpinner.adapter = adapter

        // model 값에 따라 스피너의 선택된 값을 설정
        val modelIndex = models.indexOf(selectedModel)
        if (modelIndex != -1) {
            modelSpinner.setSelection(modelIndex)
            modelImageView.setImageResource(modelImages[modelIndex])
        }

        modelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                modelImageView.setImageResource(modelImages[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    // 방향 스피너 설정
    private fun setupDirectionSpinners(dialogView: View) {
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
    }

    // TabHost 설정
    private fun setupTabHost(dialogView: View) {
        val tabHost = dialogView.findViewById<TabHost>(R.id.tabHost)
        tabHost.setup()

        val spec1 = tabHost.newTabSpec("Model").setIndicator("모델 지정").setContent(R.id.tab1)
        tabHost.addTab(spec1)

        val spec2 = tabHost.newTabSpec("Coordinates").setIndicator("좌표 지정").setContent(R.id.tab2)
        tabHost.addTab(spec2)
    }

    // 저장 버튼 클릭 이벤트
    private fun onSaveClick(marker: Marker, dialogView: View, alertDialog: AlertDialog) {
        // 기존 값과 비교하기 위한 현재 위도/경도 값 저장
        val oldLatitude = marker.position.latitude
        val oldLongitude = marker.position.longitude

        // 새로운 위도/경도 및 도분초 값 가져오기
        val degreesLat = dialogView.findViewById<EditText>(R.id.degrees_lat).text.toString().toDoubleOrNull()
        val minutesLat = dialogView.findViewById<EditText>(R.id.minutes_lat).text.toString().toDoubleOrNull()
        val secondsLat = dialogView.findViewById<EditText>(R.id.seconds_lat).text.toString().toDoubleOrNull()

        val degreesLong = dialogView.findViewById<EditText>(R.id.degrees_long).text.toString().toDoubleOrNull()
        val minutesLong = dialogView.findViewById<EditText>(R.id.minutes_long).text.toString().toDoubleOrNull()
        val secondsLong = dialogView.findViewById<EditText>(R.id.seconds_long).text.toString().toDoubleOrNull()

        val newLatitude = dialogView.findViewById<EditText>(R.id.edit_latitude).text.toString().toDoubleOrNull()
        val newLongitude = dialogView.findViewById<EditText>(R.id.edit_longitude).text.toString().toDoubleOrNull()

        // 사용자가 입력한 새로운 제목을 가져옴
        val newTitle = dialogView.findViewById<EditText>(R.id.edit_title).text.toString()

        // 선택된 모델을 가져오기
        val selectedModel = dialogView.findViewById<Spinner>(R.id.spinner_model).selectedItem.toString()

        // 입력된 각도를 가져오기
        val degreeValue = dialogView.findViewById<EditText>(R.id.edit_angle).text.toString().toLongOrNull() ?: 0L

        val isDMSChanged = degreesLat != null && minutesLat != null && secondsLat != null &&
                degreesLong != null && minutesLong != null && secondsLong != null

        val isLatLngChanged = newLatitude != null && newLongitude != null

        // 위도/경도 값 변경이 있는지 확인하고 업데이트
        if (isLatLngChanged && (newLatitude != oldLatitude || newLongitude != oldLongitude)) {
            updateMarkerPosition(marker, newLatitude ?: 0.0, newLongitude ?: 0.0, newTitle, selectedModel, degreeValue)
            alertDialog.dismiss()
        } else if (isDMSChanged) {
            val latDecimal = dmsToDecimal(degreesLat ?: 0.0, minutesLat ?: 0.0, secondsLat ?: 0.0)
            val longDecimal = dmsToDecimal(degreesLong ?: 0.0, minutesLong ?: 0.0, secondsLong ?: 0.0)

            updateMarkerPosition(marker, latDecimal, longDecimal, newTitle, selectedModel, degreeValue)
            alertDialog.dismiss()
        } else {
            Toast.makeText(this@GisActivity, "올바른 값을 입력하세요.", Toast.LENGTH_SHORT).show()
        }
    }
    // 마커 위치 업데이트 (모델과 각도 포함)
    private fun updateMarkerPosition(marker: Marker, latitude: Double, longitude: Double, title: String, model: String, degree: Long) {
        marker.position = LatLng(latitude, longitude)
        marker.title = title // 새 제목으로 마커의 title 업데이트
        marker.showInfoWindow() // InfoWindow를 갱신
        googleMap?.moveCamera(CameraUpdateFactory.newLatLng(LatLng(latitude, longitude)))

        Toast.makeText(this@GisActivity, "마커가 업데이트되었습니다.", Toast.LENGTH_SHORT).show()

        // 서버로 마커 정보 업데이트
        val updatedMarker = com.example.liststart.model.Marker(
            mno = marker.tag as? Long ?: 0L,
            regdate = "", // 필요시 처리
            update = "", // 필요시 처리
            degree = degree, // 업데이트된 각도
            latitude = latitude,
            longitude = longitude,
            bno = data?.bno ?: 0L, // 사업 ID
            model = model, // 선택한 모델
            title = title // 새로 입력된 제목으로 업데이트
        )

        // 캐시에 저장
        markerCache[updatedMarker.mno] = updatedMarker

        // 서버 업데이트
        markerViewModel.updateMarker(updatedMarker)
    }

    // 삭제 버튼 클릭 이벤트 처리
    private fun onDeleteClick(marker: Marker, alertDialog: AlertDialog) {
        val markerMno = marker.tag as? Long
        if (markerMno != null && markerMno != 0L) {
            val bno = data?.bno
            if (bno != null) {
                markerViewModel.deleteMarker(markerMno, bno) // 마커 삭제 요청
                marker.remove() // 지도에서 마커 제거
                markerMap[bno]?.remove(marker)
                markerDataMap.remove(marker)
            }
        } else {
            Toast.makeText(this, "마커 정보가 잘못되었습니다.", Toast.LENGTH_SHORT).show()
        }
        alertDialog.dismiss()
    }

    // 규제구역 로드 함수 수정: 마커 위치를 확인하고 규제구역 내에 있는 마커는 삭제
    private fun loadDevelopmentRestrictedAreas() {
        // 데이터베이스에서 첫 번째 좌표 가져오기
        markerViewModel.markerList.observe(this) { markerList ->
            if (markerList.isNotEmpty()) {
                // 첫 번째 마커의 좌표를 가져옴
                val firstMarker = markerList.first()
                val latitude = firstMarker.latitude
                val longitude = firstMarker.longitude

                // 규제구역 API 호출
                val apiKey = "05C26CB0-9905-39AC-8E59-423EE652CA06"
                val url = "https://api.vworld.kr/req/data?service=data&request=GetFeature&data=LT_C_UD801&key=$apiKey&geomFilter=BOX(${longitude - 1},${latitude - 1},${longitude + 1},${latitude + 1})&format=json&size=100"

                DownloadTask().execute(url)

                // 지도에서 마커가 규제구역 내에 있는지 확인하고, 필요한 경우 마커를 제거
                markersList.forEach { marker ->
                    val markerPosition = marker.position
                    if (isLocationInRestrictedArea(markerPosition.latitude, markerPosition.longitude)) {
                        marker.remove()
                        Toast.makeText(this, "마커가 규제구역 내에 있어 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "데이터베이스에 좌표가 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 특정 규제 구역을 삭제하는 함수 (layerKey 없이)
    private fun hideRestrictedAreas(polygonColor: Int) {
        // polygonList에서 해당 색상과 일치하는 폴리곤만 삭제
        val polygonsToRemove = polygonList.filter { polygon ->
            polygon.fillColor == polygonColor // 색상으로 폴리곤을 구분
        }

        polygonsToRemove.forEach { polygon ->
            polygon.remove() // 지도에서 폴리곤 삭제
            polygonList.remove(polygon) // 리스트에서도 제거
        }

        // 리스트에서 해당 규제구역 삭제 후 상태 업데이트
        isRestrictedAreaVisible = polygonList.isNotEmpty()
    }

    private fun hideRestrictedAreas1() {
        polygonList.forEach { it.remove() }
        polygonList.clear()
        polygonOptionsList.clear()
        isRestrictedAreaVisible = false
    }


    private fun loadGeoJsonFile(googleMap: GoogleMap, context: Context, geoJsonResId: Int, color: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                try {
                    val inputStream = context.resources.openRawResource(geoJsonResId)
                    val jsonStr = inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(jsonStr)
                    val features = jsonObject.getJSONArray("features")

                    withContext(Dispatchers.Main) {
                        for (i in 0 until features.length()) {
                            val feature = features.getJSONObject(i)
                            val geometry = feature.getJSONObject("geometry")
                            val type = geometry.getString("type")

                            if (type == "MultiPolygon") {
                                val coordinates = geometry.getJSONArray("coordinates")

                                for (j in 0 until coordinates.length()) {
                                    val polygonCoordinates = coordinates.getJSONArray(j).getJSONArray(0)
                                    val polygonOptions = PolygonOptions()

                                    for (k in 0 until polygonCoordinates.length()) {
                                        val coordinate = polygonCoordinates.getJSONArray(k)
                                        val latLng = LatLng(coordinate.getDouble(1), coordinate.getDouble(0)) // [lng, lat] 순서
                                        polygonOptions.add(latLng)
                                    }

                                    polygonOptions.fillColor(color)
                                    polygonOptions.strokeColor(Color.RED)
                                    polygonOptions.strokeWidth(2f)

                                    val polygon = googleMap.addPolygon(polygonOptions)
                                    polygon?.let { polygonList.add(it) }  // 리스트에 폴리곤 추가

                                    deleteMarkersInsidePolygon(polygon)

                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GeoJson", "GeoJSON 파일을 불러오는 중 오류 발생", e)
                }
            }
        }
    }



    // 폴리곤 내부에 있는 마커들을 찾아 삭제하는 함수
    private fun deleteMarkersInsidePolygon(polygon: Polygon) {
        val markersInsidePolygon = markersList.filter { marker ->
            isMarkerInsidePolygon(marker.position, polygon)
        }

        // 마커 삭제 처리
        markersInsidePolygon.forEach { marker ->
            val mno = marker.tag as? Long
            val bno = data?.bno
            if (mno != null && bno != null) {
                markerViewModel.deleteMarker(mno, bno) // 마커 삭제
                marker.remove() // 지도에서 마커 제거
                markerMap[bno]?.remove(marker)
                markerDataMap.remove(marker)
            }
        }
    }

    // 마커가 폴리곤 내부에 있는지 확인하는 함수
    private fun isMarkerInsidePolygon(markerPosition: LatLng, polygon: Polygon): Boolean {
        val polygonLatLngList = polygon.points
        return PolyUtil.containsLocation(markerPosition, polygonLatLngList, true)
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
                hideRestrictedAreas1()

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

    private fun toggleCheckBoxLayout() {
        if(isMarkerPreviewVisible) {
            isMarkerPreviewVisible = !isMarkerPreviewVisible
            centerMarkerPreview.visibility = if (isMarkerPreviewVisible) View.VISIBLE else View.GONE
            selectLocationTextView.visibility = if (isMarkerPreviewVisible) View.VISIBLE else View.GONE
        }
        if (isRecyclerViewVisible) {
            animateRecyclerView(false)
            isRecyclerViewVisible = !isRecyclerViewVisible
        }
        // 애니메이션 중 터치 이벤트를 차단하는 플래그
        var isAnimating = false

        // 현재 체크박스 레이아웃이 보이는지 상태에 따라 이동할 위치 설정
        val targetY = if (isCheckBoxVisible) checkBoxLayout.height.toFloat() else 0f

        // 애니메이션 시작
        ObjectAnimator.ofFloat(checkBoxLayout, "translationY", targetY).apply {
            duration = 500 // 애니메이션 지속 시간

            // 애니메이션 리스너 추가
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    // 애니메이션이 시작되면 터치 이벤트를 차단
                    isAnimating = true
                    // 체크박스 레이아웃이 보이지 않는 상태에서 시작할 때, 레이아웃을 보이게 설정
                    if (!isCheckBoxVisible) {
                        checkBoxLayout.visibility = View.VISIBLE
                    }
                }

                override fun onAnimationEnd(animation: Animator) {
                    // 애니메이션이 끝난 후 터치 이벤트를 다시 활성화
                    isAnimating = false
                    // 체크박스 레이아웃이 보이는 상태에서 애니메이션이 끝나면 숨김 처리
                    if (isCheckBoxVisible) {
                        checkBoxLayout.visibility = View.GONE
                    }
                    // 애니메이션이 끝난 후, 상태를 반전시킴
                    isCheckBoxVisible = !isCheckBoxVisible
                }
            })
            start()
        }

        // 애니메이션 중 터치 이벤트를 차단하는 로직
        checkBoxLayout.setOnTouchListener { _, _ -> isAnimating }
    }

////진석

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
// 수민

    private fun saveMarkerToServer(marker: com.example.liststart.model.Marker) {
        // 마커 데이터를 그대로 서버로 전송 (color 값 포함)
        markerViewModel.addMarker(
            marker,
            onSuccess = { savedMarker ->
                Toast.makeText(this, "마커가 서버에 저장되었습니다.", Toast.LENGTH_SHORT).show()

                // 저장 후 지도에 마커 추가
                addMarkerToMap(savedMarker)
            },
            onFailure = { errorMessage ->
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        )
    }

}