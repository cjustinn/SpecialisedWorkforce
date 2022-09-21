package io.github.cjustinn.specialisedworkforce;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLManager {

    // Data members.
    private static Connection con;

    public static String host;
    public static String port;
    public static String user;
    public static String pass;
    public static String database;

    // Functions
    public static boolean Connect() {
        if (!IsConnected()) {
            try {
                con = DriverManager.getConnection(String.format("jdbc:mysql://%s:%s/%s", host, port, database), user, pass);

                return true;
            } catch(SQLException e) {
                return false;
            }
        } else return false;
    }

    public static boolean Disconnect() {
        if (IsConnected()) {
            try {
                con.close();
                return true;
            } catch(SQLException e) {
                return false;
            }
        } else return false;
    }

    public static boolean IsConnected() { return con != null; }

    public static Connection GetConnection() { return con; }

}
