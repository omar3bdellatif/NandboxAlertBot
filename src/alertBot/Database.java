package alertBot;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
public class Database {
	
	public Connection connection = null;
	

	public Database(String dbName) throws SQLException
	{
		Connection connection = DriverManager.getConnection("jdbc:sqlite:"+dbName+".db");
		this.connection = connection;
	}
	
	public void createTable() throws SQLException
	{
		String sql = "CREATE TABLE IF NOT EXISTS alerts (\n"
                + "	chatId varchar(255),\n"
                + "	adminId varchar(255) NOT NULL,\n"
                + "	messageId varchar(255) NOT NULL,\n"
                + "	date varchar(255) NOT NULL,\n"
                + "	message varchar(255) NOT NULL,\n"
                + "	PRIMARY KEY(messageId)\n"
                + ");";
		Statement s = this.connection.createStatement();
		s.execute(sql);
	}
	
	
	
	public void insertAlert(String chatId,String adminId,String messageId,String date,String message) throws SQLException
	{
		String sql = "insert into alerts values (?,?,?,?,?)";
		
		PreparedStatement pstmt = this.connection.prepareStatement(sql);
        pstmt.setString(1, chatId);
        pstmt.setString(2, adminId);
        pstmt.setString(3, messageId);
        pstmt.setString(4, date);
        pstmt.setString(5, message);
        pstmt.executeUpdate();
	}
	
	public void deleteAlert(String messageId) throws SQLException
	{
		 String sql = "DELETE FROM alerts WHERE messageId = ?";
		
		PreparedStatement pstmt = this.connection.prepareStatement(sql);
        pstmt.setString(1, messageId);
        pstmt.executeUpdate();
	}
	
	public  ArrayList<ArrayList<String>> getAlertsByAdmin(String chatId,String adminId) throws SQLException
	{
		String sql = "Select message,messageId,date from alerts where chatId = ? and adminId = ?";
		PreparedStatement pstmt = this.connection.prepareStatement(sql);
		pstmt.setString(1, chatId);
        pstmt.setString(2, adminId);
        
        ResultSet rs = pstmt.executeQuery();
        ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
       
       while (rs.next()) {
    	   ArrayList<String> currentAlert = new ArrayList<String>();
    	   currentAlert.add(rs.getString("message"));
    	   currentAlert.add(rs.getString("messageId"));
    	   currentAlert.add(rs.getString("date"));
    	   result.add(currentAlert);
       }
       return result;
	}
	
	public boolean alertExists(String messageId) throws SQLException
	{
		String sql = "Select * from alerts where messageId = ?";
		PreparedStatement pstmt = this.connection.prepareStatement(sql);
		pstmt.setString(1, messageId);
        ResultSet rs = pstmt.executeQuery();

        if(rs.next())
        {
        	return true;
        }
        return false;
	}
}
