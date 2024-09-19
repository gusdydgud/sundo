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
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.liststart.GisActivity
import com.example.liststart.R
import com.example.liststart.adapter.BusinessAdapter
import com.example.liststart.databinding.CustomDialogBinding
import com.example.liststart.databinding.DeleteDialogBinding
import com.example.liststart.datasource.DataSourceProvider
import com.example.liststart.model.Business
import com.example.liststart.util.Constants
import com.example.liststart.viewmodel.BusinessViewModel

const val TAG = "myLog"

class MainActivity : AppCompatActivity() {

    private lateinit var businessViewModel: BusinessViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var businessAdapter: BusinessAdapter
    private lateinit var searchEditText: EditText
    private var backPressedTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ViewModel 초기화
        val viewModelFactory = DataSourceProvider.businessViewModelFactory
        businessViewModel = ViewModelProvider(this, viewModelFactory).get(BusinessViewModel::class.java)

        // RecyclerView 초기화
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 어댑터 초기화
        businessAdapter = BusinessAdapter(
            isVisible = false, // 초기값은 false
            onItemClick = { item ->
                handleClick(item)
            },
            onCheckBoxClick = { item ->
                businessViewModel.toggleBusinessItemCheck(item)
            }
        )

        recyclerView.adapter = businessAdapter

        // BusinessViewModel의 데이터를 관찰하여 RecyclerView 업데이트
        businessViewModel.businessList.observe(this) { businessList ->
            businessAdapter.updateList(businessList)
        }

        // 체크박스 가시성 상태 관찰
        businessViewModel.isCheckBoxVisible.observe(this) { isVisible ->
            businessAdapter.updateVisibility(isVisible)
            updateDeleteButtonImage(isVisible) // 체크박스 가시성 상태에 따른 삭제 버튼 이미지 업데이트
        }

        // ViewModel의 에러 메시지를 관찰하여 토스트로 표시
        businessViewModel.error.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // 네트워크 상태 확인 후 데이터 로드
        loadInitialBusinessData()

        // 추가 버튼 클릭 리스너
        val addButton = findViewById<ImageButton>(R.id.addButton)
        addButton.setOnClickListener {
            showAddBusinessDialog()
        }

        // 삭제 버튼 클릭 리스너
        val deleteButton = findViewById<ImageButton>(R.id.deleteButton)
        deleteButton.setOnClickListener {
            if (businessViewModel.isCheckBoxVisible.value == true) {
                handleDeleteBtnClick()
            }
            businessViewModel.toggleCheckBoxVisibility() // ViewModel을 통해 체크박스 가시성 토글
        }

        // 검색어 입력 시 자동 필터링
        searchEditText = findViewById(R.id.searchEditText)
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                businessViewModel.filterBusiness(query) // ViewModel을 통해 필터링
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 뒤로 가기 버튼 설정
        setupBackPressed()
    }

    // 초기 비즈니스 데이터 로드
    private fun loadInitialBusinessData() {
        if (Constants.isNetworkAvailable(this)) {
            businessViewModel.loadBusinessList() // 초기 데이터 로드
        } else {
            Toast.makeText(this, "네트워크 연결이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 아이템 클릭 처리
    private fun handleClick(data: Business) {
        // GIS 화면으로 이동
        val intent = Intent(this, GisActivity::class.java)
        intent.putExtra("data", data)
        startActivity(intent)
    }

    // 추가 다이얼로그 표시
    private fun showAddBusinessDialog() {
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
                businessViewModel.addBusiness(Business(title = title))
                customDialog.dismiss()
            }
        }

        customDialog.show()
    }

    // 삭제 다이얼로그 표시
    private fun handleDeleteBtnClick() {
        if (businessViewModel.isAnyChecked()) { // ViewModel에서 체크된 항목이 있는지 확인
            val deleteDialog = Dialog(this, R.style.CustomDialogTheme)
            val dialogBinding = DeleteDialogBinding.inflate(layoutInflater)

            // 뷰모델을 통해 삭제할 항목의 제목을 가져옴
            val checkedItemTitle = businessViewModel.getCheckedItemTitle()
            dialogBinding.deleteItem.text = checkedItemTitle

            deleteDialog.setContentView(dialogBinding.root)
            dialogResize(this, deleteDialog, 0.9f)

            // 취소 버튼 클릭 리스너
            dialogBinding.dialogCancel.setOnClickListener {
                businessViewModel.clearAllChecks() // ViewModel을 통해 체크 상태 초기화
                deleteDialog.dismiss()
            }

            // 확인 버튼 클릭 리스너
            dialogBinding.dialogConfirm.setOnClickListener {
                val selectedBusinessIds = businessViewModel.getSelectedBusinessIds() // ViewModel에서 선택된 항목의 ID 가져오기
                businessViewModel.deleteSelectedBusinesses(selectedBusinessIds) // ViewModel에 삭제 요청
                searchEditText.text.clear() // 검색 입력 초기화
                deleteDialog.dismiss()
            }

            deleteDialog.show()
        } else {
            Toast.makeText(this, "삭제할 항목을 선택하세요.", Toast.LENGTH_SHORT).show()
        }
    }

    // 체크박스 가시성 상태에 따라 삭제 버튼 이미지 변경
    private fun updateDeleteButtonImage(isVisible: Boolean) {
        val deleteButton = findViewById<ImageButton>(R.id.deleteButton)
        deleteButton.setImageResource(
            if (isVisible) R.drawable.ic_check else R.drawable.ic_trash_fill
        )
    }

    // 다이얼로그 크기 조절
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

    // 뒤로 가기 버튼 설정
    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (System.currentTimeMillis() > backPressedTime + 2000) {
                    backPressedTime = System.currentTimeMillis()
                    Toast.makeText(this@MainActivity, "뒤로가기를 한 번 더 누르면 앱이 종료됩니다", Toast.LENGTH_SHORT).show()
                } else {
                    finishAffinity()
                    System.exit(0)
                }
            }
        })
    }
}
