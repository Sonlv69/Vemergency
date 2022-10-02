package com.kiluss.vemergency.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.kiluss.vemergency.constant.EXTRA_USER_PROFILE
import com.kiluss.vemergency.constant.LOGIN_FRAGMENT_EXTRA
import com.kiluss.vemergency.data.firebase.FirebaseManager
import com.kiluss.vemergency.databinding.FragmentHomeBinding
import com.kiluss.vemergency.ui.login.LoginActivity
import com.kiluss.vemergency.ui.navigation.NavigationActivity

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // view model ktx
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseManager.init()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpView()
        observeViewModel()
        viewModel.getUserInfo()
    }

    private fun setUpView() {
        binding.btnFind.setOnClickListener {
            startActivity(Intent(activity, NavigationActivity::class.java))
        }
        binding.ivAccount.setOnClickListener {
            startActivity(Intent(activity, LoginActivity::class.java).apply {
                putExtra(
                    LOGIN_FRAGMENT_EXTRA,
                    EXTRA_USER_PROFILE
                )
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeViewModel() {
        with(viewModel) {
            avatarBitmap.observe(viewLifecycleOwner) {
                it?.let {
                    Glide.with(this@HomeFragment)
                        .load(it)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(binding.ivAccount)
                }
            }
        }
    }
}
