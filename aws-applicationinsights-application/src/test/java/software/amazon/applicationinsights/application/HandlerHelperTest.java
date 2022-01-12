package software.amazon.applicationinsights.application;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.applicationinsights.model.ApplicationInfo;
import software.amazon.awssdk.services.applicationinsights.model.DescribeApplicationResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HandlerHelperTest {
    @Test
    public void extractResourceGroupNameFromApplicationArn_correct() throws Throwable {
        String applicationArn = "arn:aws:applicationinsights:us-west-1:000000000000:application/resource-group/fff";
        String resourceGroupName = HandlerHelper.extractResourceGroupNameFromApplicationArn(applicationArn);
        assertEquals("fff", resourceGroupName);
    }

    @Test
    public void extractResourceGroupNameFromApplicationArn_incorrect() {
        String applicationArn = "afff";
        assertThrows(Exception.class, () -> HandlerHelper.extractResourceGroupNameFromApplicationArn(applicationArn));
    }

    @Test
    public void appNeedsUpdate_cwe_not_equal() {

        ResourceModel model = HandlerHelperTest.createTestModel(null, true, "sns", true);
        DescribeApplicationResponse response = HandlerHelperTest.createDescribeApplicationResponse(false, true, "sns", true);
        assertTrue(HandlerHelper.appNeedsUpdate(model, response));

        model = HandlerHelperTest.createTestModel(true, true, "sns", true);
        response = HandlerHelperTest.createDescribeApplicationResponse(false, true, "sns", true);
        assertTrue(HandlerHelper.appNeedsUpdate(model, response));

        model = HandlerHelperTest.createTestModel(false, true, "sns", true);
        response = HandlerHelperTest.createDescribeApplicationResponse(true, true, "sns", true);
        assertTrue(HandlerHelper.appNeedsUpdate(model, response));
    }

    @Test
    public void appNeedsUpdate_cwe_equal() {
        ResourceModel model = HandlerHelperTest.createTestModel(true, true, "sns", true);
        DescribeApplicationResponse response = HandlerHelperTest.createDescribeApplicationResponse(true, true, "sns", true);
        assertFalse(HandlerHelper.appNeedsUpdate(model, response));

        model = HandlerHelperTest.createTestModel(false, true, "sns", true);
        response = HandlerHelperTest.createDescribeApplicationResponse(null, true, "sns", true);
        assertFalse(HandlerHelper.appNeedsUpdate(model, response));

        model = HandlerHelperTest.createTestModel(false, true, "sns", true);
        response = HandlerHelperTest.createDescribeApplicationResponse(false, true, "sns", true);
        assertFalse(HandlerHelper.appNeedsUpdate(model, response));
    }


    @Test
    public void appNeedsUpdate_opscenter_not_equal() {

        ResourceModel model = HandlerHelperTest.createTestModel(true, true, "sns", true);
        DescribeApplicationResponse response = HandlerHelperTest.createDescribeApplicationResponse(true, false, "sns", true);
        assertTrue(HandlerHelper.appNeedsUpdate(model, response));

        model = HandlerHelperTest.createTestModel(true, null, "sns", true);
        response = HandlerHelperTest.createDescribeApplicationResponse(true, true, "sns", true);
        assertTrue(HandlerHelper.appNeedsUpdate(model, response));

        model = HandlerHelperTest.createTestModel(true, false, "sns", true);
        response = HandlerHelperTest.createDescribeApplicationResponse(true, true, "sns", true);
        assertTrue(HandlerHelper.appNeedsUpdate(model, response));

        model = HandlerHelperTest.createTestModel(true, true, "sns", true);
        response = HandlerHelperTest.createDescribeApplicationResponse(true, false, "sns", true);
        assertTrue(HandlerHelper.appNeedsUpdate(model, response));

        model = HandlerHelperTest.createTestModel(true, true, "sns", true);
        response = HandlerHelperTest.createDescribeApplicationResponse(true, null, "sns", true);
        assertTrue(HandlerHelper.appNeedsUpdate(model, response));
    }



    @Test
    public void appNeedsUpdate_opscenter_equal() {
        ResourceModel model = HandlerHelperTest.createTestModel(true, true, "sns", true);
        DescribeApplicationResponse response = HandlerHelperTest.createDescribeApplicationResponse(true, true, "sns", true);
        assertFalse(HandlerHelper.appNeedsUpdate(model, response));

        model = HandlerHelperTest.createTestModel(true, false, "sns", true);
        response = HandlerHelperTest.createDescribeApplicationResponse(true, null, "sns", true);
        assertFalse(HandlerHelper.appNeedsUpdate(model, response));

        model = HandlerHelperTest.createTestModel(true, false, "sns", true);
        response = HandlerHelperTest.createDescribeApplicationResponse(true, false, "sns", true);
        assertFalse(HandlerHelper.appNeedsUpdate(model, response));
    }


    @Test
    public void appNeedsUpdate_snstopic_not_equal() {

        ResourceModel model = HandlerHelperTest.createTestModel(true, true, "snsarn", true);
        DescribeApplicationResponse response = HandlerHelperTest.createDescribeApplicationResponse(true, true, "sns", true);
        assertTrue(HandlerHelper.appNeedsUpdate(model, response));

        model = HandlerHelperTest.createTestModel(true, true, null, true);
        response = HandlerHelperTest.createDescribeApplicationResponse(true, true, "sns", true);
        assertTrue(HandlerHelper.appNeedsUpdate(model, response));

        model = HandlerHelperTest.createTestModel(true, true, "sns", true);
        response = HandlerHelperTest.createDescribeApplicationResponse(true, true, null, true);
        assertTrue(HandlerHelper.appNeedsUpdate(model, response));
    }

    @Test
    public void appNeedsUpdate_snstopic_equal() {
        ResourceModel model = HandlerHelperTest.createTestModel(true, true, null, true);
        DescribeApplicationResponse response = HandlerHelperTest.createDescribeApplicationResponse(true, true, null, true);
        assertFalse(HandlerHelper.appNeedsUpdate(model, response));

        model = HandlerHelperTest.createTestModel(true, true, "sns", true);
        response = HandlerHelperTest.createDescribeApplicationResponse(true, true, "sns", true);
        assertFalse(HandlerHelper.appNeedsUpdate(model, response));
    }

    @Test
    public void appNeedsUpdate_autoconfig_not_equal() {

        ResourceModel model = HandlerHelperTest.createTestModel(true, true, "sns", true);
        DescribeApplicationResponse response = HandlerHelperTest.createDescribeApplicationResponse(true, true, "sns", false);
        assertTrue(HandlerHelper.appNeedsUpdate(model, response));

        model = HandlerHelperTest.createTestModel(true, true, "sns", false);
        response = HandlerHelperTest.createDescribeApplicationResponse(true, true, "sns", true);
        assertTrue(HandlerHelper.appNeedsUpdate(model, response));

        model = HandlerHelperTest.createTestModel(true, true, "sns", null);
        response = HandlerHelperTest.createDescribeApplicationResponse(true, true, "sns", true);
        assertTrue(HandlerHelper.appNeedsUpdate(model, response));

        model = HandlerHelperTest.createTestModel(true, true, "sns", true);
        response = HandlerHelperTest.createDescribeApplicationResponse(true, true, "sns", null);
        assertTrue(HandlerHelper.appNeedsUpdate(model, response));
    }

    @Test
    public void appNeedsUpdate_autoconfig_equal() {
        ResourceModel model = HandlerHelperTest.createTestModel(true, true, "sns", true);
        DescribeApplicationResponse response = HandlerHelperTest.createDescribeApplicationResponse(true, true, "sns", true);
        assertFalse(HandlerHelper.appNeedsUpdate(model, response));

        model = HandlerHelperTest.createTestModel(true, true, "sns", false);
        response = HandlerHelperTest.createDescribeApplicationResponse(true, true, "sns", false);
        assertFalse(HandlerHelper.appNeedsUpdate(model, response));

        model = HandlerHelperTest.createTestModel(true, true, "sns", null);
        response = HandlerHelperTest.createDescribeApplicationResponse(true, true, "sns", null);
        assertFalse(HandlerHelper.appNeedsUpdate(model, response));

        model = HandlerHelperTest.createTestModel(true, true, "sns", null);
        response = HandlerHelperTest.createDescribeApplicationResponse(true, true, "sns", false);
        assertFalse(HandlerHelper.appNeedsUpdate(model, response));

        model = HandlerHelperTest.createTestModel(true, true, "sns", false);
        response = HandlerHelperTest.createDescribeApplicationResponse(true, true, "sns", null);
        assertFalse(HandlerHelper.appNeedsUpdate(model, response));
    }

    public static ResourceModel createTestModel(Boolean enableCWEMonitor, Boolean enableOpsCenter, String opsSNSTopicArn, Boolean enableAutoConfiguration) {
        return ResourceModel.builder()
                .cWEMonitorEnabled(enableCWEMonitor)
                .opsCenterEnabled(enableOpsCenter)
                .opsItemSNSTopicArn(opsSNSTopicArn)
                .autoConfigurationEnabled(enableAutoConfiguration)
        .build();
    }

    public static DescribeApplicationResponse createDescribeApplicationResponse(Boolean enableCWEMonitor, Boolean enableOpsCenter, String opsSNSTopicArn, Boolean enableAutoConfiguration) {
        ApplicationInfo applicationInfo = ApplicationInfo.builder()
                .cweMonitorEnabled(enableCWEMonitor)
                .opsCenterEnabled(enableOpsCenter)
                .opsItemSNSTopicArn(opsSNSTopicArn)
                .autoConfigEnabled(enableAutoConfiguration)
                .build();
        return DescribeApplicationResponse.builder().applicationInfo(applicationInfo).build();
    }

}
