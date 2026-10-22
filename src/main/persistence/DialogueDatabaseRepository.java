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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import main.domain.combat.DialogueTemplate;
import main.domain.combat.DialogueTemplate.StatState;
import main.exceptions.DataLoadException;

public class DialogueDatabaseRepository {
    
    private static final String SELECT_ALL_TEMPLATES="SELECT STAT_STATE, TEMPLATE_TEXT FROM DIALOGUE_TEMPLATE ORDER BY ID";
    private final Connection connection;
    
    public DialogueDatabaseRepository(Connection connection){
        this.connection=connection;
    }

    private StatState parseStatState(String value) throws DataLoadException {
        try {
            return StatState.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new DataLoadException("Unknown stat state: "+value, e);
        }
    }
    
    public Map<StatState, List<DialogueTemplate>> loadDialogueTemplates() throws DataLoadException {
        Map<StatState, List<DialogueTemplate>> templatesByState = new HashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(SELECT_ALL_TEMPLATES)) {
            while (rs.next()) {
                StatState statState = parseStatState(rs.getString("STAT_STATE"));
                String template = rs.getString("TEMPLATE_TEXT");
                templatesByState.computeIfAbsent(statState, key -> new ArrayList<>()).add(new DialogueTemplate(statState, template));
            }
        } catch (SQLException e) {
            throw new DataLoadException("Loading dialogue templates from database failed.", e);
        }
        return templatesByState;
    }
}
