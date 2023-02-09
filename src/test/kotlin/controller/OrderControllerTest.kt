package controller

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import models.User


import io.restassured.specification.RequestSpecification
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import repo.UserRepo

@MicronautTest
class OrderControllerTest {

    @BeforeEach
    fun `Remove all the Users and Orders`() {
        UserRepo.userList.clear()
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
}