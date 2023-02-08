package validations

import exception.ValidationException
import models.ErrorResponse
import models.User
import repo.UserRepo

class OrderValidations {

    companion object {
        fun throwExceptionIfInvalidBuyOrder(userName:String, orderQuantity: Long, orderPrice: Long) {
            val user=UserRepo.getUser(userName)!!
            val errorList = ArrayList<String>()
            val transactionAmount = orderQuantity * orderPrice

            if (!user.isInventoryWithInLimit(userName ,orderQuantity))
                errorList.add("Inventory threshold will be exceeded")

            if (user.getFreeMoney() < transactionAmount)
                errorList.add("Insufficient balance in wallet")

            if (errorList.isNotEmpty())
                throw ValidationException(ErrorResponse(errorList))
        }
    }
}