package ru.arvrlab.ar.measurement.fragments
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.start_fragment.*
import ru.arvrlab.ar.measurement.R
import ru.arvrlab.ar.measurement.core.Constants

class StartFragment : Fragment(R.layout.start_fragment) {
    val viewModel: StartViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnStart.setOnClickListener {
            findNavController().navigate(R.id.collectFragment)
        }
    }

}