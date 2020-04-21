package com.example.recordingsystem

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.view_pager_items_page.view.*

class ViewPagerAdapter : RecyclerView.Adapter<PagerVH>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerVH =
        PagerVH(LayoutInflater.from(parent.context).inflate(R.layout.view_pager_items_page, parent, false))

    override fun getItemCount(): Int = 3

    //binding the screen with view
    override fun onBindViewHolder(holder: PagerVH, position: Int) = holder.itemView.run {
        when(position) {
            0 -> ivImage.setImageResource(R.drawable.country_1)
            1 -> ivImage.setImageResource(R.drawable.country_2)
            2-> ivImage.setImageResource(R.drawable.country_3)
        }
    }
}

class PagerVH(itemView: View) : RecyclerView.ViewHolder(itemView)