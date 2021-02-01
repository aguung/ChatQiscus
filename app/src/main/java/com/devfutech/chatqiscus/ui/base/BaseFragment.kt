package com.devfutech.chatqiscus.ui.base

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.devfutech.chatqiscus.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
open class BaseFragment : Fragment() {
    protected lateinit var dialogProgress: AlertDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dialogProgress = MaterialAlertDialogBuilder(requireContext())
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
    }
}