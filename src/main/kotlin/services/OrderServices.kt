package services

import models.DataStorage
import models.Order
import models.OrderExecutionLogs
import models.User
import repo.OrderRepo
import repo.UserRepo
import validations.OrderValidations
import java.math.BigInteger
import kotlin.math.min
import kotlin.math.roundToLong

class OrderServices {
    companion object {
        fun placeOrder(
            userName: String,
            orderQuantity: Long,
            orderType: String,
            orderPrice: Long,
            typeOfESOP: String = "NON-PERFORMANCE"
        ): MutableMap<String, Any> {

            val user = UserRepo.getUser(userName)!!

            if (orderType == "BUY") {
                OrderValidations.throwExceptionIfInvalidBuyOrder(userName, orderQuantity, orderPrice)

                placeBuyOrder(user, orderQuantity, orderPrice)

            } else if (orderType == "SELL") {
                OrderValidations.throwExceptionIfInvalidSellOrder(userName, typeOfESOP, orderQuantity, orderPrice)

                placeSellOrder(user, orderQuantity, orderPrice, typeOfESOP)
            }

            matchOrders()

            val res = mutableMapOf<String, Any>()
            res["quantity"] = orderQuantity
            res["order_type"] = orderType
            res["price"] = orderPrice

            return res
        }


        fun placeBuyOrder(user: User, orderQuantity: Long, orderPrice: Long) {

            val transactionAmount = orderQuantity * orderPrice
            user.moveFreeMoneyToLockedMoney(transactionAmount)

            val newOrder = Order(user.username, Util.generateOrderId(), orderQuantity, orderPrice, "BUY")
            user.addOrderToUser(newOrder)

            OrderRepo.addBuyOrderToList(newOrder)
        }

        fun placeSellOrder(user: User, orderQuantity: Long, orderPrice: Long, typeOfESOP: String) {

            val newOrder = Order(user.username, Util.generateOrderId(), orderQuantity, orderPrice, "SELL")
            user.addOrderToUser(newOrder)
            if (typeOfESOP == "PERFORMANCE") {

                user.moveFreePerformanceInventoryToLockedPerformanceInventory(orderQuantity)
                OrderRepo.addPerformanceSellOrderToList(newOrder)

            } else if (typeOfESOP == "NON-PERFORMANCE") {

                user.moveFreeInventoryToLockedInventory(orderQuantity)
                OrderRepo.addSellOrderToList(newOrder)
            }
        }


        fun matchOrders() {
            val buyOrder = OrderRepo.getBuyOrderToMatch() ?: return

            matchWithPerformanceSellOrders(buyOrder)
            matchWithNonPerformanceSellOrders(buyOrder)

            if (buyOrder.getRemainingQtyOfOrder() > 0)
                OrderRepo.addBuyOrderToList(buyOrder)
            else
                matchOrders()
        }

        private fun matchWithPerformanceSellOrders(buyOrder: Order) {
            val performanceSellOrders = OrderRepo.performanceSellList.iterator()
            while (performanceSellOrders.hasNext() && buyOrder.remainingOrderQuantity > 0) {
                val currentPerformanceSellOrder = performanceSellOrders.next()
                processOrder(buyOrder, currentPerformanceSellOrder, true)
                if (currentPerformanceSellOrder.remainingOrderQuantity == 0L)
                    performanceSellOrders.remove()
            }
        }

        private fun matchWithNonPerformanceSellOrders(buyOrder: Order) {
            val sellOrders = OrderRepo.sellList
            while (sellOrders.isNotEmpty() && buyOrder.remainingOrderQuantity > 0) {
                val currentSellOrder = sellOrders.poll()

                //Sell list is sorted to have best deals come first.
                //If the top of the heap is not good enough, no point searching further
                if (currentSellOrder.orderPrice > buyOrder.orderPrice) break
                processOrder(buyOrder, currentSellOrder, false)
                if (currentSellOrder.remainingOrderQuantity > 0)
                    sellOrders.add(currentSellOrder)
            }
        }

        private fun processOrder(buyOrder: Order, sellOrder: Order, isPerformanceESOP: Boolean) {
            if (sellOrder.orderPrice <= buyOrder.orderPrice) {
                val orderExecutionPrice = sellOrder.orderPrice
                val orderQuantity = findOrderQuantity(buyOrder, sellOrder)
                val orderAmount = orderQuantity * orderExecutionPrice

                UserRepo.updateSellerInventoryAndWallet(
                    sellOrder,
                    orderQuantity,
                    orderExecutionPrice,
                    isPerformanceESOP
                )
                UserRepo.updateBuyerInventoryAndWallet(buyOrder, orderQuantity, orderExecutionPrice)

                val orderFee = (orderAmount * DataStorage.COMMISSION_FEE_PERCENTAGE * 0.01).roundToLong()
                DataStorage.TOTAL_FEE_COLLECTED = DataStorage.TOTAL_FEE_COLLECTED + BigInteger.valueOf(orderFee)
                val orderExecutionLog =
                    OrderExecutionLogs(Util.generateOrderExecutionId(), orderExecutionPrice, orderQuantity)
                sellOrder.addOrderExecutionLogs(orderExecutionLog)
                buyOrder.addOrderExecutionLogs(orderExecutionLog)
            }
        }


        private fun findOrderQuantity(buyOrder: Order, sellOrder: Order): Long {
            return min(buyOrder.remainingOrderQuantity, sellOrder.remainingOrderQuantity)
        }


    }
}