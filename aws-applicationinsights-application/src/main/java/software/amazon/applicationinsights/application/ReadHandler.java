package software.amazon.applicationinsights.application;

import software.amazon.awssdk.services.applicationinsights.ApplicationInsightsClient;
import software.amazon.awssdk.services.applicationinsights.model.ValidationException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandler<CallbackContext> {

    private final ApplicationInsightsClient applicationInsightsClient = ApplicationInsightsClient.create();

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        // Read/Delete handlers are only guaranteed to receive the primaryIdentifier values
        logger.log(String.format("Read Handler called with application ARN %s", model.getApplicationARN()));

        // Extract resource group name from arn
        final String resourceGroupName;
        try {
            resourceGroupName = HandlerHelper.extractResourceGroupNameFromApplicationArn(model.getApplicationARN());
        } catch (Exception ex) {
            logger.log(String.format("Failed to parse application arn with error: %s", ex.getMessage()));
            final ValidationException exception = ValidationException.builder()
                    .message("Failed to parse application ARN: " + model.getApplicationARN())
                    .build();
            return ProgressEvent.defaultFailureHandler(exception, ExceptionMapper.mapToHandlerErrorCode(exception));
        }

        logger.log(String.format("Using resource group name %s", resourceGroupName));

        try {
            HandlerHelper.describeApplicationInsightsApplication(resourceGroupName, proxy, applicationInsightsClient);
        } catch (Exception ex) {
            logger.log(String.format("describeApplicationInsightsApplication failed with exception %s", ex.getMessage()));
            return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
        }

        ResourceModel outputModel;
        try {
            outputModel = HandlerHelper.generateReadModel(resourceGroupName, model, request, proxy, applicationInsightsClient);
        } catch (Exception ex) {
            logger.log(String.format("generateReadModel failed with exception %s", ex.getMessage()));
            return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
        }

        logger.log(String.format("Resource found: %s", outputModel));

        return ProgressEvent.defaultSuccessHandler(outputModel);
    }
}
