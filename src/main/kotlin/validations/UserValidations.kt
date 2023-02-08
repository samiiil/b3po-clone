package validations

import models.DataStorage

class UserValidations {

    companion object {

        fun validateUser(userName: String): MutableMap<String, ArrayList<String>>? {
            val errorMessages: ArrayList<String> = ArrayList()
            val response: MutableMap<String, ArrayList<String>>

            if (!DataStorage.isUserExists(userName)) {
                errorMessages.add("userName does not exists.")
                response = mapOf("error" to errorMessages) as MutableMap<String, ArrayList<String>>
                return response
            }
            return null
        }
    }
}