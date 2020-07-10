package software.amazon.applicationinsights.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.applicationinsights.application.InputConfiguration.InputComponentConfiguration;
import software.amazon.awssdk.services.applicationinsights.ApplicationInsightsClient;
import software.amazon.awssdk.services.applicationinsights.model.ApplicationComponent;
import software.amazon.awssdk.services.applicationinsights.model.CreateApplicationRequest;
import software.amazon.awssdk.services.applicationinsights.model.CreateComponentRequest;
import software.amazon.awssdk.services.applicationinsights.model.CreateLogPatternRequest;
import software.amazon.awssdk.services.applicationinsights.model.DeleteApplicationRequest;
import software.amazon.awssdk.services.applicationinsights.model.DeleteComponentRequest;
import software.amazon.awssdk.services.applicationinsights.model.DeleteLogPatternRequest;
import software.amazon.awssdk.services.applicationinsights.model.DescribeApplicationRequest;
import software.amazon.awssdk.services.applicationinsights.model.DescribeApplicationResponse;
import software.amazon.awssdk.services.applicationinsights.model.DescribeComponentConfigurationRecommendationRequest;
import software.amazon.awssdk.services.applicationinsights.model.DescribeComponentConfigurationRecommendationResponse;
import software.amazon.awssdk.services.applicationinsights.model.DescribeComponentConfigurationRequest;
import software.amazon.awssdk.services.applicationinsights.model.DescribeComponentRequest;
import software.amazon.awssdk.services.applicationinsights.model.DescribeComponentResponse;
import software.amazon.awssdk.services.applicationinsights.model.DescribeLogPatternRequest;
import software.amazon.awssdk.services.applicationinsights.model.DescribeLogPatternResponse;
import software.amazon.awssdk.services.applicationinsights.model.ListApplicationsRequest;
import software.amazon.awssdk.services.applicationinsights.model.ListApplicationsResponse;
import software.amazon.awssdk.services.applicationinsights.model.ListComponentsRequest;
import software.amazon.awssdk.services.applicationinsights.model.ListComponentsResponse;
import software.amazon.awssdk.services.applicationinsights.model.ListLogPatternsRequest;
import software.amazon.awssdk.services.applicationinsights.model.ListLogPatternsResponse;
import software.amazon.awssdk.services.applicationinsights.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.applicationinsights.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.applicationinsights.model.ResourceNotFoundException;
import software.amazon.awssdk.services.applicationinsights.model.Tag;
import software.amazon.awssdk.services.applicationinsights.model.TagResourceRequest;
import software.amazon.awssdk.services.applicationinsights.model.UntagResourceRequest;
import software.amazon.awssdk.services.applicationinsights.model.UpdateApplicationRequest;
import software.amazon.awssdk.services.applicationinsights.model.UpdateComponentConfigurationRequest;
import software.amazon.awssdk.services.applicationinsights.model.UpdateLogPatternRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HandlerHelper {
    private static final String DEFAULT_COMPONENT_CONFIG_MODE = "DEFAULT";
    private static final String CUSTOM_COMPONENT_CONFIG_MODE = "CUSTOM";
    private static final String DEFAULT_WITH_OVERWRITE_COMPONENT_CONFIG_MODE = "DEFAULT_WITH_OVERWRITE";

    public static boolean doesApplicationExist(
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient) {
        try {
            proxy.injectCredentialsAndInvokeV2(DescribeApplicationRequest.builder()
                            .resourceGroupName(model.getResourceGroupName())
                            .build(),
                    applicationInsightsClient::describeApplication);
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    public static String getApplicationLifeCycle(
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient) {
        DescribeApplicationResponse response = proxy.injectCredentialsAndInvokeV2(DescribeApplicationRequest.builder()
                        .resourceGroupName(model.getResourceGroupName())
                        .build(),
                applicationInsightsClient::describeApplication);

        return response.applicationInfo().lifeCycle();
    }

    public static void createApplicationInsightsApplication(
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient) {
        proxy.injectCredentialsAndInvokeV2(CreateApplicationRequest.builder()
                        // required fields
                        .resourceGroupName(model.getResourceGroupName())
                        // optional fields
                        .opsCenterEnabled(model.getOpsCenterEnabled())
                        .opsItemSNSTopicArn(model.getOpsItemSNSTopicArn())
                        .cweMonitorEnabled(model.getCWEMonitorEnabled())
                        .tags(translateModelTagsToSdkTags(model.getTags()))
                        .build(),
                applicationInsightsClient::createApplication);
    }

    public static DescribeApplicationResponse describeApplicationInsightsApplication(
            String resourceGroupName,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient) {
        return proxy.injectCredentialsAndInvokeV2(DescribeApplicationRequest.builder()
                        .resourceGroupName(resourceGroupName)
                        .build(),
                applicationInsightsClient::describeApplication);
    }

    private static Set<Tag> translateModelTagsToSdkTags(
            final Collection<software.amazon.applicationinsights.application.Tag> modelTags) {
        return Optional.ofNullable(modelTags).orElse(Collections.emptySet())
                .stream()
                .map(tag -> Tag.builder().key(tag.getKey()).value(tag.getValue()).build())
                .collect(Collectors.toSet());
    }

    private static Set<software.amazon.applicationinsights.application.Tag> translateSdkTagsToModelTags(
            final Collection<Tag> sdkTags) {
        return Optional.ofNullable(sdkTags).orElse(Collections.emptySet())
                .stream()
                .map(tag -> software.amazon.applicationinsights.application.Tag.builder().key(tag.key()).value(tag.value()).build())
                .collect(Collectors.toSet());
    }

    public static void deleteApplicationInsightsApplication(
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient) {
        proxy.injectCredentialsAndInvokeV2(DeleteApplicationRequest.builder()
                        .resourceGroupName(model.getResourceGroupName())
                        .build(),
                applicationInsightsClient::deleteApplication);
    }

    public static void createCustomComponent(
            CustomComponent customComponent,
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient) {
        proxy.injectCredentialsAndInvokeV2(CreateComponentRequest.builder()
                        .resourceGroupName(model.getResourceGroupName())
                        .componentName(customComponent.getComponentName())
                        .resourceList(customComponent.getResourceList())
                        .build(),
                applicationInsightsClient::createComponent);
    }

    public static boolean doesCustomComponentExist(
            String resourceGroupName,
            String componentName,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient) {
        try {
            proxy.injectCredentialsAndInvokeV2(DescribeComponentRequest.builder()
                            .resourceGroupName(resourceGroupName)
                            .componentName(componentName)
                            .build(),
                    applicationInsightsClient::describeComponent);
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    public static void createLogPattern(
            String patternSetName,
            LogPattern logPattern,
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient) {
        proxy.injectCredentialsAndInvokeV2(CreateLogPatternRequest.builder()
                        .resourceGroupName(model.getResourceGroupName())
                        .patternSetName(patternSetName)
                        .patternName(logPattern.getPatternName())
                        .pattern(logPattern.getPattern())
                        .rank(logPattern.getRank())
                        .build(),
                applicationInsightsClient::createLogPattern);
    }

    public static boolean doesLogPatternExist(
            String resourceGroupName,
            String patternSetName,
            String patternName,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient) {
        try {
            proxy.injectCredentialsAndInvokeV2(DescribeLogPatternRequest.builder()
                            .resourceGroupName(resourceGroupName)
                            .patternSetName(patternSetName)
                            .patternName(patternName)
                            .build(),
                    applicationInsightsClient::describeLogPattern);
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    public static void createComponentConfiguration(
            ComponentMonitoringSetting componentMonitoringSetting,
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient,
            Logger logger) throws IOException {
        String mode = componentMonitoringSetting.getComponentConfigurationMode();
        String componentNameOrArn = getComponentNameOrARNFromComponentMonitoringSetting(componentMonitoringSetting);

        if (mode.equals(CUSTOM_COMPONENT_CONFIG_MODE)) {
            InputComponentConfiguration inputConfig =
                    new InputComponentConfiguration(componentMonitoringSetting.getCustomComponentConfiguration());

            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            String componentConfiguration = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(inputConfig);

            logger.log("Component name or ARN: " + componentNameOrArn);
            logger.log("Component Configuration String (CUSTOM mode): " + componentConfiguration);

            proxy.injectCredentialsAndInvokeV2(UpdateComponentConfigurationRequest.builder()
                            .resourceGroupName(model.getResourceGroupName())
                            .componentName(componentNameOrArn)
                            .monitor(true)
                            .tier(componentMonitoringSetting.getTier())
                            .componentConfiguration(componentConfiguration)
                            .build(),
                    applicationInsightsClient::updateComponentConfiguration);
        } else if (mode.equals(DEFAULT_COMPONENT_CONFIG_MODE)) {

            createDefaultComponentConfiguration(
                    componentNameOrArn, componentMonitoringSetting.getTier(), model, proxy, applicationInsightsClient, logger);

        } else if (mode.equals(DEFAULT_WITH_OVERWRITE_COMPONENT_CONFIG_MODE)) {
            DescribeComponentConfigurationRecommendationResponse describeComponentConfigurationRecommendationResponse =
                    proxy.injectCredentialsAndInvokeV2(DescribeComponentConfigurationRecommendationRequest.builder()
                                    .resourceGroupName(model.getResourceGroupName())
                                    .componentName(componentNameOrArn)
                                    .tier(componentMonitoringSetting.getTier())
                                    .build(),
                            applicationInsightsClient::describeComponentConfigurationRecommendation);

            logger.log("Component name or ARN: " + componentNameOrArn);
            String recomendedComponentConfigurationString =
                    describeComponentConfigurationRecommendationResponse.componentConfiguration();
            logger.log("Recommended Component Configuration String (DEFAULT_WITH_OVERWRITE mode): " + recomendedComponentConfigurationString);

            ObjectMapper mapper = new ObjectMapper()
                    .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL);
            InputComponentConfiguration recommendedInputConfig = mapper.readValue(
                    recomendedComponentConfigurationString, InputComponentConfiguration.class);

            logger.log("Recommended Component Configuration String transformed (DEFAULT_WITH_OVERWRITE mode): " +
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(recommendedInputConfig));

            InputComponentConfiguration recommendedInputConfigWithOverwrite =
                    new InputComponentConfiguration(recommendedInputConfig, componentMonitoringSetting.getDefaultOverwriteComponentConfiguration());

            // Same as CUSTOM flow
            String componentConfiguration = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(recommendedInputConfigWithOverwrite);

            logger.log("Component Configuration String (DEFAULT_WITH_OVERWRITE mode): " + componentConfiguration);

            proxy.injectCredentialsAndInvokeV2(UpdateComponentConfigurationRequest.builder()
                            .resourceGroupName(model.getResourceGroupName())
                            .componentName(componentNameOrArn)
                            .monitor(true)
                            .tier(componentMonitoringSetting.getTier())
                            .componentConfiguration(componentConfiguration)
                            .build(),
                    applicationInsightsClient::updateComponentConfiguration);
        }
    }

    public static void createDefaultComponentConfiguration(
            String componentNameOrARN,
            String tier,
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient,
            Logger logger) throws IOException {
        DescribeComponentConfigurationRecommendationResponse describeComponentConfigurationRecommendationResponse =
                proxy.injectCredentialsAndInvokeV2(DescribeComponentConfigurationRecommendationRequest.builder()
                                .resourceGroupName(model.getResourceGroupName())
                                .componentName(componentNameOrARN)
                                .tier(tier)
                                .build(),
                        applicationInsightsClient::describeComponentConfigurationRecommendation);

        String recomendedComponentConfigurationString =
                describeComponentConfigurationRecommendationResponse.componentConfiguration();
        logger.log("Component name or ARN: " + componentNameOrARN);
        logger.log("Recommended Component Configuration String (DEFAULT mode): " + recomendedComponentConfigurationString);

        ObjectMapper mapper = new ObjectMapper()
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        InputComponentConfiguration recommendedInputConfig = mapper.readValue(
                recomendedComponentConfigurationString, InputComponentConfiguration.class);

        // same as CUSTOM branch
        String inputComponentConfigurationString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(recommendedInputConfig);

        logger.log("Component Configuration String (DEFAULT mode): " + inputComponentConfigurationString);

        proxy.injectCredentialsAndInvokeV2(UpdateComponentConfigurationRequest.builder()
                        .resourceGroupName(model.getResourceGroupName())
                        .componentName(componentNameOrARN)
                        .monitor(true)
                        .tier(tier)
                        .componentConfiguration(inputComponentConfigurationString)
                        .build(),
                applicationInsightsClient::updateComponentConfiguration);
    }

    public static String getComponentNameOrARNFromComponentMonitoringSetting(
            ComponentMonitoringSetting componentMonitoringSetting) {
        return componentMonitoringSetting.getComponentName() == null ?
                componentMonitoringSetting.getComponentARN() : componentMonitoringSetting.getComponentName();
    }

    public static List<String> getApplicationAllComponentNames(
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient) {
        ListComponentsResponse response = proxy.injectCredentialsAndInvokeV2(ListComponentsRequest.builder()
                        .resourceGroupName(model.getResourceGroupName())
                        .build(),
                applicationInsightsClient::listComponents);

        return response.applicationComponentList() == null ?
                new ArrayList<>() :
                response.applicationComponentList().stream()
                        .map(applicationComponent -> applicationComponent.componentName())
                        .collect(Collectors.toList());
    }

    public static void udpateApplicationInsightsApplication(
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient) {
        proxy.injectCredentialsAndInvokeV2(UpdateApplicationRequest.builder()
                        // required fields
                        .resourceGroupName(model.getResourceGroupName())
                        // optional fields
                        .opsCenterEnabled(model.getOpsCenterEnabled() == null ? false : model.getOpsCenterEnabled())
                        .opsItemSNSTopicArn(model.getOpsItemSNSTopicArn())
                        .cweMonitorEnabled(model.getCWEMonitorEnabled() == null ? false : model.getCWEMonitorEnabled())
                        .removeSNSTopic(model.getOpsItemSNSTopicArn() == null ? true : false)
                        .build(),
                applicationInsightsClient::updateApplication);
    }

    public static void getTagKeysToDeleteAndCreate(
            List<String> tagKeysToDelete,
            List<String> tagKeysToCreate,
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient) {
        Set<Tag> appTags = new HashSet<>(getApplicationTags(model.getApplicationARN(), proxy, applicationInsightsClient));
        Set<Tag> modelTags = translateModelTagsToSdkTags(model.getTags());

        for (Tag appTag : appTags) {
            if (!modelTags.contains(appTag)) {
                tagKeysToDelete.add(appTag.key());
            }
        }

        for (Tag modelTag : modelTags) {
            if (!appTags.contains(modelTag)) {
                tagKeysToCreate.add(modelTag.key());
            }
        }
    }

    private static List<Tag> getApplicationTags(
            String applicationARN,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient) {
        ListTagsForResourceResponse response = proxy.injectCredentialsAndInvokeV2(ListTagsForResourceRequest.builder()
                        .resourceARN(applicationARN)
                        .build(),
                applicationInsightsClient::listTagsForResource);
        return response.tags();
    }

    public static void unTagApplication(
            String tagKeyToDelete,
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient,
            Logger logger) {
        logger.log(String.format("Calling UntagResource API: %s %s", model.getApplicationARN(), tagKeyToDelete));
        proxy.injectCredentialsAndInvokeV2(UntagResourceRequest.builder()
                        .resourceARN(model.getApplicationARN())
                        .tagKeys(tagKeyToDelete)
                        .build(),
                applicationInsightsClient::untagResource);
        logger.log("Finish calling UntagResource API.");
    }

    public static boolean tagDeletedForApplicaiton(
            String tagKey,
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient,
            Logger logger) {
        logger.log(String.format("Inside tagsDeletedForApplicaiton function for tagKey %s", tagKey));
        List<Tag> appTags = getApplicationTags(model.getApplicationARN(), proxy, applicationInsightsClient);

        for (Tag appTag : appTags) {
            if (tagKey.equals(appTag.key())) {
                return false;
            }
        }

        return true;
    }

    public static void tagApplication(
            String tagKeyToCreate,
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient,
            Logger logger) {
        List<software.amazon.applicationinsights.application.Tag> tagsToCreate = model.getTags().stream()
                .filter(tag -> tagKeyToCreate.equals(tag.getKey()))
                .collect(Collectors.toList());

        logger.log(String.format("Calling TagResoruce API: %s %s", model.getApplicationARN(), translateModelTagsToSdkTags(tagsToCreate).toString()));
        proxy.injectCredentialsAndInvokeV2(TagResourceRequest.builder()
                        .resourceARN(model.getApplicationARN())
                        .tags(translateModelTagsToSdkTags(tagsToCreate))
                        .build(),
                applicationInsightsClient::tagResource);
        logger.log("Finish calling TagResoruce API.");
    }

    public static boolean tagCreatedForApplicaiton(
            String tagKey,
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient) {
        List<Tag> appTags = getApplicationTags(model.getApplicationARN(), proxy, applicationInsightsClient);

        List<software.amazon.applicationinsights.application.Tag> tagsToCheck = model.getTags().stream()
                .filter(tag -> tagKey.equals(tag.getKey()))
                .collect(Collectors.toList());

        return appTags.containsAll(translateModelTagsToSdkTags(tagsToCheck));
    }

    public static void getCustomComponentNamesToDeleteAndCreate(
            List<String> customCompnentNamesToDelete,
            List<String> customCompnentNamesToCreate,
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient,
            Logger logger) {
        ListComponentsResponse listComponentsResponse = listApplicationComponents(model.getResourceGroupName(), proxy, applicationInsightsClient);
        List<ApplicationComponent> appComponents = listComponentsResponse.applicationComponentList().stream()
                .filter(component -> component.resourceType().equals("CustomComponent"))
                .collect(Collectors.toList());
        List<String> appComponentNames = appComponents.stream()
                .map(component -> component.componentName())
                .collect(Collectors.toList());
        logger.log("app component names: " + appComponentNames.toString());

        List<CustomComponent> modelComponents = model.getCustomComponents();
        Map<String, CustomComponent> modelComponentMap = modelComponents.stream()
                .collect(Collectors.toMap(CustomComponent::getComponentName, Function.identity()));
        List<String> modelComponentNames = new ArrayList<>(modelComponentMap.keySet());
        logger.log("model component names: " + modelComponentNames.toString());

        List<String> appOnlyComponentNames = new ArrayList<>(appComponentNames);
        appOnlyComponentNames.removeAll(modelComponentNames);
        customCompnentNamesToDelete.addAll(appOnlyComponentNames);

        List<String> modelOnlyComponentNames = new ArrayList<>(modelComponentNames);
        modelOnlyComponentNames.removeAll(appComponentNames);
        customCompnentNamesToCreate.addAll(modelOnlyComponentNames);

        List<String> commonComponentNames = new ArrayList<>(appComponentNames);
        commonComponentNames.retainAll(modelComponentNames);
        logger.log("common component names: " + commonComponentNames.toString());

        for (String commonComponentName : commonComponentNames) {
            DescribeComponentResponse describeComponentResponse =
                    describeAppicationComponent(commonComponentName, model.getResourceGroupName(), proxy, applicationInsightsClient);
            List<String> resourceList = describeComponentResponse.resourceList();
            List<String> modelResourceList = modelComponentMap.get(commonComponentName).getResourceList();
            logger.log("common component name: " + commonComponentName);
            logger.log("app resource list: " + resourceList.toString());
            logger.log("model resource list: " + modelResourceList.toString());

            // if custom comopnent resource lists mistach, it needs to be deleted & recreated to avoid deadlock
            // e.g. before change: component A contains instance 1, component B contains instance 2;
            // after change: component A contains instance 2, component B contains instance 1
            if (resourceList.size() != modelResourceList.size() || !resourceList.containsAll(modelResourceList)) {
                customCompnentNamesToDelete.add(commonComponentName);
                customCompnentNamesToCreate.add(commonComponentName);
            }
        }
    }

    private static DescribeComponentResponse describeAppicationComponent(
            String componentName,
            String resourceGroupName,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient) {
        return proxy.injectCredentialsAndInvokeV2(DescribeComponentRequest.builder()
                        .resourceGroupName(resourceGroupName)
                        .componentName(componentName)
                        .build(),
                applicationInsightsClient::describeComponent);
    }

    private static ListComponentsResponse listApplicationComponents(
            String resourceGroupName,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient) {
        return proxy.injectCredentialsAndInvokeV2(ListComponentsRequest.builder()
                        .resourceGroupName(resourceGroupName)
                        .build(),
                applicationInsightsClient::listComponents);
    }

    public static void deleteCustomComponent(
            String componentNameToDelete,
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient) {
        proxy.injectCredentialsAndInvokeV2(DeleteComponentRequest.builder()
                        .resourceGroupName(model.getResourceGroupName())
                        .componentName(componentNameToDelete)
                        .build(),
                applicationInsightsClient::deleteComponent);
    }

    public static void getlogSetAndPatternToDeleteCreateUpdate(
            List<String> logSetAndPatternToDelete,
            List<String> logSetAndPatternToCreate,
            List<String> logSetAndPatternToUpdate,
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient) {
        ListLogPatternsResponse listLogPatternsResponse = listLogPatterns(model.getResourceGroupName(), proxy, applicationInsightsClient);

        List<String> appLogSetAndPatterns = listLogPatternsResponse.logPatterns().stream()
                .map(logPattern -> generateLogPatternIdentifier(logPattern.patternSetName(), logPattern.patternName()))
                .collect(Collectors.toList());

        List<String> modelLogSetAndPatterns = new ArrayList<>();
        for (LogPatternSet logPatternSet : model.getLogPatternSets()) {
            String patternSetName = logPatternSet.getPatternSetName();
            for (LogPattern logPattern : logPatternSet.getLogPatterns()) {
                String patternName = logPattern.getPatternName();
                modelLogSetAndPatterns.add(generateLogPatternIdentifier(patternSetName, patternName));
            }
        }

        List<String> deleteList = new ArrayList<>(appLogSetAndPatterns);
        deleteList.removeAll(modelLogSetAndPatterns);
        logSetAndPatternToDelete.addAll(deleteList);

        List<String> createList = new ArrayList<>(modelLogSetAndPatterns);
        createList.removeAll(appLogSetAndPatterns);
        logSetAndPatternToCreate.addAll(createList);

        List<String> retainList = new ArrayList<>(appLogSetAndPatterns);
        retainList.retainAll(modelLogSetAndPatterns);

        for (String retainLogSetAndPattern : retainList) {
            if (!isLogPatternSyncedWithModel(
                    retainLogSetAndPattern.split(":")[0],
                    retainLogSetAndPattern.split(":")[1],
                    model,
                    proxy,
                    applicationInsightsClient)) {
                logSetAndPatternToUpdate.add(retainLogSetAndPattern);
            }
        }
    }

    private static ListLogPatternsResponse listLogPatterns(
            String resourceGroupName,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient) {
        return proxy.injectCredentialsAndInvokeV2(ListLogPatternsRequest.builder()
                        .resourceGroupName(resourceGroupName)
                        .build(),
                applicationInsightsClient::listLogPatterns);
    }

    public static void deleteLogPattern(
            String patternSetName,
            String patternName,
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient) {
        proxy.injectCredentialsAndInvokeV2(DeleteLogPatternRequest.builder()
                        .resourceGroupName(model.getResourceGroupName())
                        .patternSetName(patternSetName)
                        .patternName(patternName)
                        .build(),
                applicationInsightsClient::deleteLogPattern);
    }

    public static void updateLogPattern(
            String patternSetName,
            LogPattern logPattern,
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient) {
        proxy.injectCredentialsAndInvokeV2(UpdateLogPatternRequest.builder()
                        .resourceGroupName(model.getResourceGroupName())
                        .patternSetName(patternSetName)
                        .patternName(logPattern.getPatternName()).pattern(logPattern.getPattern())
                        .rank(logPattern.getRank())
                        .build(),
                applicationInsightsClient::updateLogPattern);
    }

    public static boolean isLogPatternSyncedWithModel(
            String patternSetName,
            String patternName,
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient) {
        DescribeLogPatternResponse describeLogPatternResponse =
                proxy.injectCredentialsAndInvokeV2(DescribeLogPatternRequest.builder()
                        .resourceGroupName(model.getResourceGroupName())
                        .patternSetName(patternSetName)
                        .patternName(patternName)
                        .build(),
                applicationInsightsClient::describeLogPattern);

        LogPattern logPattern = pickLogPatternFromModel(patternSetName, patternName, model);

        if (!logPattern.getPattern().equals(describeLogPatternResponse.logPattern().pattern())) {
            return false;
        }

        if (!logPattern.getRank().equals(describeLogPatternResponse.logPattern().rank())) {
            return false;
        }

        return true;
    }

    public static LogPattern pickLogPatternFromModel(String patternSetName, String patternName, ResourceModel model) {
        for (LogPatternSet logPatternSet : model.getLogPatternSets()) {
            if (logPatternSet.getPatternSetName().equals(patternSetName)) {
                for (LogPattern logPattern : logPatternSet.getLogPatterns()) {
                    if (logPattern.getPatternName().equals(patternName)) {
                        return logPattern;
                    }
                }
            }
        }

        return null;
    }

    public static String generateLogPatternIdentifier(
            String patternSetName,
            String patternName) {
        return String.format("%s:%s", patternSetName, patternName);
    }

    public static ComponentMonitoringSetting pickNextComponentMonitoringSetting(
            ResourceModel model,
            CallbackContext callbackContext) {
        List<ComponentMonitoringSetting> componentMonitoringSettings = model.getComponentMonitoringSettings();
        if (componentMonitoringSettings == null || componentMonitoringSettings.isEmpty()) {
            return null;
        }

        for (ComponentMonitoringSetting componentMonitoringSetting : componentMonitoringSettings) {
            if (!callbackContext.getProcessedItems().contains(
                    HandlerHelper.getComponentNameOrARNFromComponentMonitoringSetting(componentMonitoringSetting))) {
                return componentMonitoringSetting;
            }
        }

        return null;
    }

    public static List<String> getConfiguredComponentNames(ResourceModel model) {
        return model.getComponentMonitoringSettings() == null ?
                new ArrayList<>() :
                model.getComponentMonitoringSettings().stream()
                        .map(componentMonitoringSetting ->
                                HandlerHelper.getComponentNameOrARNFromComponentMonitoringSetting(componentMonitoringSetting))
                        .collect(Collectors.toList());
    }

    public static String pickNextConfigurationComponent(CallbackContext callbackContext) {
        List<String> unprocessedDefaultConfiguationComponents = callbackContext.getUnprocessedItems();
        return (unprocessedDefaultConfiguationComponents == null || unprocessedDefaultConfiguationComponents.isEmpty()) ?
                null : unprocessedDefaultConfiguationComponents.get(0);
    }

    public static boolean isComponentConfigurationEnabled(
            String componentNameOrARN,
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient) {
        return proxy.injectCredentialsAndInvokeV2(DescribeComponentConfigurationRequest.builder()
                        .resourceGroupName(model.getResourceGroupName())
                        .componentName(componentNameOrARN)
                        .build(),
                applicationInsightsClient::describeComponentConfiguration)
                .monitor();
    }

    public static void disableComponentConfiguration(
            String componentNameOrARN,
            ResourceModel model,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient,
            Logger logger) {
        proxy.injectCredentialsAndInvokeV2(UpdateComponentConfigurationRequest.builder()
                        .resourceGroupName(model.getResourceGroupName())
                        .componentName(componentNameOrARN)
                        .monitor(false)
                        .build(),
                applicationInsightsClient::updateComponentConfiguration);
    }

    public static ResourceModel generateReadModel(
            String resourceGroupName,
            ResourceModel model,
            ResourceHandlerRequest<ResourceModel> request,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient) {
        ResourceModel readModel = ResourceModel.builder().build();

        readModel.setApplicationARN(String.format("arn:aws:applicationinsights:%s:%s:application/resource-group/%s",
                request.getRegion(),
                request.getAwsAccountId(),
                resourceGroupName));

        // set readModel application level attributes
        DescribeApplicationResponse describeApplicationResponse = describeApplicationInsightsApplication(resourceGroupName, proxy, applicationInsightsClient);
        readModel.setCWEMonitorEnabled(model == null ? describeApplicationResponse.applicationInfo().cweMonitorEnabled() :
                getReadModelBooleanFromModel(model.getCWEMonitorEnabled(), describeApplicationResponse.applicationInfo().cweMonitorEnabled()));
        readModel.setOpsCenterEnabled(model == null ? describeApplicationResponse.applicationInfo().opsCenterEnabled() :
                getReadModelBooleanFromModel(model.getOpsCenterEnabled(), describeApplicationResponse.applicationInfo().opsCenterEnabled()));
        readModel.setOpsItemSNSTopicArn(describeApplicationResponse.applicationInfo().opsItemSNSTopicArn());

        // set readModel tags attribute
        List<Tag> appTags = getApplicationTags(readModel.getApplicationARN(), proxy, applicationInsightsClient);
        readModel.setTags(new ArrayList<>(translateSdkTagsToModelTags(appTags)));

        // set readModel customComponents attribute
        ListComponentsResponse listComponentsResponse = listApplicationComponents(resourceGroupName, proxy, applicationInsightsClient);
        List<ApplicationComponent> appCustomComponents = listComponentsResponse.applicationComponentList().stream()
                .filter(component -> component.resourceType().equals("CustomComponent"))
                .collect(Collectors.toList());
        readModel.setCustomComponents(tanslateSdkCustomComponentsToModelCustomComponents(appCustomComponents, resourceGroupName, proxy, applicationInsightsClient));

        // set readModel logPatternSets attribute
        ListLogPatternsResponse listLogPatternsResponse = listLogPatterns(resourceGroupName, proxy, applicationInsightsClient);
        readModel.setLogPatternSets(translateSdkLogPatternsToModelLogPatternSets(listLogPatternsResponse.logPatterns()));

        return readModel;
    }

    private static List<LogPatternSet> translateSdkLogPatternsToModelLogPatternSets(
            List<software.amazon.awssdk.services.applicationinsights.model.LogPattern> logPatterns) {
        Map<String, List<software.amazon.awssdk.services.applicationinsights.model.LogPattern>> patternSetNamePatternsMap = new HashMap<>();
        for (software.amazon.awssdk.services.applicationinsights.model.LogPattern logPattern : logPatterns) {
            if (!patternSetNamePatternsMap.containsKey(logPattern.patternSetName())) {
                patternSetNamePatternsMap.put(logPattern.patternSetName(), new ArrayList<>(Arrays.asList(logPattern)));
            } else {
                patternSetNamePatternsMap.get(logPattern.patternSetName()).add(logPattern);
            }
        }

        List<LogPatternSet> modelLogPatternSets = new ArrayList<>();
        patternSetNamePatternsMap.entrySet().stream()
                .forEach(entry -> {
                    modelLogPatternSets.add(
                            LogPatternSet.builder()
                                    .patternSetName(entry.getKey())
                                    .logPatterns(entry.getValue().stream()
                                            .map(logPattern ->
                                                    LogPattern.builder()
                                                            .patternName(logPattern.patternName())
                                                            .rank(logPattern.rank())
                                                            .pattern(logPattern.pattern()).build())
                                            .collect(Collectors.toList()))
                                    .build());

                });

        return modelLogPatternSets;
    }

    private static List<CustomComponent> tanslateSdkCustomComponentsToModelCustomComponents(
            List<ApplicationComponent> appCustomComponents,
            String resourceGroupName,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient) {
        return Optional.ofNullable(appCustomComponents).orElse(Collections.emptyList())
                .stream()
                .map(appCustomComponent -> CustomComponent.builder()
                        .componentName(appCustomComponent.componentName())
                        .resourceList(describeAppicationComponent(appCustomComponent.componentName(), resourceGroupName, proxy, applicationInsightsClient)
                                .resourceList())
                        .build())
                .collect(Collectors.toList());
    }

    private static Boolean getReadModelBooleanFromModel(Boolean modelBoolean, Boolean describeResponseBoolean) {
        return (!describeResponseBoolean && modelBoolean == null) ? null : describeResponseBoolean;
    }

    public static ListApplicationsResponse listApplicationInsightsApplications(
            String nextToken,
            AmazonWebServicesClientProxy proxy,
            ApplicationInsightsClient applicationInsightsClient) {
        return proxy.injectCredentialsAndInvokeV2(ListApplicationsRequest.builder()
                        .nextToken(nextToken)
                        .build(),
                applicationInsightsClient::listApplications);
    }
}
