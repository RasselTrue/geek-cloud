import java.sql.*;

public class DataBaseService {
    private static Connection connection;
    private static Statement stmt;

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:users.db");
            stmt = connection.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getLoginAndCheckPass(String login, String pass) {
        try {
            ResultSet rs = stmt.executeQuery(
                    "SELECT login FROM main WHERE login = '" + login +"' AND password = '" + pass + "'"
            );
            if (rs.next()) {
                return rs.getString("login");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
