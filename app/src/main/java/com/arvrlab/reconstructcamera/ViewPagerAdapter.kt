package com.arvrlab.reconstructcamera

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.arvrlab.reconstructcamera.scenario.ScenarioFragment
import com.arvrlab.reconstructcamera.ui.main.MainFragment


class ViewPagerAdapterAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    override fun getCount() = 2

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> MainFragment()
            1 -> ScenarioFragment()
            else -> MainFragment()
        }
    }
}