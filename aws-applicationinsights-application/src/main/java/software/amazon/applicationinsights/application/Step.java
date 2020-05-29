package software.amazon.applicationinsights.application;

public enum Step {
    APP_CREATION(10),
    APP_UPDATE(1),
    TAG_CREATION(1),
    TAG_DELETION(1),
    COMPONENT_CREATION(1),
    COMPONENT_DELETION(1),
    LOG_PATTERN_CREATION(1),
    LOG_PATTERN_DELETION(1),
    LOG_PATTERN_UPDATE(1),
    COMPONENT_CONFIGURATION(10),
    DEFAULT_COMPONENT_CONFIGURATION(10),
    DISABLE_COMPONENT_CONFIGURATION(1);

    private final int callBackWaitSeconds;

    Step(final int callBackWaitSeconds) {
        this.callBackWaitSeconds = callBackWaitSeconds;
    }

    public int getCallBackWaitSeconds() {
        return this.callBackWaitSeconds;
    }

    public static Step fromStepName(final String stepName) {
        for (final Step step : Step.values()) {
            if (step.name().equals(stepName)) {
                return step;
            }
        }
        return null;
    }
}
