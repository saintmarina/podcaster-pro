package com.saintmarina.recordingsystem.UI

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.saintmarina.recordingsystem.DESTINATIONS
import com.saintmarina.recordingsystem.R
import kotlinx.android.synthetic.main.view_pager_items_page.view.*
// service account page
// https://console.cloud.google.com/projectselector2/iam-admin/serviceaccounts?_ga=2.18320249.150055309.1590615406-114274831.1588189614&supportedpurview=project

class PagerVH(itemView: View) : RecyclerView.ViewHolder(itemView)

class ViewPagerAdapter : RecyclerView.Adapter<PagerVH>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerVH =
        PagerVH(
            LayoutInflater.from(parent.context).inflate(
                R.layout.view_pager_items_page,
                parent,
                false
            )
        )

    override fun getItemCount(): Int = DESTINATIONS.size

    //binding the screen with view
    override fun onBindViewHolder(holder: PagerVH, position: Int) = holder.itemView.run {
        ivImage.setImageResource(DESTINATIONS[position].imgPath)
    }
}

