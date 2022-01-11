package software.amazon.applicationinsights.application.InputConfiguration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import software.amazon.applicationinsights.application.Alarm;
import software.amazon.applicationinsights.application.AlarmMetric;
import software.amazon.applicationinsights.application.ComponentConfiguration;
import software.amazon.applicationinsights.application.ConfigurationDetails;
import software.amazon.applicationinsights.application.JMXPrometheusExporter;
import software.amazon.applicationinsights.application.HANAPrometheusExporter;
import software.amazon.applicationinsights.application.HAClusterPrometheusExporter;
import software.amazon.applicationinsights.application.Log;
import software.amazon.applicationinsights.application.SubComponentConfigurationDetails;
import software.amazon.applicationinsights.application.SubComponentTypeConfiguration;
import software.amazon.applicationinsights.application.WindowsEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Data class for InputComponentConfiguration
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InputComponentConfiguration {

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<InputAlarmMetric> alarmMetrics;

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<InputLog> logs;

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<InputWindowsEvent> windowsEvents;

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<InputSubComponent> subComponents;

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<InputAlarm> alarms;

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private InputJMXPrometheusExporter jmxPrometheusExporter;

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private InputHANAPrometheusExporter hanaPrometheusExporter;

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private InputHAClusterPrometheusExporter haClusterPrometheusExporter;

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
            List<InputSubComponent> inputSubComponents = new ArrayList<>();
            for (SubComponentTypeConfiguration subComponentTypeConfiguration : subComponentTypeConfigurations) {
                InputSubComponent inputSubComponent = new InputSubComponent(subComponentTypeConfiguration);
                inputSubComponents.add(inputSubComponent);
            }
            this.subComponents = inputSubComponents;
        }

        JMXPrometheusExporter jmxPrometheusExporter = configurationDetails.getJMXPrometheusExporter();
        if (jmxPrometheusExporter != null) {
            this.jmxPrometheusExporter = new InputJMXPrometheusExporter(jmxPrometheusExporter);
        }

        HANAPrometheusExporter hanaPrometheusExporter = configurationDetails.getHANAPrometheusExporter();
        if (hanaPrometheusExporter != null) {
            this.hanaPrometheusExporter = new InputHANAPrometheusExporter(hanaPrometheusExporter);
        }

        HAClusterPrometheusExporter haClusterPrometheusExporter = configurationDetails.getHAClusterPrometheusExporter();
        if (haClusterPrometheusExporter != null) {
            this.haClusterPrometheusExporter = new InputHAClusterPrometheusExporter(haClusterPrometheusExporter);
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

        Map<String, SubComponentTypeConfiguration> defaultOverwriteSubComponentTypeConfigurationsMap =
            (defaultOverwriteComponentConfiguration.getSubComponentTypeConfigurations() == null ||
                defaultOverwriteComponentConfiguration.getSubComponentTypeConfigurations().isEmpty()) ?
                Collections.emptyMap() :
                defaultOverwriteComponentConfiguration.getSubComponentTypeConfigurations().stream()
                    .collect(Collectors.toMap(SubComponentTypeConfiguration::getSubComponentType, Function.identity()));

        Map<String, InputSubComponent> recommendedSubComponentsMap =
            (recommendedInputConfig.getSubComponents() == null ||  recommendedInputConfig.getSubComponents().isEmpty()) ?
                Collections.emptyMap() :
                recommendedInputConfig.getSubComponents().stream()
                    .collect(Collectors.toMap(InputSubComponent::getSubComponentType, Function.identity()));

        List<InputSubComponent> mergedSubComponents = new ArrayList<>();

        // If sub component type not recommended but specified by customer, set it as is
        Set<String> overwriteOnlySubComponentTypes = new HashSet<>(defaultOverwriteSubComponentTypeConfigurationsMap.keySet());
        overwriteOnlySubComponentTypes.removeAll(recommendedSubComponentsMap.keySet());
        overwriteOnlySubComponentTypes.stream().forEach(componentType -> {
            mergedSubComponents.add(new InputSubComponent(defaultOverwriteSubComponentTypeConfigurationsMap.get(componentType)));
        });

        // If sub component type recommended but not overwritten by customer, set it as recommended config
        Set<String> recommendOnlySubComponentTypes = new HashSet<>(recommendedSubComponentsMap.keySet());
        recommendOnlySubComponentTypes.removeAll(defaultOverwriteSubComponentTypeConfigurationsMap.keySet());
        recommendOnlySubComponentTypes.stream().forEach(componentType -> {
            mergedSubComponents.add(recommendedSubComponentsMap.get(componentType));
        });

        // If sub component type both recommended and overwritten by customers, merge both configs
        Set<String> mergedComponentTypes = new HashSet<>(defaultOverwriteSubComponentTypeConfigurationsMap.keySet());
        mergedComponentTypes.retainAll(recommendedSubComponentsMap.keySet());
        mergedComponentTypes.stream().forEach(componentType -> {
            mergedSubComponents.add(new InputSubComponent(
                recommendedSubComponentsMap.get(componentType),
                defaultOverwriteSubComponentTypeConfigurationsMap.get(componentType)));
        });

        if (!mergedSubComponents.isEmpty()) {
            this.subComponents = mergedSubComponents;
        }

        JMXPrometheusExporter defaultOverwriteJmxPrometheusExporter = defaultOverwriteConfigurationDetails.getJMXPrometheusExporter();
        if (defaultOverwriteJmxPrometheusExporter != null) {
            this.jmxPrometheusExporter = new InputJMXPrometheusExporter(defaultOverwriteJmxPrometheusExporter);
        } else {
            this.jmxPrometheusExporter = recommendedInputConfig.getJmxPrometheusExporter();
        }

        HANAPrometheusExporter defaultOverwriteHANAPrometheusExporter = defaultOverwriteConfigurationDetails.getHANAPrometheusExporter();
        if (defaultOverwriteHANAPrometheusExporter != null) {
            this.hanaPrometheusExporter = new InputHANAPrometheusExporter(defaultOverwriteHANAPrometheusExporter);
        } else {
            this.hanaPrometheusExporter = recommendedInputConfig.getHanaPrometheusExporter();
        }

        HAClusterPrometheusExporter defaultOverwriteHAClusterPrometheusExporter = defaultOverwriteConfigurationDetails.getHAClusterPrometheusExporter();
        if (defaultOverwriteHAClusterPrometheusExporter != null) {
            this.haClusterPrometheusExporter = new InputHAClusterPrometheusExporter(defaultOverwriteHAClusterPrometheusExporter);
        } else {
            this.haClusterPrometheusExporter = recommendedInputConfig.getHaClusterPrometheusExporter();
        }
    }

    public InputComponentConfiguration() {
    }
}