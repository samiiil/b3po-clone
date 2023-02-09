package services

import models.DataStorage
import models.Order
import models.User
import repo.OrderRepo
import repo.UserRepo
import services.Util
import validations.OrderValidations
class OrderServices {
    companion object{
        fun placeOrder(userName:String,orderQuantity:Long, orderType:String, orderPrice:Long, typeOfESOP:String="NON-PERFORMANCE"): MutableMap<String, Any> {

            val user=UserRepo.getUser(userName)!!

            if (orderType == "BUY") {
                OrderValidations.throwExceptionIfInvalidBuyOrder(userName,orderQuantity, orderPrice)

                addBuyOrder(user,orderQuantity, orderPrice)

            } else if (orderType == "SELL") {
                OrderValidations.throwExceptionIfInvalidSellOrder(userName,typeOfESOP,orderQuantity,orderPrice)

                addSellOrder(user,orderQuantity, orderPrice, typeOfESOP)
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
            OrderRepo.addBuyOrderToList(newOrder)
        }

        fun addSellOrder(user:User,orderQuantity: Long, orderPrice: Long, typeOfESOP: String){

            val newOrder = Order(user.username, Util.generateOrderId(), orderQuantity, orderPrice, "SELL")
            user.orders.add(newOrder)

            if(typeOfESOP == "PERFORMANCE") {

                user.moveFreePerformanceInventoryToLockedPerformanceInventory(orderQuantity)
               OrderRepo.addSellOrderToList(newOrder)
            }
            else if(typeOfESOP == "NON-PERFORMANCE") {
                user.moveFreeInventoryToLockedInventory(orderQuantity)
                OrderRepo.addPerformanceSellOrderToList(newOrder)
            }
        }




    }
}