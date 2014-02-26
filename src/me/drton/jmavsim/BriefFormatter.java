package me.drton.jmavsim;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class BriefFormatter extends Formatter {
    // Buffer for formatting a LogRecord

    private StringBuffer stringBuffer = new StringBuffer();

    // The character that is used to separate lines.
    // It's best to use this value instead of assuming that \n is
    // the line separator
    private String lineSeparator = System.getProperty("line.separator");

    // Synchronized because the StringBuffers are shared
    synchronized public String format(LogRecord record) {
        // Initialize the buffers
        stringBuffer.setLength(0);

        // Append the log message -- call formatMessage()
        // to format the message itself
        String message = formatMessage(record);
        stringBuffer.append(message);

        // Add a newline
        stringBuffer.append(lineSeparator);

        return stringBuffer.toString();
    }
}
