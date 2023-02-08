package services

import models.DataStorage
import models.Order
import models.User
import repo.UserRepo
import services.Util
import validations.OrderValidations.Companion.throwExceptionIfInvalidBuyOrder

class OrderServices {
    companion object{
        fun placeOrder(userName:String,orderQuantity:Long, orderType:String, orderPrice:Long, typeOfESOP:String="NON-PERFORMANCE"): MutableMap<String, Any> {
            val user=UserRepo.getUser(userName)!!

            if (orderType == "BUY") {

                throwExceptionIfInvalidBuyOrder(userName,orderQuantity, orderPrice)

                addBuyOrder(user,orderQuantity, orderPrice)
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



        fun addBuyOrder(user: User,orderQuantity: Long, orderPrice: Long){

            val transactionAmount = orderQuantity * orderPrice
            user.moveFreeMoneyToLockedMoney(transactionAmount)
            val newOrder = Order(user.username, Util.generateOrderId(), orderQuantity, orderPrice, "BUY")
            user.orders.add(newOrder)
            Util.addOrderToBuyList(newOrder)
        }

    }
}