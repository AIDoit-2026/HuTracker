package com.hutracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hutracker.data.RoomGameStore
import com.hutracker.ui.HuTrackerScreen
import com.hutracker.ui.HuTrackerTheme
import com.hutracker.ui.HuTrackerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HuTrackerTheme {
                val viewModel: HuTrackerViewModel = viewModel(
                    factory = HuTrackerViewModelFactory(RoomGameStore(applicationContext)),
                )
                HuTrackerScreen(viewModel = viewModel)
            }
        }
    }
}

private class HuTrackerViewModelFactory(
    private val store: RoomGameStore,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HuTrackerViewModel(store) as T
    }
}
