package main.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import main.exceptions.DataLoadException;


/* reads text files. */
public class FileReaderUtil {
    public List<String> readLines(String path) throws DataLoadException {
        try {
            return Files.readAllLines(Path.of(path));
        } catch (IOException e) {
            throw new DataLoadException("Could not read file: " + path, e);
        }
    }
    
    public boolean exists(String path) {
        return Files.exists(Path.of(path));
    }
}
