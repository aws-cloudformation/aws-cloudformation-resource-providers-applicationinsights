package software.amazon.applicationinsights.application;

import software.amazon.applicationinsights.application.StepWorkflow.AppCreationStepWorkflow;
import software.amazon.awssdk.services.applicationinsights.ApplicationInsightsClient;
import software.amazon.awssdk.services.applicationinsights.model.ResourceInUseException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static software.amazon.applicationinsights.application.Constants.APP_CREATION_FINISHED_LIFECYCLE;
import static software.amazon.applicationinsights.application.Constants.CONFIGURATION_FINISHED_LIFECYCLE;
import static software.amazon.applicationinsights.application.Constants.DEFAULT_TIER;
import static software.amazon.applicationinsights.application.Constants.TRANSITION_CALLBACK_DELAY_SECONDS;
import static software.amazon.applicationinsights.application.Constants.WAIT_CALLBACK_DELAY_SECONDS;

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
        final CallbackContext newCallbackContext = callbackContext == null ?
                CallbackContext.builder().stabilizationRetriesRemaining(CREATE_STATUS_POLL_RETRIES).build() :
                callbackContext;

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


//        if (currentStep == null) {
//            try {
//                HandlerHelper.createApplicationInsightsApplication(model, proxy, applicationInsightsClient);
//            } catch (Exception ex) {
//                logger.log(String.format("createApplicationInsightsApplication failed with exception %s", ex.getMessage()));
//                return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
//            }
//
//            return ProgressEvent.defaultInProgressHandler(
//                    CallbackContext.builder()
//                            .currentStep(Step.APP_CREATION.name())
//                            .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
//                            .build(),
//                    WAIT_CALLBACK_DELAY_SECONDS,
//                    model);
//        } else if (currentStep.equals(Step.APP_CREATION.name())) {
//            String currentApplicationLifeCycle = HandlerHelper.getApplicationLifeCycle(model, proxy, applicationInsightsClient);
//            if (currentApplicationLifeCycle.equals(APP_CREATION_FINISHED_LIFECYCLE)) {
//                // APP_CREATION step finished, start COMPONENT_CREATION step
//                return ProgressEvent.defaultInProgressHandler(
//                        CallbackContext.builder()
//                                .currentStep(Step.COMPONENT_CREATION.name())
//                                .processedItems(new HashSet<>())
//                                .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
//                                .build(),
//                        TRANSITION_CALLBACK_DELAY_SECONDS,
//                        model);
//
//            } else {
//                return ProgressEvent.defaultInProgressHandler(
//                        CallbackContext.builder()
//                                .currentStep(Step.APP_CREATION.name())
//                                .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining() - 1)
//                                .build(),
//                        WAIT_CALLBACK_DELAY_SECONDS,
//                        model);
//            }
//        }


        if (currentStep == null) {
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
                            .processingItem(model.getResourceGroupName())
                            .unprocessedItems(null)
                            .build(),
                    Step.APP_CREATION.getCallBackWaitSeconds(),
                    model);
        } else if (currentStep.equals(Step.APP_CREATION.name())) {
            ProgressEvent<ResourceModel, CallbackContext> componentCreationStepInitProgressEvent =
                    ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.COMPONENT_CREATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(null)
                                    .processedItems(new HashSet<>())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);

            return new AppCreationStepWorkflow(model, callbackContext, proxy, applicationInsightsClient, logger, componentCreationStepInitProgressEvent)
                    .execute();
        }




        else if (currentStep.equals(Step.COMPONENT_CREATION.name())) {
            String processingItem = newCallbackContext.getProcessingItem();
            if (processingItem == null) {
                // pick the next custom component to create
                CustomComponent nextCustomComponent = pickNextCustomComponent(model, newCallbackContext);
                if (nextCustomComponent == null) {
                    // COMPONENT_CREATION step finished, start LOG_PATTERN_CREATION step
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.LOG_PATTERN_CREATION.name())
                                    .processedItems(new HashSet<>())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                } else {
                    try {
                        HandlerHelper.createCustomComponent(nextCustomComponent, model, proxy, applicationInsightsClient);
                    } catch (Exception ex) {
                        logger.log(String.format("createCustomComponent failed with exception %s", ex.getMessage()));
                        return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
                    }

                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.COMPONENT_CREATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(nextCustomComponent.getComponentName())
                                    .processedItems(newCallbackContext.getProcessedItems())
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
                    Set<String> newProcessedItems = new HashSet<>(newCallbackContext.getProcessedItems());
                    newProcessedItems.add(processingItem);
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.COMPONENT_CREATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(null)
                                    .processedItems(newProcessedItems)
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
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                }
            }
        } else if (currentStep.equals(Step.LOG_PATTERN_CREATION.name())) {
            String processingItem = newCallbackContext.getProcessingItem();
            if (processingItem == null) {
                // pick the next log pattern to process
                Map.Entry<String, LogPattern> nextLogPattern = pickNextLogPattern(model, newCallbackContext);
                if (nextLogPattern == null) {
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
                    try {
                        HandlerHelper.createLogPattern(
                                nextLogPattern.getKey(), nextLogPattern.getValue(), model, proxy, applicationInsightsClient);
                    } catch (Exception ex) {
                        logger.log(String.format("createLogPattern failed with exception %s", ex.getMessage()));
                        return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
                    }

                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.LOG_PATTERN_CREATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(HandlerHelper.generateLogPatternIdentifier(nextLogPattern.getKey(), nextLogPattern.getValue().getPatternName()))
                                    .processedItems(newCallbackContext.getProcessedItems())
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
                    Set<String> newProcessedItems = new HashSet<>(newCallbackContext.getProcessedItems());
                    newProcessedItems.add(processingItem);
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.LOG_PATTERN_CREATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining())
                                    .processingItem(null)
                                    .processedItems(newProcessedItems)
                                    .build(),
                            TRANSITION_CALLBACK_DELAY_SECONDS,
                            model);
                } else {
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder()
                                    .currentStep(Step.LOG_PATTERN_CREATION.name())
                                    .stabilizationRetriesRemaining(newCallbackContext.getStabilizationRetriesRemaining() - 1)
                                    .processingItem(processingItem)
                                    .processedItems(newCallbackContext.getProcessedItems())
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
                    Boolean autoConfigurationEnabled = model.getAutoConfigurationEnabled();
                    if (autoConfigurationEnabled != null && autoConfigurationEnabled == true) {
                        List<String> autoConfigComponentNames = HandlerHelper.getApplicationAllComponentNames(
                                model, proxy, applicationInsightsClient);
                        List<String> configuredComponentNames = HandlerHelper.getConfiguredComponentNames(model);
                        autoConfigComponentNames.removeAll(configuredComponentNames);

                        if (autoConfigComponentNames != null && !autoConfigComponentNames.isEmpty()) {
                            return ProgressEvent.defaultInProgressHandler(
                                    CallbackContext.builder()
                                            .currentStep(Step.DEFAULT_COMPONENT_CONFIGURATION.name())
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
                String currentApplicationLifeCycle = HandlerHelper.getApplicationLifeCycle(model.getResourceGroupName(), proxy, applicationInsightsClient);
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
                String nextAutoConfigComponentNameOrARN = HandlerHelper.pickNextConfigurationComponent(newCallbackContext);

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
                String currentApplicationLifeCycle = HandlerHelper.getApplicationLifeCycle(model.getResourceGroupName(), proxy, applicationInsightsClient);
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
        } else {
            return ProgressEvent.defaultSuccessHandler(model);
        }
    }

    private Map.Entry<String, LogPattern> pickNextLogPattern(
            ResourceModel model,
            CallbackContext callbackContext) {
        List<LogPatternSet> logPatternSets = model.getLogPatternSets();
        if (logPatternSets == null || logPatternSets.isEmpty()) {
            return null;
        }

        for (LogPatternSet logPatternSet : logPatternSets) {
            String patternSetName = logPatternSet.getPatternSetName();
            List<LogPattern> logPatterns = logPatternSet.getLogPatterns();
            if (logPatterns == null || logPatterns.isEmpty()) {
                continue;
            }

            for (LogPattern logPattern : logPatterns) {
                if (!callbackContext.getProcessedItems()
                        .contains(HandlerHelper.generateLogPatternIdentifier(patternSetName, logPattern.getPatternName()))) {
                    return new AbstractMap.SimpleEntry<>(patternSetName, logPattern);
                }
            }
        }

        return null;
    }

    private CustomComponent pickNextCustomComponent(
            ResourceModel model,
            CallbackContext callbackContext) {
        List<CustomComponent> customComponents = model.getCustomComponents();
        if (customComponents == null || customComponents.isEmpty()) {
            return null;
        }

        for (CustomComponent customComponent : customComponents) {
            if (!callbackContext.getProcessedItems().contains(customComponent.getComponentName())) {
                return customComponent;
            }
        }

        return null;
    }

}
