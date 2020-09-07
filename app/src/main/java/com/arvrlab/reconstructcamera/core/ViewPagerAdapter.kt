package com.arvrlab.reconstructcamera.core

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.arvrlab.reconstructcamera.fragments.ScenarioFragment
import com.arvrlab.reconstructcamera.fragments.CameraFragment


class ViewPagerAdapterAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment =
        when (position) {
            0 -> CameraFragment()
            1 -> ScenarioFragment()
            else -> CameraFragment()
        }
}