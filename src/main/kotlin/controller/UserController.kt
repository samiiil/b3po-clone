package controller

import exception.UserNotFoundException
import exception.ValidationException
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import models.*
import repo.UserRepo
import services.UserServices
import services.Validations


@Controller("/user")
class UserController {
    @Post("/register")
    fun register(@Body body: RegisterInput): HttpResponse<RegisterResponse> {
        val errorList = arrayListOf<String>()

        val firstName: String? = body.firstName?.trim()
        val lastName: String? = body.lastName?.trim()
        val phoneNumber: String? = body.phoneNumber?.trim()
        val emailID: String? = body.emailID?.trim()
        val userName: String? = body.userName?.trim()

        for (error in Validations.validateFirstName(firstName)) errorList.add(error)
        for (error in Validations.validateLastName(lastName)) errorList.add(error)
        for (error in Validations.validatePhoneNumber(phoneNumber)) errorList.add(error)
        for (error in Validations.validateEmailIds(emailID)) errorList.add(error)
        for (error in Validations.validateUserName(userName)) errorList.add(error)

        if (errorList.isEmpty()) {
            if (userName != null && firstName != null && lastName != null && phoneNumber != null && emailID != null) {
                val user = User(userName, firstName, lastName, phoneNumber, emailID)
                UserRepo.saveUser(user)
            }
        }
        if (errorList.isNotEmpty()) {
            val errorResponse = ErrorResponse(errorList)
            throw ValidationException(errorResponse)
        }
        val res = RegisterResponse(
            firstName = firstName,
            lastName = lastName,
            phoneNumber = phoneNumber,
            emailID = emailID,
            userName = userName
        )
        return HttpResponse.status<Any>(HttpStatus.OK).body(res)
    }

    @Post("/{userName}/addToWallet")
    fun addToWallet(userName: String, @Body body: AddToWalletInput): HttpResponse<*> {
        val errorMessages: ArrayList<String> = ArrayList()

        val response: Map<String, *>
        if (!Validations.validateUser(userName)) {
            errorMessages.add("UserName does not exist.")
        }

        val amountToBeAdded: Long? = body.amount?.toLong()

        if (amountToBeAdded == null) {
            errorMessages.add("Amount field is missing.")
        }

        if (amountToBeAdded!! <= 0) {
            errorMessages.add("Amount added to wallet has to be positive.")
        }

        if (errorMessages.isNotEmpty()) {
            response = mapOf("error" to errorMessages)
            if (errorMessages[0] == "UserName does not exist.")
                throw UserNotFoundException(ErrorResponse("User does not exist"))
            return HttpResponse.badRequest(response)
        }

        val freeMoney = UserRepo.userList[userName]!!.getFreeMoney()
        val lockedMoney = UserRepo.userList[userName]!!.getLockedMoney()

        if (((amountToBeAdded + freeMoney + lockedMoney) <= 0) ||
            ((amountToBeAdded + freeMoney + lockedMoney) > DataStorage.MAX_AMOUNT)
        ) {
            errorMessages.add("Amount exceeds maximum wallet limit. Wallet range 0 to ${DataStorage.MAX_AMOUNT}")
        }
        if (errorMessages.isNotEmpty()) {
            response = mapOf("error" to errorMessages)
            return HttpResponse.status<Any>(HttpStatus.BAD_REQUEST).body(response)
        }
        UserRepo.userList[userName]!!.addMoneyToWallet(amountToBeAdded)

        response = mapOf("message" to "$amountToBeAdded amount added to account")
        return HttpResponse.status<Any>(HttpStatus.OK).body(response)
    }

    @Post("/{userName}/addToInventory")
    fun addToInventory(userName: String, @Body body: AddToInventoryInput): HttpResponse<*> {

        //Input Parsing
        val quantityToBeAdded = body.quantity?.toLong()
        val typeOfESOP = body.esop_type?.uppercase() ?: "NON-PERFORMANCE"
        val errorMessages: ArrayList<String> = ArrayList()
        val response: Map<String, *>

        if (typeOfESOP !in arrayOf("PERFORMANCE", "NON-PERFORMANCE"))
            errorMessages.add("Invalid ESOP type")

        if (!Validations.validateUser(userName))
            errorMessages.add("userName does not exists.")
        if (quantityToBeAdded == null) {
            errorMessages.add("Quantity field is missing")

        }
        if (errorMessages.isNotEmpty()) {
            response = mapOf("error" to errorMessages)
            return HttpResponse.status<Any>(HttpStatus.OK).body(response)
        } else if (quantityToBeAdded != null) {
            if (typeOfESOP == "NON-PERFORMANCE") {
                val freeInventory = UserRepo.userList[userName]!!.getFreeInventory()
                val lockedInventory = UserRepo.userList[userName]!!.getLockedInventory()
                val totalQuantity = freeInventory + lockedInventory + quantityToBeAdded


                if (totalQuantity <= 0 || totalQuantity > DataStorage.MAX_QUANTITY) {
                    errorMessages.add("ESOP quantity out of range. Limit for ESOP quantity is 0 to ${DataStorage.MAX_QUANTITY}")
                }

            } else if (typeOfESOP == "PERFORMANCE") {
                val freePerformanceInventory =
                    UserRepo.userList[userName]!!.getFreePerformanceInventory()
                val lockedPerformanceInventory =
                    UserRepo.userList[userName]!!.getFreePerformanceInventory()
                val totalQuantity = freePerformanceInventory + lockedPerformanceInventory + quantityToBeAdded


                if (totalQuantity <= 0 || totalQuantity > DataStorage.MAX_QUANTITY) {
                    errorMessages.add("ESOP inventory out of range. Limit for ESOP inventory is 0 to ${DataStorage.MAX_QUANTITY}")
                }

            }
            if (errorMessages.isNotEmpty()) {
                response = mapOf("error" to errorMessages)
                return HttpResponse.badRequest(response)
            }
            UserRepo.userList[userName]!!.addEsopToInventory(quantityToBeAdded, typeOfESOP)
        }
        if (errorMessages.size > 0) {
            response = mapOf("error" to errorMessages)
            return HttpResponse.badRequest(response)
        }

        response = mapOf("message" to "$quantityToBeAdded $typeOfESOP ESOPs added to account")
        return HttpResponse.status<Any>(HttpStatus.OK).body(response)
    }

    @Get("/{userName}/accountInformation")
    fun accountInformation(userName: String): HttpResponse<*> {

        val errorMessages: ArrayList<String> = ArrayList()

        val response: Map<String, *>
        if (!Validations.validateUser(userName)) {
            errorMessages.add("userName does not exists.")
            response = mapOf("error" to errorMessages)
            return HttpResponse.status<Any>(HttpStatus.UNAUTHORIZED).body(response)
        }

        response = mapOf(
            "FirstName" to UserRepo.userList[userName]!!.firstName,
            "LastName" to UserRepo.userList[userName]!!.lastName,
            "Phone" to UserRepo.userList[userName]!!.phoneNumber,
            "EmailID" to UserRepo.userList[userName]!!.emailId,
            "Wallet" to mapOf(
                "free" to UserRepo.userList[userName]!!.getFreeMoney(),
                "locked" to UserRepo.userList[userName]!!.getLockedMoney()
            ),
            "Inventory" to arrayListOf<Any>(
                mapOf(
                    "esop_type" to "PERFORMANCE",
                    "free" to UserRepo.userList[userName]!!.getFreePerformanceInventory(),
                    "locked" to UserRepo.userList[userName]!!.getLockedPerformanceInventory()
                ),
                mapOf(
                    "esop_type" to "NON-PERFORMANCE",
                    "free" to UserRepo.userList[userName]!!.getFreeInventory(),
                    "locked" to UserRepo.userList[userName]!!.getLockedInventory()
                )
            )
        )
        return HttpResponse.status<Any>(HttpStatus.OK).body(response)
    }


    @Get("/{userName}/orderHistory")
    fun orderHistory(userName: String): HttpResponse<*> {
        val errorMessages: ArrayList<String> = ArrayList()
        val response: Map<String, *>

        if (!Validations.validateUser(userName)) {
            errorMessages.add("userName does not exist.")
            response = mapOf("error" to errorMessages)
            return HttpResponse.status<Any>(HttpStatus.UNAUTHORIZED).body(response)
        }

        if (!Validations.validateUser(userName)) {
            errorMessages.add("UserName does not exist.")
            response = mapOf("error" to errorMessages)
            return HttpResponse.status<Any>(HttpStatus.UNAUTHORIZED).body(response)
        }
        val user=UserRepo.userList[userName]!!
        response = UserServices.getOrderDetails(user)
        return HttpResponse.status<Any>(HttpStatus.OK).body(response)
    }
}