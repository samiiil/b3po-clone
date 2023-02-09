package validations

import exception.ValidationException
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import models.CreateOrderInput
import models.ErrorResponse

object RequestValidations {


    fun checkIfRequestIsValid(body: CreateOrderInput) {
        var response: Map<String, ArrayList<String>>?
        checkIfFieldsMissingInOrderReq(body)
        checkIfOrderRequestDataIsValid(body)
    }


    fun checkIfFieldsMissingInOrderReq(body: CreateOrderInput) {

        val errorMessages: ArrayList<String> = ArrayList()


        if (body.orderType.isNullOrBlank())
            errorMessages.add("OrderType is missing, orderType should be BUY or SELL.")
        if (body.price == null)
            errorMessages.add("Price for the order is missing.")
        if (body.quantity == null)
            errorMessages.add("Quantity field for order is missing.")
        if (body.orderType != null && body.orderType == "SELL" && body.esopType.isNullOrBlank()) {
            errorMessages.add("esopType is missing, SELL order requires esopType.")
        }

        if (errorMessages.isNotEmpty())
            throw ValidationException(ErrorResponse(errorMessages))
    }


    fun checkIfOrderRequestDataIsValid(body: CreateOrderInput) {

        val errorMessages: ArrayList<String> = ArrayList()

        errorMessages.addAll(isOrderTypeValid(body.orderType!!))

        if (body.esopType != null)
            errorMessages.addAll(isESOPTypeValid(body.esopType))

        if (errorMessages.isNotEmpty()) {
            throw ValidationException(ErrorResponse(errorMessages))
        }
    }


    fun isOrderTypeValid(orderType: String): ArrayList<String> {
        val errorMessages: ArrayList<String> = ArrayList()
        if (orderType !in arrayOf("BUY", "SELL"))
            errorMessages.add("Invalid order type.")
        return errorMessages
    }

    fun isESOPTypeValid(typeOfESOP: String): ArrayList<String> {
        val errorMessages: ArrayList<String> = ArrayList()
        if (typeOfESOP !in arrayOf("PERFORMANCE", "NON-PERFORMANCE"))
            errorMessages.add("Invalid type of ESOP, ESOP type should be PERFORMANCE or NON-PERFORMANCE.")
        return errorMessages
    }

}