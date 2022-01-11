package software.amazon.applicationinsights.application.StepWorkflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import software.amazon.applicationinsights.application.CallbackContext;
import software.amazon.applicationinsights.application.ResourceModel;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.applicationinsights.ApplicationInsightsClient;
import software.amazon.awssdk.services.applicationinsights.model.ApplicationInfo;
import software.amazon.awssdk.services.applicationinsights.model.DescribeApplicationResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;

import static org.mockito.Mockito.mock;
import static software.amazon.applicationinsights.application.Constants.APP_CREATION_FINISHED_LIFECYCLE;
import static software.amazon.applicationinsights.application.Constants.CONFIGURATION_FINISHED_LIFECYCLE;

public class AppCreationStepWorkflowTest {

    @Mock
    ResourceModel model;
    @Mock
    CallbackContext callbackContext;
    @Mock
    AmazonWebServicesClientProxy proxy;
    @Mock
    ApplicationInsightsClient applicationInsightsClient;
    @Mock
    Logger logger;
    @Mock
    ProgressEvent<ResourceModel, CallbackContext> nextStepInitProgressEvent;

    private final String RESOURCE_GROUP_NAME = "resourceGroupName";

    @BeforeEach
    public void setup() {
        applicationInsightsClient = ApplicationInsightsClient.builder().region(Region.US_EAST_1).build();
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
    }


    @Test
    public void AppCreationStepWorkflowTest_isCurrentItemProcessFinishedTest_Active() {
        ApplicationInfo applicationInfo = ApplicationInfo.builder().lifeCycle(CONFIGURATION_FINISHED_LIFECYCLE).build();

        DescribeApplicationResponse describeApplicationResponse = DescribeApplicationResponse.builder().applicationInfo(applicationInfo).build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any())).thenReturn(describeApplicationResponse);

        AppCreationStepWorkflow appCreationStepWorkflow = new AppCreationStepWorkflow(model, callbackContext, proxy, applicationInsightsClient, logger, nextStepInitProgressEvent);
        assertTrue(appCreationStepWorkflow.isCurrentItemProcessFinished(RESOURCE_GROUP_NAME, model, proxy, applicationInsightsClient, logger));
    }

    @Test
    public void AppCreationStepWorkflowTest_isCurrentItemProcessFinishedTest_NotConfigured() {
        ApplicationInfo applicationInfo = ApplicationInfo.builder().lifeCycle(APP_CREATION_FINISHED_LIFECYCLE).build();

        DescribeApplicationResponse describeApplicationResponse = DescribeApplicationResponse.builder().applicationInfo(applicationInfo).build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any())).thenReturn(describeApplicationResponse);

        AppCreationStepWorkflow appCreationStepWorkflow = new AppCreationStepWorkflow(model, callbackContext, proxy, applicationInsightsClient, logger, nextStepInitProgressEvent);
        assertTrue(appCreationStepWorkflow.isCurrentItemProcessFinished(RESOURCE_GROUP_NAME, model, proxy, applicationInsightsClient, logger));
    }

    @Test
    public void AppCreationStepWorkflowTest_isCurrentItemProcessFinishedTest_UnKnown() {
        ApplicationInfo applicationInfo = ApplicationInfo.builder().lifeCycle("DELETE").build();

        DescribeApplicationResponse describeApplicationResponse = DescribeApplicationResponse.builder().applicationInfo(applicationInfo).build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any())).thenReturn(describeApplicationResponse);

        AppCreationStepWorkflow appCreationStepWorkflow = new AppCreationStepWorkflow(model, callbackContext, proxy, applicationInsightsClient, logger, nextStepInitProgressEvent);
        assertFalse(appCreationStepWorkflow.isCurrentItemProcessFinished(RESOURCE_GROUP_NAME, model, proxy, applicationInsightsClient, logger));
    }
}
