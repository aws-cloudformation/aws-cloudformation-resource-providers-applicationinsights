package software.amazon.applicationinsights.application.InputConfiguration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import software.amazon.applicationinsights.application.WindowsEvent;

import java.util.List;
import java.util.Objects;

/**
 * Data class for Windows Events
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class InputWindowsEvent {

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String logGroupName;

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String eventName;

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> eventLevels;

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String patternSet;

    public InputWindowsEvent(final WindowsEvent windowsEvent) {
        this.logGroupName = windowsEvent.getLogGroupName();
        this.eventName = windowsEvent.getEventName();
        this.eventLevels = windowsEvent.getEventLevels();
        this.patternSet = windowsEvent.getPatternSet();
    }

    public InputWindowsEvent() {
    }
}