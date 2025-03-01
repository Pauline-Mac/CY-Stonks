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
  def insertUser(name: String, email: String, password: String): Option[Int] = {
    val connection = getConnection()
    var checkStatement: PreparedStatement = null
    var insertStatement: PreparedStatement = null
    var resultSet: ResultSet = null
    var userId: Option[Int] = None

    try {
      // Vérifier si l'utilisateur existe déjà
      val checkSql = "SELECT COUNT(1) FROM users WHERE email = ? OR username = ?"
      checkStatement = connection.prepareStatement(checkSql)
      checkStatement.setString(1, email)
      checkStatement.setString(2, name)
      resultSet = checkStatement.executeQuery()

      if (resultSet.next() && resultSet.getInt(1) > 0) {
        println("User with the same email or username already exists.")
      } else {
        // Hasher le mot de passe avec BCrypt
        val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())

        // Insérer le nouvel utilisateur et récupérer son ID
        val insertSql = "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?) RETURNING user_id"
        insertStatement = connection.prepareStatement(insertSql)
        insertStatement.setString(1, name)
        insertStatement.setString(2, email)
        insertStatement.setString(3, passwordHash)

        val insertResult = insertStatement.executeQuery()
        if (insertResult.next()) {
          userId = Some(insertResult.getInt("user_id"))
          println(s"User successfully added with user_id = ${userId.get}")
        }
      }
    } catch {
      case e: Exception =>
        println(s"Error adding user: ${e.getMessage}")
    } finally {
      if (resultSet != null) resultSet.close()
      if (checkStatement != null) checkStatement.close()
      if (insertStatement != null) insertStatement.close()
      connection.close()
    }

    userId // Retourne Some(user_id) si l'insertion a réussi, sinon None
  }

  // Method to create a portfolio for a user
  def createPortfolio(userId: Int, portfolioName: String): Option[Int] = {
    var portfolioId: Option[Int] = None
    val checkSql = "SELECT portfolio_id FROM portfolios WHERE user_id = ? AND portfolio_name = ?"
    val insertSql = "INSERT INTO portfolios (user_id, portfolio_name) VALUES (?, ?)"

    val connection: Connection = getConnection()
    try {
      // Vérifier si le portfolio existe déjà
      val checkStatement: PreparedStatement = connection.prepareStatement(checkSql)
      try {
        checkStatement.setInt(1, userId)
        checkStatement.setString(2, portfolioName)
        val resultSet: ResultSet = checkStatement.executeQuery()
        try {
          if (resultSet.next()) {
            println("Portfolio already exists")
            return None // Si un portfolio existe déjà, on retourne None directement
          }
        } finally {
          resultSet.close()
        }
      } finally {
        checkStatement.close()
      }

      // Insérer le portfolio s'il n'existe pas encore
      val insertStatement: PreparedStatement = connection.prepareStatement(insertSql, java.sql.Statement.RETURN_GENERATED_KEYS)
      try {
        insertStatement.setInt(1, userId)
        insertStatement.setString(2, portfolioName)
        insertStatement.executeUpdate()

        val generatedKeys: ResultSet = insertStatement.getGeneratedKeys
        try {
          if (generatedKeys.next()) {
            portfolioId = Some(generatedKeys.getInt(1))
          }
        } finally {
          generatedKeys.close()
        }
      } finally {
        insertStatement.close()
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
