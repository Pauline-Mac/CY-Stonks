import java.sql.{Connection, DriverManager, Statement}

object DataBaseControler {
  // Establish a connection to PostgreSQL
  def getConnection(): Connection = {
    val url = sys.env.getOrElse("DATABASE_URL", "jdbc:postgresql://postgres:5432/cy_stonks")
    val user = sys.env.getOrElse("DATABASE_USER", "cy_stonks_user")
    val password = sys.env.getOrElse("DATABASE_PASSWORD", "cytech0001")
    DriverManager.getConnection(url, user, password)
  }

  // Method to insert a new user
  def insertUser(name: String, email: String): Unit = {
    try{
      val connection = getConnection()
      val statement: Statement = connection.createStatement()
      val sql = s"INSERT INTO users (name, email) VALUES ('$name', '$email')"
      statement.executeUpdate(sql)
      connection.close()
    }catch {
      case e:Exception=>println(s"Error adding user: ${e.getMessage}")
    }

  }
}
