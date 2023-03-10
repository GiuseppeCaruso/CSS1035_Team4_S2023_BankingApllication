

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.sqlserver.jdbc.SQLServerException;

/**
 * Class implementing the connection with the database and it's used
 * functionalities The Database contains a table named Userpass, in which
 * different information are specified: 1.UserID, unique for each user 2.Balance
 * 3.Account type (e.g. checking or savings) 4.Withdrawals available if savings
 * type 5.overdraft counter if checking type 6.hash of the password inserted by
 * the user, used into the login procedure.
 * 
 * SECURE PRACTICE: In the database, is not stored the password itself, but an hash of it.
 * Then, after the user insert the password, the hash of this password is computed and compared
 * to the stored one.
 * Future implementation will provide a salt to harden the security and avoid some categories of attacks.
 */
public class database {

  private static String connectionUrl = "jdbc:sqlserver://sjubank.database.windows.net:1433;" 
      + "database=SJUbank;" 
      + "user=sjubank@sjubank;" 
      + "password=vERY@iNSECURE@pASSWORD;" 
      + "encrypt=true;" 
        + "trustServerCertificate=false;" 
          + "loginTimeout=30;";
  

  /**
   * function used to compute the MD5 hash over the password. This is then used to
   * check into login function with the data contained into the database
   * 
   * 
   * @param password
   * Password inserted by the user in the login phase.
   * 
   * @throws UnsupportedEncodingException
   * Exception thrown when the password inserted by the user is not supported by the encoding used into the program.
   * This is unlikely, since the UTF-8 Encoding is adopted.
   * 
   * @throws NoSuchAlgorithmException
   * Exception thrown in case the algorithm used to perform the digest into performing the login is not a valid one.
   * 
   * @return hashtext
   * Hash of the password inserted by the user. N.B. The stored value is never exposed.
   */
  public static String md5hash(String password) 
      throws UnsupportedEncodingException, NoSuchAlgorithmException {
	  
    String x = password;
    byte[] bytesOfMessage = x.getBytes("UTF-8");
    MessageDigest md = MessageDigest.getInstance("MD5");
    byte[] messageDigest = md.digest(bytesOfMessage);
    BigInteger no = new BigInteger(1, messageDigest);
    String hashtext = no.toString(16);
    while (hashtext.length() < 32) {
      hashtext = "0" + hashtext;
    }
    return hashtext;
  }
  
  /**
   * Method used to select hash in db.
   * SECURE PRACTICE: A prepared statement is used into quering the database, to avoid SQLInjection attacks.
   * 
   * @param username
   * Username inserted by the user into the login phase.
   * 
   * @throws SQLException
   * Exception thrown when the database is queried with a malformed statement.
   * 
   * @return result
   * Value stored into the db
   * 
   * @see SQLException
   * 
   * @see PreparedStatement
   */
  public static String selectData(String username) throws SQLException {
    String result = null;
    ResultSet resultSet = null;

    try (Connection connection = DriverManager.getConnection(connectionUrl); 
        Statement statement = connection.createStatement();) {

      // Create and execute a SELECT SQL statement.
      String selectSql = "SELECT Password FROM Userpass WHERE username = ?";
      PreparedStatement stmt = connection.prepareStatement(selectSql);
      stmt.setString(1, username);
      resultSet = stmt.executeQuery();
      resultSet.next();
      result = resultSet.getString(1);

    } catch (SQLException e) {
      System.out.println("Cannot find a result");
      return "";
    }
    return result;
  }

  /**
   * This function is used to perform the login, checking the data inserted by the
   * user with the one contained into the database. The MD5Hash is used to perform
   * this computation.
   * 
   * @param username
   * Username inserted by the user into the login phase.
   * 
   * @param password
   * Password inserted by the user into the login phase.
   * 
   * @return int
   * Binary value indicating if the login procedure proceeded correctly or not.
   * 
   * @throws UnsupportedEncodingException
   * Exception thrown when the password inserted by the user is not supported by the encoding used into the program.
   * This is unlikely, since the UTF-8 Encoding is adopted.
   * 
   * @throws NoSuchAlgorithmException
   * Exception thrown in case the algorithm used to perform the digest into performing the login is not a valid one.
   * 
   * @throws SQLException
   * Exception thrown in case of malformed SQL statement.
   * 
   * @see SQLException
   */
  public static int login(String username, String password, Shell shell)
      throws UnsupportedEncodingException, NoSuchAlgorithmException, SQLException {
    String uname = selectData(username);
    System.out.println(uname);
    String pwd = md5hash(password);
    System.out.println(pwd);
    if (!uname.isEmpty() && uname.trim().equals(pwd)) {
      return 1;
    } else {
    	MessageBox box = new MessageBox(shell, SWT.OK);
		box.setText("Error");
		box.setMessage("Your username/password is invalid.");
		box.open();
      return 0;
    }
  }

  /**
   * This method is used to retrieve the UserID of the user whose username is
   * being specified.
   * 
   * SECURE PRACTICE: A prepared statement is used into quering the database, to avoid SQLInjection attacks.
   * 
   * @param username
   * Username inserted by the user into the login phase.
   * 
   * @return result
   * Data retrieved from the database.
   * 
   * @throws SQLException
   * Exception thrown in case of a malformed SQL request.
   * 
   * @see SQLException
   * 
   * @see PreparedStatement
   */
  public static int checkUID(String username) throws SQLException {
    int result = 0;
    ResultSet resultSet = null;

    try (Connection connection = DriverManager.getConnection(connectionUrl); 
        Statement statement = connection.createStatement();) {

      // Create and execute a SELECT SQL statement.
      String selectSql = "SELECT UserID FROM Userpass WHERE username = ?";
      PreparedStatement stmt = connection.prepareStatement(selectSql);
      stmt.setString(1, username);
      resultSet = stmt.executeQuery();
      if (resultSet.next() == false) {
          System.out.println("ResultSet in empty in Java");
          return -1;
        }
      else {
      result = resultSet.getInt(1);
      }
      
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return result;
  }

  /**
   * Method used to retrieve the balance of the user whose username is specified
   * as parameter.
   * 
   * SECURE PRACTICE: A prepared statement is used into quering the database, to avoid SQLInjection attacks.
   * 
   * @param username
   * Username inserted by the user into the login phase.
   *
   * @throws SQLException
   * Exception thrown in case of a malformed SQL request.
   * 
   * @return result
   * REsult of the query to the database.
   * 
   * @see SQLException
   * 
   * @see PreparedStatement
   */
  public static double checkBalance(String username, String AccountType) throws SQLException {
    double result = 0;
    ResultSet resultSet = null;

    try (Connection connection = DriverManager.getConnection(connectionUrl); 
        Statement statement = connection.createStatement();) {

      // Create and execute a SELECT SQL statement.
    	String selectSql= null;
    	if(AccountType.equalsIgnoreCase("Checking")) {
    	 selectSql = "SELECT CheckingBalance FROM Userpass WHERE username = ?";
    	}
    	if(AccountType.equalsIgnoreCase("Savings")) {
       	 selectSql = "SELECT SavingsBalance FROM Userpass WHERE username = ?";
    	}
       	 PreparedStatement stmt = connection.prepareStatement(selectSql);
      stmt.setString(1, username);
      resultSet = stmt.executeQuery();
      resultSet.next();
      result = resultSet.getDouble(1);
      // Print results from select statement

    } catch (SQLException e) {
      e.printStackTrace();
    }
    return result;
  }

  /**
   * This method is used to update the balance column of the specified user into
   * the database.
   * 
   * SECURE PRACTICE: A prepared statement is used into quering the database, to avoid SQLInjection attacks.
   * 
   * @param username
   * Username of the user currently logged in.
   * 
   * @param balance
   * Future balance of the user that will be inserted into the database.
   * 
   * @throws SQLException
   * Exception thrown in case of a malformed SQL request.
   * 
   * @see SQLException
   * 
   * @see PreparedStatement
   */
  public static void updateSQLBalance(String username, double balance, String AccountType) throws SQLException {
String selectSql = null;
    try (Connection connection = DriverManager.getConnection(connectionUrl); 
        Statement statement = connection.createStatement();) {

    	if(AccountType.equalsIgnoreCase("Checking")) {
       	 selectSql = "UPDATE Userpass SET CheckingBalance = "+balance+" WHERE username = ?";
       	}
       	if(AccountType.equalsIgnoreCase("Savings")) {
       		selectSql = "UPDATE Userpass SET SavingsBalance = "+balance+" WHERE username = ?";
          	}
      PreparedStatement stmt = connection.prepareStatement(selectSql);
      stmt.setString(1, username);
      stmt.execute();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
  public static void updateTransactionHistory(String TransactionType, long Amount,int UID) throws SQLException {
	  String tableSelection = "transactionH" + UID;
	  LocalDate dateObj = LocalDate.now();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
      String date = dateObj.format(formatter);
	  
	  String selectSql = "INSERT INTO $tableName (TransactionDate, TransactionType, Amount) "+ "VALUES ("+"'"+date+"'"+", "+"'"+ TransactionType+"'"+", "+ Amount+");";
	  String query =selectSql.replace("$tableName",tableSelection);
	  try (Connection connection = DriverManager.getConnection(connectionUrl); 
	          Statement statement = connection.createStatement();) {
   
	      	statement.execute(query);
	       // PreparedStatement stmt = connection.prepareStatement(selectSql);
	        //stmt.setString(1, tableSelection);
	        //stmt.setString(2, date.toString());
	        //stmt.setString(3, TransactionType);
	        //stmt.setLong(4, Amount);
	        //stmt.execute();
	      } catch (SQLException e) {
	        e.printStackTrace();
	      }
	    }
  
public static int getRowCount() throws SQLException {
	try (Connection connection = DriverManager.getConnection(connectionUrl); 
	        Statement statement = connection.createStatement();) {
		String selectSql = "SELECT count(*) FROM Userpass";
		PreparedStatement stmt = connection.prepareStatement(selectSql);
		ResultSet rs = stmt.executeQuery();
		rs.next();
		int count = rs.getInt(1);
		return count;
	}
	
}
public int createAccount(String UserName, String Password, double CheckingBalance, double SavingsBalance, Shell shell) throws SQLException, UnsupportedEncodingException, NoSuchAlgorithmException {
    if(passwordRequirements(Password)!=1) {
    	return -1; // returns -1 if password does not meet requirements
    }
	if(checkUID(UserName)==-1) {
	

try (Connection connection = DriverManager.getConnection(connectionUrl); 
        Statement statement = connection.createStatement();) {
	String selectSql = "INSERT INTO Userpass (UserID, Username, Password,CheckingBalance,SavingsBalance) "
			+ "VALUES (?, ?, ?, ?, ?);";  
	PreparedStatement stmt = connection.prepareStatement(selectSql);
	stmt.setLong(1, getRowCount()+1);
	stmt.setString(2, UserName);
	stmt.setString(3, md5hash(Password));
	stmt.setDouble(4, CheckingBalance);
	stmt.setDouble(5, SavingsBalance);
	stmt.execute();
	int userid = getRowCount();
	String TableName= "transactionH"+ userid;
	String Statement ="CREATE TABLE " +TableName+ "(\r\n"
			+ "    TransactionDate varchar(255),\r\n"
			+ "    TransactionType varchar(255),\r\n"
			+ "    Amount float,\r\n"
			+ ");";
	PreparedStatement stmt1 = connection.prepareStatement(Statement);
	//stmt1.setString(1,UserName +getRowCount()+1+"transactionH"); // set new table name to naming schema {UserName}{UserID}transactionH
	
	stmt1.execute();
	
	MessageBox box = new MessageBox(shell, SWT.OK);
	box.setText("Success");
	box.setMessage("Account has been created please login.");
	box.open();
	return 1; // returns 1 on successful account creation
}
	}
	else {
		MessageBox box = new MessageBox(shell, SWT.OK);
		box.setText("Error");
		box.setMessage("Account already exists.");
		box.open();
		return 0; // returns 0 if account exists
	}
}
private static int passwordRequirements(String Password) {
		String str = Password;
		int specials = 0, digits = 0, letters = 0, satisfied = 0;
		while(satisfied == 0) {
			for (int i = 0; i < str.length(); ++i) {
				   char ch = str.charAt(i);
				   if (!Character.isDigit(ch) && !Character.isLetter(ch) && !Character.isWhitespace(ch)) {
				      ++specials;
				   } else if (Character.isDigit(ch)) {
				      ++digits;
				   } else {
				      ++letters;
				   }
			}
			if(letters >= 8 && digits >= 1 && specials >= 1) {
				satisfied = 1;
			return satisfied;
			}
			else {
				System.out.println("Password needs to respect the following criteria:");
				System.out.println("1. At least 8 letters");
				System.out.println("2. At least 1 digit");
				System.out.println("3. At least 1 special char");
				return satisfied;
			}
		}
		return satisfied;
	}
}