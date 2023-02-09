package models

data class CreateOrderInput(val quantity: Int? = null,
                            val orderType: String? = null,
                            val price: Int? = null,
                            val esopType: String?=null
                            )