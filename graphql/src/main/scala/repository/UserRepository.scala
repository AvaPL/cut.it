package repository

import model.User

case class UserRepository() {
  private val usersDb = List(
    User("Pafeu", "Happy"),
    User("Josh", "At work"),
    User("Julia", "Coffee time")
  )

  def user(username: String): Option[User] =
    usersDb.find(_.username == username)

  def users: List[User] = usersDb
}
