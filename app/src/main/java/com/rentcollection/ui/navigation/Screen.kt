package com.rentcollection.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Main : Screen("main")
    object Dashboard : Screen("dashboard")
    object Buildings : Screen("buildings")
    object PaymentHistory : Screen("payment_history")

    object BuildingDetail : Screen("building_detail/{buildingId}") {
        fun createRoute(buildingId: String) = "building_detail/$buildingId"
    }

    object HouseDetail : Screen("house_detail/{buildingId}/{floorId}/{houseId}") {
        fun createRoute(buildingId: String, floorId: String, houseId: String) =
            "house_detail/$buildingId/$floorId/$houseId"
    }

    object AddEditBuilding : Screen("add_edit_building?buildingId={buildingId}") {
        fun createRoute(buildingId: String? = null) =
            "add_edit_building" + if (buildingId != null) "?buildingId=$buildingId" else ""
    }

    object AddEditFloor : Screen("add_edit_floor/{buildingId}?floorId={floorId}") {
        fun createRoute(buildingId: String, floorId: String? = null) =
            "add_edit_floor/$buildingId" + if (floorId != null) "?floorId=$floorId" else ""
    }

    object AddEditHouse : Screen("add_edit_house/{buildingId}/{floorId}?houseId={houseId}") {
        fun createRoute(buildingId: String, floorId: String, houseId: String? = null) =
            "add_edit_house/$buildingId/$floorId" + if (houseId != null) "?houseId=$houseId" else ""
    }

    object AddEditTenant : Screen("add_edit_tenant/{houseId}/{buildingId}?tenantId={tenantId}") {
        fun createRoute(houseId: String, buildingId: String, tenantId: String? = null) =
            "add_edit_tenant/$houseId/$buildingId" + if (tenantId != null) "?tenantId=$tenantId" else ""
    }

    object RecordPayment : Screen("record_payment/{houseId}/{buildingId}/{floorId}") {
        fun createRoute(houseId: String, buildingId: String, floorId: String) =
            "record_payment/$houseId/$buildingId/$floorId"
    }
}
