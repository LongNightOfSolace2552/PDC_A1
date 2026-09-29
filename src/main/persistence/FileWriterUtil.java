package main.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import main.exceptions.SaveDataException;

/*
writes text files.
PlayerFileRepository writes through this class
*/
public class FileWriterUtil {
    public void writeLines(String path, List<String> lines) throws SaveDataException {
        try {
            /* converts the file path into text */
            Path target = Path.of(path);
            /*
            a safety condition where it checks to see if the file is 
            supposed to go inside a folder. If it is, creates all necessary 
            folders before trying to create the file
            */
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            
            /* write to file (Saving to file) */
            Files.write(target, lines);
        } catch (IOException e) {
            /* error handling */
            throw new SaveDataException("Could not write file: " + path, e);
        }
    }
}
