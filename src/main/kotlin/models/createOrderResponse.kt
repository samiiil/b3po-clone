package models

data class createOrderResponse(val quantity: Long, val orderType: String, val price: Long, val esopType: String) {
}