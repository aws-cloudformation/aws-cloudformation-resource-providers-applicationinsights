package software.amazon.applicationinsights.application.StepWorkflow;

import software.amazon.applicationinsights.application.CallbackContext;
import software.amazon.applicationinsights.application.ExceptionMapper;
import software.amazon.applicationinsights.application.ResourceModel;
import software.amazon.applicationinsights.application.Step;
import software.amazon.awssdk.services.applicationinsights.ApplicationInsightsClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;

import java.util.List;

import static software.amazon.applicationinsights.application.Constants.TRANSITION_CALLBACK_DELAY_SECONDS;

public abstract class BaseStepWorkflow {

    private ResourceModel model;
    private CallbackContext callbackContext;
    private AmazonWebServicesClientProxy proxy;
    private ApplicationInsightsClient applicationInsightsClient;
    private Logger logger;
    private ProgressEvent<ResourceModel, CallbackContext> nextStepInitProgressEvent;

    public BaseStepWorkflow(
            final ResourceModel model,
            final CallbackContext callbackContext,
            final AmazonWebServicesClientProxy proxy,
            final ApplicationInsightsClient applicationInsightsClient,
            final Logger logger,
            final ProgressEvent<ResourceModel, CallbackContext> nextStepInitProgressEvent) {
        this.model = model;
        this.callbackContext = callbackContext;
        this.proxy = proxy;
        this.applicationInsightsClient = applicationInsightsClient;
        this.logger = logger;
        this.nextStepInitProgressEvent = nextStepInitProgressEvent;
    }

    public ProgressEvent<ResourceModel, CallbackContext> execute() {
        Step currentStep = Step.fromStepName(callbackContext.getCurrentStep());
        String processingItem = callbackContext.getProcessingItem();
        if (processingItem == null) {
            // pick next item to process
            String nextItemToProcess = pickNextItemToProcess(callbackContext);
            if (nextItemToProcess == null) {
                // current step finishes, start next step
                return nextStepInitProgressEvent;
            } else {

                try {
                    startProcessNextItem(nextItemToProcess, model, proxy, applicationInsightsClient, logger);
                } catch (Exception ex) {
                    logger.log(String.format("startProcessNextItem failed with exception %s", ex.getMessage()));
                    return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
                }

                List<String> unProcessedItems = callbackContext.getUnprocessedItems();
                unProcessedItems.remove(nextItemToProcess);
                return ProgressEvent.defaultInProgressHandler(
                        CallbackContext.builder()
                                .currentStep(currentStep.name())
                                .stabilizationRetriesRemaining(callbackContext.getStabilizationRetriesRemaining())
                                .processingItem(nextItemToProcess)
                                .unprocessedItems(unProcessedItems)
                                .build(),
                        currentStep.getCallBackWaitSeconds(),
                        model);
            }
        } else {
            boolean currentItemProcessFinished;
            try {
                currentItemProcessFinished = isCurrentItemProcessFinished(processingItem, model, proxy, applicationInsightsClient, logger);
            } catch (Exception ex) {
                logger.log(String.format("isCurrentItemProcessFinished failed with exception %s", ex.getMessage()));
                return ProgressEvent.defaultFailureHandler(ex, ExceptionMapper.mapToHandlerErrorCode(ex));
            }

            if (currentItemProcessFinished) {
                return ProgressEvent.defaultInProgressHandler(
                        CallbackContext.builder()
                                .currentStep(currentStep.name())
                                .stabilizationRetriesRemaining(callbackContext.getStabilizationRetriesRemaining())
                                .processingItem(null)
                                .unprocessedItems(callbackContext.getUnprocessedItems())
                                .build(),
                        TRANSITION_CALLBACK_DELAY_SECONDS,
                        model);
            } else {
                return ProgressEvent.defaultInProgressHandler(
                        CallbackContext.builder()
                                .currentStep(currentStep.name())
                                .stabilizationRetriesRemaining(callbackContext.getStabilizationRetriesRemaining() - 1)
                                .processingItem(processingItem)
                                .unprocessedItems(callbackContext.getUnprocessedItems())
                                .build(),
                        currentStep.getCallBackWaitSeconds(),
                        model);
            }
        }
    }

    protected abstract boolean isCurrentItemProcessFinished(
            String processingItem,
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient,
            Logger logger);

    protected abstract void startProcessNextItem(
            String nextItemToProcess,
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient,
            Logger logger);

    private String pickNextItemToProcess(CallbackContext callbackContext) {
        List<String> unProcessedItems = callbackContext.getUnprocessedItems();
        if (unProcessedItems == null || unProcessedItems.isEmpty()) {
            return null;
        }

        return unProcessedItems.get(0);
    }
}
