package com.example.liststart

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.liststart.databinding.CustomDialogBinding
import com.example.liststart.databinding.DeleteDialogBinding
import com.example.liststart.util.Constants
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

const val TAG = "myLog"

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var itemAdapter: ItemAdapter
    private var isVisible = false
    private lateinit var searchEditText: EditText // 검색 editText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rootLayout = findViewById<LinearLayout>(R.id.rootLayout)
        val topFadeOverlay: View = findViewById(R.id.topFadeOverlay)
        val bottomFadeOverlay: View = findViewById(R.id.bottomFadeOverlay)

        // 전역 데이터를 관리하는 MyApplication 객체 가져오기
        val app = application as MyApplication

        var exampleList: ArrayList<Item>? = null

        if(Constants.isNetworkAvailable(this)) {
            Toast.makeText(this, "인터넷 연결상태 양호", Toast.LENGTH_LONG).show()
            // 레트로핏 호출

            // ViewModel 또는 DataStore 에서 사용을 권장
            val turbineApiService = Constants.createRetrofit()

            // 콜백 받음 enqueue() 로
            turbineApiService.getBusinessAll().enqueue(object : Callback<ArrayList<Item>> {
                override fun onResponse(call: Call<ArrayList<Item>>, response: Response<ArrayList<Item>>) {
                    // 성공했을때 처리
                    if(response.isSuccessful) {
                        exampleList = response.body()
                        Log.d("myNewLog", "$exampleList")
                    } else {
                        Log.d("myNewLog", "실패")
                    }
                }

                override fun onFailure(call: Call<ArrayList<Item>>, t: Throwable) {
                    // 실패했을때 처리
                    Log.d("myNewLog", "실패")
                }

            })

        } else {
            Toast.makeText(this, "인터넷 연결이 없습니다.", Toast.LENGTH_LONG).show()
        }

        // 예시 데이터
//        val exampleList = arrayListOf(
//            Item("가산 풍력디지털 단지", "2024.09.05", R.drawable.profile, 37.479180, 126.874852),
//            Item("송파구 법조타운", "2024.09.01", R.drawable.profile, 37.483817, 127.112121),
//            Item("영등포 스마트 도시", "2024.08.30", R.drawable.profile, 37.529931, 126.887700),
//            Item("선도 디지털 단지", "2024.08.30", R.drawable.profile, 37.480417, 126.874323),
//            Item("제주 탐라 풍력 단지", "2024.08.30", R.drawable.profile, 33.499911, 126.449403),
//            Item("영흥 풍력 단지", "2024.08.30", R.drawable.profile, 37.239810, 126.446187),
//            Item("가산 풍력디지털 단지", "2024.09.05", R.drawable.profile, 37.479180, 126.874852),
//            Item("송파구 법조타운", "2024.09.01", R.drawable.profile, 37.483817, 127.112121),
//            Item("영등포 스마트 도시", "2024.08.30", R.drawable.profile, 37.529931, 126.887700),
//            Item("선도 디지털 단지", "2024.08.30", R.drawable.profile, 37.480417, 126.874323),
//            Item("제주 탐라 풍력 단지", "2024.08.30", R.drawable.profile, 33.499911, 126.449403),
//            Item("영흥 풍력 단지", "2024.08.30", R.drawable.profile, 37.239810, 126.446187)
//        )

        // 데이터 저장
        //app.setItemList(exampleList)
        //app.setItemList(exampleList!!)

        // RecyclerView 설정
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // itemAdapter 초기화
        itemAdapter = ItemAdapter(isVisible) { item -> handleClick(item) }

        // MyApplication을 Adapter에 설정
        itemAdapter.setApplicationContext(this)

        // RecyclerView에 어댑터 설정
        recyclerView.adapter = itemAdapter

        // 스크롤 리스너 추가
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // 스크롤 오프셋을 계산하여 투명도 조절
                val scrollOffset = recyclerView.computeVerticalScrollOffset()

                if (scrollOffset > 0) {
                    topFadeOverlay.visibility = View.VISIBLE
                    topFadeOverlay.alpha = 1f
                    bottomFadeOverlay.alpha = 1f
                } else {
                    topFadeOverlay.visibility = View.GONE
                }
            }
        })

        // 검색 버튼 클릭 리스너
        val searchButton = findViewById<ImageButton>(R.id.searchButton)
        searchButton.setOnClickListener {
            val target = searchEditText.text.toString()
            // MyApplication의 필터 메서드 사용
            val filteredList = app.filterItem(target)
            itemAdapter.notifyDataSetChanged() // 필터링 결과 적용 후 UI 갱신
        }

        // EditText 초기화
        searchEditText = findViewById(R.id.searchEditText)

        // 검색어 입력시 자동 필터링
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                val filteredList = app.filterItem(query) // 필터링된 리스트 가져오기
                itemAdapter.setFilteredList(filteredList) // 필터링된 리스트를 어댑터에 전달
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        setupUI(rootLayout)

        // 검색 포커스 밖에서 키보드 숨김 처리
        searchEditText.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                hideKeyboard()
            }
        }

        // 추가 버튼 클릭 리스너
        val addButton = findViewById<ImageButton>(R.id.addButton)
        addButton.setOnClickListener {
            handleAddBtnClick()
        }

        // 삭제 버튼 클릭 리스너
        val deleteButton = findViewById<ImageButton>(R.id.deleteButton)
        deleteButton.setOnClickListener {
            if (isVisible) {
                handleDeleteBtnClick()
            }
            toggleCheckBoxVisibility()
        }
    }

    // 키보드 숨김 처리
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
    }

    // 포커스 처리
    private fun setupUI(view: View) {
        if (view !is EditText) {
            view.setOnTouchListener { _, _ ->
                hideKeyboard()
                searchEditText.clearFocus()
                false
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val innerView = view.getChildAt(i)
                setupUI(innerView)
            }
        }
    }

    private fun toggleCheckBoxVisibility() {
        isVisible = !isVisible
        val deleteButton = findViewById<ImageButton>(R.id.deleteButton)

        if (isVisible) {
            deleteButton.setImageResource(R.drawable.ic_check) // 새로운 이미지로 변경
        } else {
            deleteButton.setImageResource(R.drawable.ic_trash_fill) // 원래 이미지로 복원
        }

        itemAdapter.updateVisibility(isVisible)
    }

    private fun handleClick(data: Item) {
        val intent = Intent(this, GisActivity::class.java)
        val bundle = Bundle()
        bundle.putString("title", data.title)
        bundle.putDouble("lat", data.lat)
        bundle.putDouble("long", data.long)
        intent.putExtras(bundle)
        startActivity(intent)
    }

    private fun handleAddBtnClick() {
        val customDialog = Dialog(this, R.style.CustomDialogTheme)
        val dialogBinding = CustomDialogBinding.inflate(layoutInflater)
        customDialog.setContentView(dialogBinding.root)
        dialogResize(this, customDialog, 0.9f)

        dialogBinding.dialogCancel.setOnClickListener {
            customDialog.dismiss()
        }

        dialogBinding.dialogConfirm.setOnClickListener {
            val title = dialogBinding.addEditText.text.toString()
            if (title.isBlank()) {
                dialogBinding.addEditText.error = "Title cannot be empty"
            } else {
                val app = application as MyApplication
                val newItem = Item(1L, title, "2024.09.05")
                itemAdapter.addItem(newItem) // 아이템 추가
                customDialog.dismiss()
            }
        }

        customDialog.show()
    }

    private fun handleDeleteBtnClick() {
        if (itemAdapter.isAnyChecked()) {
            val deleteDialog = Dialog(this, R.style.CustomDialogTheme)
            val dialogBinding = DeleteDialogBinding.inflate(layoutInflater)
            dialogBinding.deleteItem.text = itemAdapter.getCheckedItemTitle()
            deleteDialog.setContentView(dialogBinding.root)
            dialogResize(this, deleteDialog, 0.9f)

            dialogBinding.dialogCancel.setOnClickListener {
                itemAdapter.changeIsCheckedToDefault()
                deleteDialog.dismiss()
            }

            dialogBinding.dialogConfirm.setOnClickListener {
                itemAdapter.deleteCheckedItems() // 체크된 항목 삭제
                searchEditText.text.clear() // 검색 입력 초기화
                deleteDialog.dismiss()
            }

            deleteDialog.show()
        }
    }

    private fun dialogResize(context: Context, dialog: Dialog, width: Float, height: Float = 0f) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val size = if (Build.VERSION.SDK_INT < 30) {
            val display = windowManager.defaultDisplay
            val point = Point()
            display.getSize(point)
            point
        } else {
            windowManager.currentWindowMetrics.bounds.let { Point(it.width(), it.height()) }
        }

        dialog.window?.setLayout(
            (size.x * width).toInt(),
            if (height != 0f) (size.y * height).toInt() else WindowManager.LayoutParams.WRAP_CONTENT
        )
    }
}
