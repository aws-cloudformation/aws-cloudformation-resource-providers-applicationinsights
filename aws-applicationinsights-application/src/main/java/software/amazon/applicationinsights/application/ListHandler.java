package software.amazon.applicationinsights.application;

import software.amazon.awssdk.services.applicationinsights.ApplicationInsightsClient;
import software.amazon.awssdk.services.applicationinsights.model.ListApplicationsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.List;

public class ListHandler extends BaseHandler<CallbackContext> {

    private final ApplicationInsightsClient applicationInsightsClient = ApplicationInsightsClient.create();

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final List<ResourceModel> models = new ArrayList<>();

        logger.log("List Handler called");

        ListApplicationsResponse listApplicationsResponse;
        try {
            listApplicationsResponse = HandlerHelper.listApplicationInsightsApplications(request.getNextToken(), proxy, applicationInsightsClient);
        } catch (Exception ex) {
            logger.log(String.format("listApplicationInsightsApplications failed with exception %s", ex.getMessage()));
            return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
        }

        request.setNextToken(listApplicationsResponse.nextToken());

        try {
            listApplicationsResponse.applicationInfoList().stream()
                    .forEach(applicationInfo ->
                            models.add(HandlerHelper.generateReadModel(applicationInfo.resourceGroupName(), null, request, proxy, applicationInsightsClient)));
        } catch (Exception ex) {
            logger.log(String.format("generateReadModel failed with exception %s", ex.getMessage()));
            return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
        }

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .nextToken(request.getNextToken())
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
