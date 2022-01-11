package software.amazon.applicationinsights.application.InputConfiguration;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import software.amazon.applicationinsights.application.ComponentConfiguration;
import software.amazon.applicationinsights.application.ConfigurationDetails;
import software.amazon.applicationinsights.application.HAClusterPrometheusExporter;
import software.amazon.applicationinsights.application.HANAPrometheusExporter;
import software.amazon.applicationinsights.application.Log;
import software.amazon.applicationinsights.application.SubComponentConfigurationDetails;
import software.amazon.applicationinsights.application.SubComponentTypeConfiguration;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class InputComponentConfigurationTest {

    @Test
    public void constructor_Test_HANAHA(){
        ComponentConfiguration componentConfiguration = new ComponentConfiguration();
        ConfigurationDetails configurationDetails = getConfigurationDetails("9668",
                "PRD", "30014", "Secret1", "9664", true);

        componentConfiguration.setConfigurationDetails(configurationDetails);
        InputComponentConfiguration inputConfig = new InputComponentConfiguration(componentConfiguration);
        verifyInputConfig(inputConfig, "9668", "PRD",
                "30014", "Secret1", "9664", true);

        ConfigurationDetails configurationDetailsOverwrite = getConfigurationDetails("9667",
                "DEV", "30015", "Secret2", "9665", false);
        ComponentConfiguration componentConfigurationOverwrite = new ComponentConfiguration();
        componentConfigurationOverwrite.setConfigurationDetails(configurationDetailsOverwrite);
        InputComponentConfiguration inputConfigOverwrite = new InputComponentConfiguration(inputConfig,
                componentConfigurationOverwrite);
        verifyInputConfig(inputConfigOverwrite, "9667", "DEV",
                "30015", "Secret2", "9665", false);
    }

    private void verifyInputConfig(InputComponentConfiguration inputConfig, String hanaPromPort, String hanaSid, String hanaPort, String hanaSecret, String haPort, Boolean agreed) {
        assertEquals(haPort, inputConfig.getHaClusterPrometheusExporter().getPrometheusPort());
        assertEquals(hanaPromPort, inputConfig.getHanaPrometheusExporter().getPrometheusPort());
        assertEquals(hanaPort, inputConfig.getHanaPrometheusExporter().getHanaPort());
        assertEquals(hanaSid, inputConfig.getHanaPrometheusExporter().getHanaSid());
        assertEquals(agreed, inputConfig.getHanaPrometheusExporter().getAgreeToInstallHANADBClient());
        assertEquals(hanaSecret, inputConfig.getHanaPrometheusExporter().getHanaSecretName());
    }

    private ConfigurationDetails getConfigurationDetails(String hanaPromPort, String hanaSid, String hanaPort,
                                                         String hanaSecret, String haPort, Boolean agreed) {
        ConfigurationDetails configurationDetails = new ConfigurationDetails();
        HANAPrometheusExporter hanaPrometheusExporter = new HANAPrometheusExporter();
        hanaPrometheusExporter.setPrometheusPort(hanaPromPort);
        hanaPrometheusExporter.setHANASID(hanaSid);
        hanaPrometheusExporter.setHANAPort(hanaPort);
        hanaPrometheusExporter.setHANASecretName(hanaSecret);
        hanaPrometheusExporter.setAgreeToInstallHANADBClient(agreed);
        HAClusterPrometheusExporter haClusterPrometheusExporter = new HAClusterPrometheusExporter();
        haClusterPrometheusExporter.setPrometheusPort(haPort);
        configurationDetails.setHANAPrometheusExporter(hanaPrometheusExporter);
        configurationDetails.setHAClusterPrometheusExporter(haClusterPrometheusExporter);
        return configurationDetails;
    }

    @Test
    public void constructor_Test_Recommend_Overwrite_Merge() throws JsonProcessingException {
        InputComponentConfiguration recommendedInputConfig = new InputComponentConfiguration();
        InputSubComponent inputSubComponent = new InputSubComponent();
        inputSubComponent.setSubComponentType("AWS::EC2::Instance");
        InputLog inputLog = new InputLog();
        inputLog.setLogGroupName("logGroup");
        inputLog.setLogPath("");
        inputLog.setLogType("SQL_SERVER");
        inputSubComponent.setLogs(Arrays.asList(inputLog));
        recommendedInputConfig.setSubComponents(Arrays.asList(inputSubComponent));

        String logPath = "C:\\Program Files\\Microsoft SQL Server\\MSSQL**.MSSQLSERVER\\MSSQL\\Log\\ERRORLOG";
        ComponentConfiguration defaultOverwriteComponentConfiguration = new ComponentConfiguration();
        SubComponentTypeConfiguration subComponentTypeConfiguration = new SubComponentTypeConfiguration();
        subComponentTypeConfiguration.setSubComponentType("AWS::EC2::Instance");
        SubComponentConfigurationDetails subComponentConfigurationDetails = new SubComponentConfigurationDetails();
        Log log = new Log();
        log.setLogGroupName("logGroup2");
        log.setLogType("SQL_SERVER");
        log.setLogPath(logPath);
        subComponentConfigurationDetails.setLogs(Arrays.asList(log));
        subComponentTypeConfiguration.setSubComponentConfigurationDetails(subComponentConfigurationDetails);
        defaultOverwriteComponentConfiguration.setSubComponentTypeConfigurations(Arrays.asList(subComponentTypeConfiguration));

        InputComponentConfiguration mergedConfig = new InputComponentConfiguration(recommendedInputConfig, defaultOverwriteComponentConfiguration);

        assertNull(mergedConfig.getAlarmMetrics());
        assertNull(mergedConfig.getAlarms());
        assertNull(mergedConfig.getLogs());
        assertNull(mergedConfig.getJmxPrometheusExporter());
        assertNull(mergedConfig.getHanaPrometheusExporter());
        assertNull(mergedConfig.getHaClusterPrometheusExporter());
        assertEquals(1, mergedConfig.getSubComponents().size());
        assertEquals("AWS::EC2::Instance", mergedConfig.getSubComponents().get(0).getSubComponentType());
        assertEquals(1, mergedConfig.getSubComponents().get(0).getLogs().size());
        assertEquals(logPath, mergedConfig.getSubComponents().get(0).getLogs().get(0).getLogPath());
        assertEquals("logGroup2", mergedConfig.getSubComponents().get(0).getLogs().get(0).getLogGroupName());
    }
}
