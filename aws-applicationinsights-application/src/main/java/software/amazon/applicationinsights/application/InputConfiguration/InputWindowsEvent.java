package software.amazon.applicationinsights.application.InputConfiguration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import software.amazon.applicationinsights.application.WindowsEvent;

import java.util.List;
import java.util.Objects;

/**
 * Data class for Windows Events
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class InputWindowsEvent {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String logGroupName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String eventName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> eventLevels;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String patternSet;

    public String getLogGroupName() {
        return logGroupName;
    }

    public void setLogGroupName(final String logGroupName) {
        this.logGroupName = logGroupName;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(final String eventName) {
        this.eventName = eventName;
    }

    public List<String> getEventLevels() {
        return eventLevels;
    }

    public void setEventLevels(final List<String> eventLevels) {
        this.eventLevels = eventLevels;
    }

    public String getPatternSet() {
        return patternSet;
    }

    public void setPatternSet(final String patternSet) {
        this.patternSet = patternSet;
    }

    public InputWindowsEvent(final WindowsEvent windowsEvent) {
        this.logGroupName = windowsEvent.getLogGroupName();
        this.eventName = windowsEvent.getEventName();
        this.eventLevels = windowsEvent.getEventLevels();
        this.patternSet = windowsEvent.getPatternSet();
    }

    public InputWindowsEvent() {
    }
}