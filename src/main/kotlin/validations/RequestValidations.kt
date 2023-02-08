package validations

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import models.CreateOrderInput

class RequestValidations {

    companion object {

        fun checkIfRequestIsValid(body: CreateOrderInput): Map<String, ArrayList<String>>? {

            var response: Map<String, ArrayList<String>>?

            response= checkIfFieldsMissingInOrderReq(body)

            if(response!=null)
                return response

            response= checkIfOrderRequestDataIsValid(body)
            if(response!=null)
                return response

            return null
        }




        fun checkIfFieldsMissingInOrderReq(body: CreateOrderInput): Map<String, ArrayList<String>>? {

            val errorMessages: ArrayList<String> = ArrayList()

            var response: Map<String, ArrayList<String>>?

            if (body.orderType.isNullOrBlank())
                errorMessages.add("orderType is missing, orderType should be BUY or SELL.")
            if (body.price == null)
                errorMessages.add("price for the order is missing.")
            if (body.quantity == null)
                errorMessages.add("quantity field for order is missing.")
            if (body.orderType != null && body.orderType == "SELL" && body.esopType.isNullOrBlank()) {
                errorMessages.add("esopType is missing, SELL order requires esopType.")
            }

            if (errorMessages.isNotEmpty()) {
                response = mapOf("error" to errorMessages)
                return response
            }
            return null
        }


        fun checkIfOrderRequestDataIsValid(body: CreateOrderInput): Map<String, ArrayList<String>>? {

            val errorMessages: ArrayList<String> = ArrayList()
            var response: Map<String, ArrayList<String>>?

            errorMessages.addAll(isOrderTypeValid(body.orderType!!))

            if(body.esopType!=null)
                errorMessages.addAll(isESOPTypeValid(body.esopType))



            if (errorMessages.isNotEmpty()) {
                response = mapOf("error" to errorMessages)
                return response
            }
            return null
        }


        fun isOrderTypeValid(orderType : String): ArrayList<String> {
            val errorMessages: ArrayList<String> = ArrayList()
            if (orderType !in arrayOf("BUY", "SELL"))
                errorMessages.add("Invalid order type.")
            return errorMessages
        }

        fun isESOPTypeValid(typeOfESOP : String): ArrayList<String> {
            val errorMessages: ArrayList<String> = ArrayList()
            if (typeOfESOP !in arrayOf("PERFORMANCE", "NON-PERFORMANCE"))
                errorMessages.add("Invalid type of ESOP, ESOP type should be PERFORMANCE or NON-PERFORMANCE.")
            return errorMessages
        }


    }
}