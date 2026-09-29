package main.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import main.domain.combat.DialogueTemplate;
import main.domain.combat.DialogueTemplate.StatState;
import main.exceptions.DataLoadException;

public class DialogueFileRepository {
    private static final String DIALOGUE_TEMPLATES_PATH = "data/dialogue_templates.txt";
    private static final String FIELD_SEPARATOR = "\\|";

    private final FileReaderUtil fileReaderUtil;

    public DialogueFileRepository(FileReaderUtil fileReaderUtil) {
        this.fileReaderUtil = fileReaderUtil;
    }

    /*
    reads dialogue_templates.txt and adds each dialogue into the map
    templatesByState. the keys are the StatState enum values, and each
    key maps to the list of DialogueTemplate objects for that state.
    */
    public Map<StatState, List<DialogueTemplate>> loadDialogueTemplates() throws DataLoadException {
        Map<StatState, List<DialogueTemplate>> templatesByState = new HashMap<>();
        for (String line : readDataLines()) {
            String[] fields = line.split(FIELD_SEPARATOR, 2);
            if (fields.length != 2) {
                throw new DataLoadException("Malformed line in " + DIALOGUE_TEMPLATES_PATH + ": " + line);
            }

            StatState statState = parseStatState(fields[0].trim());
            String template = fields[1].trim();

            templatesByState.computeIfAbsent(statState, key -> new ArrayList<>()).add(new DialogueTemplate(statState, template));
        }
        return templatesByState;
    }

    private StatState parseStatState(String value) throws DataLoadException {
        try {
            return StatState.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new DataLoadException("Unknown stat state '" + value + "' in " + DIALOGUE_TEMPLATES_PATH, e);
        }
    }

    /*
    reads the lines in dialogue_templates.txt, ignoring comments and
    empty lines.
    */
    private List<String> readDataLines() throws DataLoadException {
        List<String> result = new ArrayList<>();
        for (String line : fileReaderUtil.readLines(DIALOGUE_TEMPLATES_PATH)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            result.add(trimmed);
        }
        return result;
    }
}
