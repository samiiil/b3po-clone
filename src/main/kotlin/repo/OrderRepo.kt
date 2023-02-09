package repo

import models.Order
import services.BuyOrderingComparator
import services.SellOrderingComparator
import java.util.*

object OrderRepo {
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

}
