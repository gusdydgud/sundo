package com.example.liststart.view

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.liststart.GisActivity
import com.example.liststart.R
import com.example.liststart.adapter.BusinessAdapter
import com.example.liststart.databinding.CustomDialogBinding
import com.example.liststart.databinding.DeleteDialogBinding
import com.example.liststart.datasource.BusinessDataSourceImpl
import com.example.liststart.model.Business
import com.example.liststart.util.Constants
import com.example.liststart.viewmodel.BusinessViewModel
import com.example.liststart.viewmodel.BusinessViewModelFactory

const val TAG = "myLog"

class MainActivity : AppCompatActivity() {

    private lateinit var businessViewModel: BusinessViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var businessAdapter: BusinessAdapter
    private var isVisible = false
    private lateinit var searchEditText: EditText // 검색 editText
    private var backPressedTime: Long = 0 // 마지막으로 뒤로가기를 누른 시간을 저장하는 변수

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //현용 뒤로가기2번눌러서 앱종료
        // onBackPressedDispatcher를 통한 뒤로가기 콜백 등록
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 두 번 눌러야 앱이 종료되도록 처리
                if (System.currentTimeMillis() > backPressedTime + 2000) {
                    backPressedTime = System.currentTimeMillis()
                    Toast.makeText(this@MainActivity, "뒤로가기를 한 번 더 누르면 앱이 종료됩니다", Toast.LENGTH_SHORT).show()
                } else {
                    finishAffinity() // 현재 액티비티를 포함한 모든 액티비티 종료
                    System.exit(0) // 앱 프로세스 종료
                }
            }
        })
        //현용 뒤로가기2번눌러서 앱종료

        // BusinessDataSource 인스턴스를 생성
        val dataSource = BusinessDataSourceImpl(Constants.turbineApiService)
        val viewModelFactory = BusinessViewModelFactory(dataSource)

        // ViewModel을 Factory를 통해 생성
        businessViewModel = ViewModelProvider(this, viewModelFactory).get(BusinessViewModel::class.java)

        val rootLayout = findViewById<LinearLayout>(R.id.rootLayout)
        val topFadeOverlay: View = findViewById(R.id.topFadeOverlay)
        val bottomFadeOverlay: View = findViewById(R.id.bottomFadeOverlay)

        // RecyclerView 초기화
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 어댑터 초기화
        businessAdapter = BusinessAdapter(isVisible) { item -> handleClick(item) }
        recyclerView.adapter = businessAdapter

        // ViewModel의 데이터를 관찰하여 RecyclerView 업데이트
        businessViewModel.businessList.observe(this) { businessList ->
            businessAdapter.updateList(businessList)
        }

        // ViewModel의 에러 메시지를 관찰하여 토스트로 표시
        businessViewModel.error.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // 네트워크 상태 확인 후 데이터 로딩
        if (Constants.isNetworkAvailable(this)) {
            // 네트워크가 있을 때 데이터 로딩
            businessViewModel.loadBusinessList()
        } else {
            // 네트워크가 없을 때 사용자에게 알림
            Toast.makeText(this, "네트워크 연결이 필요합니다.", Toast.LENGTH_SHORT).show()
        }

        // 검색어 입력시 자동 필터링
        searchEditText = findViewById(R.id.searchEditText)
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                businessViewModel.filterBusiness(query) // ViewModel에 필터링 처리
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        setupUI(rootLayout)

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

        businessAdapter.updateVisibility(isVisible)
    }

    private fun handleClick(data: Business) {
        // GIS 화면으로 이동
        val intent = Intent(this, GisActivity::class.java)
        val bundle = Bundle()
        bundle.putString("title", data.title)
        //bundle.putDouble("lat", data.lat)
        //bundle.putDouble("long", data.long)
        intent.putExtras(bundle)
        startActivity(intent)
    }

    private fun handleAddBtnClick() {
        // UI 처리만 담당하고, 데이터 처리는 ViewModel에 위임
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
                dialogBinding.addEditText.error = "사업 이름을 입력하세요"
            } else {
                businessViewModel.addBusiness(Business(title = title)) // ViewModel에 데이터 추가 요청
                customDialog.dismiss()
            }
        }

        customDialog.show()
    }

    private fun handleDeleteBtnClick() {
        // 선택된 항목을 ViewModel로 삭제 요청
        if (businessAdapter.isAnyChecked()) {
            val deleteDialog = Dialog(this, R.style.CustomDialogTheme)
            val dialogBinding = DeleteDialogBinding.inflate(layoutInflater)
            dialogBinding.deleteItem.text = businessAdapter.getCheckedItemTitle()
            deleteDialog.setContentView(dialogBinding.root)
            dialogResize(this, deleteDialog, 0.9f)

            dialogBinding.dialogCancel.setOnClickListener {
                businessAdapter.changeIsCheckedToDefault()
                deleteDialog.dismiss()
            }

            dialogBinding.dialogConfirm.setOnClickListener {
                val selectedBusinessIds = businessAdapter.getSelectedBusinessIds()
                businessViewModel.deleteSelectedBusinesses(selectedBusinessIds) // ViewModel에 삭제 요청
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
