package db.mysql;
//contains MySQL version of DBConnection implementation.

public class MySQLDBUtil {
	private static final String HOSTNAME = "localhost";
	private static final String PORT_NUM = "3307"; // my current mysql port number //on aws it's 3306
	public static final String DB_NAME = "eventProject";
	private static final String USERNAME = "root";
	private static final String PASSWORD = "root";
	public static final String URL = "jdbc:mysql://" + HOSTNAME + ":" + PORT_NUM + "/" + DB_NAME
	+ "?user=" + USERNAME + "&password=" + PASSWORD + "&autoReconnect=true";

}
