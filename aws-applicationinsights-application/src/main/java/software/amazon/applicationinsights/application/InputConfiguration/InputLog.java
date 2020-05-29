package software.amazon.applicationinsights.application.InputConfiguration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Objects;
import software.amazon.applicationinsights.application.Log;

/**
 * Data class for InputLog
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InputLog {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String logGroupName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String logPath;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String logType;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String encoding;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String patternSet;

    public String getLogGroupName() {
        return logGroupName;
    }

    public void setLogGroupName(final String logGroupName) {
        this.logGroupName = logGroupName;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(final String logPath) {
        this.logPath = logPath;
    }

    public String getLogType() {
        return logType;
    }

    public void setLogType(final String logType) {
        this.logType = logType;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(final String encoding) {
        this.encoding = encoding;
    }

    public String getPatternSet() {
        return patternSet;
    }

    public void setPatternSet(final String patternSet) {
        this.patternSet = patternSet;
    }

    public InputLog(final Log log) {
        this.logGroupName = log.getLogGroupName();
        this.logPath = log.getLogPath();
        this.logType = log.getLogType();
        this.encoding = log.getEncoding();
        this.patternSet = log.getPatternSet();
    }

    public InputLog() {
    }
}