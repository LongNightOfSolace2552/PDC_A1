/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import main.domain.combat.Boss;
import main.domain.combat.Enemy;
import main.domain.combat.EnemyLoot;
import main.domain.player.Stats;
import main.exceptions.DataLoadException;

public class EnemyDatabaseRepository {
    
    private static final String MONSTER_TYPE="MONSTER";
    private static final String BOSS_TYPE="BOSS";
    private final Connection connection;
    
    private static final String SELECT_BY_TYPE ="SELECT NODE_ID, ID, NAME, STRENGTH, MAGIC, AGILITY, STAT_DROP_CHANCE, STAT_DROP_MIN, STAT_DROP_MAX, ITEM_DROP_CHANCE, ITEM_DROP_ID FROM ENEMY WHERE ENEMY_TYPE = ? ORDER BY NODE_ID, ID";
    
    public EnemyDatabaseRepository(Connection connection){
        this.connection=connection;
    }
    
    private Enemy buildEnemy(ResultSet rs) throws SQLException {
        return new Enemy(rs.getString("ID"), rs.getString("NAME"), buildStats(rs), buildLoot(rs));
    }

    private Boss buildBoss(ResultSet rs) throws SQLException {
        return new Boss(rs.getString("ID"), rs.getString("NAME"), buildStats(rs), buildLoot(rs));
    }

    private Stats buildStats(ResultSet rs) throws SQLException {
        return new Stats(rs.getInt("STRENGTH"), rs.getInt("MAGIC"), rs.getInt("AGILITY"));
    }

    private EnemyLoot buildLoot(ResultSet rs) throws SQLException {
        return new EnemyLoot(rs.getDouble("STAT_DROP_CHANCE"),rs.getInt("STAT_DROP_MIN"),rs.getInt("STAT_DROP_MAX"),rs.getDouble("ITEM_DROP_CHANCE"),rs.getString("ITEM_DROP_ID"));
    }
    
    public Map<String, List<Enemy>> loadEnemyPoolsByNode() throws DataLoadException {
        Map<String, List<Enemy>> pools = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_TYPE)) {
            ps.setString(1, MONSTER_TYPE);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nodeId = rs.getString("NODE_ID");
                    pools.computeIfAbsent(nodeId, key -> new ArrayList<>()).add(buildEnemy(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataLoadException("Loading enemies from database failed.", e);
        }
        return pools;
    }    
    
    public Map<String, Boss> loadBosses() throws DataLoadException {
        Map<String, Boss> bosses = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(SELECT_BY_TYPE)) {
            ps.setString(1, BOSS_TYPE);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nodeId = rs.getString("NODE_ID");
                    bosses.put(nodeId, buildBoss(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataLoadException("Loading bosses from database failed.", e);
        }
        return bosses;
    }    
}
