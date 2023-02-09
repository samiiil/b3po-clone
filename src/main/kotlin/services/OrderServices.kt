package services

import models.*
import repo.OrderRepo
import repo.UserRepo
import validations.OrderValidations
import kotlin.math.min
import kotlin.math.roundToLong

object OrderServices {

        fun placeOrder(user: User, createOrderInput:CreateOrderInput): createOrderResponse {


            val orderQuantity: Long = createOrderInput.quantity!!.toLong()
            val orderType: String = createOrderInput.orderType!!.trim().uppercase()
            val orderPrice: Long = createOrderInput.price!!.toLong()
            val typeOfESOP: String = (createOrderInput.esopType ?: "NON-PERFORMANCE").trim().uppercase()
            var orderId: Long? =null
            var esopType:String?=null

            if (orderType == "BUY") {
                OrderValidations.throwExceptionIfInvalidBuyOrder(user, orderQuantity, orderPrice)
                orderId=placeBuyOrder(user, orderQuantity, orderPrice)
            }
            else if (orderType == "SELL") {
                OrderValidations.throwExceptionIfInvalidSellOrder(user, typeOfESOP, orderQuantity, orderPrice)
                orderId=placeSellOrder(user, orderQuantity, orderPrice, typeOfESOP)
                esopType=typeOfESOP
            }

            matchOrders()

            return createOrderResponse(orderId,orderQuantity,orderType,orderPrice,esopType)
        }


        private fun placeBuyOrder(user: User, orderQuantity: Long, orderPrice: Long): Long {

            val transactionAmount = orderQuantity * orderPrice
            user.moveFreeMoneyToLockedMoney(transactionAmount)

            val newOrder = Order(user.getUserName(), OrderRepo.generateOrderId(), orderQuantity, orderPrice, "BUY")
            user.addOrderToUser(newOrder)

            OrderRepo.addBuyOrderToList(newOrder)

            return newOrder.getOrderId()
        }

        private fun placeSellOrder(user: User, orderQuantity: Long, orderPrice: Long, typeOfESOP: String): Long {

            val newOrder = Order(user.getUserName(), OrderRepo.generateOrderId(), orderQuantity, orderPrice, "SELL")
            user.addOrderToUser(newOrder)
            if (typeOfESOP == "PERFORMANCE") {

                user.moveFreePerformanceInventoryToLockedPerformanceInventory(orderQuantity)
                OrderRepo.addPerformanceSellOrderToList(newOrder)

            } else if (typeOfESOP == "NON-PERFORMANCE") {

                user.moveFreeInventoryToLockedInventory(orderQuantity)
                OrderRepo.addSellOrderToList(newOrder)
            }

            return newOrder.getOrderId()
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
                if (currentPerformanceSellOrder.getOrderPrice() > buyOrder.getOrderPrice()) break
                processOrder(buyOrder, currentPerformanceSellOrder, true)
                if (currentPerformanceSellOrder.remainingOrderQuantity == 0L)
                    performanceSellOrders.remove()
            }
        }

        private fun matchWithNonPerformanceSellOrders(buyOrder: Order) {

            while (buyOrder.remainingOrderQuantity > 0) {

                val currentSellOrder = OrderRepo.getSellOrderToMatch() ?: break
                if (currentSellOrder.getOrderPrice() > buyOrder.getOrderPrice()) break

                processOrder(buyOrder, currentSellOrder, false)

                if (currentSellOrder.remainingOrderQuantity > 0)
                    OrderRepo.addSellOrderToList(currentSellOrder)
            }
        }

        private fun processOrder(buyOrder: Order, sellOrder: Order, isPerformanceESOP: Boolean) {

            val orderExecutionPrice = sellOrder.getOrderPrice()
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