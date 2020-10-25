package support.impl.context

import support.impl.model.User

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
    scribe.debug(s"Added $user to repository")
    user
  }

  def deleteUser(username: String): Option[User] = {
    val user = usersDb.find(_.username == username)
    user.foreach { user =>
      usersDb.remove(user)
      scribe.debug(s"Removed $user from repository")
    }
    user
  }
}
