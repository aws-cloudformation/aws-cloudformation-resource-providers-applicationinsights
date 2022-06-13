package software.amazon.applicationinsights.application;

import software.amazon.awssdk.services.applicationinsights.ApplicationInsightsClient;
import software.amazon.awssdk.services.applicationinsights.model.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static software.amazon.applicationinsights.application.Constants.ACCOUNT_BASED_GROUPING_TYPE;
import static software.amazon.applicationinsights.application.Constants.SHADOW_RG_PREFIX_ACCOUNT_BASED;
import static software.amazon.applicationinsights.application.Constants.WAIT_CALLBACK_DELAY_SECONDS;

public class DeleteHandler extends BaseHandler<CallbackContext> {

    static final int STATUS_POLL_RETRIES = 10;
    static final String TIMED_OUT_MESSAGE = "Timed out waiting for application deletion.";

    private final ApplicationInsightsClient applicationInsightsClient = ApplicationInsightsClient.create();

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
                                                                       final ResourceHandlerRequest<ResourceModel> request,
                                                                       final CallbackContext callbackContext,
                                                                       final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        logger.log(String.format("Delete Handler called with resourceGroupName %s", model.getResourceGroupName()));

        // For Account based application AppInsights creates a Resource Group with prefix "ApplicationInsights-", accounting for it
        if ((ACCOUNT_BASED_GROUPING_TYPE).equals(model.getGroupingType())) {
            logger.log(String.format("Delete the account based application for resourceGroupName %s", model.getResourceGroupName()));

            if (!model.getResourceGroupName().startsWith(SHADOW_RG_PREFIX_ACCOUNT_BASED)) {
                model.setResourceGroupName(SHADOW_RG_PREFIX_ACCOUNT_BASED + model.getResourceGroupName());
            }
        }

        final CallbackContext newCallbackContext = callbackContext == null ?
                CallbackContext.builder().stabilizationRetriesRemaining(STATUS_POLL_RETRIES).build() :
                callbackContext;

        if (callbackContext == null) {
            if (!HandlerHelper.doesApplicationExist(model.getResourceGroupName(), proxy, applicationInsightsClient)) {
                // if the application does not exit, fail the delete
                final Exception ex = ResourceNotFoundException.builder()
                        .message("Application does not exit for resource group " + model.getResourceGroupName())
                        .build();
                return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
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
                    WAIT_CALLBACK_DELAY_SECONDS,
                    model);
        }
    }
}
