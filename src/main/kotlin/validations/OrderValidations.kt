package validations

import exception.ValidationException
import models.DataStorage
import models.ErrorResponse
import repo.UserRepo
import kotlin.math.ceil

class OrderValidations {

    companion object {
        fun throwExceptionIfInvalidBuyOrder(userName: String, orderQuantity: Long, orderPrice: Long) {
            val user = UserRepo.getUser(userName)!!
            val errorList = ArrayList<String>()
            val transactionAmount = orderQuantity * orderPrice

            if (!user.isInventoryWithInLimit(userName, orderQuantity))
                errorList.add("Inventory threshold will be exceeded")

            if (user.getFreeMoney() < transactionAmount)
                errorList.add("Insufficient balance in wallet")

            if (errorList.isNotEmpty())
                throw ValidationException(ErrorResponse(errorList))
        }

        fun throwExceptionIfInvalidSellOrder(
            userName: String,
            typeOfEsop: String,
            orderQuantity: Long,
            orderPrice: Long
        ) {
            val errorList = ArrayList<String>()
            val user = UserRepo.getUser(userName)!!

            val freeQuantity =
                if (typeOfEsop == "NON-PERFORMANCE") user.getFreeInventory() else user.getFreePerformanceInventory()

            if (freeQuantity < orderQuantity) {
                errorList.add("Insufficient $typeOfEsop ESOPs in inventory")
                throw ValidationException(ErrorResponse(errorList))
            }

            var transactionAmount = orderQuantity * orderPrice
            if (typeOfEsop == "PERFORMANCE") {
                val commisionFee = ceil(DataStorage.COMMISSION_FEE_PERCENTAGE * 0.01).toLong()
                transactionAmount -= commisionFee
            }

            if (!user.isAmountWithInLimit(userName, transactionAmount)) {
                errorList.add("Wallet threshold will be exceeded")
                throw ValidationException(ErrorResponse(errorList))
            }
        }


    }
}