package software.amazon.applicationinsights.application.InputConfiguration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import software.amazon.applicationinsights.application.AlarmMetric;

/**
 * Data class for InputAlarmMetric
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InputAlarmMetric {

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String alarmMetricName;

    public InputAlarmMetric(final AlarmMetric alarmMetric) {
        this.alarmMetricName = alarmMetric.getAlarmMetricName();
    }

    public InputAlarmMetric() {
    }
}
