/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.rest.model;

import org.sonatype.nexus.rest.repositories.RepositoryBaseResourceConverter;
import org.sonatype.nexus.rest.repositories.RepositoryResourceResponseConverter;
import org.sonatype.nexus.rest.schedules.ScheduledServiceBaseResourceConverter;
import org.sonatype.nexus.rest.schedules.ScheduledServicePropertyResourceConverter;
import org.sonatype.nexus.rest.schedules.ScheduledServiceResourceResponseConverter;
import org.sonatype.security.rest.model.AuthenticationClientPermissions;
import org.sonatype.security.rest.model.AuthenticationLoginResourceResponse;
import org.sonatype.security.rest.model.ClientPermission;
import org.sonatype.security.rest.model.ExternalRoleMappingListResourceResponse;
import org.sonatype.security.rest.model.ExternalRoleMappingResource;
import org.sonatype.security.rest.model.ExternalRoleMappingResourceResponse;
import org.sonatype.security.rest.model.PlexusRoleListResourceResponse;
import org.sonatype.security.rest.model.PlexusRoleResource;
import org.sonatype.security.rest.model.PlexusUserListResourceResponse;
import org.sonatype.security.rest.model.PlexusUserResource;
import org.sonatype.security.rest.model.PlexusUserResourceResponse;
import org.sonatype.security.rest.model.PlexusUserSearchCriteriaResourceRequest;
import org.sonatype.security.rest.model.PrivilegeListResourceResponse;
import org.sonatype.security.rest.model.PrivilegeProperty;
import org.sonatype.security.rest.model.PrivilegeStatusResource;
import org.sonatype.security.rest.model.PrivilegeStatusResourceResponse;
import org.sonatype.security.rest.model.PrivilegeTypePropertyResource;
import org.sonatype.security.rest.model.PrivilegeTypeResource;
import org.sonatype.security.rest.model.PrivilegeTypeResourceResponse;
import org.sonatype.security.rest.model.RoleListResourceResponse;
import org.sonatype.security.rest.model.RoleResource;
import org.sonatype.security.rest.model.RoleResourceRequest;
import org.sonatype.security.rest.model.RoleResourceResponse;
import org.sonatype.security.rest.model.UserChangePasswordRequest;
import org.sonatype.security.rest.model.UserForgotPasswordRequest;
import org.sonatype.security.rest.model.UserListResourceResponse;
import org.sonatype.security.rest.model.UserResource;
import org.sonatype.security.rest.model.UserResourceRequest;
import org.sonatype.security.rest.model.UserResourceResponse;
import org.sonatype.security.rest.model.UserToRoleResource;
import org.sonatype.security.rest.model.UserToRoleResourceRequest;

import com.thoughtworks.xstream.XStream;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Scm;

/**
 * The "lightweight" configurator that makes possible the use of it when plexus-restlet-bridge is not on classpath, as
 * the actual implementation (their classes) are passed in as parameters. This class should be used on "lightweight"
 * client side, where the presence of the module above (and it's entourage) is unwanted.
 *
 * @author cstamas
 * @since 2.1
 */
public class XStreamConfiguratorLightweight
{
  public static XStream configureXStream(XStream xstream, Class<?> errorResponseClazz, Class<?> errorMessageClazz) {
    // protect against XSS, escape HTML from input.
    xstream.registerConverter(new HtmlEscapeStringConverter());

    xstream.registerConverter(
        new RepositoryBaseResourceConverter(xstream.getMapper(), xstream.getReflectionProvider()),
        XStream.PRIORITY_VERY_HIGH);
    xstream.registerConverter(
        new RepositoryResourceResponseConverter(xstream.getMapper(), xstream.getReflectionProvider()),
        XStream.PRIORITY_VERY_HIGH); // strips the class="class.name" attribute from
    // data

    xstream.registerConverter(
        new ScheduledServiceBaseResourceConverter(xstream.getMapper(), xstream.getReflectionProvider()),
        XStream.PRIORITY_VERY_HIGH);
    xstream.registerConverter(
        new ScheduledServicePropertyResourceConverter(xstream.getMapper(), xstream.getReflectionProvider()),
        XStream.PRIORITY_VERY_HIGH);
    xstream.registerConverter(
        new ScheduledServiceResourceResponseConverter(xstream.getMapper(), xstream.getReflectionProvider()),
        XStream.PRIORITY_VERY_HIGH); // strips the class="class.name" attribute from

    // data

    // Maven POM
    xstream.alias("project", Model.class);

    xstream.processAnnotations(ArtifactResolveResourceResponse.class);
    xstream.processAnnotations(GlobalConfigurationListResourceResponse.class);
    xstream.processAnnotations(GlobalConfigurationResourceResponse.class);
    xstream.processAnnotations(RepositoryStatusListResourceResponse.class);
    xstream.processAnnotations(RepositoryListResourceResponse.class);
    xstream.processAnnotations(RepositoryResourceResponse.class);
    xstream.processAnnotations(RepositoryStatusResourceResponse.class);
    xstream.processAnnotations(RepositoryMetaResourceResponse.class);
    xstream.processAnnotations(RepositoryGroupListResourceResponse.class);
    xstream.processAnnotations(RepositoryGroupResourceResponse.class);
    xstream.processAnnotations(RepositoryRouteListResourceResponse.class);
    xstream.processAnnotations(RepositoryRouteResourceResponse.class);
    xstream.processAnnotations(ScheduledServiceListResourceResponse.class);
    xstream.processAnnotations(ScheduledServiceResourceStatusResponse.class);
    xstream.processAnnotations(ScheduledServiceResourceResponse.class);
    xstream.processAnnotations(ScheduledServiceTypeResourceResponse.class);
    xstream.processAnnotations(ContentListResourceResponse.class);
    xstream.processAnnotations(ContentListDescribeResourceResponse.class);
    xstream.processAnnotations(ConfigurationsListResourceResponse.class);
    xstream.processAnnotations(FeedListResourceResponse.class);
    xstream.processAnnotations(NFCResourceResponse.class);
    xstream.processAnnotations(StatusResourceResponse.class);
    xstream.processAnnotations(WastebasketResourceResponse.class);
    xstream.processAnnotations(RepositoryTargetListResourceResponse.class);
    xstream.processAnnotations(RepositoryTargetResourceResponse.class);
    xstream.processAnnotations(RepositoryContentClassListResourceResponse.class);
    xstream.processAnnotations(MirrorResourceListResponse.class);
    xstream.processAnnotations(MirrorResourceListRequest.class);
    xstream.processAnnotations(MirrorStatusResourceListResponse.class);
    xstream.processAnnotations(SmtpSettingsResourceRequest.class);
    xstream.processAnnotations(PlexusComponentListResourceResponse.class);
    xstream.processAnnotations(NexusRepositoryTypeListResourceResponse.class);
    xstream.processAnnotations(PrivilegeResourceRequest.class);
    xstream.processAnnotations(Maven2ArtifactInfoResourceRespose.class);

    xstream.alias("nexus-error", errorResponseClazz);
    xstream.alias("error", errorMessageClazz);
    xstream.registerLocalConverter(errorResponseClazz, "errors", new AliasingListConverter(errorMessageClazz,
        "error"));

    xstream.registerLocalConverter(ContentListResourceResponse.class, "data", new AliasingListConverter(
        ContentListResource.class, "content-item"));

    xstream.registerLocalConverter(RepositoryListResourceResponse.class, "data", new AliasingListConverter(
        RepositoryListResource.class, "repositories-item"));

    xstream.registerLocalConverter(NexusRepositoryTypeListResourceResponse.class, "data",
        new AliasingListConverter(NexusRepositoryTypeListResource.class, "repositoryType"));

    xstream.registerLocalConverter(RepositoryStatusListResourceResponse.class, "data", new AliasingListConverter(
        RepositoryStatusListResource.class, "repository-status-list-item"));

    xstream.registerLocalConverter(RepositoryGroupListResource.class, "repositories", new AliasingListConverter(
        RepositoryGroupMemberRepository.class, "repo-group-member"));
    xstream.registerLocalConverter(RepositoryGroupResource.class, "repositories", new AliasingListConverter(
        RepositoryGroupMemberRepository.class, "repo-group-member"));
    xstream.registerLocalConverter(RepositoryGroupListResourceResponse.class, "data", new AliasingListConverter(
        RepositoryGroupListResource.class, "repo-group-list-item"));

    xstream.registerLocalConverter(RepositoryRouteListResourceResponse.class, "data", new AliasingListConverter(
        RepositoryRouteListResource.class, "repo-routes-list-item"));
    xstream.registerLocalConverter(RepositoryRouteListResource.class, "repositories", new AliasingListConverter(
        RepositoryRouteMemberRepository.class, "repo-routes-member"));

    xstream.registerLocalConverter(RepositoryRouteResource.class, "repositories", new AliasingListConverter(
        RepositoryRouteMemberRepository.class, "repository"));

    xstream.registerLocalConverter(GlobalConfigurationListResourceResponse.class, "data",
        new AliasingListConverter(GlobalConfigurationListResource.class, "global-settings-list-item"));

    xstream.registerLocalConverter(ConfigurationsListResourceResponse.class, "data", new AliasingListConverter(
        ConfigurationsListResource.class, "configs-list-item"));

    xstream.registerLocalConverter(FeedListResourceResponse.class, "data", new AliasingListConverter(
        FeedListResource.class, "feeds-list-item"));

    xstream.alias("authentication-login", AuthenticationLoginResourceResponse.class); // Look at
    // NexusAuthenticationLoginResourceConverter,
    // we are only converting
    // the clientPermissions
    // field

    xstream.registerLocalConverter(AuthenticationClientPermissions.class, "permissions",
        new AliasingListConverter(ClientPermission.class, "permission"));

    xstream.registerLocalConverter(StatusConfigurationValidationResponse.class, "validationErrors",
        new AliasingListConverter(String.class, "error"));
    xstream.registerLocalConverter(StatusConfigurationValidationResponse.class, "validationWarnings",
        new AliasingListConverter(String.class, "warning"));

    xstream.registerLocalConverter(ScheduledServiceBaseResource.class, "properties", new AliasingListConverter(
        ScheduledServicePropertyResource.class, "scheduled-task-property"));
    xstream.registerLocalConverter(ScheduledServiceWeeklyResource.class, "recurringDay",
        new AliasingListConverter(String.class, "day"));
    xstream.registerLocalConverter(ScheduledServiceTypeResourceResponse.class, "data", new AliasingListConverter(
        ScheduledServiceTypeResource.class, "schedule-type"));
    xstream.registerLocalConverter(ScheduledServiceTypeResource.class, "formFields", new AliasingListConverter(
        FormFieldResource.class, "form-field"));
    xstream.registerLocalConverter(ScheduledServiceListResourceResponse.class, "data", new AliasingListConverter(
        ScheduledServiceListResource.class, "schedules-list-item"));

    xstream.aliasField("methods", PrivilegeResource.class, "method");

    xstream.registerLocalConverter(NFCResource.class, "nfcContents", new AliasingListConverter(
        NFCRepositoryResource.class, "nfc-repo-info"));
    xstream.registerLocalConverter(NFCRepositoryResource.class, "nfcPaths", new AliasingListConverter(
        String.class, "path"));

    xstream.registerLocalConverter(RepositoryTargetResource.class, "patterns", new AliasingListConverter(
        String.class, "pattern"));
    xstream.registerLocalConverter(RepositoryTargetListResourceResponse.class, "data", new AliasingListConverter(
        RepositoryTargetListResource.class, "repo-targets-list-item"));

    xstream.registerLocalConverter(RepositoryContentClassListResourceResponse.class, "data",
        new AliasingListConverter(RepositoryContentClassListResource.class, "repo-content-classes-list-item"));

    xstream.registerLocalConverter(PlexusComponentListResourceResponse.class, "data", new AliasingListConverter(
        PlexusComponentListResource.class, "component"));

    xstream.registerLocalConverter(MirrorResourceListRequest.class, "data", new AliasingListConverter(
        MirrorResource.class, "mirrorResource"));
    xstream.registerLocalConverter(MirrorResourceListResponse.class, "data", new AliasingListConverter(
        MirrorResource.class, "mirrorResource"));
    xstream.registerLocalConverter(MirrorStatusResourceListResponse.class, "data", new AliasingListConverter(
        MirrorStatusResource.class, "mirrorResource"));

    xstream.registerLocalConverter(ContentListDescribeRequestResource.class, "requestContext",
        new AliasingListConverter(String.class, "requestContextItem"));

    xstream.registerLocalConverter(ContentListDescribeResponseResource.class, "appliedMappings",
        new AliasingListConverter(String.class, "appliedMappingItem"));
    xstream.registerLocalConverter(ContentListDescribeResponseResource.class, "attributes",
        new AliasingListConverter(String.class, "attributeItem"));
    xstream.registerLocalConverter(ContentListDescribeResponseResource.class, "processedRepositoriesList",
        new AliasingListConverter(String.class, "processedRepositoriesListItem"));
    xstream.registerLocalConverter(ContentListDescribeResponseResource.class, "properties",
        new AliasingListConverter(String.class, "propertyItem"));
    xstream.registerLocalConverter(ContentListDescribeResponseResource.class, "sources",
        new AliasingListConverter(String.class, "sourceItem"));

    xstream.registerLocalConverter(RepositoryStatusResource.class, "dependentRepos", new AliasingListConverter(
        RepositoryDependentStatusResource.class, "dependentRepoItem"));

    xstream.registerLocalConverter(GlobalConfigurationResource.class, "securityRealms", new AliasingListConverter(
        String.class, "securityRealmItem"));

    // Maven model
    xstream.omitField(Model.class, "modelEncoding");
    xstream.omitField(ModelBase.class, "modelEncoding");
    xstream.omitField(Scm.class, "modelEncoding");

    // SECURITY below
    xstream.processAnnotations(AuthenticationLoginResourceResponse.class);
    xstream.processAnnotations(UserResourceResponse.class);
    xstream.processAnnotations(UserListResourceResponse.class);
    xstream.processAnnotations(UserResourceRequest.class);
    xstream.processAnnotations(UserForgotPasswordRequest.class);
    xstream.processAnnotations(UserChangePasswordRequest.class);
    xstream.registerLocalConverter(UserResource.class, "roles", new AliasingListConverter(String.class, "role"));
    xstream.registerLocalConverter(UserListResourceResponse.class, "data", new AliasingListConverter(
        UserResource.class, "users-list-item"));

    xstream.processAnnotations(RoleListResourceResponse.class);
    xstream.processAnnotations(RoleResource.class);
    xstream.processAnnotations(RoleResourceRequest.class);

    xstream.processAnnotations(RoleResourceResponse.class);
    xstream.registerLocalConverter(RoleListResourceResponse.class, "data", new AliasingListConverter(
        RoleResource.class, "roles-list-item"));
    xstream.registerLocalConverter(RoleResource.class, "roles", new AliasingListConverter(String.class, "role"));
    xstream.registerLocalConverter(RoleResource.class, "privileges", new AliasingListConverter(String.class,
        "privilege"));

    xstream.processAnnotations(PrivilegeListResourceResponse.class);
    xstream.processAnnotations(PrivilegeStatusResourceResponse.class);
    xstream.processAnnotations(PrivilegeTypeResourceResponse.class);
    xstream.registerLocalConverter(PrivilegeListResourceResponse.class, "data", new AliasingListConverter(
        PrivilegeStatusResource.class, "privilege-item"));
    xstream.registerLocalConverter(PrivilegeResource.class, "method", new AliasingListConverter(String.class,
        "method"));
    xstream.registerLocalConverter(PrivilegeStatusResource.class, "properties", new AliasingListConverter(
        PrivilegeProperty.class, "privilege-property"));
    xstream.registerLocalConverter(PrivilegeTypeResourceResponse.class, "data", new AliasingListConverter(
        PrivilegeTypeResource.class, "privilege-type"));
    xstream.registerLocalConverter(PrivilegeTypeResource.class, "properties", new AliasingListConverter(
        PrivilegeTypePropertyResource.class, "privilege-type-property"));

    xstream.processAnnotations(UserToRoleResourceRequest.class);
    xstream.registerLocalConverter(UserToRoleResource.class, "roles", new AliasingListConverter(String.class,
        "role"));

    xstream.processAnnotations(PlexusUserResourceResponse.class);
    xstream.registerLocalConverter(PlexusUserResource.class, "roles", new AliasingListConverter(
        PlexusRoleResource.class, "plexus-role"));

    xstream.processAnnotations(PlexusRoleResource.class);

    xstream.processAnnotations(PlexusUserListResourceResponse.class);
    xstream.registerLocalConverter(PlexusUserListResourceResponse.class, "data", new AliasingListConverter(
        PlexusUserResource.class, "plexus-user"));

    xstream.processAnnotations(ExternalRoleMappingListResourceResponse.class);
    xstream.processAnnotations(ExternalRoleMappingResourceResponse.class);
    xstream.processAnnotations(ExternalRoleMappingResource.class);

    xstream.registerLocalConverter(ExternalRoleMappingListResourceResponse.class, "data",
        new AliasingListConverter(ExternalRoleMappingResource.class, "mapping"));
    xstream.registerLocalConverter(ExternalRoleMappingResource.class, "mappedRoles", new AliasingListConverter(
        PlexusRoleResource.class, "plexus-role"));

    xstream.processAnnotations(PlexusRoleListResourceResponse.class);
    xstream.registerLocalConverter(PlexusRoleListResourceResponse.class, "data", new AliasingListConverter(
        PlexusRoleResource.class, "plexus-role"));

    xstream.processAnnotations(PlexusUserSearchCriteriaResourceRequest.class);

    xstream.processAnnotations(org.sonatype.security.rest.model.PlexusComponentListResourceResponse.class);
    xstream.processAnnotations(org.sonatype.security.rest.model.PlexusComponentListResource.class);
    xstream.registerLocalConverter(org.sonatype.security.rest.model.PlexusComponentListResourceResponse.class,
        "data", new AliasingListConverter(org.sonatype.security.rest.model.PlexusComponentListResource.class,
        "component"));

    xstream.processAnnotations(UserAccount.class);
    xstream.processAnnotations(UserAccountRequestResponseWrapper.class);

    // Automatic routing aka proxy404 (NEXUS-5472)
    xstream.processAnnotations(RoutingDiscoveryStatusMessage.class);
    xstream.processAnnotations(RoutingStatusMessage.class);
    xstream.processAnnotations(RoutingStatusMessageWrapper.class);
    xstream.processAnnotations(RoutingConfigMessage.class);
    xstream.processAnnotations(RoutingConfigMessageWrapper.class);

    return xstream;
  }
}
