package validations

import exception.ValidationException
import models.ErrorResponse
import repo.UserRepo

object UserValidations {

    fun validateUser(userName: String) {
        val errorMessages: ArrayList<String> = ArrayList()

        if (!UserRepo.isUserExists(userName)) {
            errorMessages.add("userName does not exists.")
            throw ValidationException(ErrorResponse(errorMessages))
        }
    }
}
