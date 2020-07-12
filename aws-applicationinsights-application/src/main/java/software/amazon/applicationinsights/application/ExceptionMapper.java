package software.amazon.applicationinsights.application;

import software.amazon.awssdk.services.applicationinsights.model.ResourceInUseException;
import software.amazon.awssdk.services.applicationinsights.model.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

public class ExceptionMapper {

    /**
     * Translates Application Insights' client exception to a Cfn Handler Error Code.
     * Ref: https://w.amazon.com/bin/view/AWS21/Design/Uluru/HandlerContract
     */
    public static HandlerErrorCode mapToHandlerErrorCode(final Exception exception) {
        if (exception instanceof ResourceInUseException) {
            return HandlerErrorCode.ResourceConflict;
        } else if (exception instanceof ResourceNotFoundException) {
            return HandlerErrorCode.NotFound;
        } else {
            return HandlerErrorCode.InternalFailure;
        }
    }
}
