package com.example.recordingsystem

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_page.view.*

class ViewPagerAdapter : RecyclerView.Adapter<PagerVH>() {

    //array of colors to change the background color of screen

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerVH =
        PagerVH(LayoutInflater.from(parent.context).inflate(R.layout.item_page, parent, false))

    //get the size of color array
    override fun getItemCount(): Int = Int.MAX_VALUE

    //binding the screen with view
    override fun onBindViewHolder(holder: PagerVH, position: Int) = holder.itemView.run {
        if(position == 0){

            ivImage.setImageResource(R.drawable.country_1)

        }
        if(position == 1) {

            ivImage.setImageResource(R.drawable.country_2)

        }
        if(position == 2) {

        ivImage.setImageResource(R.drawable.country_3)

    }
        if(position == 3) {

            ivImage.setImageResource(R.drawable.country_1)

        }
    }
}

class PagerVH(itemView: View) : RecyclerView.ViewHolder(itemView)