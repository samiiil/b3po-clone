package models

import services.OrderServices
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import repo.OrderRepo
import repo.UserRepo
import services.saveUser
import kotlin.math.roundToLong

class TestOrderExecution {
    @BeforeEach
    fun setup(){
        val buyer = User("jake", "Jake", "Peralta", "9844427549", "jake@gmail.com") //Buyer
        buyer.addMoneyToWallet(10000)
        val seller = User("amy", "Amy", "Santiago", "9472919384", "amy@gmail.com") //Seller
        seller.addEsopToInventory(100, "NON-PERFORMANCE")
        seller.addEsopToInventory(100, "PERFORMANCE")

        saveUser(buyer)
        saveUser(seller)
    }

    @AfterEach
    fun tearDown(){
        UserRepo.userList.clear()
        UserRepo.registeredEmails.clear()
        UserRepo.registeredPhoneNumbers.clear()
        OrderRepo.buyList.clear()
        OrderRepo.sellList.clear()
        OrderRepo.performanceSellList.clear()
        DataStorage.orderId = 1
        DataStorage.orderExecutionId = 1
    }
    @Test
    fun `multiple buy orders by one user and one sell order by another user to fulfill them completely`(){
        val buyer = UserRepo.userList["jake"]!!
        val seller = UserRepo.userList["amy"]!!
        val expectedSellerWallet = (150*(1-DataStorage.COMMISSION_FEE_PERCENTAGE*0.01)).roundToLong()

        OrderServices.placeOrder(buyer,5, "BUY", 10)
        OrderServices.placeOrder(buyer,5, "BUY", 10)
        OrderServices.placeOrder(buyer,5, "BUY", 10)
        OrderServices.placeOrder(seller,15, "SELL", 10)


        assert(OrderRepo.buyList.isEmpty())
        assert(OrderRepo.sellList.isEmpty())
        assertEquals(9850, buyer.getFreeMoney())
        assertEquals(15, buyer.getFreeInventory())
        assertEquals(expectedSellerWallet, seller.getFreeMoney())
        assertEquals(85, seller.getFreeInventory())
    }

    @Test
    fun `should take sell price as order price when buy price is higher`(){
        val buyer = UserRepo.userList["jake"]!!
        val seller = UserRepo.userList["amy"]!!
        val expectedSellerWallet = (5*(1-DataStorage.COMMISSION_FEE_PERCENTAGE*0.01)).roundToLong()

        OrderServices.placeOrder(buyer,1, "BUY", 10)
        OrderServices.placeOrder(seller,1, "SELL", 5)



        assertEquals(10000 - 5, buyer.getFreeMoney())
        assertEquals(expectedSellerWallet, seller.getFreeMoney())
    }

    @Test
    fun `should prioritize sell order that has lower price`(){
        val buyer = UserRepo.userList["jake"]!!
        val seller = UserRepo.userList["amy"]!!


        OrderServices.placeOrder(seller,1, "SELL", 10)
        OrderServices.placeOrder(seller,1, "SELL", 5)
        OrderServices.placeOrder(buyer,1, "BUY", 10)



        assertEquals("Unfilled", seller.orders[0].orderStatus)
        assertEquals(10, seller.orders[0].orderPrice)
        assertEquals("Filled", seller.orders[1].orderStatus)
        assertEquals(5, seller.orders[1].orderPrice)
        assertEquals("Filled", buyer.orders[0].orderStatus)
        assertEquals(10, buyer.orders[0].orderPrice)
        assertEquals(10000-5, buyer.getFreeMoney())
    }

    @Test
    fun `should prioritize buy order that has higher price`(){
        val buyer = UserRepo.userList["jake"]!!
        val seller = UserRepo.userList["amy"]!!


        OrderServices.placeOrder(buyer,1, "BUY", 5)
        OrderServices.placeOrder(buyer,1, "BUY", 10)
        OrderServices.placeOrder(seller,1, "SELL", 5)



        assertEquals("Unfilled", buyer.orders[0].orderStatus)
        assertEquals(5, buyer.orders[0].orderPrice)
        assertEquals("Filled", buyer.orders[1].orderStatus)
        assertEquals(10, buyer.orders[1].orderPrice)
        assertEquals("Filled", seller.orders[0].orderStatus)
        assertEquals(5, seller.orders[0].orderPrice)
    }

    @Test
    fun `should prioritize performance ESOP sell orders over non-performance ESOP sell orders`(){
        val buyer = UserRepo.userList["jake"]!!
        val seller = UserRepo.userList["amy"]!!

        OrderServices.placeOrder(seller,1, "SELL", 5,"NON-PERFORMANCE")
        OrderServices.placeOrder(seller,1, "SELL", 10,"PERFORMANCE")
        OrderServices.placeOrder(buyer,1, "BUY", 10)




        assertEquals("Unfilled", seller.orders[0].orderStatus)
        assertEquals(5, seller.orders[0].orderPrice)
        assertEquals("Filled", seller.orders[1].orderStatus)
        assertEquals(10, seller.orders[1].orderPrice)
        assertEquals("Filled", buyer.orders[0].orderStatus)
        assertEquals(10, buyer.orders[0].orderPrice)
        assertEquals(10000-10, buyer.getFreeMoney())
    }

    @Test
    fun `buyer should get non-performance ESOP even if seller sells performance ESOPs`(){
        val buyer = UserRepo.userList["jake"]!!
        val seller = UserRepo.userList["amy"]!!


        OrderServices.placeOrder(seller,1, "SELL", 10,"PERFORMANCE")
        OrderServices.placeOrder(buyer,1, "BUY", 10)



        assertEquals(0, buyer.getLockedPerformanceInventory())
        assertEquals(0, buyer.getFreePerformanceInventory())
        assertEquals(0, buyer.getLockedInventory())
        assertEquals(1, buyer.getFreeInventory())
    }

    @Test
    fun `should prioritize order that came first among multiple performance ESOP sell orders irrespective of price`(){
        val buyer = UserRepo.userList["jake"]!!
        val seller = UserRepo.userList["amy"]!!

        OrderServices.placeOrder(seller,1, "SELL", 10,"PERFORMANCE")
        OrderServices.placeOrder(seller,1, "SELL", 5,"PERFORMANCE")
        OrderServices.placeOrder(buyer,1, "BUY", 10)



        assertEquals(10000-10, buyer.getFreeMoney())
        assertEquals(0,buyer.getLockedMoney())
        assertEquals("Filled", seller.orders[0].orderStatus)
        assertEquals(10, seller.orders[0].orderPrice)
        assertEquals("Unfilled", seller.orders[1].orderStatus)
        assertEquals(5, seller.orders[1].orderPrice)
        assertEquals("Filled", buyer.orders[0].orderStatus)
        assertEquals(10, buyer.orders[0].orderPrice)
    }
}