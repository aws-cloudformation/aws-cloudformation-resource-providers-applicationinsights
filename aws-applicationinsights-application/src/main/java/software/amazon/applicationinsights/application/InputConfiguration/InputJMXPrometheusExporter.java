package software.amazon.applicationinsights.application.InputConfiguration;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import software.amazon.applicationinsights.application.JMXPrometheusExporter;

public class InputJMXPrometheusExporter {

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jmxURL;

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String hostPort;

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String prometheusPort;

    public InputJMXPrometheusExporter(final JMXPrometheusExporter jmxPrometheusExporter) {
        this.jmxURL = jmxPrometheusExporter.getJMXURL();
        this.hostPort = jmxPrometheusExporter.getHostPort();
        this.prometheusPort = jmxPrometheusExporter.getPrometheusPort();
    }

    public InputJMXPrometheusExporter() {
    }
}
