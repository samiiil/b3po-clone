package models

data class createOrderResponse(val orderId: Long?, val quantity:Long, val type:String, val price:Long,val esopType:String?) {
}
