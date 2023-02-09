package repo

import models.DataStorage
import models.Order
import services.BuyOrderingComparator
import services.SellOrderingComparator
import java.util.*

object OrderRepo {

    var orderId: Long = 1L
    var orderExecutionId = 1L

    val buyList = PriorityQueue(BuyOrderingComparator)
    val sellList = PriorityQueue(SellOrderingComparator)
    val performanceSellList = LinkedList<Order>()


    fun addBuyOrderToList(order: Order) {
        buyList.add(order)
    }

    fun addSellOrderToList(order: Order) {
        sellList.add(order)
    }

    fun addPerformanceSellOrderToList(order: Order) {
        performanceSellList.add(order)
    }

    fun getBuyOrderToMatch(): Order? {
        if (buyList.isEmpty())
            return null
        return buyList.poll()
    }

    fun getSellOrderToMatch(): Order? {
        if (sellList.isEmpty())
            return null
        return sellList.poll()
    }

    @Synchronized
    fun generateOrderExecutionId(): Long {
        return orderExecutionId++
    }


    @Synchronized
    fun generateOrderId(): Long {
        return orderId++
    }

}
