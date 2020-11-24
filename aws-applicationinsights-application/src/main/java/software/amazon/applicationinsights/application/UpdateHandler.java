package software.amazon.applicationinsights.application;

import software.amazon.applicationinsights.application.StepWorkflow.AppCreationStepWorkflow;
import software.amazon.applicationinsights.application.StepWorkflow.AppUpdateStepWorkflow;
import software.amazon.applicationinsights.application.StepWorkflow.ComponentConfigurationStepWorkflow;
import software.amazon.applicationinsights.application.StepWorkflow.ComponentCreationStepWorkflow;
import software.amazon.applicationinsights.application.StepWorkflow.ComponentDeletionStepWorkflow;
import software.amazon.applicationinsights.application.StepWorkflow.DefaultComponentConfigurationStepWorkflow;
import software.amazon.applicationinsights.application.StepWorkflow.DisableComponentConfigurationStepWorkflow;
import software.amazon.applicationinsights.application.StepWorkflow.LogPatternCreationStepWorkflow;
import software.amazon.applicationinsights.application.StepWorkflow.LogPatternDeletionStepWorkflow;
import software.amazon.applicationinsights.application.StepWorkflow.LogPatternUpdateStepWorkflow;
import software.amazon.applicationinsights.application.StepWorkflow.TagCreationStepWorkflow;
import software.amazon.applicationinsights.application.StepWorkflow.TagDeletionStepWorkflow;
import software.amazon.awssdk.services.applicationinsights.ApplicationInsightsClient;
import software.amazon.awssdk.services.applicationinsights.model.DescribeApplicationResponse;
import software.amazon.awssdk.services.applicationinsights.model.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static software.amazon.applicationinsights.application.Constants.APP_CREATION_FINISHED_LIFECYCLE;
import static software.amazon.applicationinsights.application.Constants.CONFIGURATION_FINISHED_LIFECYCLE;
import static software.amazon.applicationinsights.application.Constants.DEFAULT_TIER;
import static software.amazon.applicationinsights.application.Constants.TRANSITION_CALLBACK_DELAY_SECONDS;
import static software.amazon.applicationinsights.application.Constants.WAIT_CALLBACK_DELAY_SECONDS;

public class UpdateHandler extends BaseHandler<CallbackContext> {
    static final int UPDATE_STATUS_POLL_RETRIES = 60;
    static final String UPDATE_TIMED_OUT_MESSAGE = "Timed out waiting for application update.";

    private final ApplicationInsightsClient applicationInsightsClient = ApplicationInsightsClient.create();

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        logger.log(String.format("Update Handler called with resourceGroupName %s", model.getResourceGroupName()));
        logger.log("Resource Model: " + model.toString());

        final ResourceModel previousModel = request.getPreviousResourceState();
        if (previousModel != null && !model.getResourceGroupName().equals(previousModel.getResourceGroupName())) {
            return ProgressEvent.failed(null, null, HandlerErrorCode.NotUpdatable,
                    String.format("Application cannot be updated as the Resource Group Name was changed"));
        }

        final CallbackContext newCallbackContext = callbackContext == null ?
                CallbackContext.builder().stabilizationRetriesRemaining(UPDATE_STATUS_POLL_RETRIES).build() :
                callbackContext;

        logger.log("Callback Context: " + newCallbackContext.toString());

        model.setApplicationARN(String.format("arn:%s:applicationinsights:%s:%s:application/resource-group/%s",
                request.getAwsPartition(),
                request.getRegion(),
                request.getAwsAccountId(),
                model.getResourceGroupName()));

        String currentStep = newCallbackContext.getCurrentStep();

        if (newCallbackContext.getStabilizationRetriesRemaining() == 0) {
            throw new RuntimeException(UPDATE_TIMED_OUT_MESSAGE);
        }

        if (currentStep == null) {
            if (!HandlerHelper.doesApplicationExist(model.getResourceGroupName(), proxy, applicationInsightsClient)) {
                // if the application does not exit, fail the update
                final Exception ex = ResourceNotFoundException.builder()
                        .message("Application does not exit for resource group " + model.getResourceGroupName())
                        .build();
                return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
            } else {
                // update the app
                return ProgressEvent.defaultInProgressHandler(
                        CallbackContext.builder()
                                .currentStep(Step.APP_UPDATE.name())
                                .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                .processingItem(null)
                                .unprocessedItems(new ArrayList<>(Arrays.asList(model.getResourceGroupName())))
                                .build(),
                        Step.APP_UPDATE.getCallBackWaitSeconds(),
                        model);
            }
        } else if (currentStep.equals(Step.APP_UPDATE.name())) {
            // go to TAG_DELETION step next
            List<String> tagKeysToDelete = HandlerHelper.getTagKeysToDelete(model, proxy, applicationInsightsClient);
            ProgressEvent<ResourceModel, CallbackContext> tagDeletionStepInitProgressEvent =
                    ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.TAG_DELETION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(null)
                                    .unprocessedItems(tagKeysToDelete)
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);

            return new AppUpdateStepWorkflow(model, newCallbackContext, proxy, applicationInsightsClient, logger, tagDeletionStepInitProgressEvent)
                    .execute();
        } else if (currentStep.equals(Step.TAG_DELETION.name())) {
            // go to TAG_CREATION step next
            List<String> tagKeysToCreate = HandlerHelper.getTagKeysToCreate(model, proxy, applicationInsightsClient);
            //HandlerHelper.getTagKeysToDeleteAndCreate(tagKeysToDelete, tagKeysToCreate, model, proxy, applicationInsightsClient);
            ProgressEvent<ResourceModel, CallbackContext> tagCreationStepInitProgressEvent =
                    ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.TAG_CREATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(null)
                                    .unprocessedItems(tagKeysToCreate)
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);

            return new TagDeletionStepWorkflow(model, newCallbackContext, proxy, applicationInsightsClient, logger, tagCreationStepInitProgressEvent)
                    .execute();
        } else if (currentStep.equals(Step.TAG_CREATION.name())) {
            // go to COMPONENT_DELETION step next
            List<String> customComponentNamesToDelete = HandlerHelper.getCustomComponentNamesToDelete(model, proxy, applicationInsightsClient, logger);
            ProgressEvent<ResourceModel, CallbackContext> componentDeletionStepInitProgressEvent =
                    ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.COMPONENT_DELETION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(null)
                                    .unprocessedItems(customComponentNamesToDelete)
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);

            return new TagCreationStepWorkflow(model, newCallbackContext, proxy, applicationInsightsClient, logger, componentDeletionStepInitProgressEvent)
                    .execute();
        } else if (currentStep.equals(Step.COMPONENT_DELETION.name())) {
            // go to COMPONENT_CREATION step next
            List<String> customComponentNamesToCreate = HandlerHelper.getCustomComponentNamesToCreate(model, proxy, applicationInsightsClient, logger);
            ProgressEvent<ResourceModel, CallbackContext> componentCreationStepInitProgressEvent =
                    ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.COMPONENT_CREATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(null)
                                    .unprocessedItems(customComponentNamesToCreate)
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);

            return new ComponentDeletionStepWorkflow(model, newCallbackContext, proxy, applicationInsightsClient, logger, componentCreationStepInitProgressEvent)
                    .execute();
        } else if (currentStep.equals(Step.COMPONENT_CREATION.name())) {
            // go to LOG_PATTERN_DELETION step next
            List<String> logPatternIdentifiersToDelete = HandlerHelper.getLogPatternIdentifiersToDelete(model, proxy, applicationInsightsClient);
            ProgressEvent<ResourceModel, CallbackContext> logPatternDeletionStepInitProgressEvent =
                    ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.LOG_PATTERN_DELETION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(null)
                                    .unprocessedItems(logPatternIdentifiersToDelete)
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);

            return new ComponentCreationStepWorkflow(model, newCallbackContext, proxy, applicationInsightsClient, logger, logPatternDeletionStepInitProgressEvent)
                    .execute();
        } else if (currentStep.equals(Step.LOG_PATTERN_DELETION.name())) {
            // go to LOG_PATTERN_CREATION step next
            List<String> logPatternIdentifiersToCreate = HandlerHelper.getLogPatternIdentifiersToCreate(model, proxy, applicationInsightsClient);
            ProgressEvent<ResourceModel, CallbackContext> logPatternCreationStepInitProgressEvent =
                    ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.LOG_PATTERN_CREATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(null)
                                    .unprocessedItems(logPatternIdentifiersToCreate)
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);

            return new LogPatternDeletionStepWorkflow(model, newCallbackContext, proxy, applicationInsightsClient, logger, logPatternCreationStepInitProgressEvent)
                    .execute();
        } else if (currentStep.equals(Step.LOG_PATTERN_CREATION.name())) {
            // go to LOG_PATTERN_UPDATE step next
            List<String> logPatternIdentifiersToUpdate = HandlerHelper.getLogPatternIdentifiersToUpdate(model, proxy, applicationInsightsClient);
            ProgressEvent<ResourceModel, CallbackContext> logPatternUpdateStepInitProgressEvent =
                    ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.LOG_PATTERN_UPDATE.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(null)
                                    .unprocessedItems(logPatternIdentifiersToUpdate)
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);

            return new LogPatternCreationStepWorkflow(model, newCallbackContext, proxy, applicationInsightsClient, logger, logPatternUpdateStepInitProgressEvent)
                    .execute();
        } else if (currentStep.equals(Step.LOG_PATTERN_UPDATE.name())) {
            // go to COMPONENT_CONFIGURATION step next
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

            return new LogPatternUpdateStepWorkflow(model, newCallbackContext, proxy, applicationInsightsClient, logger, componentConfigurationStepInitProgressEvent)
                    .execute();
        } else if (currentStep.equals(Step.COMPONENT_CONFIGURATION.name())) {
            // if not all components have monitoring settings,
            // go to DEFAULT_COMPONENT_CONFIGURATION step if auto config is enabled,
            // go to DISABLE_COMPONENT_CONFIGURATION step if auto config is disabled,
            // otherwise succeed
            ProgressEvent<ResourceModel, CallbackContext> successOrDefaultOrDisableConfigurationStepInitProgressEvent =
                    ProgressEvent.defaultSuccessHandler(model);

            List<String> configComponentNamesWithoutMonitoringSettings = HandlerHelper.getApplicationAllComponentNames(
                    model, proxy, applicationInsightsClient);
            List<String> allComponentNamesWithMonitoringSettings = HandlerHelper.getAllComponentNamesWithMonitoringSettings(model, logger);
            configComponentNamesWithoutMonitoringSettings.removeAll(allComponentNamesWithMonitoringSettings);

            if (configComponentNamesWithoutMonitoringSettings != null && !configComponentNamesWithoutMonitoringSettings.isEmpty()) {
                Boolean autoConfigurationEnabled = model.getAutoConfigurationEnabled();
                if (autoConfigurationEnabled != null && autoConfigurationEnabled == true) {
                    successOrDefaultOrDisableConfigurationStepInitProgressEvent =
                            ProgressEvent.defaultInProgressHandler(
                                    CallbackContext.builder()
                                            .currentStep(Step.DEFAULT_COMPONENT_CONFIGURATION.name())
                                            .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                            .processingItem(null)
                                            .unprocessedItems(configComponentNamesWithoutMonitoringSettings)
                                            .build(),
                                    TRANSITION_CALLBACK_DELAY_SECONDS,
                                    model);
                } else {
                    successOrDefaultOrDisableConfigurationStepInitProgressEvent =
                            ProgressEvent.defaultInProgressHandler(
                                    CallbackContext.builder()
                                            .currentStep(Step.DISABLE_COMPONENT_CONFIGURATION.name())
                                            .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                            .processingItem(null)
                                            .unprocessedItems(configComponentNamesWithoutMonitoringSettings)
                                            .build(),
                                    TRANSITION_CALLBACK_DELAY_SECONDS,
                                    model);
                }
            }

            return new ComponentConfigurationStepWorkflow(model, newCallbackContext, proxy, applicationInsightsClient, logger, successOrDefaultOrDisableConfigurationStepInitProgressEvent)
                    .execute();
        } else if (currentStep.equals(Step.DEFAULT_COMPONENT_CONFIGURATION.name())) {
            ProgressEvent<ResourceModel, CallbackContext> successInitProgressEvent =
                    ProgressEvent.defaultSuccessHandler(model);

            return new DefaultComponentConfigurationStepWorkflow(model, newCallbackContext, proxy, applicationInsightsClient, logger, successInitProgressEvent)
                    .execute();
        } else if (currentStep.equals(Step.DISABLE_COMPONENT_CONFIGURATION.name())) {
            ProgressEvent<ResourceModel, CallbackContext> successInitProgressEvent =
                    ProgressEvent.defaultSuccessHandler(model);

            return new DisableComponentConfigurationStepWorkflow(model, newCallbackContext, proxy, applicationInsightsClient, logger, successInitProgressEvent)
                    .execute();
        } else {
            return ProgressEvent.defaultSuccessHandler(model);
        }
    }
}
