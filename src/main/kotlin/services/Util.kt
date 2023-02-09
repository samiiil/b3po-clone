package services

import models.DataStorage
import models.Order
import models.OrderExecutionLogs
import repo.OrderRepo
import repo.UserRepo
import java.math.BigInteger
import kotlin.math.min
import kotlin.math.roundToLong


class Util {
    companion object {

        @Synchronized
        fun generateOrderId(): Long {
            return DataStorage.orderId++
        }

        @Synchronized
        fun generateOrderExecutionId(): Long {
            return DataStorage.orderExecutionId++
        }



        fun matchOrders() {
            val buyOrders = OrderRepo.buyList
            if(buyOrders.isEmpty()) return
            val currentBuyOrder = buyOrders.poll()
            matchWithPerformanceSellOrders(currentBuyOrder)
            matchWithNonPerformanceSellOrders(currentBuyOrder)

            if(currentBuyOrder.remainingOrderQuantity > 0) buyOrders.add(currentBuyOrder)
            else matchOrders()
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

                updateSellerInventoryAndWallet(sellOrder, orderQuantity, orderExecutionPrice, isPerformanceESOP)
                updateBuyerInventoryAndWallet(buyOrder, orderQuantity, orderExecutionPrice)

                val orderFee = (orderAmount*DataStorage.COMMISSION_FEE_PERCENTAGE*0.01).roundToLong()
                DataStorage.TOTAL_FEE_COLLECTED = DataStorage.TOTAL_FEE_COLLECTED + BigInteger.valueOf(orderFee)
                val orderExecutionLog =
                    OrderExecutionLogs(generateOrderExecutionId(), orderExecutionPrice, orderQuantity)
                sellOrder.addOrderExecutionLogs(orderExecutionLog)
                buyOrder.addOrderExecutionLogs(orderExecutionLog)
            }
        }

        private fun findOrderQuantity(buyOrder: Order, sellOrder: Order): Long {
            return min(buyOrder.remainingOrderQuantity, sellOrder.remainingOrderQuantity)
        }

        private fun updateSellerInventoryAndWallet(
            sellOrder: Order,
            orderQuantity: Long,
            orderExecutionPrice: Long,
            isPerformanceESOP: Boolean
        ) {
            val seller = UserRepo.userList[sellOrder.userName]!!
            val orderAmount = orderQuantity * orderExecutionPrice
            seller.updateLockedInventory(orderQuantity, isPerformanceESOP)
            seller.addMoneyToWallet((orderAmount * (1 - DataStorage.COMMISSION_FEE_PERCENTAGE * 0.01)).roundToLong())
        }

        private fun updateBuyerInventoryAndWallet(buyOrder: Order, orderQuantity: Long, orderExecutionPrice: Long) {
            val buyer = UserRepo.userList[buyOrder.userName]!!
            val orderAmount = orderQuantity * orderExecutionPrice
            buyer.updateLockedMoney(orderAmount)
            buyer.addEsopToInventory(orderQuantity)

            //Need to send difference back to free wallet when high buy and low sell are paired
            if (buyOrder.orderPrice > orderExecutionPrice) {
                val amountToBeMovedFromLockedWalletToFreeWallet =
                    orderQuantity * (buyOrder.orderPrice - orderExecutionPrice)
                buyer.updateLockedMoney(amountToBeMovedFromLockedWalletToFreeWallet)
                buyer.addMoneyToWallet(amountToBeMovedFromLockedWalletToFreeWallet)
            }
        }
    }
}