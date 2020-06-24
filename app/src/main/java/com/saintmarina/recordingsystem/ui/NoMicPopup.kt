package com.saintmarina.recordingsystem.ui

import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import android.widget.RelativeLayout
import com.saintmarina.recordingsystem.R
import kotlinx.android.synthetic.main.pop_up_mic_out.view.*

class NoMicPopup(private val rootView: View) {
    private val view: View

    init {
        val inflater = LayoutInflater.from(rootView.context)
        view = inflater.inflate(R.layout.pop_up_mic_out, null)
    }

    private var popupWindow: PopupWindow? = null

    var isMicPresent: Boolean = true
    set(value) {
        field = value;
        invalidate();
    }

    private fun invalidate() {
        updateView()

        if (popupWindow == null && !isMicPresent)
            createPopup()
    }

    private fun updateView() {
        if (isMicPresent) {
            view.message.text = "The microphone was disconnected due to a faulty cable or power outage.\nIt is now reconnected"
            view.btnDismiss.visibility = View.VISIBLE
            view.background = ColorDrawable(0xff69A75E.toInt())
        } else {
            view.message.text = "The microphone seems to be disconnected.\nThis can be due to a faulty cable or power outage.\nCheck cable connections"
            view.btnDismiss.visibility = View.INVISIBLE
            view.background = ColorDrawable(0xffff7f7f.toInt())
        }
    }

    private fun createPopup() {
        popupWindow = PopupWindow(view).apply {
            width = RelativeLayout.LayoutParams.MATCH_PARENT
            height = RelativeLayout.LayoutParams.MATCH_PARENT
            isFocusable = true

            setOnDismissListener { popupWindow = null }
            showAtLocation(rootView, Gravity.CENTER, 0, 0)

            view.btnDismiss.setOnClickListener { dismiss() }
        }
    }
}