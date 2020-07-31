package software.amazon.applicationinsights.application;

import software.amazon.applicationinsights.application.StepWorkflow.AppCreationStepWorkflow;
import software.amazon.applicationinsights.application.StepWorkflow.ComponentConfigurationStepWorkflow;
import software.amazon.applicationinsights.application.StepWorkflow.ComponentCreationStepWorkflow;
import software.amazon.applicationinsights.application.StepWorkflow.DefaultComponentConfigurationStepWorkflow;
import software.amazon.applicationinsights.application.StepWorkflow.LogPatternCreationStepWorkflow;
import software.amazon.awssdk.services.applicationinsights.ApplicationInsightsClient;
import software.amazon.awssdk.services.applicationinsights.model.ResourceInUseException;
import software.amazon.awssdk.services.applicationinsights.model.ValidationException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static software.amazon.applicationinsights.application.Constants.TRANSITION_CALLBACK_DELAY_SECONDS;

public class CreateHandler extends BaseHandler<CallbackContext> {
    static final int CREATE_STATUS_POLL_RETRIES = 60;
    static final String CREATE_TIMED_OUT_MESSAGE = "Timed out waiting for application creation.";

    private final ApplicationInsightsClient applicationInsightsClient = ApplicationInsightsClient.create();

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        logger.log(String.format("Create Handler called with resourceGroupName %s", model.getResourceGroupName()));
        logger.log("Resource Model: " + model.toString());

        if (model.getApplicationARN() != null) {
            final Exception ex = ValidationException.builder()
                    .message("ApplicationARN is read only property and should not be set.")
                    .build();
            return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
        }

        final CallbackContext newCallbackContext = callbackContext == null ?
                CallbackContext.builder().stabilizationRetriesRemaining(CREATE_STATUS_POLL_RETRIES).build() :
                callbackContext;

        logger.log("Callback Context: " + newCallbackContext.toString());

        model.setApplicationARN(String.format("arn:aws:applicationinsights:%s:%s:application/resource-group/%s",
                request.getRegion(),
                request.getAwsAccountId(),
                model.getResourceGroupName()));

        // check if application is already created for resource group before the call
        if (callbackContext == null && HandlerHelper.doesApplicationExist(model.getResourceGroupName(), proxy, applicationInsightsClient)) {
            final Exception ex = ResourceInUseException.builder()
                    .message("Application Insights application already exists for resource group: " + model.getResourceGroupName())
                    .build();
            return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
        }

        String currentStep = newCallbackContext.getCurrentStep();

        if (newCallbackContext.getStabilizationRetriesRemaining() == 0) {
            throw new RuntimeException(CREATE_TIMED_OUT_MESSAGE);
        }

        if (currentStep == null) {
            // go to APP_CREATION step
            return ProgressEvent.defaultInProgressHandler(
                    CallbackContext.builder()
                            .currentStep(Step.APP_CREATION.name())
                            .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                            .processingItem(null)
                            .unprocessedItems(new ArrayList<>(Arrays.asList(model.getResourceGroupName())))
                            .build(),
                    Step.APP_CREATION.getCallBackWaitSeconds(),
                    model);
        } else if (currentStep.equals(Step.APP_CREATION.name())) {
            // go to COMPONENT_CREATION step
            List<String> allCustomComponentNamesToCreate = HandlerHelper.getAllCustomComponentNamesToCreate(model);
            ProgressEvent<ResourceModel, CallbackContext> componentCreationStepInitProgressEvent =
                    ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.COMPONENT_CREATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(null)
                                    .unprocessedItems(allCustomComponentNamesToCreate)
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);

            return new AppCreationStepWorkflow(model, newCallbackContext, proxy, applicationInsightsClient, logger, componentCreationStepInitProgressEvent)
                    .execute();
        } else if (currentStep.equals(Step.COMPONENT_CREATION.name())) {
            // go to LOG_PATTERN_CREATION step
            List<String> allLogPatternIdentifierToCreate = HandlerHelper.getModelLogPatternIdentifiers(model);
            ProgressEvent<ResourceModel, CallbackContext> logPatternCreationStepInitProgressEvent =
                    ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.LOG_PATTERN_CREATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(null)
                                    .unprocessedItems(allLogPatternIdentifierToCreate)
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);

            return new ComponentCreationStepWorkflow(model, newCallbackContext, proxy, applicationInsightsClient, logger, logPatternCreationStepInitProgressEvent)
                    .execute();
        } else if (currentStep.equals(Step.LOG_PATTERN_CREATION.name())) {
            // go to COMPONENT_CONFIGURATION step
            List<String> allComponentNamesWithMonitoringSettings = HandlerHelper.getAllComponentNamesWithMonitoringSettings(model, logger);
            logger.log("All Component Names With Monitoring Settings: " + allComponentNamesWithMonitoringSettings.toString());
            ProgressEvent<ResourceModel, CallbackContext> componentConfigurationStepInitProgressEvent =
                    ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.COMPONENT_CONFIGURATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(null)
                                    .unprocessedItems(allComponentNamesWithMonitoringSettings)
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);

            return new LogPatternCreationStepWorkflow(model, newCallbackContext, proxy, applicationInsightsClient, logger, componentConfigurationStepInitProgressEvent)
                    .execute();
        } else if (currentStep.equals(Step.COMPONENT_CONFIGURATION.name())) {
            // if not all components have monitoring settings,
            // go to DEFAULT_COMPONENT_CONFIGURATION step if auto config is enabled,
            // otherwise succeed
            ProgressEvent<ResourceModel, CallbackContext> successOrDefaultConfigurationStepInitProgressEvent =
                    ProgressEvent.defaultSuccessHandler(model);

            Boolean autoConfigurationEnabled = model.getAutoConfigurationEnabled();
            if (autoConfigurationEnabled != null && autoConfigurationEnabled == true) {
                List<String> defaultConfigComponentNames = HandlerHelper.getApplicationAllComponentNames(
                        model, proxy, applicationInsightsClient);
                List<String> allComponentNamesWithMonitoringSettings = HandlerHelper.getAllComponentNamesWithMonitoringSettings(model, logger);
                defaultConfigComponentNames.removeAll(allComponentNamesWithMonitoringSettings);

                if (defaultConfigComponentNames != null && !defaultConfigComponentNames.isEmpty()) {
                    successOrDefaultConfigurationStepInitProgressEvent =
                            ProgressEvent.defaultInProgressHandler(
                                    CallbackContext.builder()
                                            .currentStep(Step.DEFAULT_COMPONENT_CONFIGURATION.name())
                                            .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                            .processingItem(null)
                                            .unprocessedItems(defaultConfigComponentNames)
                                            .build(),
                                    TRANSITION_CALLBACK_DELAY_SECONDS,
                                    model);
                }
            }

            return new ComponentConfigurationStepWorkflow(model, newCallbackContext, proxy, applicationInsightsClient, logger, successOrDefaultConfigurationStepInitProgressEvent)
                    .execute();
        } else if (currentStep.equals(Step.DEFAULT_COMPONENT_CONFIGURATION.name())) {
            ProgressEvent<ResourceModel, CallbackContext> successInitProgressEvent =
                    ProgressEvent.defaultSuccessHandler(model);

            return new DefaultComponentConfigurationStepWorkflow(model, newCallbackContext, proxy, applicationInsightsClient, logger, successInitProgressEvent)
                    .execute();
        } else {
            return ProgressEvent.defaultSuccessHandler(model);
        }
    }
}
