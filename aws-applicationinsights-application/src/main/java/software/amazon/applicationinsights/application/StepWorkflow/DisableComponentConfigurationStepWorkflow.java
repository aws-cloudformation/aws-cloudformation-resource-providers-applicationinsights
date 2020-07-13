package software.amazon.applicationinsights.application.StepWorkflow;

import software.amazon.applicationinsights.application.CallbackContext;
import software.amazon.applicationinsights.application.HandlerHelper;
import software.amazon.applicationinsights.application.ResourceModel;
import software.amazon.awssdk.services.applicationinsights.ApplicationInsightsClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;

import java.io.IOException;

import static software.amazon.applicationinsights.application.Constants.CONFIGURATION_FINISHED_LIFECYCLE;
import static software.amazon.applicationinsights.application.Constants.DEFAULT_TIER;

public class DisableComponentConfigurationStepWorkflow extends BaseStepWorkflow {

    private ResourceModel model;
    private CallbackContext callbackContext;
    private AmazonWebServicesClientProxy proxy;
    private ApplicationInsightsClient applicationInsightsClient;
    private Logger logger;
    private ProgressEvent<ResourceModel, CallbackContext> nextStepInitProgressEvent;

    public DisableComponentConfigurationStepWorkflow(
            final ResourceModel model,
            final CallbackContext callbackContext,
            final AmazonWebServicesClientProxy proxy,
            final ApplicationInsightsClient applicationInsightsClient,
            final Logger logger,
            final ProgressEvent<ResourceModel, CallbackContext> nextStepInitProgressEvent) {
        super(model, callbackContext, proxy, applicationInsightsClient, logger, nextStepInitProgressEvent);
    }

    @Override
    protected boolean isCurrentItemProcessFinished(
            String componentName,
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient,
            Logger logger) {
        return !HandlerHelper.isComponentConfigurationEnabled(componentName, model, proxy, applicationInsightsClient);
    }

    @Override
    protected void startProcessNextItem(
            String componentName,
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient,
            Logger logger) throws IOException {
        HandlerHelper.disableComponentConfiguration(componentName, model, proxy, applicationInsightsClient, logger);
    }
}
