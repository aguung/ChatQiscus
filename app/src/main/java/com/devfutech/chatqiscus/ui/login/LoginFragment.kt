package com.devfutech.chatqiscus.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.devfutech.chatqiscus.R
import com.devfutech.chatqiscus.databinding.LoginFragmentBinding
import com.devfutech.chatqiscus.ui.base.BaseFragment
import com.devfutech.chatqiscus.utils.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : BaseFragment() {

    private lateinit var binding: LoginFragmentBinding
    private val viewModels by viewModels<LoginViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LoginFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupAction()
        setupObservable()
    }

    private fun setupUI() {
        binding.apply {
            etUserID.clearInput(inputUserID)
            etName.clearInput(inputName)
            etPassword.clearInput(inputPassword)
        }
    }

    private fun setupObservable() {
        viewModels.currentUser.observe(viewLifecycleOwner, { result ->
            when (result) {
                is ResultOf.Success -> {
                    dialogProgress.dismiss()
                    if (result.value != null) findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                }
                is ResultOf.Failure -> {
                    dialogProgress.dismiss()
                    requireContext().toast(result.throwable?.message)
                }
                else -> dialogProgress.show()
            }
        })
        viewModels.login.observe(viewLifecycleOwner, { result ->
            when (result) {
                is ResultOf.Success -> {
                    dialogProgress.dismiss()
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                }
                is ResultOf.Failure -> {
                    dialogProgress.dismiss()
                    requireContext().toast(result.throwable?.message)
                }
                else -> dialogProgress.show()
            }
        })
    }

    private fun setupAction() {
        binding.apply {
            btnLogin.setOnClickListener {
                val validation = arrayOfNulls<Boolean>(3)
                validation[0] = if (binding.inputUserID.inputError(
                        etUserID.text.toString().trim(),
                        resources.getString(
                            R.string.empty_fields,
                            "Email"
                        )
                    )
                ) {
                    if (!etUserID.text.toString().trim().isEmailValid()) {
                        binding.inputUserID.error = resources.getString(
                            R.string.not_valid,
                            "Email"
                        )
                        false
                    } else {
                        true
                    }
                } else {
                    false
                }
                validation[1] = inputPassword.inputError(
                    etPassword.text.toString().trim(), resources.getString(
                        R.string.empty_fields,
                        "Password"
                    )
                )
                validation[2] = inputName.inputError(
                    etName.text.toString().trim(), resources.getString(
                        R.string.empty_fields,
                        "Display name"
                    )
                )

                if (!validation.contains(false)) {
                    viewModels.login(
                        email = etUserID.text.toString().trim(),
                        password = etPassword.text.toString().trim(),
                        name = etName.text.toString().trim(),
                    )
                }
            }
        }
    }
}