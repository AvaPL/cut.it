package repository

import model.User

import scala.collection.mutable

case class UserRepository() {
  private val usersDb = mutable.HashSet(
    User("Pafeu", "Happy"),
    User("Josh", "At work"),
    User("Julia", "Coffee time")
  )

  def user(username: String): Option[User] =
    usersDb.find(_.username == username)

  def users: List[User] = usersDb.toList

  def addUser(user: User): User = {
    usersDb.addOne(user)
    user
  }

//  def removeUser(username: String): Option[User] = {
//    val user = usersDb.find(_.username == username)
//    user.foreach(usersDb.remove)
//    user
//  }
}
