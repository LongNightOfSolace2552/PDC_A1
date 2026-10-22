/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main.persistence;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import main.domain.world.Node;
import main.domain.world.NodeLocation;
import main.exceptions.DataLoadException;

public class NodeDatabaseRepository {
    private final Connection connection;
    
    public NodeDatabaseRepository(Connection connection){
        this.connection=connection;
    }
    
    private static final String SELECT_ALL_NODES="SELECT N.ID AS NODE_ID, N.DIFFICULTY, N.HAS_BOSS, L.ID AS LOC_ID, L.NAME, L.DESCRIPTION FROM NODE N JOIN NODE_LOCATION L ON N.LOCATION_ID = L.ID ORDER BY N.DIFFICULTY";
    
    // Builds node that contains NodeLocation object
    private Node buildNode(ResultSet rs) throws SQLException{
        NodeLocation location=new NodeLocation(rs.getString("LOC_ID"),rs.getString("NAME"),rs.getString("DESCRIPTION"));
        return new Node(rs.getString("NODE_ID"),location,rs.getInt("DIFFICULTY"),rs.getBoolean("HAS_BOSS"));
    }
    
    // Builds node with buildNode and adds them in nodes list
    public List<Node> loadNodes() throws DataLoadException{
        List<Node> nodes=new ArrayList<>();
        try(Statement statement=connection.createStatement();
            ResultSet rs=statement.executeQuery(SELECT_ALL_NODES)){
            while(rs.next()){
                nodes.add(buildNode(rs));
            }
        }catch(SQLException e){
            throw new DataLoadException("Loading nodes from database failed.",e);
        }
        return nodes;
    }
}
