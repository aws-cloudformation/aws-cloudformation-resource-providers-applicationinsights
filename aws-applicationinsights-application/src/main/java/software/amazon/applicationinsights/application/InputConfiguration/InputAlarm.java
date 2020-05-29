package software.amazon.applicationinsights.application.InputConfiguration;

import com.fasterxml.jackson.annotation.JsonInclude;
import software.amazon.applicationinsights.application.Alarm;

/**
 * Data class for InputAlarm
 */
public class InputAlarm {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String alarmName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String severity;

    public String getAlarmName() {
        return alarmName;
    }

    public void setAlarmName(final String alarmName) {
        this.alarmName = alarmName;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(final String severity) {
        this.severity = severity;
    }

    public InputAlarm(final Alarm alarm) {
        this.alarmName = alarm.getAlarmName();
        this.severity = alarm.getSeverity();
    }

    public InputAlarm() {
    }
}