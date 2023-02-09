package repo

import models.DataStorage
import models.Order
import services.BuyOrderingComparator
import services.SellOrderingComparator
import java.util.*

class OrderRepo {
    companion object{
        val buyList = PriorityQueue<Order>(BuyOrderingComparator)
        val sellList = PriorityQueue<Order>(SellOrderingComparator)
        val performanceSellList = LinkedList<Order>()

        fun addBuyOrderToList(order:Order)
        {
            buyList.add(order)
        }

        fun addSellOrderToList(order: Order){
            sellList.add(order)
        }

        fun addPerformanceSellOrderToList(order: Order){
            performanceSellList.add(order)
        }




    }
}