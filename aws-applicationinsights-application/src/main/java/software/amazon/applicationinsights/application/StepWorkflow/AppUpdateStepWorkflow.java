package software.amazon.applicationinsights.application.StepWorkflow;

import software.amazon.applicationinsights.application.CallbackContext;
import software.amazon.applicationinsights.application.HandlerHelper;
import software.amazon.applicationinsights.application.ResourceModel;
import software.amazon.awssdk.services.applicationinsights.ApplicationInsightsClient;
import software.amazon.awssdk.services.applicationinsights.model.DescribeApplicationResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;

import java.io.IOException;

public class AppUpdateStepWorkflow extends BaseStepWorkflow {

    private ResourceModel model;
    private CallbackContext callbackContext;
    private AmazonWebServicesClientProxy proxy;
    private ApplicationInsightsClient applicationInsightsClient;
    private Logger logger;
    private ProgressEvent<ResourceModel, CallbackContext> nextStepInitProgressEvent;

    public AppUpdateStepWorkflow(
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
            String resourceGroupName,
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient,
            Logger logger) {
        DescribeApplicationResponse response = HandlerHelper.describeApplicationInsightsApplication(resourceGroupName, proxy, applicationInsightsClient);
        return !HandlerHelper.appNeedsUpdate(model, response);
    }

    @Override
    protected void startProcessNextItem(
            String resourceGroupName,
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient,
            Logger logger) throws IOException {
        HandlerHelper.updateApplicationInsightsApplication(model, proxy, applicationInsightsClient);
    }
}
