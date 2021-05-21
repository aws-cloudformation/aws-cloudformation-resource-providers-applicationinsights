package software.amazon.applicationinsights.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
