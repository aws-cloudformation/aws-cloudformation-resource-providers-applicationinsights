package software.amazon.applicationinsights.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallbackContext {
    private String currentStep;
    private Integer stabilizationRetriesRemaining;
    private Set<String> processedItems;
    private List<String> unprocessedItems;
    private List<String> undeletedItems;
    private List<String> uncreatedItems;
    private List<String> unupdatedItems;
    private String processingItem;
}
