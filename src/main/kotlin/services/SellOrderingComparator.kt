package services

import models.Order

class SellOrderingComparator {
    companion object : Comparator<Order> {
        override fun compare(o1: Order, o2: Order): Int {
            if (o1.getOrderPrice() != o2.getOrderPrice()) {
                if (o1.getOrderPrice() > o2.getOrderPrice()) {
                    return 1
                } else {
                    return -1
                }
            } else {
                if (o1.getOrderId() > o2.getOrderId()) {
                    return 1
                } else {
                    return -1
                }
            }
        }
    }
}