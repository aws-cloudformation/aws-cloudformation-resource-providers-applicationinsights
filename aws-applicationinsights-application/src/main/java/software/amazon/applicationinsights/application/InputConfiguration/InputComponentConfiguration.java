package software.amazon.applicationinsights.application.InputConfiguration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import software.amazon.applicationinsights.application.Alarm;
import software.amazon.applicationinsights.application.AlarmMetric;
import software.amazon.applicationinsights.application.ComponentConfiguration;
import software.amazon.applicationinsights.application.ConfigurationDetails;
import software.amazon.applicationinsights.application.Log;
import software.amazon.applicationinsights.application.SubComponentConfigurationDetails;
import software.amazon.applicationinsights.application.SubComponentTypeConfiguration;
import software.amazon.applicationinsights.application.WindowsEvent;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Data class for InputComponentConfiguration
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InputComponentConfiguration {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<InputAlarmMetric> alarmMetrics;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<InputLog> logs;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<InputWindowsEvent> windowsEvents;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private InputInstances instances;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<InputAlarm> alarms;

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

    public InputInstances getInstances() {
        return this.instances;
    }

    public void setInstances(final InputInstances instances) {
        this.instances = instances;
    }

    public List<InputAlarm> getAlarms() {
        return alarms;
    }

    public void setAlarms(final List<InputAlarm> alarms) {
        this.alarms = alarms;
    }

    public InputComponentConfiguration(final ComponentConfiguration componentConfiguration) {
        ConfigurationDetails configurationDetails =
                componentConfiguration.getConfigurationDetails() == null ?
                        new ConfigurationDetails() : componentConfiguration.getConfigurationDetails();

        List<AlarmMetric> alarmMetrics = configurationDetails.getAlarmMetrics();
        if (alarmMetrics != null && !alarmMetrics.isEmpty()) {
            List<InputAlarmMetric> inputAlarmMetrics = alarmMetrics.stream()
                    .map(alarmMetric -> new InputAlarmMetric(alarmMetric))
                    .collect(Collectors.toList());
            this.alarmMetrics = inputAlarmMetrics;
        }

        List<Log> logs = configurationDetails.getLogs();
        if (logs != null && !logs.isEmpty()) {
            List<InputLog> inputLogs = logs.stream()
                    .map(log -> new InputLog(log))
                    .collect(Collectors.toList());
            this.logs = inputLogs;
        }

        List<WindowsEvent> windowsEvents = configurationDetails.getWindowsEvents();
        if (windowsEvents != null && !windowsEvents.isEmpty()) {
            List<InputWindowsEvent> inputWindowsEvents = windowsEvents.stream()
                    .map(windowsEvent -> new InputWindowsEvent(windowsEvent))
                    .collect(Collectors.toList());
            this.windowsEvents = inputWindowsEvents;
        }

        List<Alarm> alarms = configurationDetails.getAlarms();
        if (alarms != null && !alarms.isEmpty()) {
            List<InputAlarm> inputAlarms = alarms.stream()
                    .map(alarm -> new InputAlarm(alarm))
                    .collect(Collectors.toList());
            this.alarms = inputAlarms;
        }

        List<SubComponentTypeConfiguration> subComponentTypeConfigurations =
                componentConfiguration.getSubComponentTypeConfigurations();

        if (subComponentTypeConfigurations != null && !subComponentTypeConfigurations.isEmpty()) {
            for (SubComponentTypeConfiguration subComponentTypeConfiguration : subComponentTypeConfigurations) {
                if (subComponentTypeConfiguration.getSubComponentType() == "EC2_INSTANCE") {
                    InputInstances inputInstances = new InputInstances(
                            subComponentTypeConfiguration.getSubComponentConfigurationDetails());
                    this.instances = inputInstances;
                }
            }
        }
    }

    public InputComponentConfiguration(
            final InputComponentConfiguration recommendedInputConfig,
            final ComponentConfiguration defaultOverwriteComponentConfiguration) {
        ConfigurationDetails defaultOverwriteConfigurationDetails =
                defaultOverwriteComponentConfiguration.getConfigurationDetails() == null ?
                        new ConfigurationDetails() : defaultOverwriteComponentConfiguration.getConfigurationDetails();

        List<AlarmMetric> defaultOverwriteAlarmMetrics = defaultOverwriteConfigurationDetails.getAlarmMetrics();
        if (defaultOverwriteAlarmMetrics != null) {
            List<InputAlarmMetric> inputAlarmMetrics = defaultOverwriteAlarmMetrics.stream()
                    .map(alarmMetric -> new InputAlarmMetric(alarmMetric))
                    .collect(Collectors.toList());
            this.alarmMetrics = inputAlarmMetrics;
        } else {
            this.alarmMetrics = recommendedInputConfig.getAlarmMetrics();
        }

        List<Log> defaultOverwriteLogs = defaultOverwriteConfigurationDetails.getLogs();
        if (defaultOverwriteLogs != null) {
            List<InputLog> inputLogs = defaultOverwriteLogs.stream()
                    .map(log -> new InputLog(log))
                    .collect(Collectors.toList());
            this.logs = inputLogs;
        } else {
            this.logs = recommendedInputConfig.getLogs();
        }

        List<WindowsEvent> defaultOverwriteWindowsEvents = defaultOverwriteConfigurationDetails.getWindowsEvents();
        if (defaultOverwriteWindowsEvents != null) {
            List<InputWindowsEvent> inputWindowsEvents = defaultOverwriteWindowsEvents.stream()
                    .map(windowsEvent -> new InputWindowsEvent(windowsEvent))
                    .collect(Collectors.toList());
            this.windowsEvents = inputWindowsEvents;
        } else {
            this.windowsEvents = recommendedInputConfig.getWindowsEvents();
        }

        List<Alarm> defaultOverwriteAlarms = defaultOverwriteConfigurationDetails.getAlarms();
        if (defaultOverwriteAlarms != null) {
            List<InputAlarm> inputAlarms = defaultOverwriteAlarms.stream()
                    .map(alarm -> new InputAlarm(alarm))
                    .collect(Collectors.toList());
            this.alarms = inputAlarms;
        } else {
            this.alarms = recommendedInputConfig.getAlarms();
        }

        List<SubComponentTypeConfiguration> defaultOverwriteSubComponentTypeConfigurations =
                defaultOverwriteComponentConfiguration.getSubComponentTypeConfigurations();

        boolean instancesOverwritten = false;
        if (defaultOverwriteSubComponentTypeConfigurations != null &&
                !defaultOverwriteSubComponentTypeConfigurations.isEmpty()) {
            for (SubComponentTypeConfiguration defaultOverwriteSubComponentTypeConfiguration :
                    defaultOverwriteSubComponentTypeConfigurations) {
                if (defaultOverwriteSubComponentTypeConfiguration.getSubComponentType().equals("EC2_INSTANCE")) {
                    InputInstances inputInstances = new InputInstances(
                            recommendedInputConfig.getInstances(),
                            defaultOverwriteSubComponentTypeConfiguration.getSubComponentConfigurationDetails());
                    this.instances = inputInstances;
                    instancesOverwritten = true;
                }
            }
        }
        if (!instancesOverwritten) {
            this.instances = recommendedInputConfig.getInstances();
        }
    }

    public InputComponentConfiguration() {
    }
}