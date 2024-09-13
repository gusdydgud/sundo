package com.example.liststart.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.liststart.R
import com.example.liststart.model.Business

class BusinessAdapter(
    private var isVisible: Boolean, // 체크박스가 보이는지 여부
    private val func: (data: Business) -> Unit // 클릭 이벤트 처리 함수
) : RecyclerView.Adapter<BusinessAdapter.BusinessViewHolder>() {

    private var businessList: List<Business> = listOf()

    class BusinessViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemRoot = view.rootView
        val titleText: TextView = view.findViewById(R.id.titleText)
        val dateText: TextView = view.findViewById(R.id.dateText)
        val profileImage: ImageView = view.findViewById(R.id.profileImage)
        val checkBox: ImageButton = view.findViewById(R.id.checkBoxImageButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusinessViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return BusinessViewHolder(view)
    }

    override fun onBindViewHolder(holder: BusinessViewHolder, position: Int) {
        val item = businessList[position]

        holder.titleText.text = item.title
        holder.dateText.text = item.regdate
        holder.profileImage.setImageResource(R.drawable.profile) // 프로필 이미지 설정
        holder.checkBox.setImageResource(
            if (item.isChecked) R.drawable.ic_checkbox_checked else R.drawable.ic_checkbox
        )

        // 체크박스의 가시성 설정
        holder.checkBox.visibility = if (isVisible) View.VISIBLE else View.GONE

        // 아이템 클릭 처리
        holder.itemRoot.setOnClickListener {
            if (!isVisible) {
                func(item) // 클릭 이벤트 발생 시 ViewModel에 처리 전달
            } else {
                item.isChecked = !item.isChecked
                notifyItemChanged(position) // 체크 상태 변경 시 UI 갱신
            }
        }

        // 체크박스 클릭 처리
        holder.checkBox.setOnClickListener {
            item.isChecked = !item.isChecked
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int {
        return businessList.size
    }

    // 새로운 리스트를 설정하고 UI 갱신
    fun updateList(newList: List<Business>) {
        businessList = newList
        notifyDataSetChanged()
    }

    // 선택된 비즈니스의 ID 목록 반환
    fun getSelectedBusinessIds(): List<Long> {
        return businessList.filter { it.isChecked }.map { it.bno!! }
    }

    // 체크된 항목이 있는지 확인
    fun isAnyChecked(): Boolean {
        return businessList.any { it.isChecked }
    }

    // 체크 상태 초기화
    fun changeIsCheckedToDefault() {
        businessList.forEach { it.isChecked = false }
        notifyDataSetChanged()
    }

    // 필터링된 리스트를 설정
    fun setFilteredList(newList: List<Business>) {
        businessList = newList
        notifyDataSetChanged()
    }

    // 체크박스 가시성 변경
    fun updateVisibility(visible: Boolean) {
        isVisible = visible
        notifyDataSetChanged()
    }

    // 체크된 아이템의 제목 반환 (삭제 다이얼로그에 표시할 제목)
    fun getCheckedItemTitle(): String {
        val checkedItems = businessList.filter { it.isChecked }.map { it.title }
        return if (checkedItems.size > 3) {
            "선택된 사업 ${checkedItems.size}개"
        } else {
            checkedItems.joinToString("\n")
        }
    }
}
