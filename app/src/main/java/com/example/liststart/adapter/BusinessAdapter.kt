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
    private var isGisActivity: Boolean,
    private var isVisible: Boolean,
    private val onItemClick: (Business) -> Unit,
    private val onCheckBoxClick: (Business) -> Unit
) : RecyclerView.Adapter<BusinessAdapter.BusinessViewHolder>() {

    private var businessList: List<Business> = listOf()

    class BusinessViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemRoot: View = view
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
        holder.profileImage.setImageResource(R.drawable.profile)
        holder.checkBox.setImageResource(
            if (item.isChecked) R.drawable.ic_checkbox_checked else R.drawable.ic_checkbox
        )

        // 체크박스의 가시성 설정
        holder.checkBox.visibility = if (isVisible) View.VISIBLE else View.GONE

        // 아이템 클릭 처리
        holder.itemRoot.setOnClickListener {
            if (!isVisible) {
                onItemClick(item)
            } else {
                onCheckBoxClick(item)
            }
        }

        // 체크박스 클릭 처리
        holder.checkBox.setOnClickListener {
            if(isGisActivity) {
                item.isChecked = !item.isChecked // 체크 상태 토글
                notifyItemChanged(position) // UI 업데이트
            }
            onCheckBoxClick(item) // 체크박스 클릭 이벤트 처리
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

    // 체크박스 가시성 변경
    fun updateVisibility(visible: Boolean) {
        isVisible = visible
        notifyDataSetChanged()
    }
}
