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
  def insertUser(name: String, email: String, password: String): Unit = {
    val connection = getConnection()
    var preparedStatement: PreparedStatement = null

    try {
      // Hash the password using BCrypt
      val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())

      // Using prepared statement to prevent SQL injection
      val sql = "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)"
      preparedStatement = connection.prepareStatement(sql)
      preparedStatement.setString(1, name)
      preparedStatement.setString(2, email)
      preparedStatement.setString(3, passwordHash)

      preparedStatement.executeUpdate()
    } catch {
      case e: Exception => println(s"Error adding user: ${e.getMessage}")
    } finally {
      if (preparedStatement != null) preparedStatement.close()
      connection.close()
    }
  }

  // Method to create a portfolio for a user
  def createPortfolio(userId: Int, portfolioName: String): Int = {
    val connection = getConnection()
    var preparedStatement: PreparedStatement = null
    var generatedKeys: ResultSet = null
    var portfolioId = 0

    try {
      val sql = "INSERT INTO portfolios (user_id, portfolio_name) VALUES (?, ?)"
      preparedStatement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)
      preparedStatement.setInt(1, userId)
      preparedStatement.setString(2, portfolioName)

      preparedStatement.executeUpdate()
      generatedKeys = preparedStatement.getGeneratedKeys

      if (generatedKeys.next()) {
        portfolioId = generatedKeys.getInt(1)
      }
    } catch {
      case e: Exception => println(s"Error creating portfolio: ${e.getMessage}")
    } finally {
      if (generatedKeys != null) generatedKeys.close()
      if (preparedStatement != null) preparedStatement.close()
      connection.close()
    }

    portfolioId
  }

  // Method to add an asset to a portfolio
  def addAssetToPortfolio(portfolioId: Int, assetType: String, assetSymbol: String, quantity: Double, purchasePrice: Double): Unit = {
    val connection = getConnection()
    var preparedStatement: PreparedStatement = null

    try {
      val sql = "INSERT INTO assets (portfolio_id, asset_type, asset_symbol, quantity, purchase_price) VALUES (?, ?, ?, ?, ?)"
      preparedStatement = connection.prepareStatement(sql)
      preparedStatement.setInt(1, portfolioId)
      preparedStatement.setString(2, assetType)
      preparedStatement.setString(3, assetSymbol)
      preparedStatement.setDouble(4, quantity)
      preparedStatement.setDouble(5, purchasePrice)

      preparedStatement.executeUpdate()
    } catch {
      case e: Exception => println(s"Error adding asset: ${e.getMessage}")
    } finally {
      if (preparedStatement != null) preparedStatement.close()
      connection.close()
    }
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
