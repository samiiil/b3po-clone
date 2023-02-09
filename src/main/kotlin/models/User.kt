package models

import repo.UserRepo

class User(
    val username: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val emailId: String
) {
    private val account: Account = Account()
    val orders: ArrayList<Order> = ArrayList()

    fun getUserName(user: User): String {
        return user.username
    }

    fun getOrderDetails(): Map<String, ArrayList<Map<String, Any>>> {
        if (orders.size == 0) {
            return mapOf("order_history" to ArrayList())
        }

        val orderDetails = ArrayList<Map<String, Any>>()
        orders.forEach { order ->
            val currentOrderDetails = mutableMapOf<String, Any>()
            currentOrderDetails["order_id"] = order.orderId
            currentOrderDetails["quantity"] = order.orderQuantity
            currentOrderDetails["type"] = order.orderType
            currentOrderDetails["price"] = order.orderPrice

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
        val partiallyFilledOrderExecutionLogs = ArrayList<Map<String, Any>>()
        val prices = mutableMapOf<Long, MutableMap<String, Long>>()

        order.orderExecutionLogs.forEach {
            val currentOrderExecutionLogs = mutableMapOf<String, Long>()
            if (prices.containsKey(it.orderExecutionPrice)) {
                prices[it.orderExecutionPrice]!!["quantity"] =
                    prices[it.orderExecutionPrice]!!["quantity"]!! + it.orderExecutionQuantity
            } else {
                currentOrderExecutionLogs["price"] = it.orderExecutionPrice
                currentOrderExecutionLogs["quantity"] = it.orderExecutionQuantity
                prices[it.orderExecutionPrice] = currentOrderExecutionLogs
                partiallyFilledOrderExecutionLogs.add(currentOrderExecutionLogs)
            }
        }
        return partiallyFilledOrderExecutionLogs
    }


    private fun getOrderHistoryOfUnfilledOrder(order: Order): ArrayList<Map<String, Any>> {
        val unfilledOrderExecutionLogs = ArrayList<Map<String, Any>>()
        val currentOrderExecutionLogs = mutableMapOf<String, Any>()
        currentOrderExecutionLogs["price"] = order.orderPrice
        currentOrderExecutionLogs["quantity"] = order.remainingOrderQuantity
        unfilledOrderExecutionLogs.add(currentOrderExecutionLogs)
        return unfilledOrderExecutionLogs
    }

    fun addMoneyToWallet(amountToBeAdded: Long) {
        this.account.wallet.freeMoney = this.account.wallet.freeMoney + amountToBeAdded
    }

    fun getFreeMoney(): Long {
        return this.account.wallet.freeMoney
    }

    fun getLockedMoney(): Long {
        return this.account.wallet.lockedMoney
    }

    fun updateLockedMoney(amountToBeUpdated: Long) {
        this.account.wallet.lockedMoney = this.account.wallet.lockedMoney - amountToBeUpdated
    }

    fun moveFreeMoneyToLockedMoney(amountToBeLocked: Long) {
        this.account.wallet.freeMoney = this.account.wallet.freeMoney - amountToBeLocked
        this.account.wallet.lockedMoney = this.account.wallet.lockedMoney + amountToBeLocked
    }


    fun addEsopToInventory(esopsToBeAdded: Long, type: String = "NON-PERFORMANCE") {
        if (type == "PERFORMANCE") {
            this.account.inventory.freePerformanceInventory =
                this.account.inventory.freePerformanceInventory + esopsToBeAdded
        } else {
            this.account.inventory.freeInventory = this.account.inventory.freeInventory + esopsToBeAdded
        }

    }

    fun getFreeInventory(): Long {
        return this.account.inventory.freeInventory
    }

    fun getLockedInventory(): Long {
        return this.account.inventory.lockedInventory
    }

    fun getFreePerformanceInventory(): Long {
        return this.account.inventory.freePerformanceInventory
    }

    fun getLockedPerformanceInventory(): Long {
        return this.account.inventory.lockedPerformanceInventory
    }


    fun updateLockedInventory(inventoryToBeUpdated: Long, isPerformanceESOP: Boolean) {
        if (isPerformanceESOP)
            this.account.inventory.lockedPerformanceInventory =
                this.account.inventory.lockedPerformanceInventory - inventoryToBeUpdated
        else
            this.account.inventory.lockedInventory = this.account.inventory.lockedInventory - inventoryToBeUpdated
    }

    fun moveFreeInventoryToLockedInventory(esopsToBeLocked: Long): String {
        if (this.account.inventory.freeInventory < esopsToBeLocked) {
            return "Insufficient ESOPs in Inventory"
        }
        this.account.inventory.freeInventory = this.account.inventory.freeInventory - esopsToBeLocked
        this.account.inventory.lockedInventory = this.account.inventory.lockedInventory + esopsToBeLocked
        return "Success"
    }

    fun moveFreePerformanceInventoryToLockedPerformanceInventory(esopsToBeLocked: Long): String {
        if (this.account.inventory.freePerformanceInventory < esopsToBeLocked) {
            return "Insufficient ESOPs in Inventory"
        }
        this.account.inventory.freePerformanceInventory =
            this.account.inventory.freePerformanceInventory - esopsToBeLocked
        this.account.inventory.lockedPerformanceInventory =
            this.account.inventory.lockedPerformanceInventory + esopsToBeLocked
        return "Success"
    }

    fun isInventoryWithInLimit(user: User, orderQuantity: Long): Boolean {
        return (user.getFreeInventory() + user.getLockedInventory() + orderQuantity <= DataStorage.MAX_QUANTITY)
    }

    fun isAmountWithInLimit(orderAmount: Long): Boolean {
        return (getFreeMoney() + getLockedMoney() + orderAmount <= DataStorage.MAX_AMOUNT)

    }


    fun getFreeMoney(userName: String): Long {
        return UserRepo.userList[userName]!!.getFreeMoney()

    }

    fun addOrderToUser(order: Order) {
        orders.add(order)
    }


}