/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    // embedded DB
    private static final String URL = "jdbc:derby:RPGDB;create=true";
    private static final String USER = "pdc";
    private static final String PASSWORD = "pdc";
    private static DatabaseManager manager;
    private Connection connection;
    
    // No class can create instance of DatabaseManager
    private DatabaseManager(){
        
    }
    
    // Opens a connection, opened if not opened and re-used if already opened.
    public synchronized Connection getConnection() throws SQLException{
        if (connection==null || connection.isClosed()){
            connection=DriverManager.getConnection(URL,USER,PASSWORD);
        }
        return connection;
    }
    
    // Creates DatabaseManager instance if it does not exist yet.
    // Prevents a thread slipping in and causing more than one instance to be created.
    public static synchronized DatabaseManager getManager(){
        if (manager==null){
            manager=new DatabaseManager();
        }
        return manager;
    }
}
