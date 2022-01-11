package software.amazon.applicationinsights.application.InputConfiguration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Objects;
import lombok.Getter;
import lombok.Setter;
import software.amazon.applicationinsights.application.Log;

/**
 * Data class for InputLog
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InputLog {

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String logGroupName;

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String logPath;

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String logType;

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String encoding;

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String patternSet;

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