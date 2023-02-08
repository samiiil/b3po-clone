package repo

import models.User
import java.util.HashMap

class UserRepo {

    companion object {

    val userList: HashMap<String, User> = HashMap()
    val registeredEmails = mutableSetOf<String>()
    val registeredPhoneNumbers = mutableSetOf<String>()

        fun isUserExists(userName: String): Boolean {

            return (userList.containsKey(userName))

        }


        fun getUser(userName: String): User? {
            return userList[userName]
        }
    }

}