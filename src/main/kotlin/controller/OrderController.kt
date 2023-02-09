package controller

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import models.CreateOrderInput
import repo.UserRepo
import services.OrderServices
import validations.RequestValidations
import validations.UserValidations

@Controller("/user")
class OrderController {
    @Post("/{userName}/createOrder")
    fun createOrder(userName: String, @Body createOrderInput: CreateOrderInput): HttpResponse<*> {
        var response: Map<String, *>?

        response = UserValidations.validateUser(userName)
        if (response != null)
            return HttpResponse.status<Any>(HttpStatus.UNAUTHORIZED).body(response)

        response = RequestValidations.checkIfRequestIsValid(createOrderInput)
        if (response != null)
            return HttpResponse.status<Any>(HttpStatus.UNAUTHORIZED).body(response)

        val orderQuantity: Long = createOrderInput.quantity!!.toLong()
        val orderType: String = createOrderInput.orderType!!.trim().uppercase()
        val orderPrice: Long = createOrderInput.price!!.toLong()
        val typeOfESOP: String = (createOrderInput.esopType ?: "NON-PERFORMANCE").trim().uppercase()

        val user = UserRepo.getUser(userName)!!
        response = OrderServices.placeOrder(user, orderQuantity, orderType, orderPrice, typeOfESOP)

        return HttpResponse.status<Any>(HttpStatus.OK).body(response)
    }

}