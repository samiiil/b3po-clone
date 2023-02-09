package repo

import models.DataStorage
import models.Order
import models.User
import kotlin.math.roundToLong

object UserRepo {
    
    val userList: HashMap<String, User> = HashMap()
    val registeredEmails = mutableSetOf<String>()
    val registeredPhoneNumbers = mutableSetOf<String>()

    fun isUserExists(userName: String): Boolean {
        return (userList.containsKey(userName))

    }

    fun getUser(userName: String): User? {
        return userList[userName]
    }


    fun updateSellerInventoryAndWallet(
        sellOrder: Order,
        orderQuantity: Long,
        orderExecutionPrice: Long,
        isPerformanceESOP: Boolean
    ) {
        val seller = userList[sellOrder.userName]!!
        val orderAmount = orderQuantity * orderExecutionPrice
        seller.updateLockedInventory(orderQuantity, isPerformanceESOP)
        seller.addMoneyToWallet((orderAmount * (1 - DataStorage.COMMISSION_FEE_PERCENTAGE * 0.01)).roundToLong())
    }

    fun updateBuyerInventoryAndWallet(buyOrder: Order, orderQuantity: Long, orderExecutionPrice: Long) {
        val buyer = userList[buyOrder.userName]!!
        val orderAmount = orderQuantity * orderExecutionPrice
        buyer.updateLockedMoney(orderAmount)
        buyer.addEsopToInventory(orderQuantity)

        if (buyOrder.orderPrice > orderExecutionPrice) {
            val amountToBeMovedFromLockedWalletToFreeWallet =
                orderQuantity * (buyOrder.orderPrice - orderExecutionPrice)
            buyer.updateLockedMoney(amountToBeMovedFromLockedWalletToFreeWallet)
            buyer.addMoneyToWallet(amountToBeMovedFromLockedWalletToFreeWallet)
        }
    }


}