package software.amazon.applicationinsights.application;

import software.amazon.awssdk.services.applicationinsights.ApplicationInsightsClient;
import software.amazon.awssdk.services.applicationinsights.model.DescribeApplicationResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.AbstractMap;
import java.util.ArrayList;
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
        final CallbackContext newCallbackContext = callbackContext == null ?
                CallbackContext.builder().stabilizationRetriesRemaining(UPDATE_STATUS_POLL_RETRIES).build() :
                callbackContext;

        model.setApplicationARN(String.format("arn:aws:applicationinsights:%s:%s:application/resource-group/%s",
                request.getRegion(),
                request.getAwsAccountId(),
                model.getResourceGroupName()));

        String currentStep = newCallbackContext.getCurrentStep();

        if (newCallbackContext.getStabilizationRetriesRemaining() == 0) {
            throw new RuntimeException(UPDATE_TIMED_OUT_MESSAGE);
        }

        if (currentStep == null) {
            if (!HandlerHelper.doesApplicationExist(model, proxy, applicationInsightsClient)) {
                // recreate the application if it's deleted
                try {
                    HandlerHelper.createApplicationInsightsApplication(model, proxy, applicationInsightsClient);
                } catch (Exception ex) {
                    logger.log(String.format("createApplicationInsightsApplication failed with exception %s", ex.getMessage()));
                    return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
                }

                return ProgressEvent.defaultInProgressHandler(
                        CallbackContext.builder()
                                .currentStep(Step.APP_CREATION.name())
                                .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                .build(),
                        WAIT_CALLBACK_DELAY_SECONDS,
                        model);
            } else {
                DescribeApplicationResponse response;
                try {
                    response = HandlerHelper.describeApplicationInsightsApplication(model.getResourceGroupName(), proxy, applicationInsightsClient);
                } catch (Exception ex) {
                    logger.log(String.format("describeApplicationInsightsApplication failed with exception %s", ex.getMessage()));
                    return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
                }

                if (appNeedsUpdate(model, response)) {
                    try {
                        HandlerHelper.udpateApplicationInsightsApplication(model, proxy, applicationInsightsClient);
                    } catch (Exception ex) {
                        logger.log(String.format("udpateApplicationInsightsApplication failed with exception %s", ex.getMessage()));
                        return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
                    }

                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.APP_UPDATE.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                } else {
                    // app doesn't need update, start TAG_DELETION step
                    List<String> tagKeysToDelete = new ArrayList<>();
                    List<String> tagKeysToCreate = new ArrayList<>();
                    HandlerHelper.getTagKeysToDeleteAndCreate(tagKeysToDelete, tagKeysToCreate, model, proxy, applicationInsightsClient);

                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.TAG_DELETION.name())
                                    .undeletedItems(tagKeysToDelete)
                                    .uncreatedItems(tagKeysToCreate)
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                }
            }
        } else if (currentStep.equals(Step.APP_CREATION.name())) {
            String currentApplicationLifeCycle = HandlerHelper.getApplicationLifeCycle(model, proxy, applicationInsightsClient);
            if (currentApplicationLifeCycle.equals(APP_CREATION_FINISHED_LIFECYCLE)) {
                // APP_CREATION step finished, since tag creation is already done, start COMPONENT_DELETION step
                List<String> customCompnentNamesToDelete = new ArrayList<>();
                List<String> customCompnentNamesToCreate = new ArrayList<>();
                HandlerHelper.getCustomComponentNamesToDeleteAndCreate(
                        customCompnentNamesToDelete, customCompnentNamesToCreate, model, proxy, applicationInsightsClient, logger);

                return ProgressEvent.defaultInProgressHandler(
                        CallbackContext.builder()
                                .currentStep(Step.COMPONENT_DELETION.name())
                                .undeletedItems(customCompnentNamesToDelete)
                                .uncreatedItems(customCompnentNamesToCreate)
                                .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                .build(),
                        TRANSITION_CALLBACK_DELAY_SECONDS,
                        model);
            } else {
                return ProgressEvent.defaultInProgressHandler(
                        CallbackContext.builder()
                                .currentStep(Step.APP_CREATION.name())
                                .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining() - 1)
                                .build(),
                        WAIT_CALLBACK_DELAY_SECONDS,
                        model);
            }
        } else if (currentStep.equals(Step.APP_UPDATE.name())) {
            DescribeApplicationResponse response;
            try {
                response = HandlerHelper.describeApplicationInsightsApplication(model.getResourceGroupName(), proxy, applicationInsightsClient);
            } catch (Exception ex) {
                logger.log(String.format("describeApplicationInsightsApplication failed with exception %s", ex.getMessage()));
                return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
            }

            if (appNeedsUpdate(model, response)) {
                return ProgressEvent.defaultInProgressHandler(
                        CallbackContext.builder()
                                .currentStep(Step.APP_UPDATE.name())
                                .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining() - 1)
                                .build(),
                        TRANSITION_CALLBACK_DELAY_SECONDS,
                        model);
            } else {
                // APP_UPDATE step finished, start TAG_DELETION step
                List<String> tagKeysToDelete = new ArrayList<>();
                List<String> tagKeysToCreate = new ArrayList<>();
                HandlerHelper.getTagKeysToDeleteAndCreate(tagKeysToDelete, tagKeysToCreate, model, proxy, applicationInsightsClient);

                return ProgressEvent.defaultInProgressHandler(
                        CallbackContext.builder()
                                .currentStep(Step.TAG_DELETION.name())
                                .undeletedItems(tagKeysToDelete)
                                .uncreatedItems(tagKeysToCreate)
                                .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                .build(),
                        TRANSITION_CALLBACK_DELAY_SECONDS,
                        model);
            }
        } else if (currentStep.equals(Step.TAG_DELETION.name())) {
            String processingItem = newCallbackContext.getProcessingItem();
            if (processingItem == null) {
                String nextTagKeyToDelete = pickNextTagKeyToDelete(callbackContext);
                if (nextTagKeyToDelete == null) {
                    // TAG_DELETION step finished, start TAG_CREATION step
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.TAG_CREATION.name())
                                    .uncreatedItems(newCallbackContext.getUncreatedItems())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                } else {
                    try {
                        HandlerHelper.unTagApplication(nextTagKeyToDelete, model, proxy, applicationInsightsClient, logger);
                    } catch (Exception ex) {
                        logger.log(String.format("unTagApplication failed with exception %s", ex.getMessage()));
                        return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
                    }

                    List<String> tagKeysToDelete = newCallbackContext.getUndeletedItems();
                    tagKeysToDelete.remove(nextTagKeyToDelete);
                    logger.log(String.format("Updated tag keys to delete %s", tagKeysToDelete.toString()));
                    try {
                        return ProgressEvent.defaultInProgressHandler(
                                CallbackContext.builder()
                                        .currentStep(Step.TAG_DELETION.name())
                                        .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                        .processingItem(nextTagKeyToDelete)
                                        .undeletedItems(tagKeysToDelete)
                                        .uncreatedItems(newCallbackContext.getUncreatedItems())
                                        .build(),
                                TRANSITION_CALLBACK_DELAY_SECONDS,
                                model);
                    } catch (Exception ex) {
                        logger.log(String.format("return tag deletion in progress handler failed with exception %s", ex.getMessage()));
                        return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
                    }
                }
            } else {
                logger.log("On UpdateHandler lin 214.");
                if (HandlerHelper.tagDeletedForApplicaiton(processingItem, model, proxy, applicationInsightsClient, logger)) {
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.TAG_DELETION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(null)
                                    .undeletedItems(newCallbackContext.getUndeletedItems())
                                    .uncreatedItems(newCallbackContext.getUncreatedItems())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                } else {
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.TAG_DELETION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining() - 1)
                                    .processingItem(processingItem)
                                    .undeletedItems(newCallbackContext.getUndeletedItems())
                                    .uncreatedItems(newCallbackContext.getUncreatedItems())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                }
            }
        } else if (currentStep.equals(Step.TAG_CREATION.name())) {
            String processingItem = newCallbackContext.getProcessingItem();
            if (processingItem == null) {
                String nextTagKeyToCreate = pickNextTagKeyToCreate(callbackContext);

                if (nextTagKeyToCreate == null) {
                    // TAG_CREATION step finished, start COMPONENT_DELETION step
                    List<String> customCompnentNamesToDelete = new ArrayList<>();
                    List<String> customCompnentNamesToCreate = new ArrayList<>();
                    HandlerHelper.getCustomComponentNamesToDeleteAndCreate(
                            customCompnentNamesToDelete, customCompnentNamesToCreate, model, proxy, applicationInsightsClient, logger);

                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.COMPONENT_DELETION.name())
                                    .undeletedItems(customCompnentNamesToDelete)
                                    .uncreatedItems(customCompnentNamesToCreate)
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                } else {
                    try {
                        HandlerHelper.tagApplication(nextTagKeyToCreate, model, proxy, applicationInsightsClient, logger);
                    } catch (Exception ex) {
                        logger.log(String.format("tagApplication failed with exception %s", ex.getMessage()));
                        return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
                    }

                    List<String> tagKeysToCreate = newCallbackContext.getUncreatedItems();
                    tagKeysToCreate.remove(nextTagKeyToCreate);
                    logger.log(String.format("Updated tag keys to create %s", tagKeysToCreate.toString()));
                    try {
                        return ProgressEvent.defaultInProgressHandler(
                                CallbackContext.builder()
                                        .currentStep(Step.TAG_CREATION.name())
                                        .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                        .processingItem(nextTagKeyToCreate)
                                        .uncreatedItems(tagKeysToCreate)
                                        .build(),
                                TRANSITION_CALLBACK_DELAY_SECONDS,
                                model);
                    } catch (Exception ex) {
                        logger.log(String.format("return tag creation in progress handler failed with exception %s", ex.getMessage()));
                        return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
                    }
                }
            } else {
                if (HandlerHelper.tagCreatedForApplicaiton(processingItem, model, proxy, applicationInsightsClient)) {
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.TAG_CREATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(null)
                                    .uncreatedItems(newCallbackContext.getUncreatedItems())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                } else {
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.TAG_CREATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining() - 1)
                                    .processingItem(processingItem)
                                    .uncreatedItems(newCallbackContext.getUncreatedItems())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                }
            }
        } else if (currentStep.equals(Step.COMPONENT_DELETION.name())) {
            String processingItem = newCallbackContext.getProcessingItem();
            if (processingItem == null) {
                // pick the next custom component to delete
                String nextCustomComponentNameToDelete = pickNextCustomComponentNameToDelete(model, newCallbackContext);
                if (nextCustomComponentNameToDelete == null) {
                    // COMPONENT_DELETION step finished, start COMPONENT_CREATION step
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.COMPONENT_CREATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .uncreatedItems(newCallbackContext.getUncreatedItems())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                } else {
                    try {
                        HandlerHelper.deleteCustomComponent(nextCustomComponentNameToDelete, model, proxy, applicationInsightsClient);
                    } catch (Exception ex) {
                        logger.log(String.format("deleteCustomComponent failed with exception %s", ex.getMessage()));
                        return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
                    }

                    List<String> compnoentNamesToDelete = newCallbackContext.getUndeletedItems();
                    compnoentNamesToDelete.remove(nextCustomComponentNameToDelete);
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.COMPONENT_DELETION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(nextCustomComponentNameToDelete)
                                    .uncreatedItems(newCallbackContext.getUncreatedItems())
                                    .undeletedItems(compnoentNamesToDelete)
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                }
            } else {
                if (!HandlerHelper.doesCustomComponentExist(
                        model.getResourceGroupName(),
                        processingItem,
                        proxy,
                        applicationInsightsClient)) {
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.COMPONENT_DELETION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .undeletedItems(newCallbackContext.getUndeletedItems())
                                    .uncreatedItems(newCallbackContext.getUncreatedItems())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                } else {
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.COMPONENT_DELETION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining() - 1)
                                    .processingItem(processingItem)
                                    .undeletedItems(newCallbackContext.getUndeletedItems())
                                    .uncreatedItems(newCallbackContext.getUncreatedItems())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                }
            }
        } else if (currentStep.equals(Step.COMPONENT_CREATION.name())) {
            String processingItem = newCallbackContext.getProcessingItem();
            if (processingItem == null) {
                // pick the next custom component to create
                CustomComponent nextCustomComponentToCreate = pickNextCustomComponentToCreate(model, newCallbackContext);
                if (nextCustomComponentToCreate == null) {
                    // COMPONENT_CREATION step finished, start LOG_PATTERN_DELETION step
                    List<String> logSetAndPatternToDelete = new ArrayList<>();
                    List<String> logSetAndPatternToCreate = new ArrayList<>();
                    List<String> logSetAndPatternToUpdate = new ArrayList<>();

                    HandlerHelper.getlogSetAndPatternToDeleteCreateUpdate(
                            logSetAndPatternToDelete,
                            logSetAndPatternToCreate,
                            logSetAndPatternToUpdate,
                            model, proxy, applicationInsightsClient);

                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.LOG_PATTERN_DELETION.name())
                                    .undeletedItems(logSetAndPatternToDelete)
                                    .uncreatedItems(logSetAndPatternToCreate)
                                    .unupdatedItems(logSetAndPatternToUpdate)
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                } else {
                    try {
                        HandlerHelper.createCustomComponent(nextCustomComponentToCreate, model, proxy, applicationInsightsClient);
                    } catch (Exception ex) {
                        logger.log(String.format("createCustomComponent failed with exception %s", ex.getMessage()));
                        return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
                    }

                    List<String> compnoentNamesToCreate = newCallbackContext.getUncreatedItems();
                    compnoentNamesToCreate.remove(nextCustomComponentToCreate.getComponentName());
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.COMPONENT_CREATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(nextCustomComponentToCreate.getComponentName())
                                    .uncreatedItems(compnoentNamesToCreate)
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                }
            } else {
                if (HandlerHelper.doesCustomComponentExist(
                        model.getResourceGroupName(),
                        processingItem,
                        proxy,
                        applicationInsightsClient)) {
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.COMPONENT_CREATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .uncreatedItems(newCallbackContext.getUncreatedItems())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                } else {
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.COMPONENT_CREATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining() - 1)
                                    .processingItem(processingItem)
                                    .processedItems(newCallbackContext.getProcessedItems())
                                    .build(),
                            WAIT_CALLBACK_DELAY_SECONDS,
                            model);
                }
            }
        } else if (currentStep.equals(Step.LOG_PATTERN_DELETION.name())) {
            String processingItem = newCallbackContext.getProcessingItem();
            if (processingItem == null) {
                // pick the next log pattern to delete
                String nextSetPatternNamesToDelete = pickNextSetAndPatternNameToDelete(newCallbackContext);
                if (nextSetPatternNamesToDelete == null) {
                    // LOG_PATTERN_DELETION step finished, start LOG_PATTERN_CREATION step
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.LOG_PATTERN_CREATION.name())
                                    .uncreatedItems(newCallbackContext.getUncreatedItems())
                                    .unupdatedItems(newCallbackContext.getUnupdatedItems())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                } else {
                    try {
                        HandlerHelper.deleteLogPattern(
                                nextSetPatternNamesToDelete.split(":")[0], nextSetPatternNamesToDelete.split(":")[1], model, proxy, applicationInsightsClient);
                    } catch (Exception ex) {
                        logger.log(String.format("deleteLogPattern failed with exception %s", ex.getMessage()));
                        return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
                    }

                    List<String> logSetAndPatternToDelete = newCallbackContext.getUndeletedItems();
                    logSetAndPatternToDelete.remove(nextSetPatternNamesToDelete);
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.LOG_PATTERN_DELETION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(nextSetPatternNamesToDelete)
                                    .undeletedItems(logSetAndPatternToDelete)
                                    .uncreatedItems(newCallbackContext.getUncreatedItems())
                                    .unupdatedItems(newCallbackContext.getUnupdatedItems())
                                    .build(),
                            WAIT_CALLBACK_DELAY_SECONDS,
                            model);
                }
            } else {
                if (!HandlerHelper.doesLogPatternExist(
                        model.getResourceGroupName(),
                        processingItem.split(":")[0],
                        processingItem.split(":")[1],
                        proxy,
                        applicationInsightsClient)) {
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.LOG_PATTERN_DELETION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .undeletedItems(newCallbackContext.getUndeletedItems())
                                    .uncreatedItems(newCallbackContext.getUncreatedItems())
                                    .unupdatedItems(newCallbackContext.getUnupdatedItems())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                } else {
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.LOG_PATTERN_DELETION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining() - 1)
                                    .processingItem(processingItem)
                                    .undeletedItems(newCallbackContext.getUndeletedItems())
                                    .uncreatedItems(newCallbackContext.getUncreatedItems())
                                    .unupdatedItems(newCallbackContext.getUnupdatedItems())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                }
            }
        } else if (currentStep.equals(Step.LOG_PATTERN_CREATION.name())) {
            String processingItem = newCallbackContext.getProcessingItem();
            if (processingItem == null) {
                // pick the next log pattern to create
                String nextSetPatternNamesToCreate = pickNextSetAndPatternNameToCreate(newCallbackContext);

                if (nextSetPatternNamesToCreate == null) {
                    // LOG_PATTERN_CREATION step finished, start LOG_PATTERN_UPDATE step
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.LOG_PATTERN_UPDATE.name())
                                    .unupdatedItems(newCallbackContext.getUnupdatedItems())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                } else {
                    LogPattern nextLogPatternToCreate = HandlerHelper.pickLogPatternFromModel(
                            nextSetPatternNamesToCreate.split(":")[0],
                            nextSetPatternNamesToCreate.split(":")[1],
                            model);
                    try {
                        HandlerHelper.createLogPattern(
                                nextSetPatternNamesToCreate.split(":")[0], nextLogPatternToCreate, model, proxy, applicationInsightsClient);
                    } catch (Exception ex) {
                        logger.log(String.format("createLogPattern failed with exception %s", ex.getMessage()));
                        return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
                    }

                    List<String> logSetAndPatternToCreate = newCallbackContext.getUncreatedItems();
                    logSetAndPatternToCreate.remove(nextSetPatternNamesToCreate);
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.LOG_PATTERN_CREATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(nextSetPatternNamesToCreate)
                                    .uncreatedItems(logSetAndPatternToCreate)
                                    .unupdatedItems(newCallbackContext.getUnupdatedItems())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                }
            } else {
                if (HandlerHelper.doesLogPatternExist(
                        model.getResourceGroupName(),
                        processingItem.split(":")[0],
                        processingItem.split(":")[1],
                        proxy,
                        applicationInsightsClient)) {
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.LOG_PATTERN_CREATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .uncreatedItems(newCallbackContext.getUncreatedItems())
                                    .unupdatedItems(newCallbackContext.getUnupdatedItems())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                } else {
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.LOG_PATTERN_CREATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining() - 1)
                                    .processingItem(processingItem)
                                    .uncreatedItems(newCallbackContext.getUncreatedItems())
                                    .unupdatedItems(newCallbackContext.getUnupdatedItems())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                }
            }
        } else if (currentStep.equals(Step.LOG_PATTERN_UPDATE.name())) {
            String processingItem = newCallbackContext.getProcessingItem();
            if (processingItem == null) {
                // pick the next log pattern to update
                String nextSetPatternNamesToUpdate = pickNextSetAndPatternNameToUpdate(newCallbackContext);

                if (nextSetPatternNamesToUpdate == null) {
                    // LOG_PATTERN_CREATION step finished, start COMPONENT_CONFIGURATION step
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.COMPONENT_CONFIGURATION.name())
                                    .processedItems(new HashSet<>())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                } else {
                    LogPattern nextLogPatternToUpdate = HandlerHelper.pickLogPatternFromModel(
                            nextSetPatternNamesToUpdate.split(":")[0],
                            nextSetPatternNamesToUpdate.split(":")[1],
                            model);
                    try {
                        HandlerHelper.updateLogPattern(
                                nextSetPatternNamesToUpdate.split(":")[0],
                                nextLogPatternToUpdate,
                                model,
                                proxy,
                                applicationInsightsClient);
                    } catch (Exception ex) {
                        logger.log(String.format("updateLogPattern failed with exception %s", ex.getMessage()));
                        return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
                    }

                    List<String> logSetAndPatternToUpdate = newCallbackContext.getUnupdatedItems();
                    logSetAndPatternToUpdate.remove(nextSetPatternNamesToUpdate);
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.LOG_PATTERN_UPDATE.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(nextSetPatternNamesToUpdate)
                                    .unupdatedItems(logSetAndPatternToUpdate)
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                }
            } else {
                if (HandlerHelper.isLogPatternSyncedWithModel(
                        processingItem.split(":")[0],
                        processingItem.split(":")[1],
                        model,
                        proxy,
                        applicationInsightsClient)) {
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.LOG_PATTERN_UPDATE.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .unupdatedItems(newCallbackContext.getUnupdatedItems())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                } else {
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.LOG_PATTERN_UPDATE.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining() - 1)
                                    .processingItem(processingItem)
                                    .unupdatedItems(newCallbackContext.getUnupdatedItems())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                }
            }
        } else if (currentStep.equals(Step.COMPONENT_CONFIGURATION.name())) {
            String processingItem = newCallbackContext.getProcessingItem();
            if (processingItem == null) {
                // pick the next component configuration to process
                ComponentMonitoringSetting nextComponentMonitoringSetting = HandlerHelper.pickNextComponentMonitoringSetting(model, newCallbackContext);
                if (nextComponentMonitoringSetting == null) {
                    // COMPONENT_CONFIGURATION step finished,
                    // start DEFAULT_COMPONENT_CONFIGURATION step if AutoConfigurationEnabled set to true,
                    // otherwise succeed the process.

                    List<String> autoConfigComponentNames = HandlerHelper.getApplicationAllComponentNames(
                            model, proxy, applicationInsightsClient);
                    List<String> configuredComponentNames = HandlerHelper.getConfiguredComponentNames(model);
                    autoConfigComponentNames.removeAll(configuredComponentNames);

                    if (autoConfigComponentNames != null && !autoConfigComponentNames.isEmpty()) {
                        Boolean autoConfigurationEnabled = model.getAutoConfigurationEnabled();
                        if (autoConfigurationEnabled != null && autoConfigurationEnabled == true) {
                            return ProgressEvent.defaultInProgressHandler(
                                    CallbackContext.builder()
                                            .currentStep(Step.DEFAULT_COMPONENT_CONFIGURATION.name())
                                            .unprocessedItems(autoConfigComponentNames)
                                            .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                            .build(),
                                    TRANSITION_CALLBACK_DELAY_SECONDS,
                                    model);
                        } else {
                            return ProgressEvent.defaultInProgressHandler(
                                    CallbackContext.builder()
                                            .currentStep(Step.DISABLE_COMPONENT_CONFIGURATION.name())
                                            .unprocessedItems(autoConfigComponentNames)
                                            .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                            .build(),
                                    TRANSITION_CALLBACK_DELAY_SECONDS,
                                    model);
                        }
                    }

                    return ProgressEvent.defaultSuccessHandler(model);
                } else {
                    try {
                        HandlerHelper.createComponentConfiguration(nextComponentMonitoringSetting, model, proxy, applicationInsightsClient, logger);
                    } catch (Exception ex) {
                        logger.log(String.format("createComponentConfiguration failed with exception %s", ex.getMessage()));
                        return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
                    }

                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.COMPONENT_CONFIGURATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(HandlerHelper.getComponentNameOrARNFromComponentMonitoringSetting(nextComponentMonitoringSetting))
                                    .processedItems(newCallbackContext.getProcessedItems())
                                    .build(),
                            WAIT_CALLBACK_DELAY_SECONDS,
                            model);
                }
            } else {
                String currentApplicationLifeCycle = HandlerHelper.getApplicationLifeCycle(model, proxy, applicationInsightsClient);
                if (currentApplicationLifeCycle.equals(CONFIGURATION_FINISHED_LIFECYCLE)) {
                    Set<String> newProcessedItems = new HashSet<>(newCallbackContext.getProcessedItems());
                    newProcessedItems.add(processingItem);
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.COMPONENT_CONFIGURATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(null)
                                    .processedItems(newProcessedItems)
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                } else {
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.COMPONENT_CONFIGURATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining() - 1)
                                    .processingItem(processingItem)
                                    .processedItems(newCallbackContext.getProcessedItems())
                                    .build(),
                            WAIT_CALLBACK_DELAY_SECONDS,
                            model);
                }
            }
        } else if (currentStep.equals(Step.DEFAULT_COMPONENT_CONFIGURATION.name())) {
            String processingItem = newCallbackContext.getProcessingItem();
            if (processingItem == null) {
                // pick the next component for default configuration
                String nextAutoConfigComponentNameOrARN = HandlerHelper.pickNextConfigurationComponent(callbackContext);

                if (nextAutoConfigComponentNameOrARN == null) {
                    return ProgressEvent.defaultSuccessHandler(model);
                } else {
                    try {
                        HandlerHelper.createDefaultComponentConfiguration(
                                nextAutoConfigComponentNameOrARN, DEFAULT_TIER, model, proxy, applicationInsightsClient, logger);
                    } catch (Exception ex) {
                        logger.log(String.format("createDefaultComponentConfiguration failed with exception %s", ex.getMessage()));
                        return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
                    }

                    List<String> unprocessedDefaultConfiguationComponents = newCallbackContext.getUnprocessedItems();
                    unprocessedDefaultConfiguationComponents.remove(nextAutoConfigComponentNameOrARN);
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.DEFAULT_COMPONENT_CONFIGURATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(nextAutoConfigComponentNameOrARN)
                                    .unprocessedItems(unprocessedDefaultConfiguationComponents)
                                    .build(),
                            WAIT_CALLBACK_DELAY_SECONDS,
                            model);
                }
            } else {
                String currentApplicationLifeCycle = HandlerHelper.getApplicationLifeCycle(model, proxy, applicationInsightsClient);
                if (currentApplicationLifeCycle.equals(CONFIGURATION_FINISHED_LIFECYCLE)) {
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.DEFAULT_COMPONENT_CONFIGURATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(null)
                                    .unprocessedItems(newCallbackContext.getUnprocessedItems())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                } else {
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.DEFAULT_COMPONENT_CONFIGURATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining() - 1)
                                    .processingItem(processingItem)
                                    .unprocessedItems(newCallbackContext.getUnprocessedItems())
                                    .build(),
                            WAIT_CALLBACK_DELAY_SECONDS,
                            model);
                }
            }
        } else if (currentStep.equals(Step.DISABLE_COMPONENT_CONFIGURATION.name())) {
            String processingItem = newCallbackContext.getProcessingItem();
            if (processingItem == null) {
                // pick the next component to disable configuration
                String nextDisableConfigComponentNameOrARN = HandlerHelper.pickNextConfigurationComponent(callbackContext);

                if (nextDisableConfigComponentNameOrARN == null) {
                    return ProgressEvent.defaultSuccessHandler(model);
                } else {
                    try {
                        HandlerHelper.disableComponentConfiguration(
                                nextDisableConfigComponentNameOrARN, model, proxy, applicationInsightsClient, logger);
                    } catch (Exception ex) {
                        logger.log(String.format("disableComponentConfiguration failed with exception %s", ex.getMessage()));
                        return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
                    }

                    List<String> unprocessedDisableConfiguationComponents = newCallbackContext.getUnprocessedItems();
                    unprocessedDisableConfiguationComponents.remove(nextDisableConfigComponentNameOrARN);
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.DISABLE_COMPONENT_CONFIGURATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(nextDisableConfigComponentNameOrARN)
                                    .unprocessedItems(unprocessedDisableConfiguationComponents)
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                }
            } else {
                if (!HandlerHelper.isComponentConfigurationEnabled(processingItem, model, proxy, applicationInsightsClient)) {
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.DISABLE_COMPONENT_CONFIGURATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(null)
                                    .unprocessedItems(newCallbackContext.getUnprocessedItems())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                } else {
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.DISABLE_COMPONENT_CONFIGURATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining() - 1)
                                    .processingItem(processingItem)
                                    .unprocessedItems(newCallbackContext.getUnprocessedItems())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                }
            }
        }

        else {
            return ProgressEvent.defaultSuccessHandler(model);
        }
    }

    private String pickNextSetAndPatternNameToUpdate(
            CallbackContext callbackContext) {
        List<String> logSetAndPatternToUpdate = callbackContext.getUnupdatedItems();
        if (logSetAndPatternToUpdate == null || logSetAndPatternToUpdate.isEmpty()) {
            return null;
        }

        return logSetAndPatternToUpdate.get(0);
    }

    private String pickNextSetAndPatternNameToCreate(
            CallbackContext callbackContext) {
        List<String> logSetAndPatternToCreate = callbackContext.getUncreatedItems();
        if (logSetAndPatternToCreate == null || logSetAndPatternToCreate.isEmpty()) {
            return null;
        }

        return logSetAndPatternToCreate.get(0);
    }

    private String pickNextSetAndPatternNameToDelete(
            CallbackContext callbackContext) {
        List<String> logSetAndPatternToDelete = callbackContext.getUndeletedItems();
        if (logSetAndPatternToDelete == null || logSetAndPatternToDelete.isEmpty()) {
            return null;
        }

        return logSetAndPatternToDelete.get(0);
    }

    private CustomComponent pickNextCustomComponentToCreate(
            ResourceModel model,
            CallbackContext callbackContext) {
        List<String> unCreatedCompnentNames = callbackContext.getUncreatedItems();
        if (unCreatedCompnentNames == null || unCreatedCompnentNames.isEmpty()) {
            return null;
        }

        String nextCustomComponentNameToCreate = unCreatedCompnentNames.get(0);
        for (CustomComponent customComponent : model.getCustomComponents()) {
            if (customComponent.getComponentName().equals(nextCustomComponentNameToCreate)) {
                return customComponent;
            }
        }

        return null;
    }

    private String pickNextCustomComponentNameToDelete(
            ResourceModel model,
            CallbackContext newCallbackContext) {
        List<String> unDeletedCompnentNames = newCallbackContext.getUndeletedItems();
        if (unDeletedCompnentNames == null || unDeletedCompnentNames.isEmpty()) {
            return null;
        } else {
            return unDeletedCompnentNames.get(0);
        }
    }

    private String pickNextTagKeyToCreate(CallbackContext callbackContext) {
        List<String> tagKeysToCreate = callbackContext.getUncreatedItems();
        if (tagKeysToCreate == null || tagKeysToCreate.isEmpty()) {
            return null;
        } else {
            return tagKeysToCreate.get(0);
        }
    }

    private String pickNextTagKeyToDelete(CallbackContext callbackContext) {
        List<String> tagKeysToDelete = callbackContext.getUndeletedItems();
        if (tagKeysToDelete == null || tagKeysToDelete.isEmpty()) {
            return null;
        } else {
            return tagKeysToDelete.get(0);
        }
    }

    private boolean appNeedsUpdate(ResourceModel model, DescribeApplicationResponse response) {
        Boolean newCWEMonitorEnabled = model.getCWEMonitorEnabled() == null ?
                false : model.getCWEMonitorEnabled();
        Boolean preCWEMonitorEnabled = response.applicationInfo().cweMonitorEnabled() == null ?
                false : response.applicationInfo().cweMonitorEnabled();
        if (newCWEMonitorEnabled != preCWEMonitorEnabled) {
            return true;
        }

        Boolean newOpsCenterEnabled = model.getOpsCenterEnabled() == null ?
                false : model.getOpsCenterEnabled();
        Boolean preOpsCenterEnabled = response.applicationInfo().opsCenterEnabled() == null ?
                false : response.applicationInfo().opsCenterEnabled();
        if (newOpsCenterEnabled != preOpsCenterEnabled) {
            return true;
        }

        String newOpsItemSNSTopicArn = model.getOpsItemSNSTopicArn();
        String preOpsItemSNSTopicArn = response.applicationInfo().opsItemSNSTopicArn();
        if (!((newOpsItemSNSTopicArn == null && preOpsItemSNSTopicArn == null) ||
                newOpsItemSNSTopicArn.equals(preOpsItemSNSTopicArn))) {
            return true;
        }

        return false;
    }
}
