package software.amazon.applicationinsights.application.InputConfiguration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import software.amazon.applicationinsights.application.AlarmMetric;

/**
 * Data class for InputAlarmMetric
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InputAlarmMetric {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String alarmMetricName;

    public String getAlarmMetricName() {
        return alarmMetricName;
    }

    public void setAlarmMetricName(final String alarmMetricName) {
        this.alarmMetricName = alarmMetricName;
    }

    public InputAlarmMetric(final AlarmMetric alarmMetric) {
        this.alarmMetricName = alarmMetric.getAlarmMetricName();
    }

    public InputAlarmMetric() {
    }
}
