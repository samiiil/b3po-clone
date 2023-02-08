package services

import models.DataStorage
import services.Util

class OrderServices {
    companion object{
        fun placeOrder(userName:String,orderQuantity:Long, orderType:String, orderPrice:Long, typeOfESOP:String="NON-PERFORMANCE"): MutableMap<String, Any> {
            val user=DataStorage.getUser(userName)

            if (orderType == "BUY") {
                user!!.addBuyOrder(orderQuantity, orderPrice)
            } else if (orderType == "SELL") {
                user!!.addSellOrder(orderQuantity, orderPrice, typeOfESOP)
            }
            Util.matchOrders()

            val res = mutableMapOf<String, Any>()
            res["quantity"] = orderQuantity
            res["order_type"] = orderType
            res["price"] = orderPrice

            return res
        }
    }
}