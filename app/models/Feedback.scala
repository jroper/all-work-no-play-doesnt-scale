package models
import java.sql.Connection
import anorm._
import anorm.SqlParser._


case class Feedback(username: String, feedback:String) {
  def insert()(implicit c: Connection) =
    SQL("insert into Feedback(username, feedback) values ({username}, {feedback})").on(
      "username" -> username,
      "feedback" -> feedback
    ).executeInsert()
}

object Feedback {
  def findAll(implicit c: Connection) =
    SQL("select * from Feedback order by id desc")
      .as(str("username") ~ str("feedback") *)
      .map(flatten).map((Feedback.apply _).tupled).take(10)
}
