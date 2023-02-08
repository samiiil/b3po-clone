package services

import models.DataStorage
import models.User
import repo.UserRepo

fun saveUser(user: User) {
    UserRepo.userList[user.username] = user
    UserRepo.registeredEmails.add(user.emailId)
    UserRepo.registeredPhoneNumbers.add(user.phoneNumber)
}