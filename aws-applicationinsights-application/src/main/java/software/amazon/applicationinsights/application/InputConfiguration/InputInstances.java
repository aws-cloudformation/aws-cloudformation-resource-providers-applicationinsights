package software.amazon.applicationinsights.application.InputConfiguration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import software.amazon.applicationinsights.application.AlarmMetric;
import software.amazon.applicationinsights.application.Log;
import software.amazon.applicationinsights.application.SubComponentConfigurationDetails;
import software.amazon.applicationinsights.application.WindowsEvent;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Data class for InputInstances
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InputInstances {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<InputAlarmMetric> alarmMetrics;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<InputLog> logs;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<InputWindowsEvent> windowsEvents;

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

    public InputInstances(final SubComponentConfigurationDetails subComponentConfigurationDetails) {
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

    public InputInstances(
            final InputInstances recommendedInstances,
            final SubComponentConfigurationDetails defaultOverwriteSubComponentConfigurationDetails) {
        List<AlarmMetric> defaultOverwriteAlarmMetrics = defaultOverwriteSubComponentConfigurationDetails.getAlarmMetrics();
        if (defaultOverwriteAlarmMetrics != null) {
            List<InputAlarmMetric> inputAlarmMetrics = defaultOverwriteAlarmMetrics.stream()
                    .map(alarmMetric -> new InputAlarmMetric(alarmMetric))
                    .collect(Collectors.toList());
            this.alarmMetrics = inputAlarmMetrics;
        } else {
            this.alarmMetrics = recommendedInstances.getAlarmMetrics();
        }

        List<Log> defaultOverwriteLogs = defaultOverwriteSubComponentConfigurationDetails.getLogs();
        if (defaultOverwriteLogs != null) {
            List<InputLog> inputLogs = defaultOverwriteLogs.stream()
                    .map(log -> new InputLog(log))
                    .collect(Collectors.toList());
            this.logs = inputLogs;
        } else {
            this.logs = recommendedInstances.getLogs();
        }

        List<WindowsEvent> defaultOverwriteWindowsEvents = defaultOverwriteSubComponentConfigurationDetails.getWindowsEvents();
        if (defaultOverwriteWindowsEvents != null) {
            List<InputWindowsEvent> inputWindowsEvents = defaultOverwriteWindowsEvents.stream()
                    .map(windowsEvent -> new InputWindowsEvent(windowsEvent))
                    .collect(Collectors.toList());
            this.windowsEvents = inputWindowsEvents;
        } else {
            this.windowsEvents = recommendedInstances.getWindowsEvents();
        }
    }

    public InputInstances() {
    }
}