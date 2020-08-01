package software.amazon.applicationinsights.application;

import software.amazon.awssdk.services.applicationinsights.ApplicationInsightsClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandler<CallbackContext> {
    static final int STATUS_POLL_RETRIES = 10;
    static final int CALLBACK_DELAY_SECONDS = 10;
    static final String TIMED_OUT_MESSAGE = "Timed out waiting for application deletion.";

    private final ApplicationInsightsClient applicationInsightsClient = ApplicationInsightsClient.create();

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        logger.log(String.format("Delete Handler called with resourceGroupName %s", model.getResourceGroupName()));
        final CallbackContext newCallbackContext = callbackContext == null ?
                CallbackContext.builder().stabilizationRetriesRemaining(STATUS_POLL_RETRIES).build() :
                callbackContext;

        if (callbackContext == null) {
            if (!HandlerHelper.doesApplicationExist(model.getResourceGroupName(), proxy, applicationInsightsClient)) {
                // Check if application is already deleted before the call
                return ProgressEvent.defaultSuccessHandler(null);
            } else {
                HandlerHelper.deleteApplicationInsightsApplication(model, proxy, applicationInsightsClient);
            }
        }

        if (newCallbackContext.getStabilizationRetriesRemaining() == 0) {
            throw new RuntimeException(TIMED_OUT_MESSAGE);
        }

        if (!HandlerHelper.doesApplicationExist(model.getResourceGroupName(), proxy, applicationInsightsClient)) {
            return ProgressEvent.defaultSuccessHandler(null);
        } else {
            return ProgressEvent.defaultInProgressHandler(
                    CallbackContext.builder()
                            .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining() - 1)
                            .build(),
                    CALLBACK_DELAY_SECONDS,
                    model);
        }
    }
}
