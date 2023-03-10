package models

import services.OrderServices
import exception.ValidationException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import repo.OrderRepo
import repo.UserRepo
import services.UserServices


class TestOrderCreation {
    @AfterEach
    fun tearDown(){
        UserRepo.userList.clear()
        UserRepo.registeredEmails.clear()
        UserRepo.registeredPhoneNumbers.clear()
        OrderRepo.buyList.clear()
        OrderRepo.sellList.clear()
        OrderRepo.performanceSellList.clear()
        OrderRepo.orderId = 1L
        OrderRepo.orderExecutionId = 1L
    }
    @Test
    fun `can create buy order if user has money in wallet`(){
        val user = User(firstName = "user", lastName = "user", emailId = "user@example.com", phoneNumber = "+911234567890", username = "user")
        UserRepo.userList["user"]=user

        user.addMoneyToWallet(100)



        OrderServices.placeOrder(user, CreateOrderInput(1,"BUY",100))




        assertEquals(1, user.orders.size)
    }

    @Test
    fun `cannot create buy order if user doesn't have enough money in wallet`(){
        val user = User(firstName = "user", lastName = "user", emailId = "user@example.com", phoneNumber = "+911234567890", username = "user")
        UserRepo.userList["user"]=user

        val exception = assertThrows(ValidationException::class.java){ OrderServices.placeOrder(user, CreateOrderInput(1,"BUY",100))
 }
        val errors = exception.errorResponse.error

        assertEquals("Insufficient balance in wallet", errors[0])
        assertEquals(0, user.getFreeMoney())
        assertEquals(0, user.getLockedMoney())
    }

    @Test
    fun `creating buy order moves money to locked wallet`(){
        val user = User(firstName = "user", lastName = "user", emailId = "user@example.com", phoneNumber = "+911234567890", username = "user")
        UserRepo.userList["user"]=user
        user.addMoneyToWallet(100)

        OrderServices.placeOrder(user, CreateOrderInput(1,"BUY",100))

        assertEquals(0, user.getFreeMoney())
        assertEquals(100, user.getLockedMoney())
    }

    @Test
    fun `correct buy order is created`(){
        val user = User(firstName = "user", lastName = "user", emailId = "user@example.com", phoneNumber = "+911234567890", username = "user")
        UserRepo.userList["user"]=user
        user.addMoneyToWallet(100)

        OrderServices.placeOrder(user, CreateOrderInput(1,"BUY",100))


        assertEquals("Unfilled", OrderRepo.buyList.peek().orderStatus)
        assertEquals(1, OrderRepo.buyList.peek().orderQuantity)
        assertEquals("BUY", OrderRepo.buyList.peek().orderType)
        assertEquals(100, OrderRepo.buyList.peek().getOrderPrice())
        assertEquals(0, OrderRepo.buyList.peek().orderExecutionLogs.size)
    }

    @Test
    fun `creating buy order adds order to global buy list`(){
        val user = User(firstName = "user", lastName = "user", emailId = "user@example.com", phoneNumber = "+911234567890", username = "user")
        UserRepo.userList["user"]=user
        user.addMoneyToWallet(100)

        OrderServices.placeOrder(user, CreateOrderInput(1,"BUY",100))

        assertEquals(1, OrderRepo.buyList.size)
        assertEquals(user.orders[0], OrderRepo.buyList.peek())
    }

    @Test
    fun `can create sell order if user has enough esops`(){
        val user = User(firstName = "user", lastName = "user", emailId = "user@example.com", phoneNumber = "+911234567890", username = "user")
        UserRepo.userList["user"]=user
        user.addEsopToInventory(1)

        OrderServices.placeOrder(user, CreateOrderInput(1,"SELL",100))

        assertEquals(1, user.orders.size)
    }

    @Test
    fun `cannot create sell order if user doesn't have enough esops in inventory`(){
        val user = User(firstName = "user", lastName = "user", emailId = "user@example.com", phoneNumber = "+911234567890", username = "user")
        UserRepo.userList["user"]=user

        val exception = assertThrows(ValidationException::class.java){   OrderServices.placeOrder(user, CreateOrderInput(1,"SELL",100))
 }
        val errors = exception.errorResponse.error

        assertEquals("Insufficient NON-PERFORMANCE ESOPs in inventory", errors[0])
        assertEquals(0, user.getFreeInventory())
        assertEquals(0, user.getLockedInventory())
    }

    @Test
    fun `creating sell order locks esops`(){
        val user = User(firstName = "user", lastName = "user", emailId = "user@example.com", phoneNumber = "+911234567890", username = "user")
        UserRepo.userList["user"]=user
        user.addEsopToInventory(1)

        OrderServices.placeOrder(user, CreateOrderInput(1,"SELL",100))

        assertEquals(0, user.getFreeInventory())
        assertEquals(1, user.getLockedInventory())
    }

    @Test
    fun `correct sell order is created`(){
        val user = User(firstName = "user", lastName = "user", emailId = "user@example.com", phoneNumber = "+911234567890", username = "user")
        UserRepo.userList["user"]=user
        user.addEsopToInventory(1)
        OrderServices.placeOrder(user, CreateOrderInput(1,"SELL",100))


        assertEquals("Unfilled", OrderRepo.sellList.peek().orderStatus)
        assertEquals(1, OrderRepo.sellList.peek().orderQuantity)
        assertEquals("SELL", OrderRepo.sellList.peek().orderType)
        assertEquals(100, OrderRepo.sellList.peek().getOrderPrice())
        assertEquals(0, OrderRepo.sellList.peek().orderExecutionLogs.size)
    }

    @Test
    fun `creating sell order adds order to global sell list`(){
        val user = User(firstName = "user", lastName = "user", emailId = "user@example.com", phoneNumber = "+911234567890", username = "user")
        UserRepo.userList["user"]=user
        user.addEsopToInventory(1)

        OrderServices.placeOrder(user, CreateOrderInput(1,"SELL",100))


        assertEquals(1, OrderRepo.sellList.size)
        assertEquals(user.orders[0], OrderRepo.sellList.peek())
    }

    @Test
    fun `can create performance sell order if user has enough performance esops`(){
        val user = User(firstName = "user", lastName = "user", emailId = "user@example.com", phoneNumber = "+911234567890", username = "user")
        UserRepo.userList["user"]=user
        user.addEsopToInventory(1, "PERFORMANCE")

        OrderServices.placeOrder(user, CreateOrderInput(1,"SELL",100,"PERFORMANCE"))


        assertEquals(1, user.orders.size)
    }

    @Test
    fun `cannot create performance sell order if user doesn't have enough performance esops in inventory`(){
        val user = User(firstName = "user", lastName = "user", emailId = "user@example.com", phoneNumber = "+911234567890", username = "user")
        UserRepo.userList["user"]=user

        val exception = assertThrows(ValidationException::class.java){
            OrderServices.placeOrder(user, CreateOrderInput(1,"SELL",100,"PERFORMANCE"))

        }
        val errors = exception.errorResponse.error

        assertEquals("Insufficient PERFORMANCE ESOPs in inventory", errors[0])
        assertEquals(0, user.getFreePerformanceInventory())
        assertEquals(0, user.getLockedPerformanceInventory())
    }

    @Test
    fun `creating performance sell order locks esops`(){
        val user = User(firstName = "user", lastName = "user", emailId = "user@example.com", phoneNumber = "+911234567890", username = "user")
        UserRepo.userList["user"]=user
        user.addEsopToInventory(1, "PERFORMANCE")

        OrderServices.placeOrder(user, CreateOrderInput(1,"SELL",100,"PERFORMANCE"))


        assertEquals(0, user.getFreePerformanceInventory())
        assertEquals(1, user.getLockedPerformanceInventory())
    }

    @Test
    fun `correct performance sell order is created`(){
        val user = User(firstName = "user", lastName = "user", emailId = "user@example.com", phoneNumber = "+911234567890", username = "user")
        UserRepo.userList["user"]=user
        user.addEsopToInventory(1, "PERFORMANCE")

        OrderServices.placeOrder(user, CreateOrderInput(1,"SELL",100,"PERFORMANCE"))


        assertEquals("Unfilled", OrderRepo.performanceSellList.peek().orderStatus)
        assertEquals(1, OrderRepo.performanceSellList.peek().orderQuantity)
        assertEquals("SELL", OrderRepo.performanceSellList.peek().orderType)
        assertEquals(100, OrderRepo.performanceSellList.peek().getOrderPrice())
        assertEquals(0, OrderRepo.performanceSellList.peek().orderExecutionLogs.size)
    }

    @Test
    fun `creating performance sell order adds order to global performance sell list`(){
        val user = User(firstName = "user", lastName = "user", emailId = "user@example.com", phoneNumber = "+911234567890", username = "user")
        UserRepo.userList["user"]=user
        user.addEsopToInventory(1, "PERFORMANCE")

        OrderServices.placeOrder(user, CreateOrderInput(1,"SELL",100,"PERFORMANCE"))



        assertEquals(1, OrderRepo.performanceSellList.size)
        assertEquals(user.orders[0], OrderRepo.performanceSellList.peek())
    }

    @Test
    fun `order details is initially empty`(){
        val user = User(firstName = "user", lastName = "user", emailId = "user@example.com", phoneNumber = "+911234567890", username = "user")
        UserRepo.userList["user"]=user
        val orderDetails = UserServices.getOrderDetails(user)

        assert(orderDetails.keys.contains("order_history"))
        assertEquals(0, orderDetails["order_history"]!!.size)
    }

    @Test
    fun `order details for unfilled order is set correctly`(){
        val user = User(firstName = "user", lastName = "user", emailId = "user@example.com", phoneNumber = "+911234567890", username = "user")
        UserRepo.userList["user"]=user

        user.addMoneyToWallet(100)

        OrderServices.placeOrder(user, CreateOrderInput(1,"BUY",100))


        val orderDetails = UserServices.getOrderDetails(user)

        assertEquals(1, orderDetails.size)
        assert(orderDetails.keys.contains("order_history"))
        assertEquals(
            "{order_id=1, quantity=1, type=BUY, price=100, unfilled=[{price=100, quantity=1}]}",
            orderDetails["order_history"]!![0].toString()
        )
    }

    @Test
    fun `order details for partially filled order is set correctly`(){
        val buyer = User(firstName = "user1", lastName = "user1", emailId = "user1@example.com", phoneNumber = "+911234567891", username = "user1")
        val seller = User(firstName = "user2", lastName = "user2", emailId = "user2@example.com", phoneNumber = "+911234567892", username = "user2")
        UserRepo.saveUser(buyer)
        UserRepo.saveUser(seller)
        buyer.addMoneyToWallet(200)
        seller.addEsopToInventory(1)
        OrderServices.placeOrder(buyer, CreateOrderInput(2,"BUY",100))

        OrderServices.placeOrder(seller, CreateOrderInput(1,"SELL",100))



        val orderDetails = UserServices.getOrderDetails(buyer)

        assertEquals(1, orderDetails.size)
        assert(orderDetails.keys.contains("order_history"))
        assertEquals(
            "{order_id=1, quantity=2, type=BUY, price=100, partially_filled=[{price=100, quantity=1}], unfilled=[{price=100, quantity=1}]}",
            orderDetails["order_history"]!![0].toString()
        )
    }

    @Test
    fun `order details for fully filled order is set correctly`(){
        val buyer = User(firstName = "user1", lastName = "user1", emailId = "user1@example.com", phoneNumber = "+911234567891", username = "user1")
        val seller = User(firstName = "user2", lastName = "user2", emailId = "user2@example.com", phoneNumber = "+911234567892", username = "user2")
        UserRepo.saveUser(buyer)
        UserRepo.saveUser(seller)

        buyer.addMoneyToWallet(200)
        seller.addEsopToInventory(1)

        OrderServices.placeOrder(buyer, CreateOrderInput(2,"BUY",100))

        OrderServices.placeOrder(seller, CreateOrderInput(1,"SELL",100))



        val orderDetails = UserServices.getOrderDetails(seller)

        assertEquals(1, orderDetails.size)
        assert(orderDetails.keys.contains("order_history"))
        assertEquals(
            "{order_id=2, quantity=1, type=SELL, price=100, filled=[{price=100, quantity=1}]}",
            orderDetails["order_history"]!![0].toString()
        )
    }

    @Test
    fun `can log or print an order`(){
        val user = User(firstName = "user", lastName = "user", emailId = "user@example.com", phoneNumber = "+911234567890", username = "user")

        UserRepo.userList["user"]=user

        user.addEsopToInventory(1, "PERFORMANCE")
        val expected = """
            username:user
            orderId:1
            orderQuantity:1
            orderPrice:10
            orderType:SELL
            orderStatus:Unfilled
            remainingQuantity:1

        """.trimIndent()



        OrderServices.placeOrder(user,CreateOrderInput(1, "SELL", 10,"PERFORMANCE"))

        assertEquals(expected,user.orders[0].toString())
    }

    @Test
    fun `cannot create sell order that will cause wallet limit to be exceeded`(){

        val user = User(firstName = "user", lastName = "user", emailId = "user@example.com", phoneNumber = "+911234567890", username = "user")
        UserRepo.userList["user"]=user
        user.addMoneyToWallet(DataStorage.MAX_AMOUNT-10L)
        user.addEsopToInventory(1)
        val exception = assertThrows(ValidationException::class.java) {
            OrderServices.placeOrder(user, CreateOrderInput(1, "SELL", 15))
        }

        val errors = exception.errorResponse.error


        assertEquals(1, errors.size)
        assertEquals("Wallet threshold will be exceeded", errors[0])
    }

    @Test
    fun `cannot create buy order that will cause inventory limit to be exceeded`(){
        val user = User(firstName = "user", lastName = "user", emailId = "user@example.com", phoneNumber = "+911234567890", username = "user")
        UserRepo.userList["user"]=user
        user.addEsopToInventory(DataStorage.MAX_QUANTITY-10L)
        user.addMoneyToWallet(150)

        val exception = assertThrows(ValidationException::class.java){  OrderServices.placeOrder(user,
            CreateOrderInput(15, "BUY", 10))
        }
        val errors = exception.errorResponse.error

        assertEquals(1, errors.size)
        assertEquals("Inventory threshold will be exceeded", errors[0])
    }
}