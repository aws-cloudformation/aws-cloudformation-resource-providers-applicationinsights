package software.amazon.applicationinsights.application;

import org.junitpioneer.jupiter.SetEnvironmentVariable;
import software.amazon.awssdk.services.applicationinsights.model.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static software.amazon.applicationinsights.application.Constants.ACCOUNT_BASED_GROUPING_TYPE;
import static software.amazon.applicationinsights.application.Constants.SHADOW_RG_PREFIX_ACCOUNT_BASED;
import static software.amazon.applicationinsights.application.CreateHandlerTest.RESOURCE_GROUP_NAME;

@ExtendWith(MockitoExtension.class)
@SetEnvironmentVariable(key = "AWS_REGION", value = "us-east-1")
public class UpdateHandlerTest {
    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private ResourceModel desiredResourceModel;
    private ResourceModel previousResourceStateModel;
    private ResourceHandlerRequest<ResourceModel> request;
    private UpdateHandler updateHandler;

    @BeforeEach
    public void setup() {

        desiredResourceModel = ResourceModel.builder().build();
        previousResourceStateModel = ResourceModel.builder().build();
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredResourceModel)
                .previousResourceState(previousResourceStateModel)
                .awsPartition("aws")
                .region("us-east-1")
                .awsAccountId("123456789101")
                .build();
        updateHandler = new UpdateHandler();
    }

    @Test
    public void handleRequest_AccountBased() {

        desiredResourceModel.setResourceGroupName(RESOURCE_GROUP_NAME);
        desiredResourceModel.setGroupingType(ACCOUNT_BASED_GROUPING_TYPE);
        previousResourceStateModel.setResourceGroupName(RESOURCE_GROUP_NAME);
        previousResourceStateModel.setGroupingType(ACCOUNT_BASED_GROUPING_TYPE);

        final ProgressEvent<ResourceModel, CallbackContext> response = updateHandler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getResult()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(1);
        assertThat(response.getResourceModels()).isNull();
    }

    @Test
    public void handleRequest_new_callback_context() {

        desiredResourceModel.setResourceGroupName(RESOURCE_GROUP_NAME);
        previousResourceStateModel.setResourceGroupName(RESOURCE_GROUP_NAME);
        CallbackContext callbackContext = new CallbackContext();
        callbackContext.setStabilizationRetriesRemaining(0);
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(()->updateHandler.handleRequest(proxy, request, callbackContext, logger));
    }

    @Test
    public void handleRequest_AppUpdate_Step() {

        desiredResourceModel.setResourceGroupName(SHADOW_RG_PREFIX_ACCOUNT_BASED + RESOURCE_GROUP_NAME);
        desiredResourceModel.setGroupingType(ACCOUNT_BASED_GROUPING_TYPE);
        previousResourceStateModel.setResourceGroupName(SHADOW_RG_PREFIX_ACCOUNT_BASED + RESOURCE_GROUP_NAME);
        previousResourceStateModel.setGroupingType(ACCOUNT_BASED_GROUPING_TYPE);

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(ResourceNotFoundException.builder().message("Could not find app").build());
        final ProgressEvent<ResourceModel, CallbackContext> response = updateHandler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo("Application does not exit for resource group ApplicationInsights-TestRGName");
        assertThat(response.getResult()).isNull();
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
    }

    @Test
    public void handleRequest_different_resource_group() {

        desiredResourceModel.setResourceGroupName(RESOURCE_GROUP_NAME);
        previousResourceStateModel.setResourceGroupName(RESOURCE_GROUP_NAME + RESOURCE_GROUP_NAME);
        final ProgressEvent<ResourceModel, CallbackContext> response = updateHandler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo("Application cannot be updated as the Resource Group Name was changed");
        assertThat(response.getResult()).isNull();
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
    }
}
