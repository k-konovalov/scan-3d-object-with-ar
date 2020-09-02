package com.arvrlab.reconstructcamera

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.arvrlab.reconstructcamera.scenario.ScenarioFragment
import com.arvrlab.reconstructcamera.ui.main.MainFragment


class ViewPagerAdapterAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment =
        when (position) {
            0 -> MainFragment()
            1 -> ScenarioFragment()
            else -> MainFragment()
        }
}