package services

import models.*
import repo.OrderRepo
import repo.UserRepo
import validations.OrderValidations
import kotlin.math.min
import kotlin.math.roundToLong

class OrderServices {
    companion object {
        fun placeOrder(user: User, orderQuantity: Long, orderType: String, orderPrice: Long, typeOfESOP: String = "NON-PERFORMANCE"): createOrderResponse {

            if (orderType == "BUY") {
                OrderValidations.throwExceptionIfInvalidBuyOrder(user, orderQuantity, orderPrice)
                placeBuyOrder(user, orderQuantity, orderPrice)
            }
            else if (orderType == "SELL") {
                OrderValidations.throwExceptionIfInvalidSellOrder(user, typeOfESOP, orderQuantity, orderPrice)
                placeSellOrder(user, orderQuantity, orderPrice, typeOfESOP)
            }

            matchOrders()

            return createOrderResponse(orderQuantity,orderType,orderPrice,typeOfESOP)
        }


        private fun placeBuyOrder(user: User, orderQuantity: Long, orderPrice: Long) {

            val transactionAmount = orderQuantity * orderPrice
            user.moveFreeMoneyToLockedMoney(transactionAmount)

            val newOrder = Order(user.username, OrderRepo.generateOrderId(), orderQuantity, orderPrice, "BUY")
            user.addOrderToUser(newOrder)

            OrderRepo.addBuyOrderToList(newOrder)
        }

        private fun placeSellOrder(user: User, orderQuantity: Long, orderPrice: Long, typeOfESOP: String) {

            val newOrder = Order(user.getUserName(user), OrderRepo.generateOrderId(), orderQuantity, orderPrice, "SELL")
            user.addOrderToUser(newOrder)
            if (typeOfESOP == "PERFORMANCE") {

                user.moveFreePerformanceInventoryToLockedPerformanceInventory(orderQuantity)
                OrderRepo.addPerformanceSellOrderToList(newOrder)

            } else if (typeOfESOP == "NON-PERFORMANCE") {

                user.moveFreeInventoryToLockedInventory(orderQuantity)
                OrderRepo.addSellOrderToList(newOrder)
            }
        }


        private fun matchOrders() {
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
                if (currentPerformanceSellOrder.orderPrice > buyOrder.orderPrice) break
                processOrder(buyOrder, currentPerformanceSellOrder, true)
                if (currentPerformanceSellOrder.remainingOrderQuantity == 0L)
                    performanceSellOrders.remove()
            }
        }

        private fun matchWithNonPerformanceSellOrders(buyOrder: Order) {

            while (buyOrder.remainingOrderQuantity > 0) {

                val currentSellOrder = OrderRepo.getSellOrderToMatch() ?: break
                if (currentSellOrder.orderPrice > buyOrder.orderPrice) break

                processOrder(buyOrder, currentSellOrder, false)

                if (currentSellOrder.remainingOrderQuantity > 0)
                    OrderRepo.addSellOrderToList(currentSellOrder)
            }
        }

        private fun processOrder(buyOrder: Order, sellOrder: Order, isPerformanceESOP: Boolean) {

            val orderExecutionPrice = sellOrder.orderPrice
            val orderQuantity = findOrderQuantity(buyOrder, sellOrder)
            val orderAmount = orderQuantity * orderExecutionPrice

            UserRepo.updateSellerInventoryAndWallet(sellOrder, orderQuantity, orderExecutionPrice, isPerformanceESOP)
            UserRepo.updateBuyerInventoryAndWallet(buyOrder, orderQuantity, orderExecutionPrice)

            val orderFee = (orderAmount * DataStorage.COMMISSION_FEE_PERCENTAGE * 0.01).roundToLong()

            DataStorage.addTransactionFee(orderFee.toBigInteger())

            val orderExecutionLog = OrderExecutionLogs(OrderRepo.generateOrderExecutionId(), orderExecutionPrice, orderQuantity)
            sellOrder.addOrderExecutionLogs(orderExecutionLog)
            buyOrder.addOrderExecutionLogs(orderExecutionLog)
        }




        private fun findOrderQuantity(buyOrder: Order, sellOrder: Order): Long {
            return min(buyOrder.remainingOrderQuantity, sellOrder.remainingOrderQuantity)
        }
    }
}