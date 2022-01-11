package software.amazon.applicationinsights.application.InputConfiguration;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import software.amazon.applicationinsights.application.HANAPrometheusExporter;

public class InputHANAPrometheusExporter {

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String hanaSid;

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String hanaPort;

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String hanaSecretName;

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String prometheusPort;

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean agreeToInstallHANADBClient;

    public InputHANAPrometheusExporter(final HANAPrometheusExporter hanaPrometheusExporter) {
        this.hanaPort = hanaPrometheusExporter.getHANAPort();
        this.hanaSid = hanaPrometheusExporter.getHANASID();
        this.hanaSecretName = hanaPrometheusExporter.getHANASecretName();
        this.prometheusPort = hanaPrometheusExporter.getPrometheusPort();
        this.agreeToInstallHANADBClient = hanaPrometheusExporter.getAgreeToInstallHANADBClient();
    }

    public InputHANAPrometheusExporter() {
    }
}
