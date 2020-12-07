package software.amazon.applicationinsights.application.InputConfiguration;

import com.fasterxml.jackson.annotation.JsonInclude;
import software.amazon.applicationinsights.application.JMXPrometheusExporter;

public class InputJMXPrometheusExporter {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jmxURL;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String hostPort;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String prometheusPort;

    public String getJmxURL() {
        return jmxURL;
    }

    public void setJmxURL(final String jmxURL) {
        this.jmxURL = jmxURL;
    }

    public String getHostPort() {
        return hostPort;
    }

    public void setHostPort(final String hostPort) {
        this.hostPort = hostPort;
    }

    public String getPrometheusPort() {
        return prometheusPort;
    }

    public void setPrometheusPort(final String prometheusPort) {
        this.prometheusPort = prometheusPort;
    }

    public InputJMXPrometheusExporter(final JMXPrometheusExporter jmxPrometheusExporter) {
        this.jmxURL = jmxPrometheusExporter.getJMXURL();
        this.hostPort = jmxPrometheusExporter.getHostPort();
        this.prometheusPort = jmxPrometheusExporter.getPrometheusPort();
    }

    public InputJMXPrometheusExporter() {
    }
}
