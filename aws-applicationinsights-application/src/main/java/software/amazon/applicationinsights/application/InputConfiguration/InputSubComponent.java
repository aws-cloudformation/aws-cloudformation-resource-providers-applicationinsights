package software.amazon.applicationinsights.application.InputConfiguration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import software.amazon.applicationinsights.application.AlarmMetric;
import software.amazon.applicationinsights.application.Log;
import software.amazon.applicationinsights.application.SubComponentConfigurationDetails;
import software.amazon.applicationinsights.application.SubComponentTypeConfiguration;
import software.amazon.applicationinsights.application.WindowsEvent;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Data class for InputSubComponent
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InputSubComponent {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String subComponentType;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<InputAlarmMetric> alarmMetrics;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<InputLog> logs;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<InputWindowsEvent> windowsEvents;

    public String getSubComponentType() {
        return subComponentType;
    }

    public void setSubComponentType(final String subComponentType) {
        this.subComponentType = subComponentType;
    }

    public List<InputAlarmMetric> getAlarmMetrics() {
        return alarmMetrics;
    }

    public void setAlarmMetrics(final List<InputAlarmMetric> alarmMetrics) {
        this.alarmMetrics = alarmMetrics;
    }

    public List<InputLog> getLogs() {
        return logs;
    }

    public void setLogs(final List<InputLog> logs) {
        this.logs = logs;
    }

    public List<InputWindowsEvent> getWindowsEvents() {
        return windowsEvents;
    }

    public void setWindowsEvents(final List<InputWindowsEvent> windowsEvents) {
        this.windowsEvents = windowsEvents;
    }

    public InputSubComponent(final SubComponentTypeConfiguration subComponentTypeConfiguration) {
        this.subComponentType = subComponentTypeConfiguration.getSubComponentType();

        SubComponentConfigurationDetails subComponentConfigurationDetails =
            subComponentTypeConfiguration.getSubComponentConfigurationDetails();

        List<AlarmMetric> alarmMetrics = subComponentConfigurationDetails.getAlarmMetrics();
        if (alarmMetrics != null && !alarmMetrics.isEmpty()) {
            List<InputAlarmMetric> inputAlarmMetrics = alarmMetrics.stream()
                .map(alarmMetric -> new InputAlarmMetric(alarmMetric))
                .collect(Collectors.toList());
            this.alarmMetrics = inputAlarmMetrics;
        }

        List<Log> logs = subComponentConfigurationDetails.getLogs();
        if (logs != null && !logs.isEmpty()) {
            List<InputLog> inputLogs = logs.stream()
                .map(log -> new InputLog(log))
                .collect(Collectors.toList());
            this.logs = inputLogs;
        }

        List<WindowsEvent> windowsEvents = subComponentConfigurationDetails.getWindowsEvents();
        if (windowsEvents != null && !windowsEvents.isEmpty()) {
            List<InputWindowsEvent> inputWindowsEvents = windowsEvents.stream()
                .map(windowsEvent -> new InputWindowsEvent(windowsEvent))
                .collect(Collectors.toList());
            this.windowsEvents = inputWindowsEvents;
        }
    }

    public InputSubComponent(
        final InputSubComponent recommendedSubComponent,
        final SubComponentTypeConfiguration defaultOverwriteSubComponentConfiguration) {

        if (!recommendedSubComponent.getSubComponentType().equals(defaultOverwriteSubComponentConfiguration.getSubComponentType())) {
            return;
        }

        this.subComponentType = recommendedSubComponent.getSubComponentType();

        SubComponentConfigurationDetails defaultOverwriteSubComponentConfigurationDetails =
            defaultOverwriteSubComponentConfiguration.getSubComponentConfigurationDetails();

        List<AlarmMetric> defaultOverwriteAlarmMetrics = defaultOverwriteSubComponentConfigurationDetails.getAlarmMetrics();
        if (defaultOverwriteAlarmMetrics != null) {
            List<InputAlarmMetric> inputAlarmMetrics = defaultOverwriteAlarmMetrics.stream()
                .map(alarmMetric -> new InputAlarmMetric(alarmMetric))
                .collect(Collectors.toList());
            this.alarmMetrics = inputAlarmMetrics;
        } else {
            this.alarmMetrics = recommendedSubComponent.getAlarmMetrics();
        }

        List<Log> defaultOverwriteLogs = defaultOverwriteSubComponentConfigurationDetails.getLogs();
        if (defaultOverwriteLogs != null) {
            List<InputLog> inputLogs = defaultOverwriteLogs.stream()
                .map(log -> new InputLog(log))
                .collect(Collectors.toList());
            this.logs = inputLogs;
        } else {
            this.logs = recommendedSubComponent.getLogs();
        }

        List<WindowsEvent> defaultOverwriteWindowsEvents = defaultOverwriteSubComponentConfigurationDetails.getWindowsEvents();
        if (defaultOverwriteWindowsEvents != null) {
            List<InputWindowsEvent> inputWindowsEvents = defaultOverwriteWindowsEvents.stream()
                .map(windowsEvent -> new InputWindowsEvent(windowsEvent))
                .collect(Collectors.toList());
            this.windowsEvents = inputWindowsEvents;
        } else {
            this.windowsEvents = recommendedSubComponent.getWindowsEvents();
        }
    }

    public InputSubComponent() {
    }
}
