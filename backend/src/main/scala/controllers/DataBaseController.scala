package controllers
import java.sql.{Connection, DriverManager, PreparedStatement, ResultSet}
import org.mindrot.jbcrypt.BCrypt



object DataBaseController {

  // Establish a connection to PostgreSQL
  def getConnection(): Connection = {
    val url = sys.env.getOrElse("DATABASE_URL", "jdbc:postgresql://postgres:5432/cy_stonks")
    val user = sys.env.getOrElse("DATABASE_USER", "cy_stonks_user")
    val password = sys.env.getOrElse("DATABASE_PASSWORD", "cytech0001")
    DriverManager.getConnection(url, user, password)
  }

  // Method to insert a new user
  def insertUser(name: String, email: String, password: String): Boolean = {
    val connection = getConnection()
    var preparedStatement: PreparedStatement = null
    var resultSet: ResultSet = null
    var userExists = false

    try {
      // Check if the user already exists
      val checkSql = "SELECT COUNT(1) FROM users WHERE email = ? OR username = ?"
      preparedStatement = connection.prepareStatement(checkSql)
      preparedStatement.setString(1, email)
      preparedStatement.setString(2, name)
      resultSet = preparedStatement.executeQuery()

      if (resultSet.next() && resultSet.getInt(1) > 0) {
        userExists = true
        println("User with the same email or username already exists.")
      } else {
        // Hash the password using BCrypt
        val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())

        // Insert the new user
        val insertSql = "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)"
        preparedStatement = connection.prepareStatement(insertSql)
        preparedStatement.setString(1, name)
        preparedStatement.setString(2, email)
        preparedStatement.setString(3, passwordHash)

        preparedStatement.executeUpdate()
        println("User successfully added.")
      }
    } catch {
      case e: Exception =>
        println(s"Error adding user: ${e.getMessage}")
        userExists = true // Consider the operation failed if an exception occurs
    } finally {
      if (resultSet != null) resultSet.close()
      if (preparedStatement != null) preparedStatement.close()
      connection.close()
    }

    !userExists // Return true if the user was added successfully, false otherwise
  }

  // Method to create a portfolio for a user
  def createPortfolio(userId: Int, portfolioName: String): Option[Int] = {
    var portfolioId: Option[Int] = None
    val sql = "INSERT INTO portfolios (user_id, portfolio_name) VALUES (?, ?)"

    val connection: Connection = getConnection()
    try {
      val preparedStatement: PreparedStatement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)
      try {
        preparedStatement.setInt(1, userId)
        preparedStatement.setString(2, portfolioName)
        preparedStatement.executeUpdate()

        val generatedKeys: ResultSet = preparedStatement.getGeneratedKeys
        try {
          if (generatedKeys.next()) {
            portfolioId = Some(generatedKeys.getInt(1))
          }
        } finally {
          generatedKeys.close()
        }
      } finally {
        preparedStatement.close()
      }
    } catch {
      case e: Exception =>
        println(s"Error creating portfolio: ${e.getMessage}")
    } finally {
      connection.close()
    }

    portfolioId
  }

  // Method to add an asset to a portfolio
  def addAssetToPortfolio(portfolioId: Int, assetType: String, assetSymbol: String, quantity: Double, purchasePrice: Double): Boolean = {
    val sql = "INSERT INTO assets (portfolio_id, asset_type, asset_symbol, quantity, purchase_price) VALUES (?, ?, ?, ?, ?)"
    var success = false

    val connection: Connection = getConnection()
    try {
      val preparedStatement: PreparedStatement = connection.prepareStatement(sql)
      try {
        preparedStatement.setInt(1, portfolioId)
        preparedStatement.setString(2, assetType)
        preparedStatement.setString(3, assetSymbol)
        preparedStatement.setDouble(4, quantity)
        preparedStatement.setDouble(5, purchasePrice)

        preparedStatement.executeUpdate()
        success = true
      } finally {
        preparedStatement.close()
      }
    } catch {
      case e: Exception =>
        println(s"Error adding asset: ${e.getMessage}")
      // Consider logging the exception or rethrowing it as a custom exception
    } finally {
      connection.close()
    }

    success
  }

  // Method to verify user credentials
  def verifyUser(email: String, password: String): Boolean = {
    val connection = getConnection()
    var preparedStatement: PreparedStatement = null
    var resultSet: ResultSet = null

    try {
      val sql = "SELECT password_hash FROM users WHERE email = ?"
      preparedStatement = connection.prepareStatement(sql)
      preparedStatement.setString(1, email)

      resultSet = preparedStatement.executeQuery()
      if (resultSet.next()) {
        val hashedPassword = resultSet.getString("password_hash")
        BCrypt.checkpw(password, hashedPassword) // Check password match
      } else {
        false
      }
    } catch {
      case e: Exception =>
        println(s"Error verifying user: ${e.getMessage}")
        false
    } finally {
      if (resultSet != null) resultSet.close()
      if (preparedStatement != null) preparedStatement.close()
      connection.close()
    }
  }
}
