package db.mysql;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;

public class MySQLTableCreation {
	// Run this as Java application to reset db schema.
	public static void main(String[] args) {
		try {
			// Ensure the driver is registered.
			//Class.forName("com.mysql.jdbc.Driver").newInstance();
			// This is java.sql.Connection. Not com.mysql.jdbc.Connection.
			Connection conn = null;

			// Step 1 Connect to MySQL.
			try {
				System.out.println("Connecting to " + MySQLDBUtil.URL);
				Class.forName("com.mysql.cj.jdbc.Driver").getConstructor().newInstance(); //把“com.mysql.jdbc.Driver”加到DriverManager这个ArrayList里去
				conn = DriverManager.getConnection(MySQLDBUtil.URL); //遍历所有可用的driver，找到“com.mysql.jdbc.Driver”这个用来建立连接的class
				//to set up connection，JDBC的要求写法
			} catch (SQLException e) {
				e.printStackTrace();
				//System.out.println("SQLException " + e.getMessage());
				//System.out.println("SQLState " + e.getSQLState());
				//System.out.println("VendorError " + e.getErrorCode());
			}
			if (conn == null) {
				return;
			}

			
			// Step 2 Drop tables in case they exist.
			Statement stmt = conn.createStatement();
			String sql = "DROP TABLE IF EXISTS categories";  //foreign key声明在哪个table就先删这个table
			stmt.executeUpdate(sql);
						
			sql = "DROP TABLE IF EXISTS history";  //the order of dropping the table matters
			stmt.executeUpdate(sql);
						
			sql = "DROP TABLE IF EXISTS items";
			stmt.executeUpdate(sql);
						
			sql = "DROP TABLE IF EXISTS users";
			stmt.executeUpdate(sql);
			
			// Step 3  Create tables
			sql = "CREATE TABLE items " + "(item_id VARCHAR(255) NOT NULL, " + " name VARCHAR(255), " + "rating FLOAT,"
					+ "address VARCHAR(255), " + "image_url VARCHAR(255), " + "url VARCHAR(255), " + "distance FLOAT, "
					+ " PRIMARY KEY ( item_id ))";  //把MySQL语句拼成一个完整string
			stmt.executeUpdate(sql); //return int 1:success 0:failure

			sql = "CREATE TABLE categories " + "(item_id VARCHAR(255) NOT NULL, " + " category VARCHAR(255) NOT NULL, "
					+ " PRIMARY KEY ( item_id, category), " + "FOREIGN KEY (item_id) REFERENCES items(item_id))";
			stmt.executeUpdate(sql);

			sql = "CREATE TABLE users " + "(user_id VARCHAR(255) NOT NULL, " + " password VARCHAR(255) NOT NULL, "
					+ " first_name VARCHAR(255), last_name VARCHAR(255), " + " PRIMARY KEY ( user_id ))";
			stmt.executeUpdate(sql);

			sql = "CREATE TABLE history " + "(user_id VARCHAR(255) NOT NULL , " + " item_id VARCHAR(255) NOT NULL, "
					+ "last_favor_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, " + " PRIMARY KEY (user_id, item_id),"
					+ "FOREIGN KEY (item_id) REFERENCES items(item_id),"
					+ "FOREIGN KEY (user_id) REFERENCES users(user_id))";
			stmt.executeUpdate(sql);

			// Step 4: insert a fake user 3229c1097c00d497a0fd282d586be050  encoded password  ensure security of DB  用户密码加密是在前端JS实现
			//   \ in Java 使“ ”就显示为单纯的字符串
			sql = "INSERT INTO users VALUES (\"1111\", \"3229c1097c00d497a0fd282d586be050\", \"John\", \"Smith\")";
			stmt.executeUpdate(sql);
			
			System.out.println("Import is done successfully.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

}
