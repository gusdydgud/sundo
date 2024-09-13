package com.example.myapplication

import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.util.Constants
import com.example.myapplication.util.Menu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var todolist: ArrayList<String>
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var todoEidt:EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        enableEdgeToEdge()
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }ㅣ

        if(Constants.isNetworkAvailable(this)) {
            Toast.makeText(this, "인터넷 연결상태 양호", Toast.LENGTH_LONG).show()
            // 레트로핏 호출

            // ViewModel 또는 DataStore 에서 사용을 권장
            callData()

//            val apiService = Constants.createRetrofit()

            // 콜백 받음 enqueue() 로
//            apiService.getMenu().enqueue(object : Callback<List<Menu>> {
//                override fun onResponse(call: Call<List<Menu>>, response: Response<List<Menu>>) {
//                    // 성공했을때 처리
//                    if(response.isSuccessful) {
//                        val list: List<Menu>? = response.body()
//                        Log.d("myNewLog", "$list")
//                    } else {
//                        // 실패했을때
//                    }
//                }
//
//                override fun onFailure(call: Call<List<Menu>>, t: Throwable) {
//                    // 실패했을때 처리
//                }
//
//            })

        } else {
            Toast.makeText(this, "인터넷 연결이 없습니다.", Toast.LENGTH_LONG).show()
        }

        //ArrayList 초기화
        todolist = ArrayList()

        //ArrayAdapter 초기화(context, layout,list)
        adapter = ArrayAdapter(this, R.layout.list_item, todolist)

//        UI객체 생성

        val listView: ListView = findViewById(R.id.list_view)
        val addBtn: Button = findViewById(R.id.add_btn)
        todoEidt = findViewById(R.id.todo_edit)

        //Adapter 적용
        listView.adapter = adapter
        //버튼 이벤트
        addBtn.setOnClickListener {
            addItem()
        }

        //리스트 아이템 클릭 이벤트
        listView.setOnItemClickListener { adapterView, view, i, l ->
            val textView : TextView = view as TextView

            //취소선 넣기
            textView.paintFlags = textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }

    } //onCreate
    private fun addItem(){

        //입력값 변수에 담기
        val  todo : String = todoEidt.text.toString()

        //값이 비워있지 않다면
        if(todo.isNotEmpty()){
            todolist.add(todo)

            //적용
            adapter.notifyDataSetChanged()

            todoEidt.text.clear()
        }else{
            Toast.makeText(this,"할 일을 적어주세요",Toast.LENGTH_SHORT).show()
        }

    }

    private fun callData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val apiService = Constants.createRetrofit()

            val response: Response<List<Menu>> = apiService.getMenu()

            if(response.isSuccessful) { // 성공
                val list: List<Menu>? = response.body()
                Log.d("myNewLog", "$list")

            } else { // 실패

            }
        }

    }
}