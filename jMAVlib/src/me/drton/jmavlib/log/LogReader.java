package src.me.drton.jmavlib.log;

import java.io.IOException;
import java.util.Map;

/**
 * User: ton Date: 03.06.13 Time: 17:45
 */
public interface LogReader {
    void close() throws IOException;

    boolean seek(long time) throws IOException, FormatErrorException;

    long readUpdate(Map<String, Object> update) throws IOException, FormatErrorException;

    Map<String, String> getFields();

    String getFormat();

    long getSizeUpdates();

    long getStartMicroseconds();

    long getSizeMicroseconds();

    Map<String, Object> getVersion();

    Map<String, Object> getParameters();
}
