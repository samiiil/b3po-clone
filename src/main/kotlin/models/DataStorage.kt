package models

import services.BuyOrderingComparator
import services.SellOrderingComparator
import java.math.BigInteger
import java.util.*

class DataStorage {
    companion object {

        var orderId: Long = 1L
        var orderExecutionId = 1L


        const val COMMISSION_FEE_PERCENTAGE = 2.0F
        const val MAX_AMOUNT = 10_000_000
        const val MAX_QUANTITY = 10_000_000
        var TOTAL_FEE_COLLECTED: BigInteger = BigInteger.valueOf(0)


    }
}