package com.rentcollection.ui.screens.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rentcollection.ui.screens.buildings.BuildingsScreen
import com.rentcollection.ui.screens.dashboard.DashboardScreen
import com.rentcollection.ui.screens.history.PaymentHistoryScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(navController: NavController, viewModel: MainViewModel = hiltViewModel()) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val pendingCount by viewModel.pendingCount.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = {
                        BadgedBox(badge = {
                            if (pendingCount > 0) Badge { Text("$pendingCount") }
                        }) {
                            Icon(Icons.Default.Home, contentDescription = "Dashboard")
                        }
                    },
                    label = { Text("Dashboard") },
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Business, contentDescription = "Buildings") },
                    label = { Text("Buildings") },
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History") },
                    selected = pagerState.currentPage == 2,
                    onClick = { scope.launch { pagerState.animateScrollToPage(2) } }
                )
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            beyondBoundsPageCount = 2,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            when (page) {
                0 -> DashboardScreen(navController)
                1 -> BuildingsScreen(navController)
                else -> PaymentHistoryScreen(navController)
            }
        }
    }
}
