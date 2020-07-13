package software.amazon.applicationinsights.application.StepWorkflow;

import software.amazon.applicationinsights.application.CallbackContext;
import software.amazon.applicationinsights.application.HandlerHelper;
import software.amazon.applicationinsights.application.ResourceModel;
import software.amazon.awssdk.services.applicationinsights.ApplicationInsightsClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;

import java.io.IOException;

public class LogPatternDeletionStepWorkflow extends BaseStepWorkflow {

    private ResourceModel model;
    private CallbackContext callbackContext;
    private AmazonWebServicesClientProxy proxy;
    private ApplicationInsightsClient applicationInsightsClient;
    private Logger logger;
    private ProgressEvent<ResourceModel, CallbackContext> nextStepInitProgressEvent;

    public LogPatternDeletionStepWorkflow(
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
            String logPatternIdentifier,
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient,
            Logger logger) {
        return !HandlerHelper.doesLogPatternExist(
                model.getResourceGroupName(),
                logPatternIdentifier.split(":")[0],
                logPatternIdentifier.split(":")[1],
                proxy,
                applicationInsightsClient);
    }

    @Override
    protected void startProcessNextItem(
            String logPatternIdentifier,
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient,
            Logger logger) throws IOException {
        HandlerHelper.deleteLogPattern(
                logPatternIdentifier.split(":")[0],
                logPatternIdentifier.split(":")[1],
                model,
                proxy,
                applicationInsightsClient);    }
}
