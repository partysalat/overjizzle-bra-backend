package repos

import java.sql.Date
import javax.inject.Inject

import com.google.inject.Singleton
import models.User
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile

import scala.concurrent.Future

@Singleton()
class UsersRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends UsersTable with HasDatabaseConfigProvider[JdbcProfile] {
  def insert(user: User): Future[Int] = db.run {
    userTableQueryInc += user
  }
}


private[repos] trait UsersTable {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import driver.api._

  implicit def dateTime =
    MappedColumnType.base[DateTime, Date](
      dateTime => new Date(dateTime.getMillis),
      date => new DateTime(date.getTime)
    )

  lazy protected val userTableQuery = TableQuery[UsersTable]
  lazy protected val userTableQueryInc = userTableQuery returning userTableQuery.map(_.id)

  private[UsersTable] class UsersTable(tag: Tag) extends Table[User](tag, "user") {
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    val userName: Rep[String] = column[String]("userName", O.SqlType("VARCHAR(200)"))
    val createdAt: Rep[DateTime] = column[DateTime]("createdAt", O.SqlType("date"))


    def * = (id, userName, createdAt) <> ((User.apply _).tupled, User.unapply)
  }

}