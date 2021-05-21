package software.amazon.applicationinsights.application;

import com.amazon.junit.extension.EnvironmentVariable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.exceptions.misusing.InvalidUseOfMatchersException;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.applicationinsights.model.Tag;
import software.amazon.awssdk.services.applicationinsights.model.*;
import software.amazon.cloudformation.proxy.*;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@EnvironmentVariable(key = "AWS_REGION", value = "us-east-1")
public class ReadHandlerTest {

    private static final String RESOURCE_GROUP_NAME = "TestRGName";
    private static final String APP_ARN = "arn:aws:applicationinsights:us-east-1:000000000000:application/resource-group/" + RESOURCE_GROUP_NAME;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private ResourceModel desiredResourceModel;
    private ResourceHandlerRequest<ResourceModel> request;

    private ReadHandler readHandler;

    @BeforeEach
    public void setup() {
        desiredResourceModel = ResourceModel.builder().build();
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredResourceModel)
                .awsPartition("aws")
                .region("us-east-1")
                .awsAccountId("000000000000")
                .build();
        readHandler = new ReadHandler();
    }

    @Test
    public void handleRequest_Failed_CouldNotParseApplicationArn() {
        desiredResourceModel.setApplicationARN("fff");

        ProgressEvent<ResourceModel, CallbackContext> result =
                readHandler.handleRequest(proxy, request, null, logger);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(result.getMessage()).isEqualTo("Failed to parse application ARN: fff");
    }

    @Test
    public void handleRequest_Failed_CouldNotFindApplication() {
        desiredResourceModel.setApplicationARN(APP_ARN);

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(ResourceNotFoundException.builder().message("Could not find app").build());

        ProgressEvent<ResourceModel, CallbackContext> result =
                readHandler.handleRequest(proxy, request, null, logger);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OperationStatus.FAILED);
    }

    @Test
    public void handleRequest_Failed_CouldNotGenerateReadModel() {
        desiredResourceModel.setApplicationARN(APP_ARN);

        ProgressEvent<ResourceModel, CallbackContext> result =
                readHandler.handleRequest(proxy, request, null, logger);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OperationStatus.FAILED);
    }

    @Test
    public void handleRequest_Success() {
        desiredResourceModel.setApplicationARN(APP_ARN);

        when(proxy.injectCredentialsAndInvokeV2(any(), any())).then(invocation -> {
            ApplicationInsightsRequest request = invocation.getArgument(0);
            if (request.equals(DescribeApplicationRequest.builder()
                    .resourceGroupName(RESOURCE_GROUP_NAME)
                    .build())) {
                return DescribeApplicationResponse.builder()
                        .applicationInfo(
                                ApplicationInfo.builder()
                                        .cweMonitorEnabled(false)
                                        .opsCenterEnabled(true)
                                        .opsItemSNSTopicArn("opsItemArn")
                                        .build())
                        .build();
            } else if (request.equals(ListTagsForResourceRequest.builder().resourceARN(APP_ARN).build())) {
                return ListTagsForResourceResponse.builder()
                        .tags(Tag.builder().key("key").value("val").build())
                        .build();
            } else if (request.equals(ListComponentsRequest.builder().resourceGroupName(RESOURCE_GROUP_NAME).maxResults(40).build())) {
                return ListComponentsResponse.builder().build();
            } else if (request.equals(ListLogPatternsRequest.builder().resourceGroupName(RESOURCE_GROUP_NAME).build())) {
                return ListLogPatternsResponse.builder().build();
            }
            throw new InvalidUseOfMatchersException("Argument has no matcher: " + request);
        });

        ProgressEvent<ResourceModel, CallbackContext> result =
                readHandler.handleRequest(proxy, request, null, logger);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(result.getResourceModel().getResourceGroupName()).isEqualTo(RESOURCE_GROUP_NAME);
        assertThat(result.getResourceModel().getApplicationARN()).isEqualTo(APP_ARN);
        assertThat(result.getResourceModel().getOpsCenterEnabled()).isEqualTo(true);
        assertThat(result.getResourceModel().getOpsItemSNSTopicArn()).isEqualTo("opsItemArn");
        assertThat(result.getResourceModel().getTags())
                .isEqualTo(Arrays.asList(
                        software.amazon.applicationinsights.application.Tag.builder().key("key").value("val").build()));
    }
}
