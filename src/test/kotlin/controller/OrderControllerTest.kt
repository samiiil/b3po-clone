package controller

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import models.User


import io.restassured.specification.RequestSpecification
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import repo.OrderRepo
import repo.UserRepo

@MicronautTest
class OrderControllerTest {

    @BeforeEach
    fun `Remove all the Users and Orders`() {
        UserRepo.userList.clear()
        OrderRepo.buyList.clear()
        OrderRepo.sellList.clear()
        OrderRepo.performanceSellList.clear()
        OrderRepo.orderId=1L
        OrderRepo.orderExecutionId=1L
    }

    @Test
    fun `check if user exists while placing order`(spec: RequestSpecification) {

        spec.`when`()
            .header("Content-Type", "application/json")
            .body("{}")
            .post("/user/hi/createOrder")
            .then()
            .statusCode(400).and()
            .body(
                "error",
                Matchers.contains("userName does not exists.")
            )
    }


    @Test
    fun `check if order request has missing quantity,type and price fields`(spec: RequestSpecification) {
        val user = User(firstName = "user", lastName = "user", emailId = "user@example.com", phoneNumber = "+911234567890", username = "vishal")
        UserRepo.userList["vishal"]=user

        spec.`when`()
            .header("Content-Type", "application/json")
            .body("{}")
            .post("/user/${user.username}/createOrder")
            .then()
            .statusCode(400).and()
            .body(
                "error",
                Matchers.contains(
                    "OrderType is missing, orderType should be BUY or SELL.", "Price for the order is missing.", "Quantity field for order is missing."
                )
            )
    }

    @Test
    fun `Check if successful buy order is placed if order request is valid`(spec: RequestSpecification) {
        val user = User(firstName = "user", lastName = "user", emailId = "user@example.com", phoneNumber = "+911234567890", username = "vishal")
        UserRepo.userList[user.username] = user
        user.addMoneyToWallet(100)

        spec.`when`()
            .header("Content-Type", "application/json")
            .body(
                """
                {
                    "quantity": 1,
                    "orderType": "BUY",
                    "price": 20
                }
            """.trimIndent()
            )
            .post("/user/${user.username}/createOrder")
            .then()
            .statusCode(200).and()
            .body(
                "orderId", Matchers.equalTo(1),
                "quantity", Matchers.equalTo(1),
                "type", Matchers.equalTo("BUY"),
                "price", Matchers.equalTo(20)
            )
    }


    @Test
    fun `Check if successful sell order is placed if order request is valid`(spec: RequestSpecification) {
        val user = User(firstName = "user", lastName = "user", emailId = "user@example.com", phoneNumber = "+911234567890", username = "vishal")
        UserRepo.userList[user.username] = user
        user.addEsopToInventory(10)

        spec.`when`()
            .header("Content-Type", "application/json")
            .body(
                """
                {
                    "quantity": 1,
                    "orderType": "SELL",
                    "price": 20
                }
            """.trimIndent()
            )
            .post("/user/${user.username}/createOrder")
            .then()
            .statusCode(200).and()
            .body(
                "orderId", Matchers.equalTo(1),
                "quantity", Matchers.equalTo(1),
                "type", Matchers.equalTo("SELL"),
                "price", Matchers.equalTo(20)
            )
    }

}