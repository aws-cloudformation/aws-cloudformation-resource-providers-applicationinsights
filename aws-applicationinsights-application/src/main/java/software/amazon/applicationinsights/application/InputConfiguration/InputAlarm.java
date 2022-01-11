package software.amazon.applicationinsights.application.InputConfiguration;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import software.amazon.applicationinsights.application.Alarm;

/**
 * Data class for InputAlarm
 */
public class InputAlarm {
    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String alarmName;

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String severity;

    public InputAlarm(final Alarm alarm) {
        this.alarmName = alarm.getAlarmName();
        this.severity = alarm.getSeverity();
    }

    public InputAlarm() {
    }
}