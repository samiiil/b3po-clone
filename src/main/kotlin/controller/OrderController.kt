package controller

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import models.CreateOrderInput
import models.createOrderResponse
import repo.UserRepo
import services.OrderServices
import validations.RequestValidations
import validations.UserValidations

@Controller("/user")
class OrderController {
    @Post("/{userName}/createOrder")
    fun createOrder(userName: String, @Body createOrderInput: CreateOrderInput): HttpResponse<createOrderResponse> {

        UserValidations.validateUser(userName)

        RequestValidations.checkIfRequestIsValid(createOrderInput)

        val orderQuantity: Long = createOrderInput.quantity!!.toLong()
        val orderType: String = createOrderInput.orderType!!.trim().uppercase()
        val orderPrice: Long = createOrderInput.price!!.toLong()
        val typeOfESOP: String = (createOrderInput.esopType ?: "NON-PERFORMANCE").trim().uppercase()

        val user = UserRepo.getUser(userName)!!
        val response = OrderServices.placeOrder(user, orderQuantity, orderType, orderPrice, typeOfESOP)

        return HttpResponse.status<Any>(HttpStatus.OK).body(response)
    }

}