package software.amazon.applicationinsights.application;

import software.amazon.awssdk.services.applicationinsights.ApplicationInsightsClient;
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

        logger.log(String.format("Read Handler called with resourceGroupName %s", model.getResourceGroupName()));

        try {
            HandlerHelper.describeApplicationInsightsApplication(model.getResourceGroupName(), proxy, applicationInsightsClient);
        } catch (Exception ex) {
            logger.log(String.format("describeApplicationInsightsApplication failed with exception %s", ex.getMessage()));
            return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
        }

        ResourceModel outputModel;
        try {
            outputModel = HandlerHelper.generateReadModel(model.getResourceGroupName(), model, request, proxy, applicationInsightsClient);
        } catch (Exception ex) {
            logger.log(String.format("generateReadModel failed with exception %s", ex.getMessage()));
            return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
        }

        return ProgressEvent.defaultSuccessHandler(outputModel);
    }
}
