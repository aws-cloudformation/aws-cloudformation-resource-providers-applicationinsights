package software.amazon.applicationinsights.application.InputConfiguration;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import software.amazon.applicationinsights.application.HAClusterPrometheusExporter;

public class InputHAClusterPrometheusExporter {
    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String prometheusPort;


    public InputHAClusterPrometheusExporter(final HAClusterPrometheusExporter haClusterPrometheusExporter) {
        this.prometheusPort = haClusterPrometheusExporter.getPrometheusPort();
    }

    public InputHAClusterPrometheusExporter() {
    }
}
