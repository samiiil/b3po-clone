package services

import models.Order
import models.User
import repo.UserRepo


object UserServices {

    fun getOrderDetails(user: User): Map<String, ArrayList<Map<String, Any>>> {

        var orders = user.getAllOrdersOfUser()

        if (orders.size == 0)
            return mapOf("order_history" to ArrayList())

        val orderDetails = ArrayList<Map<String, Any>>()
        orders.forEach { order ->
            val currentOrderDetails = mutableMapOf<String, Any>()
            currentOrderDetails["order_id"] = order.getOrderId()
            currentOrderDetails["quantity"] = order.orderQuantity
            currentOrderDetails["type"] = order.orderType
            currentOrderDetails["price"] = order.getOrderPrice()

            when (order.orderStatus) {

                "Unfilled" -> {
                    currentOrderDetails["unfilled"] = getOrderHistoryOfUnfilledOrder(order)
                }

                "Partially Filled" -> {
                    currentOrderDetails["partially_filled"] = getAllCompletedTransactionsOfOrder(order)
                    currentOrderDetails["unfilled"] = getOrderHistoryOfUnfilledOrder(order)
                }

                "Filled" -> {
                    currentOrderDetails["filled"] = getAllCompletedTransactionsOfOrder(order)
                }
            }
            orderDetails.add(currentOrderDetails)
        }
        return mapOf("order_history" to orderDetails)
    }

    private fun getAllCompletedTransactionsOfOrder(order: Order): ArrayList<Map<String, Any>> {
        val executionLogs = ArrayList<Map<String, Any>>()
        val prices = mutableMapOf<Long, MutableMap<String, Long>>()

        order.orderExecutionLogs.forEach {
            val currentTransactionExecutionLogs = mutableMapOf<String, Long>()
            if (prices.containsKey(it.orderExecutionPrice)) {
                prices[it.orderExecutionPrice]!!["quantity"] =
                    prices[it.orderExecutionPrice]!!["quantity"]!! + it.orderExecutionQuantity
            } else {
                currentTransactionExecutionLogs["price"] = it.orderExecutionPrice
                currentTransactionExecutionLogs["quantity"] = it.orderExecutionQuantity
                prices[it.orderExecutionPrice] = currentTransactionExecutionLogs
                executionLogs.add(currentTransactionExecutionLogs)
            }
        }
        return executionLogs
    }


    private fun getOrderHistoryOfUnfilledOrder(order: Order): ArrayList<Map<String, Any>> {
        val executionLogs = ArrayList<Map<String, Any>>()
        val pendingTransactionLog = mutableMapOf<String, Any>()
        pendingTransactionLog["price"] = order.getOrderPrice()
        pendingTransactionLog["quantity"] = order.remainingOrderQuantity
        executionLogs.add(pendingTransactionLog)
        return executionLogs
    }


}




