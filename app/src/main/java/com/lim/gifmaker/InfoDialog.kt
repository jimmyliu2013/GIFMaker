package com.lim.gifmaker

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle

class InfoDialog(context: Context) : AlertDialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.info_layout)
    }
}