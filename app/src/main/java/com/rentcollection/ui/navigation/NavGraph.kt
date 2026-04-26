package com.rentcollection.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.*
import androidx.navigation.compose.*
import com.rentcollection.ui.screens.addedit.*
import com.rentcollection.ui.screens.auth.LoginScreen
import com.rentcollection.ui.screens.buildings.BuildingDetailScreen
import com.rentcollection.ui.screens.house.HouseDetailScreen
import com.rentcollection.ui.screens.main.MainScreen
import com.rentcollection.ui.screens.splash.SplashScreen

@Composable
fun RentNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }
        composable(Screen.Main.route) {
            MainScreen(navController)
        }
        composable(
            route = Screen.BuildingDetail.route,
            arguments = listOf(navArgument("buildingId") { type = NavType.StringType })
        ) {
            BuildingDetailScreen(navController, it.arguments?.getString("buildingId") ?: "")
        }
        composable(
            route = Screen.HouseDetail.route,
            arguments = listOf(
                navArgument("buildingId") { type = NavType.StringType },
                navArgument("floorId") { type = NavType.StringType },
                navArgument("houseId") { type = NavType.StringType }
            )
        ) {
            HouseDetailScreen(
                navController,
                it.arguments?.getString("buildingId") ?: "",
                it.arguments?.getString("floorId") ?: "",
                it.arguments?.getString("houseId") ?: ""
            )
        }
        composable(
            route = Screen.AddEditBuilding.route,
            arguments = listOf(navArgument("buildingId") {
                type = NavType.StringType; nullable = true; defaultValue = null
            })
        ) {
            AddEditBuildingScreen(navController, it.arguments?.getString("buildingId"))
        }
        composable(
            route = Screen.AddEditFloor.route,
            arguments = listOf(
                navArgument("buildingId") { type = NavType.StringType },
                navArgument("floorId") {
                    type = NavType.StringType; nullable = true; defaultValue = null
                }
            )
        ) {
            AddEditFloorScreen(
                navController,
                it.arguments?.getString("buildingId") ?: "",
                it.arguments?.getString("floorId")
            )
        }
        composable(
            route = Screen.AddEditHouse.route,
            arguments = listOf(
                navArgument("buildingId") { type = NavType.StringType },
                navArgument("floorId") { type = NavType.StringType },
                navArgument("houseId") {
                    type = NavType.StringType; nullable = true; defaultValue = null
                }
            )
        ) {
            AddEditHouseScreen(
                navController,
                it.arguments?.getString("buildingId") ?: "",
                it.arguments?.getString("floorId") ?: "",
                it.arguments?.getString("houseId")
            )
        }
        composable(
            route = Screen.AddEditTenant.route,
            arguments = listOf(
                navArgument("houseId") { type = NavType.StringType },
                navArgument("buildingId") { type = NavType.StringType },
                navArgument("tenantId") {
                    type = NavType.StringType; nullable = true; defaultValue = null
                }
            )
        ) {
            AddEditTenantScreen(
                navController,
                it.arguments?.getString("houseId") ?: "",
                it.arguments?.getString("buildingId") ?: "",
                it.arguments?.getString("tenantId")
            )
        }
        composable(
            route = Screen.RecordPayment.route,
            arguments = listOf(
                navArgument("houseId") { type = NavType.StringType },
                navArgument("buildingId") { type = NavType.StringType },
                navArgument("floorId") { type = NavType.StringType }
            )
        ) {
            RecordPaymentScreen(
                navController,
                it.arguments?.getString("houseId") ?: "",
                it.arguments?.getString("buildingId") ?: "",
                it.arguments?.getString("floorId") ?: ""
            )
        }
    }
}
