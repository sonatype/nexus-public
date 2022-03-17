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
package org.sonatype.nexus.client.model;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeDescriptor;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeGroupPropertyDescriptor;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeRepositoryPropertyDescriptor;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeRepositoryTargetPropertyDescriptor;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.rest.NexusApplication;
import org.sonatype.nexus.rest.model.ArtifactResolveResource;
import org.sonatype.nexus.rest.model.ArtifactResolveResourceResponse;
import org.sonatype.nexus.rest.model.AuthenticationSettings;
import org.sonatype.nexus.rest.model.ConfigurationsListResource;
import org.sonatype.nexus.rest.model.ConfigurationsListResourceResponse;
import org.sonatype.nexus.rest.model.ContentListDescribeRequestResource;
import org.sonatype.nexus.rest.model.ContentListDescribeResource;
import org.sonatype.nexus.rest.model.ContentListDescribeResourceResponse;
import org.sonatype.nexus.rest.model.ContentListDescribeResponseResource;
import org.sonatype.nexus.rest.model.ContentListResource;
import org.sonatype.nexus.rest.model.ContentListResourceResponse;
import org.sonatype.nexus.rest.model.FeedListResource;
import org.sonatype.nexus.rest.model.FeedListResourceResponse;
import org.sonatype.nexus.rest.model.FormFieldResource;
import org.sonatype.nexus.rest.model.GlobalConfigurationListResource;
import org.sonatype.nexus.rest.model.GlobalConfigurationListResourceResponse;
import org.sonatype.nexus.rest.model.GlobalConfigurationResource;
import org.sonatype.nexus.rest.model.GlobalConfigurationResourceResponse;
import org.sonatype.nexus.rest.model.MirrorResource;
import org.sonatype.nexus.rest.model.MirrorResourceListRequest;
import org.sonatype.nexus.rest.model.MirrorResourceListResponse;
import org.sonatype.nexus.rest.model.MirrorStatusResource;
import org.sonatype.nexus.rest.model.MirrorStatusResourceListResponse;
import org.sonatype.nexus.rest.model.NFCRepositoryResource;
import org.sonatype.nexus.rest.model.NFCResource;
import org.sonatype.nexus.rest.model.NFCResourceResponse;
import org.sonatype.nexus.rest.model.NFCStats;
import org.sonatype.nexus.rest.model.NexusAuthenticationClientPermissions;
import org.sonatype.nexus.rest.model.NexusRepositoryTypeListResource;
import org.sonatype.nexus.rest.model.NexusRepositoryTypeListResourceResponse;
import org.sonatype.nexus.rest.model.PlexusComponentListResource;
import org.sonatype.nexus.rest.model.PlexusComponentListResourceResponse;
import org.sonatype.nexus.rest.model.PrivilegeResource;
import org.sonatype.nexus.rest.model.PrivilegeResourceRequest;
import org.sonatype.nexus.rest.model.RemoteConnectionSettings;
import org.sonatype.nexus.rest.model.RemoteHttpProxySettingsDTO;
import org.sonatype.nexus.rest.model.RemoteProxySettingsDTO;
import org.sonatype.nexus.rest.model.RepositoryContentClassListResource;
import org.sonatype.nexus.rest.model.RepositoryContentClassListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryDependentStatusResource;
import org.sonatype.nexus.rest.model.RepositoryGroupListResource;
import org.sonatype.nexus.rest.model.RepositoryGroupListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryGroupMemberRepository;
import org.sonatype.nexus.rest.model.RepositoryGroupResource;
import org.sonatype.nexus.rest.model.RepositoryGroupResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryListResource;
import org.sonatype.nexus.rest.model.RepositoryListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryMetaResource;
import org.sonatype.nexus.rest.model.RepositoryMetaResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryProxyResource;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.rest.model.RepositoryResourceRemoteStorage;
import org.sonatype.nexus.rest.model.RepositoryResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryRouteListResource;
import org.sonatype.nexus.rest.model.RepositoryRouteListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryRouteMemberRepository;
import org.sonatype.nexus.rest.model.RepositoryRouteResource;
import org.sonatype.nexus.rest.model.RepositoryRouteResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryShadowResource;
import org.sonatype.nexus.rest.model.RepositoryStatusListResource;
import org.sonatype.nexus.rest.model.RepositoryStatusListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryStatusResource;
import org.sonatype.nexus.rest.model.RepositoryStatusResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryTargetListResource;
import org.sonatype.nexus.rest.model.RepositoryTargetListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryTargetResource;
import org.sonatype.nexus.rest.model.RepositoryTargetResourceResponse;
import org.sonatype.nexus.rest.model.RestApiSettings;
import org.sonatype.nexus.rest.model.ScheduledServiceAdvancedResource;
import org.sonatype.nexus.rest.model.ScheduledServiceBaseResource;
import org.sonatype.nexus.rest.model.ScheduledServiceDailyResource;
import org.sonatype.nexus.rest.model.ScheduledServiceListResource;
import org.sonatype.nexus.rest.model.ScheduledServiceListResourceResponse;
import org.sonatype.nexus.rest.model.ScheduledServiceMonthlyResource;
import org.sonatype.nexus.rest.model.ScheduledServiceOnceResource;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.rest.model.ScheduledServiceResourceResponse;
import org.sonatype.nexus.rest.model.ScheduledServiceResourceStatus;
import org.sonatype.nexus.rest.model.ScheduledServiceResourceStatusResponse;
import org.sonatype.nexus.rest.model.ScheduledServiceTypeResource;
import org.sonatype.nexus.rest.model.ScheduledServiceTypeResourceResponse;
import org.sonatype.nexus.rest.model.ScheduledServiceWeeklyResource;
import org.sonatype.nexus.rest.model.SmtpSettings;
import org.sonatype.nexus.rest.model.SmtpSettingsResource;
import org.sonatype.nexus.rest.model.SmtpSettingsResourceRequest;
import org.sonatype.nexus.rest.model.StatusConfigurationValidationResponse;
import org.sonatype.nexus.rest.model.StatusResource;
import org.sonatype.nexus.rest.model.StatusResourceResponse;
import org.sonatype.nexus.rest.model.WastebasketResource;
import org.sonatype.nexus.rest.model.WastebasketResourceResponse;
import org.sonatype.plexus.rest.resource.error.ErrorMessage;
import org.sonatype.plexus.rest.resource.error.ErrorResponse;
import org.sonatype.plexus.rest.xstream.json.JsonOrgHierarchicalStreamDriver;
import org.sonatype.plexus.rest.xstream.xml.LookAheadXppDriver;
import org.sonatype.security.realms.privileges.application.ApplicationPrivilegeDescriptor;
import org.sonatype.security.realms.privileges.application.ApplicationPrivilegeMethodPropertyDescriptor;
import org.sonatype.security.realms.privileges.application.ApplicationPrivilegePermissionPropertyDescriptor;
import org.sonatype.security.rest.model.AuthenticationClientPermissions;
import org.sonatype.security.rest.model.AuthenticationLoginResource;
import org.sonatype.security.rest.model.AuthenticationLoginResourceResponse;
import org.sonatype.security.rest.model.ClientPermission;
import org.sonatype.security.rest.model.ExternalRoleMappingListResourceResponse;
import org.sonatype.security.rest.model.ExternalRoleMappingResource;
import org.sonatype.security.rest.model.PlexusRoleListResourceResponse;
import org.sonatype.security.rest.model.PlexusRoleResource;
import org.sonatype.security.rest.model.PlexusUserListResourceResponse;
import org.sonatype.security.rest.model.PlexusUserResource;
import org.sonatype.security.rest.model.PlexusUserResourceResponse;
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
import org.sonatype.security.rest.model.UserChangePasswordResource;
import org.sonatype.security.rest.model.UserForgotPasswordResource;
import org.sonatype.security.rest.model.UserListResourceResponse;
import org.sonatype.security.rest.model.UserResource;
import org.sonatype.security.rest.model.UserResourceRequest;
import org.sonatype.security.rest.model.UserResourceResponse;
import org.sonatype.security.rest.model.UserToRoleResource;
import org.sonatype.security.rest.model.UserToRoleResourceRequest;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.lang.time.DateUtils;
import org.codehaus.plexus.util.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestMarshalUnmarchal
{
  private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

  private XStream xstreamXML;

  private XStream xstreamJSON;

  @Before
  public void setUp()
      throws Exception
  {

    NexusApplication napp = new NexusApplication();

    xstreamXML = napp.doConfigureXstream(new XStream(new LookAheadXppDriver()));

    xstreamJSON = napp.doConfigureXstream(new XStream(new JsonOrgHierarchicalStreamDriver()));

    // We only do this because it is test code
    xstreamXML.allowTypesByWildcard(new String[] {"**"});
    xstreamJSON.allowTypesByWildcard(new String[] {"**"});
  }

  protected XStream getXmlXStream() {
    return xstreamXML;
  }

  protected XStream getJsonXStream() {
    return xstreamJSON;
  }

  @Test
  public void testErrorResponse() {
    ErrorResponse errorResponse = new ErrorResponse();
    ErrorMessage error = new ErrorMessage();
    error.setId("ID");
    error.setMsg("Error Message");
    errorResponse.addError(error);

    this.marshalUnmarchalThenCompare(errorResponse);

    // System.out.println( "JSON:\n" + this.xstreamJSON.toXML( errorResponse ));
    // System.out.println( "XML:\n" + this.xstreamXML.toXML( errorResponse ));

    this.validateXmlHasNoPackageNames(errorResponse);

  }

  @Test
  public void testContentListDescribeResourceResponse()
      throws ParseException
  {
    ContentListDescribeResource resource = new ContentListDescribeResource();
    resource.setProcessingTimeMillis(1000);
    resource.setRequestUrl("requesturl");

    ContentListDescribeRequestResource requestRes = new ContentListDescribeRequestResource();
    // TODO: Figure out why this is causing test to fail.
    // requestRes.setRequestContext( Arrays.asList( "item1", "item2" ) );
    requestRes.setRequestPath("requestpath");
    requestRes.setRequestUrl("requestUrl");

    resource.setRequest(requestRes);

    ContentListDescribeResponseResource responseRes = new ContentListDescribeResponseResource();
    // TODO: Figure out why this is causing test to fail.
    // responseRes.setAppliedMappings( Arrays.asList( "map1", "map2" ) );
    // TODO: Figure out why this is causing test to fail.
    // responseRes.setAttributes( Arrays.asList( "attr1", "attr2" ) );
    responseRes.setOriginatingRepositoryId("originatingRepoId");
    responseRes.setOriginatingRepositoryMainFacet("mainfacet");
    responseRes.setOriginatingRepositoryName("name");
    // TODO: Figure out why this is causing test to fail.
    // responseRes.setProcessedRepositoriesList( Arrays.asList( "proc1", "proc2" ) );
    // TODO: Figure out why this is causing test to fail.
    // responseRes.setProperties( Arrays.asList( "prop1", "prop2" ) );
    responseRes.setResponseActualClass("actualclass");
    responseRes.setResponsePath("responsepath");
    responseRes.setResponseType("responseType");
    responseRes.setResponseUid("responseuid");
    // TODO: Figure out why this is causing test to fail.
    // responseRes.setSources( Arrays.asList( "source1", "source2" ) );

    resource.setResponse(responseRes);

    ContentListDescribeResourceResponse response = new ContentListDescribeResourceResponse();
    response.setData(resource);

    // TODO: UNCOMMENT ME WHEN JSON DRIVER IS FIXED, SO LONGS CAN BE HANDLED!!
    // this.marshalUnmarchalThenCompare( response );
    this.validateXmlHasNoPackageNames(response);
  }

  @Test
  public void testContentListResourceResponse()
      throws ParseException
  {

    ContentListResourceResponse responseResponse = new ContentListResourceResponse();
    ContentListResource resource1 = new ContentListResource();
    resource1.setLastModified(this.dateFormat.parse("01/01/2001"));
    resource1.setLeaf(false);
    resource1.setRelativePath("relativePath1");
    resource1.setResourceURI("resourceURI1");
    resource1.setSizeOnDisk(41);
    resource1.setText("resource1");

    ContentListResource resource2 = new ContentListResource();
    resource2.setLastModified(this.dateFormat.parse("01/01/2002"));
    resource2.setLeaf(true);
    resource2.setRelativePath("relativePath2");
    resource2.setResourceURI("resourceURI2");
    resource2.setSizeOnDisk(42);
    resource2.setText("resource2");

    ContentListResource resource3 = new ContentListResource();
    resource3.setLastModified(this.dateFormat.parse("01/01/2003"));
    resource3.setLeaf(true);
    resource3.setRelativePath("relativePath3");
    resource3.setResourceURI("resourceURI3");
    resource3.setSizeOnDisk(43);
    resource3.setText("resource3");

    responseResponse.addData(resource1);
    responseResponse.addData(resource2);

    this.marshalUnmarchalThenCompare(responseResponse, this.xstreamXML); // FIXME: JSON READER CANNOT PARSE DATES
    // CORRECTLY.
    this.validateXmlHasNoPackageNames(responseResponse);

  }

  @Test
  public void testRepositoryResource() {
    RepositoryResource repo = new RepositoryResource();

    repo.setId("createTestRepo");
    repo.setRepoType("hosted");
    repo.setName("Create Test Repo");
    repo.setFormat("maven2");
    repo.setWritePolicy(RepositoryWritePolicy.ALLOW_WRITE.name());
    repo.setBrowseable(true);
    repo.setIndexable(true);
    repo.setNotFoundCacheTTL(1440);
    repo.setRepoPolicy(RepositoryPolicy.RELEASE.name());
    repo.setDownloadRemoteIndexes(true);
    repo.setChecksumPolicy("IGNORE");
    repo.setContentResourceURI("contentResourceURI");
    repo.setDefaultLocalStorageUrl("defaultlocalstorage");
    repo.setExposed(true);
    repo.setOverrideLocalStorageUrl("overridelocalstorage");
    repo.setProvider("provider");
    repo.setProviderRole("providerRole");

    RepositoryResourceRemoteStorage remoteStorage = new RepositoryResourceRemoteStorage();
    remoteStorage.setRemoteStorageUrl("remoteStorageUrl");

    AuthenticationSettings auth = new AuthenticationSettings();
    auth.setNtlmDomain("ntlmdomain");
    auth.setNtlmHost("ntmlhost");
    auth.setPassword("password");
    auth.setUsername("username");

    remoteStorage.setAuthentication(auth);

    RemoteConnectionSettings connection = new RemoteConnectionSettings();
    connection.setConnectionTimeout(50);
    connection.setQueryString("querystring");
    connection.setRetrievalRetryCount(5);
    connection.setUserAgentString("useragent");

    remoteStorage.setConnectionSettings(connection);

    repo.setRemoteStorage(remoteStorage);

    RepositoryResourceResponse resourceResponse = new RepositoryResourceResponse();
    resourceResponse.setData(repo);

    this.marshalUnmarchalThenCompare(resourceResponse);
    // this.marshalUnmarchalThenCompare( resourceResponse, xstreamJSON );
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testRepositoryShadowResource() {
    RepositoryShadowResource repo = new RepositoryShadowResource();

    repo.setId("createTestRepo");
    repo.setRepoType("virtual");
    repo.setName("Create Test Repo");
    repo.setFormat("maven2");
    repo.setShadowOf("Shadow Of");
    repo.setSyncAtStartup(true);
    repo.setContentResourceURI("contentResourceURI");
    repo.setExposed(true);
    repo.setProvider("provider");
    repo.setProviderRole("providerrole");

    RepositoryResourceResponse resourceResponse = new RepositoryResourceResponse();
    resourceResponse.setData(repo);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testRepositoryProxyResource() {
    RepositoryProxyResource repo = new RepositoryProxyResource();

    repo.setId("createTestRepo");
    repo.setRepoType("proxy");
    repo.setName("Create Test Repo");
    repo.setFormat("maven2");
    repo.setWritePolicy(RepositoryWritePolicy.ALLOW_WRITE.name());
    repo.setBrowseable(true);
    repo.setIndexable(true);
    repo.setNotFoundCacheTTL(1440);
    repo.setRepoPolicy(RepositoryPolicy.RELEASE.name());
    repo.setDownloadRemoteIndexes(true);
    repo.setChecksumPolicy("IGNORE");
    repo.setMetadataMaxAge(42);
    repo.setArtifactMaxAge(41);
    repo.setItemMaxAge(43);
    repo.setContentResourceURI("contentResourceURI");
    repo.setDefaultLocalStorageUrl("defaultlocalstorage");
    repo.setExposed(true);
    repo.setOverrideLocalStorageUrl("overridelocalstorage");
    repo.setProvider("provider");
    repo.setProviderRole("providerRole");

    RepositoryResourceRemoteStorage remoteStorage = new RepositoryResourceRemoteStorage();
    remoteStorage.setRemoteStorageUrl("remoteStorageUrl");

    AuthenticationSettings auth = new AuthenticationSettings();
    auth.setNtlmDomain("ntlmdomain");
    auth.setNtlmHost("ntmlhost");
    auth.setPassword("password");
    auth.setUsername("username");

    remoteStorage.setAuthentication(auth);

    RemoteConnectionSettings connection = new RemoteConnectionSettings();
    connection.setConnectionTimeout(50);
    connection.setQueryString("querystring");
    connection.setRetrievalRetryCount(5);
    connection.setUserAgentString("useragent");

    remoteStorage.setConnectionSettings(connection);

    repo.setRemoteStorage(remoteStorage);

    RepositoryResourceResponse resourceResponse = new RepositoryResourceResponse();
    resourceResponse.setData(repo);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testRepositoryListResourceResponse() {
    RepositoryListResourceResponse listResourceResponse = new RepositoryListResourceResponse();

    RepositoryListResource listResource1 = new RepositoryListResource();
    listResource1.setId("item1");
    listResource1.setFormat("maven2");
    listResource1.setEffectiveLocalStorageUrl("effectiveLocalStorageUrl1");
    listResource1.setName("name1");
    listResource1.setRemoteUri("remoteUri1");
    listResource1.setRepoPolicy("remotePolicy1");
    listResource1.setRepoType("hosted");
    listResource1.setResourceURI("resourceURI1");
    listResource1.setContentResourceURI("contentResourceUri1");
    listResource1.setExposed(true);
    listResource1.setProvider("provider1");
    listResource1.setProviderRole("providerRole1");
    listResource1.setUserManaged(true);
    listResourceResponse.addData(listResource1);

    RepositoryListResource listResource2 = new RepositoryListResource();
    listResource2.setId("item2");
    listResource2.setFormat("maven2");
    listResource2.setEffectiveLocalStorageUrl("effectiveLocalStorageUrl2");
    listResource2.setName("name2");
    listResource2.setRemoteUri("remoteUri2");
    listResource2.setRepoPolicy("remotePolicy2");
    listResource2.setRepoType("virtual");
    listResource2.setResourceURI("resourceURI2");
    listResource2.setContentResourceURI("contentResourceUri2");
    listResource2.setExposed(true);
    listResource2.setProvider("provider2");
    listResource2.setProviderRole("providerRole2");
    listResource2.setUserManaged(true);
    listResourceResponse.addData(listResource2);

    this.marshalUnmarchalThenCompare(listResourceResponse);
    this.validateXmlHasNoPackageNames(listResourceResponse);
  }

  @Test
  public void testRepositoryStatusResourceResponse() {

    RepositoryStatusResourceResponse resourceResponse = new RepositoryStatusResourceResponse();
    RepositoryStatusResource resource = new RepositoryStatusResource();
    resource.setFormat("maven1");
    resource.setId("resource");
    resource.setLocalStatus("localStatus");
    resource.setProxyMode("proxyMode");
    resource.setRepoType("repoType");
    resource.setRemoteStatus("remoteStatus");

    RepositoryDependentStatusResource dep = new RepositoryDependentStatusResource();
    dep.setFormat("maven4");
    dep.setId("someid");
    dep.setLocalStatus("somestatus");
    dep.setRepoType("type");

    resource.addDependentRepo(dep);

    resourceResponse.setData(resource);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testRepositoryStatusListResourceResponse() {
    RepositoryStatusListResourceResponse resourceResponse = new RepositoryStatusListResourceResponse();

    RepositoryStatusListResource listResource1 = new RepositoryStatusListResource();
    listResource1.setFormat("maven1");
    listResource1.setId("item1");
    listResource1.setName("name1");
    listResource1.setRepoPolicy("repoPolicy1");
    listResource1.setRepoType("repoType1");
    listResource1.setResourceURI("resourceURI");

    RepositoryStatusResource statusResource1 = new RepositoryStatusResource();
    statusResource1.setFormat("maven1");
    statusResource1.setId("status1");
    statusResource1.setLocalStatus("localStatus1");
    statusResource1.setProxyMode("proxyMode");
    statusResource1.setRemoteStatus("remoteStatus");
    statusResource1.setRepoType("repoType");
    listResource1.setStatus(statusResource1);

    RepositoryStatusListResource listResource2 = new RepositoryStatusListResource();
    listResource2.setFormat("maven1");
    listResource2.setId("item2");
    listResource2.setName("name2");
    listResource2.setRepoPolicy("repoPolicy2");
    listResource2.setRepoType("repoType2");
    listResource2.setResourceURI("resourceURI");

    RepositoryStatusResource statusResource2 = new RepositoryStatusResource();
    statusResource2.setFormat("maven1");
    statusResource2.setId("status1");
    statusResource2.setLocalStatus("localStatus1");
    statusResource2.setProxyMode("proxyMode");
    statusResource2.setRemoteStatus("remoteStatus");
    statusResource2.setRepoType("repoType");
    listResource2.setStatus(statusResource2);

    resourceResponse.addData(listResource1);
    resourceResponse.addData(listResource2);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testRepositoryMetaResourceResponse() {
    RepositoryMetaResourceResponse resourceResponse = new RepositoryMetaResourceResponse();
    RepositoryMetaResource metaResource = new RepositoryMetaResource();
    metaResource.setFileCountInRepository(1000);
    metaResource.setFormat("format");
    metaResource.setFreeSpaceOnDisk(55);
    metaResource.setId("metaResource");
    metaResource.setLocalStorageErrorsCount(7);
    metaResource.setNotFoundCacheHits(2);
    metaResource.setNotFoundCacheMisses(3);
    metaResource.setNotFoundCacheSize(4);
    metaResource.setRemoteStorageErrorsCount(9);
    metaResource.setRepoType("repoType");
    metaResource.setSizeOnDisk(42);
    metaResource.setGroups(Arrays.asList("group1", "group2"));

    resourceResponse.setData(metaResource);

    this.marshalUnmarchalThenCompare(resourceResponse, this.xstreamXML); // FIXME: Need some sort of type map, for
    // the json reader to figure out if some
    // fields are longs not ints.
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testRepositoryGroupListResourceResponse() {
    RepositoryGroupListResourceResponse resourceResponse = new RepositoryGroupListResourceResponse();

    RepositoryGroupListResource listItem1 = new RepositoryGroupListResource();
    listItem1.setFormat("format");
    listItem1.setId("id");
    listItem1.setName("name");
    listItem1.setResourceURI("resourceURI");
    listItem1.setContentResourceURI("contentResourceURI");
    listItem1.setExposed(true);

    RepositoryGroupListResource listItem2 = new RepositoryGroupListResource();
    listItem2.setFormat("format2");
    listItem2.setId("id2");
    listItem2.setName("name2");
    listItem2.setResourceURI("resourceURI2");
    listItem2.setContentResourceURI("contentResourceURI2");
    listItem2.setExposed(true);

    resourceResponse.addData(listItem1);
    resourceResponse.addData(listItem2);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testRepositoryGroupResourceResponse() {
    RepositoryGroupResourceResponse resourceResponse = new RepositoryGroupResourceResponse();
    RepositoryGroupResource groupResource = new RepositoryGroupResource();
    groupResource.setFormat("format");
    groupResource.setId("groupResource");
    groupResource.setName("name");
    groupResource.setContentResourceURI("contentResourceURI");
    groupResource.setExposed(true);
    groupResource.setProvider("provider");
    groupResource.setProviderRole("providerRole");
    groupResource.setRepoType("group");

    RepositoryGroupMemberRepository memberRepo1 = new RepositoryGroupMemberRepository();
    memberRepo1.setId("memberRepo1");
    memberRepo1.setName("memberRepo1");
    memberRepo1.setResourceURI("memberRepoURI1");

    RepositoryGroupMemberRepository memberRepo2 = new RepositoryGroupMemberRepository();
    memberRepo2.setId("memberRepo2");
    memberRepo2.setName("memberRepo2");
    memberRepo2.setResourceURI("memberRepoURI2");
    groupResource.addRepository(memberRepo1);
    groupResource.addRepository(memberRepo2);

    resourceResponse.setData(groupResource);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testRepositoryRouteListResourceResponse() {
    RepositoryRouteListResourceResponse resourceResponse = new RepositoryRouteListResourceResponse();

    RepositoryRouteListResource item1 = new RepositoryRouteListResource();
    item1.setGroupId("id1");
    item1.setPattern("pattern1");
    item1.setResourceURI("resourceURI1");
    item1.setRuleType("ruleType1");

    RepositoryRouteMemberRepository memberRepository1 = new RepositoryRouteMemberRepository();
    memberRepository1.setId("member1");
    memberRepository1.setName("memberRepository1");
    memberRepository1.setResourceURI("memberRepositoryURI1");
    item1.addRepository(memberRepository1);

    RepositoryRouteMemberRepository memberRepository2 = new RepositoryRouteMemberRepository();
    memberRepository2.setId("member2");
    memberRepository2.setName("memberRepository2");
    memberRepository2.setResourceURI("memberRepositoryURI2");
    item1.addRepository(memberRepository2);

    RepositoryRouteListResource item2 = new RepositoryRouteListResource();
    item2.setGroupId("id2");
    item2.setPattern("pattern2");
    item2.setResourceURI("resourceURI2");
    item2.setRuleType("ruleType2");

    RepositoryRouteMemberRepository memberRepository3 = new RepositoryRouteMemberRepository();
    memberRepository3.setId("member3");
    memberRepository3.setName("memberRepository3");
    memberRepository3.setResourceURI("memberRepositoryURI3");
    item2.addRepository(memberRepository3);

    resourceResponse.addData(item1);
    resourceResponse.addData(item2);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);

  }

  @Test
  public void testRepositoryRouteResourceResponse() {
    RepositoryRouteResourceResponse resourceResponse = new RepositoryRouteResourceResponse();

    RepositoryRouteResource resource = new RepositoryRouteResource();
    resource.setGroupId("groupId");
    resource.setId("id");
    resource.setPattern("pattern");
    resource.setRuleType("ruleType");

    RepositoryRouteMemberRepository memberRepository1 = new RepositoryRouteMemberRepository();
    memberRepository1.setId("member1");
    memberRepository1.setName("memberRepository1");
    memberRepository1.setResourceURI("memberRepositoryURI1");
    resource.addRepository(memberRepository1);

    RepositoryRouteMemberRepository memberRepository2 = new RepositoryRouteMemberRepository();
    memberRepository2.setId("member2");
    memberRepository2.setName("memberRepository2");
    memberRepository2.setResourceURI("memberRepositoryURI2");
    resource.addRepository(memberRepository2);

    resourceResponse.setData(resource);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testGlobalConfigurationListResourceResponse() {
    GlobalConfigurationListResourceResponse resourceResponse = new GlobalConfigurationListResourceResponse();

    GlobalConfigurationListResource resource1 = new GlobalConfigurationListResource();
    resource1.setName("name1");
    resource1.setResourceURI("resourceURI1");

    GlobalConfigurationListResource resource2 = new GlobalConfigurationListResource();
    resource2.setName("name1");
    resource2.setResourceURI("resourceURI1");

    resourceResponse.addData(resource1);
    resourceResponse.addData(resource2);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testGlobalConfigurationResourceResponse() {
    GlobalConfigurationResourceResponse resourceResponse = new GlobalConfigurationResourceResponse();

    GlobalConfigurationResource resource = new GlobalConfigurationResource();
    resource.setSecurityAnonymousAccessEnabled(true);
    resource.setSecurityAnonymousPassword("anonPass");
    resource.setSecurityAnonymousUsername("anonUser");
    // TODO: Figure out why this is causing test to fail...
    // resource.setSecurityRealms( Arrays.asList( "realm1", "realm2" ) );

    RestApiSettings restSet = new RestApiSettings();
    restSet.setBaseUrl("baseUrl");
    restSet.setForceBaseUrl(false);
    resource.setGlobalRestApiSettings(restSet);

    RemoteConnectionSettings connSet = new RemoteConnectionSettings();
    connSet.setConnectionTimeout(2);
    connSet.setQueryString("queryString");
    connSet.setRetrievalRetryCount(6);
    connSet.setUserAgentString("userAgentString");
    resource.setGlobalConnectionSettings(connSet);

    RemoteHttpProxySettingsDTO proxyHttpSet = new RemoteHttpProxySettingsDTO();
    proxyHttpSet.setProxyHostname("proxyHostname1");
    proxyHttpSet.setProxyPort(78);
    AuthenticationSettings authSet = new AuthenticationSettings();
    authSet.setNtlmDomain("ntlmDomain1");
    authSet.setNtlmHost("ntlmHost1");
    authSet.setPassword("password1");
    authSet.setUsername("username1");
    proxyHttpSet.setAuthentication(authSet);

    RemoteHttpProxySettingsDTO proxyHttpsSet = new RemoteHttpProxySettingsDTO();
    proxyHttpsSet.setProxyHostname("proxyHostname2");
    proxyHttpsSet.setProxyPort(87);
    AuthenticationSettings httpsAuthSet = new AuthenticationSettings();
    httpsAuthSet.setNtlmDomain("ntlmDomain2");
    httpsAuthSet.setNtlmHost("ntlmHost2");
    httpsAuthSet.setPassword("password2");
    httpsAuthSet.setUsername("username2");
    proxyHttpsSet.setAuthentication(httpsAuthSet);

    RemoteProxySettingsDTO proxySet = new RemoteProxySettingsDTO();
    proxySet.setHttpProxySettings(proxyHttpSet);
    proxySet.setHttpsProxySettings(proxyHttpsSet);

    resource.setRemoteProxySettings(proxySet);

    SmtpSettings smtpSet = new SmtpSettings();
    smtpSet.setHost("host");
    smtpSet.setPassword("password");
    smtpSet.setPort(42);
    smtpSet.setSslEnabled(true);
    smtpSet.setSystemEmailAddress("foo@bar.com");
    smtpSet.setTlsEnabled(true);
    smtpSet.setUsername("username");
    resource.setSmtpSettings(smtpSet);

    resourceResponse.setData(resource);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testWastebasketResourceResponse() {
    WastebasketResourceResponse resourceResponse = new WastebasketResourceResponse();

    WastebasketResource resource = new WastebasketResource();
    resource.setItemCount(1000);
    resource.setSize(42);

    resourceResponse.setData(resource);

    this.marshalUnmarchalThenCompare(resourceResponse, this.xstreamXML); // FIXME: Need some sort of type map, for
    // the json reader to figure out if some
    // fields are longs not ints.
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testConfigurationsListResourceResponse() {
    ConfigurationsListResourceResponse resourceResponse = new ConfigurationsListResourceResponse();

    ConfigurationsListResource item1 = new ConfigurationsListResource();
    item1.setName("name1");
    item1.setResourceURI("resourceURI1");

    ConfigurationsListResource item2 = new ConfigurationsListResource();
    item2.setName("name2");
    item2.setResourceURI("resourceURI2");

    resourceResponse.addData(item1);
    resourceResponse.addData(item2);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testFeedListResourceResponse() {
    FeedListResourceResponse resourceResponse = new FeedListResourceResponse();

    FeedListResource item1 = new FeedListResource();
    item1.setName("feed1");
    item1.setResourceURI("resourceURI1");

    FeedListResource item2 = new FeedListResource();
    item2.setName("feed2");
    item2.setResourceURI("resourceURI2");

    resourceResponse.addData(item1);
    resourceResponse.addData(item2);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testAuthenticationLoginResourceResponse() {
    AuthenticationLoginResourceResponse resourceResponse = new AuthenticationLoginResourceResponse();
    AuthenticationLoginResource loginResource = new AuthenticationLoginResource();
    AuthenticationClientPermissions perms = new AuthenticationClientPermissions();
    ClientPermission permission = new ClientPermission();
    permission.setId("id");
    permission.setValue(5);
    perms.addPermission(permission);
    perms.setLoggedIn(true);
    perms.setLoggedInUsername("fred");

    loginResource.setClientPermissions(perms);
    resourceResponse.setData(loginResource);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testStatusResourceResponse()
      throws ParseException
  {
    StatusResourceResponse resourceResponse = new StatusResourceResponse();

    StatusResource status = new StatusResource();
    NexusAuthenticationClientPermissions perms = new NexusAuthenticationClientPermissions();
    ClientPermission permission = new ClientPermission();
    permission.setId("id");
    permission.setValue(5);
    perms.addPermission(permission);
    perms.setLoggedIn(true);
    perms.setLoggedInUsername("fred");
    perms.setLoggedInUserSource("source");

    status.setClientPermissions(perms);
    status.setConfigurationUpgraded(true);
    status.setErrorCause("errorCause");
    status.setFirstStart(true);
    status.setInitializedAt(this.dateFormat.parse("01/01/2001"));
    status.setInstanceUpgraded(true);
    status.setLastConfigChange(this.dateFormat.parse("01/01/2002"));
    status.setOperationMode("operationMode");
    status.setStartedAt(this.dateFormat.parse("01/01/2003"));
    status.setState("STATE");
    status.setVersion("version");
    status.setApiVersion("apiversion");
    status.setAppName("appname");
    status.setBaseUrl("baseurl");
    status.setEditionLong("long edition name");
    status.setEditionShort("short");
    status.setAttributionsURL("http://my.attributions.com/url");
    status.setPurchaseURL("http://my.store.com/url");
    status.setUserLicenseURL("http://my.userlicense.com/url");

    status.setFormattedAppName("formatted");

    StatusConfigurationValidationResponse validation = new StatusConfigurationValidationResponse();
    validation.setModified(true);
    validation.setValid(true);

    validation.addValidationError("error1");
    validation.addValidationError("error2");

    validation.addValidationWarning("warning1");
    validation.addValidationWarning("warning2");
    status.setConfigurationValidationResponse(validation);

    resourceResponse.setData(status);

    this.marshalUnmarchalThenCompare(resourceResponse, this.xstreamXML); // FIXME: JSON READER CANNOT PARSE DATES
    // CORRECTLY.
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testScheduledServiceListResourceResponse() {
    ScheduledServiceListResourceResponse resourceResponse = new ScheduledServiceListResourceResponse();

    ScheduledServiceListResource item1 = new ScheduledServiceListResource();
    item1.setCreated("created1");
    item1.setEnabled(true);
    item1.setId("id1");
    item1.setLastRunResult("result1");
    item1.setLastRunTime("Time");
    item1.setName("name1");
    item1.setNextRunTime("nextRunTime1");
    item1.setResourceURI("resourceURI1");
    item1.setSchedule("schedule1");
    item1.setStatus("status1");
    item1.setTypeId("typeId1");
    item1.setTypeName("typeName1");
    resourceResponse.addData(item1);

    ScheduledServiceListResource item2 = new ScheduledServiceListResource();
    item2.setCreated("created2");
    item2.setEnabled(true);
    item2.setId("id2");
    item2.setLastRunResult("result2");
    item2.setLastRunTime("Time2");
    item2.setName("name2");
    item2.setNextRunTime("nextRunTime2");
    item2.setResourceURI("resourceURI2");
    item2.setSchedule("schedule2");
    item2.setStatus("status2");
    item2.setTypeId("typeId2");
    item2.setTypeName("typeName2");
    resourceResponse.addData(item2);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testScheduledServiceResourceStatusResponse() {
    ScheduledServiceResourceStatus status = new ScheduledServiceResourceStatus();
    status.setCreated("created");
    status.setLastRunResult("lastrunresult");
    status.setLastRunTime("lastruntime");
    status.setNextRunTime("nextruntime");
    status.setResourceURI("resourceuri");
    status.setStatus("status");

    ScheduledServiceBaseResource base = new ScheduledServiceBaseResource();
    base.setId("Id");
    base.setSchedule("manual");
    base.setTypeId("TypeId");
    base.setName("Name");
    base.setAlertEmail("foo@bar.org");
    base.setEnabled(true);

    ScheduledServicePropertyResource prop1 = new ScheduledServicePropertyResource();
    prop1.setKey("id1");
    prop1.setValue("value1");
    base.addProperty(prop1);

    ScheduledServicePropertyResource prop2 = new ScheduledServicePropertyResource();
    prop2.setKey("id2");
    prop2.setValue("value2");
    base.addProperty(prop2);

    status.setResource(base);

    ScheduledServiceResourceStatusResponse resource = new ScheduledServiceResourceStatusResponse();
    resource.setData(status);

    this.marshalUnmarchalThenCompare(resource);
    this.validateXmlHasNoPackageNames(resource);
  }

  @Test
  public void testScheduledServiceBaseResource() {
    ScheduledServiceBaseResource resource = new ScheduledServiceBaseResource();
    resource.setId("Id");
    resource.setSchedule("manual");
    resource.setTypeId("TypeId");
    resource.setName("Name");
    resource.setAlertEmail("foo@bar.org");
    resource.setEnabled(true);

    ScheduledServicePropertyResource prop1 = new ScheduledServicePropertyResource();
    prop1.setKey("id1");
    prop1.setValue("value1");
    resource.addProperty(prop1);

    ScheduledServicePropertyResource prop2 = new ScheduledServicePropertyResource();
    prop2.setKey("id2");
    prop2.setValue("value2");
    resource.addProperty(prop2);

    ScheduledServiceResourceResponse resourceResponse = new ScheduledServiceResourceResponse();
    resourceResponse.setData(resource);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);

  }

  @Test
  public void testScheduledServiceOnceResource() {
    ScheduledServiceOnceResource resource = new ScheduledServiceOnceResource();
    resource.setStartDate("StartDate");
    resource.setStartTime("StartTime");
    resource.setId("Id");
    resource.setSchedule("once");
    resource.setTypeId("TypeId");
    resource.setAlertEmail("foo@bar.org");
    resource.setName("Name");
    resource.setEnabled(true);

    ScheduledServicePropertyResource prop1 = new ScheduledServicePropertyResource();
    prop1.setKey("id1");
    prop1.setValue("value1");
    resource.addProperty(prop1);

    ScheduledServicePropertyResource prop2 = new ScheduledServicePropertyResource();
    prop2.setKey("id2");
    prop2.setValue("value2");
    resource.addProperty(prop2);

    ScheduledServiceResourceResponse resourceResponse = new ScheduledServiceResourceResponse();
    resourceResponse.setData(resource);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testScheduledServiceDailyResource() {
    ScheduledServiceDailyResource resource = new ScheduledServiceDailyResource();
    resource.setStartDate("StartDate");
    resource.setId("Id");
    resource.setSchedule("daily");
    resource.setTypeId("TypeId");
    resource.setAlertEmail("foo@bar.org");
    resource.setName("Name");
    resource.setEnabled(true);
    resource.setRecurringTime("recurringTime");

    ScheduledServicePropertyResource prop1 = new ScheduledServicePropertyResource();
    prop1.setKey("id1");
    prop1.setValue("value1");
    resource.addProperty(prop1);

    ScheduledServicePropertyResource prop2 = new ScheduledServicePropertyResource();
    prop2.setKey("id2");
    prop2.setValue("value2");
    resource.addProperty(prop2);

    ScheduledServiceResourceResponse resourceResponse = new ScheduledServiceResourceResponse();
    resourceResponse.setData(resource);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testScheduledServiceAdvancedResource() {
    ScheduledServiceAdvancedResource resource = new ScheduledServiceAdvancedResource();
    resource.setId("Id");
    resource.setSchedule("advanced");
    resource.setTypeId("TypeId");
    resource.setAlertEmail("foo@bar.org");
    resource.setName("Name");
    resource.setEnabled(true);
    resource.setCronCommand("cronCommand");

    ScheduledServicePropertyResource prop1 = new ScheduledServicePropertyResource();
    prop1.setKey("id1");
    prop1.setValue("value1");
    resource.addProperty(prop1);

    ScheduledServicePropertyResource prop2 = new ScheduledServicePropertyResource();
    prop2.setKey("id2");
    prop2.setValue("value2");
    resource.addProperty(prop2);

    ScheduledServiceResourceResponse resourceResponse = new ScheduledServiceResourceResponse();
    resourceResponse.setData(resource);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testScheduledServiceMonthlyResource() {
    ScheduledServiceMonthlyResource resource = new ScheduledServiceMonthlyResource();
    resource.setId("Id");
    resource.setSchedule("monthly");
    resource.setTypeId("TypeId");
    resource.setAlertEmail("foo@bar.org");
    resource.setName("Name");
    resource.setEnabled(true);
    resource.setRecurringTime("recurringTime");
    resource.addRecurringDay("recurringDay1");
    resource.addRecurringDay("recurringDay2");

    ScheduledServicePropertyResource prop1 = new ScheduledServicePropertyResource();
    prop1.setKey("id1");
    prop1.setValue("value1");
    resource.addProperty(prop1);

    ScheduledServicePropertyResource prop2 = new ScheduledServicePropertyResource();
    prop2.setKey("id2");
    prop2.setValue("value2");
    resource.addProperty(prop2);

    ScheduledServiceResourceResponse resourceResponse = new ScheduledServiceResourceResponse();
    resourceResponse.setData(resource);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testScheduledServiceWeeklyResource() {
    ScheduledServiceWeeklyResource resource = new ScheduledServiceWeeklyResource();
    resource.setId("Id");
    resource.setSchedule("weekly");
    resource.setTypeId("TypeId");
    resource.setAlertEmail("foo@bar.org");
    resource.setName("Name");
    resource.setEnabled(true);
    resource.setRecurringTime("recurringTime");
    resource.addRecurringDay("recurringDay1");
    resource.addRecurringDay("recurringDay2");

    ScheduledServicePropertyResource prop1 = new ScheduledServicePropertyResource();
    prop1.setKey("id1");
    prop1.setValue("value1");
    resource.addProperty(prop1);

    ScheduledServicePropertyResource prop2 = new ScheduledServicePropertyResource();
    prop2.setKey("id2");
    prop2.setValue("value2");
    resource.addProperty(prop2);

    ScheduledServiceResourceResponse resourceResponse = new ScheduledServiceResourceResponse();
    resourceResponse.setData(resource);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testScheduledServiceTypeResourceResponse() {
    ScheduledServiceTypeResourceResponse resourceResponse = new ScheduledServiceTypeResourceResponse();

    ScheduledServiceTypeResource item1 = new ScheduledServiceTypeResource();
    item1.setId("id1");
    item1.setName("name1");

    FormFieldResource prop1 = new FormFieldResource();
    prop1.setHelpText("helpText1");
    prop1.setId("id1");
    prop1.setLabel("name1");
    prop1.setRequired(true);
    prop1.setType("type1");
    prop1.setRegexValidation("regex");
    item1.addFormField(prop1);

    FormFieldResource prop2 = new FormFieldResource();
    prop2.setHelpText("helpText2");
    prop2.setId("id2");
    prop2.setLabel("name2");
    prop2.setRequired(true);
    prop2.setType("type2");
    prop2.setRegexValidation("regex2");
    item1.addFormField(prop2);

    ScheduledServiceTypeResource item2 = new ScheduledServiceTypeResource();
    item2.setId("id1");
    item2.setName("name1");

    FormFieldResource prop3 = new FormFieldResource();
    prop3.setHelpText("helpText3");
    prop3.setId("id3");
    prop3.setLabel("name3");
    prop3.setRequired(true);
    prop3.setType("type3");
    prop3.setRegexValidation("regex3");
    item2.addFormField(prop3);

    FormFieldResource prop4 = new FormFieldResource();
    prop4.setHelpText("helpText4");
    prop4.setId("id4");
    prop4.setLabel("name4");
    prop4.setRequired(true);
    prop4.setType("type4");
    prop4.setRegexValidation("regex4");
    item2.addFormField(prop4);

    resourceResponse.addData(item1);
    resourceResponse.addData(item2);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testUserListResourceResponse() {
    UserListResourceResponse resourceResponse = new UserListResourceResponse();

    UserResource user1 = new UserResource();
    user1.setResourceURI("ResourceURI1");
    user1.setEmail("Email1");
    user1.setUserId("UserId1");
    user1.setFirstName("Name1");
    user1.setStatus("Status1");
    user1.addRole("role1");
    user1.addRole("role2");
    resourceResponse.addData(user1);

    UserResource user2 = new UserResource();
    user2.setResourceURI("ResourceURI2");
    user2.setEmail("Email2");
    user2.setUserId("UserId2");
    user2.setFirstName("Name2");
    user2.setStatus("Status2");
    user2.addRole("role3");
    user2.addRole("role4");
    resourceResponse.addData(user2);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);

  }

  @Test
  public void testUserResourceRequest() {
    UserResourceRequest resourceRequest = new UserResourceRequest();

    UserResource user1 = new UserResource();
    user1.setResourceURI("ResourceURI1");
    user1.setEmail("Email1");
    user1.setUserId("UserId1");
    user1.setFirstName("Name1");
    user1.setStatus("Status1");
    user1.addRole("role1");
    user1.addRole("role2");
    resourceRequest.setData(user1);

    this.marshalUnmarchalThenCompare(resourceRequest);
    this.validateXmlHasNoPackageNames(resourceRequest);
  }

  @Test
  public void testUserResourceResponse() {
    UserResourceResponse resourceResponse = new UserResourceResponse();

    UserResource user1 = new UserResource();
    user1.setResourceURI("ResourceURI1");
    user1.setEmail("Email1");
    user1.setUserId("UserId1");
    user1.setFirstName("Name1");
    user1.setStatus("Status1");
    user1.addRole("role1");
    user1.addRole("role2");
    resourceResponse.setData(user1);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);
  }
  @Test
  public void testUserChangePasswordRequest() {
    UserChangePasswordRequest request = new UserChangePasswordRequest();

    UserChangePasswordResource resource = new UserChangePasswordResource();
    resource.setNewPassword("newPassword");
    resource.setOldPassword("oldPassword");
    resource.setUserId("userId");

    request.setData(resource);

    this.marshalUnmarchalThenCompare(request);
    this.validateXmlHasNoPackageNames(request);
  }

  @Test
  public void testRoleListResourceResponse() {
    RoleListResourceResponse resourceResponse = new RoleListResourceResponse();

    RoleResource item1 = new RoleResource();
    item1.setId("Id1");
    item1.setResourceURI("ResourceURI1");
    item1.addPrivilege("privilege1");
    item1.addPrivilege("privilege2");
    item1.addRole("role1");
    item1.addRole("role2");
    item1.setSessionTimeout(42);
    item1.setName("Name1");
    item1.setDescription("Description1");
    resourceResponse.addData(item1);

    RoleResource item2 = new RoleResource();
    item2.setId("Id2");
    item2.setResourceURI("ResourceURI2");
    item2.addPrivilege("privilege3");
    item2.addPrivilege("privilege4");
    item2.addRole("role4");
    item2.addRole("role3");
    item2.setSessionTimeout(42);
    item2.setName("Name2");
    item2.setDescription("Description2");
    resourceResponse.addData(item2);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testRoleResourceRequest() {
    RoleResourceRequest resourceRequest = new RoleResourceRequest();

    RoleResource item1 = new RoleResource();
    item1.setId("Id1");
    item1.setResourceURI("ResourceURI1");
    item1.addPrivilege("privilege1");
    item1.addPrivilege("privilege2");
    item1.addRole("role1");
    item1.addRole("role2");
    item1.setSessionTimeout(42);
    item1.setName("Name1");
    item1.setDescription("Description1");
    resourceRequest.setData(item1);

    this.marshalUnmarchalThenCompare(resourceRequest);
    this.validateXmlHasNoPackageNames(resourceRequest);
  }

  @Test
  public void testRoleResourceResponse() {
    RoleResourceResponse resourceResponse = new RoleResourceResponse();

    RoleResource item1 = new RoleResource();
    item1.setId("Id1");
    item1.setResourceURI("ResourceURI1");
    item1.addPrivilege("privilege1");
    item1.addPrivilege("privilege2");
    item1.addRole("role1");
    item1.addRole("role2");
    item1.setSessionTimeout(42);
    item1.setName("Name1");
    item1.setDescription("Description1");
    resourceResponse.setData(item1);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testPrivilegeTargetResource() {
    PrivilegeResourceRequest resourceRequest = new PrivilegeResourceRequest();

    PrivilegeResource resource = new PrivilegeResource();
    resource.setRepositoryGroupId("RepositoryGroupId");
    resource.setRepositoryId("RepositoryId");
    resource.setRepositoryTargetId("RepositoryTargetId");
    resource.setName("Name");
    resource.addMethod("Method1");
    resource.addMethod("Method2");
    resource.setDescription("Description");
    resource.setType("target");

    resourceRequest.setData(resource);

    this.marshalUnmarchalThenCompare(resourceRequest);
    this.validateXmlHasNoPackageNames(resourceRequest);
  }

  @Test
  public void testPrivilegeListResourceResponse() {
    PrivilegeListResourceResponse resourceResponse = new PrivilegeListResourceResponse();

    PrivilegeStatusResource appResource1 = new PrivilegeStatusResource();
    appResource1.setId("Id1");
    appResource1.setResourceURI("ResourceURI1");
    appResource1.setName("Name1");
    appResource1.setDescription("Description1");
    appResource1.setType(ApplicationPrivilegeDescriptor.TYPE);

    PrivilegeProperty prop = new PrivilegeProperty();
    prop.setKey(ApplicationPrivilegeMethodPropertyDescriptor.ID);
    prop.setValue("Method1");
    appResource1.addProperty(prop);

    prop = new PrivilegeProperty();
    prop.setKey(ApplicationPrivilegePermissionPropertyDescriptor.ID);
    prop.setValue("Permission1");
    appResource1.addProperty(prop);

    PrivilegeStatusResource appResource2 = new PrivilegeStatusResource();
    appResource2.setId("Id2");
    appResource2.setResourceURI("ResourceURI2");
    appResource2.setName("Name2");
    appResource2.setDescription("Description2");
    appResource2.setType(ApplicationPrivilegeDescriptor.TYPE);

    prop = new PrivilegeProperty();
    prop.setKey(ApplicationPrivilegeMethodPropertyDescriptor.ID);
    prop.setValue("Method2");
    appResource2.addProperty(prop);

    prop = new PrivilegeProperty();
    prop.setKey(ApplicationPrivilegePermissionPropertyDescriptor.ID);
    prop.setValue("Permission2");
    appResource2.addProperty(prop);

    PrivilegeStatusResource targetResource1 = new PrivilegeStatusResource();
    targetResource1.setId("Id1");
    targetResource1.setResourceURI("ResourceURI1");
    targetResource1.setName("Name1");
    targetResource1.setDescription("Description1");
    targetResource1.setType(TargetPrivilegeDescriptor.TYPE);

    prop = new PrivilegeProperty();
    prop.setKey(TargetPrivilegeGroupPropertyDescriptor.ID);
    prop.setValue("RepositoryGroupId1");
    targetResource1.addProperty(prop);

    prop = new PrivilegeProperty();
    prop.setKey(TargetPrivilegeRepositoryPropertyDescriptor.ID);
    prop.setValue("RepositoryId1");
    targetResource1.addProperty(prop);

    prop = new PrivilegeProperty();
    prop.setKey(TargetPrivilegeRepositoryTargetPropertyDescriptor.ID);
    prop.setValue("RepositoryTargetId1");
    targetResource1.addProperty(prop);

    prop = new PrivilegeProperty();
    prop.setKey(ApplicationPrivilegeMethodPropertyDescriptor.ID);
    prop.setValue("Method1");
    targetResource1.addProperty(prop);

    PrivilegeStatusResource targetResource2 = new PrivilegeStatusResource();
    targetResource2.setId("Id2");
    targetResource2.setResourceURI("ResourceURI2");
    targetResource2.setName("Name2");
    targetResource2.setDescription("Description2");
    targetResource2.setType(TargetPrivilegeDescriptor.TYPE);

    prop = new PrivilegeProperty();
    prop.setKey(TargetPrivilegeGroupPropertyDescriptor.ID);
    prop.setValue("RepositoryGroupId2");
    targetResource2.addProperty(prop);

    prop = new PrivilegeProperty();
    prop.setKey(TargetPrivilegeRepositoryPropertyDescriptor.ID);
    prop.setValue("RepositoryId2");
    targetResource2.addProperty(prop);

    prop = new PrivilegeProperty();
    prop.setKey(TargetPrivilegeRepositoryTargetPropertyDescriptor.ID);
    prop.setValue("RepositoryTargetId2");
    targetResource2.addProperty(prop);

    prop = new PrivilegeProperty();
    prop.setKey(ApplicationPrivilegeMethodPropertyDescriptor.ID);
    prop.setValue("Method2");
    targetResource2.addProperty(prop);

    resourceResponse.addData(appResource1);
    resourceResponse.addData(targetResource1);
    resourceResponse.addData(appResource2);
    resourceResponse.addData(targetResource2);

    this.marshalUnmarchalThenCompare(resourceResponse, this.xstreamXML); // FIXME: list of multiple objects would
    // need a converter
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testPrivilegeStatusResource() {
    PrivilegeStatusResource appResource1 = new PrivilegeStatusResource();
    appResource1.setId("Id1");
    appResource1.setResourceURI("ResourceURI1");
    appResource1.setName("Name1");
    appResource1.setDescription("Description1");
    appResource1.setType(ApplicationPrivilegeDescriptor.TYPE);

    PrivilegeProperty prop = new PrivilegeProperty();
    prop.setKey(ApplicationPrivilegePermissionPropertyDescriptor.ID);
    prop.setValue("Permission1");
    appResource1.addProperty(prop);

    prop = new PrivilegeProperty();
    prop.setKey(ApplicationPrivilegeMethodPropertyDescriptor.ID);
    prop.setValue("Method1");
    appResource1.addProperty(prop);

    PrivilegeStatusResourceResponse resourceResponse = new PrivilegeStatusResourceResponse();
    resourceResponse.setData(appResource1);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);

    PrivilegeStatusResource targetResource1 = new PrivilegeStatusResource();
    targetResource1.setId("Id1");
    targetResource1.setResourceURI("ResourceURI1");
    targetResource1.setName("Name1");
    targetResource1.setDescription("Description1");
    targetResource1.setType(TargetPrivilegeDescriptor.TYPE);

    prop = new PrivilegeProperty();
    prop.setKey(TargetPrivilegeGroupPropertyDescriptor.ID);
    prop.setValue("RepositoryGroupId1");
    targetResource1.addProperty(prop);

    prop = new PrivilegeProperty();
    prop.setKey(TargetPrivilegeRepositoryPropertyDescriptor.ID);
    prop.setValue("RepositoryId1");
    targetResource1.addProperty(prop);

    prop = new PrivilegeProperty();
    prop.setKey(TargetPrivilegeRepositoryTargetPropertyDescriptor.ID);
    prop.setValue("RepositoryTargetId1");
    targetResource1.addProperty(prop);

    prop = new PrivilegeProperty();
    prop.setKey(ApplicationPrivilegeMethodPropertyDescriptor.ID);
    prop.setValue("Method1");
    targetResource1.addProperty(prop);

    resourceResponse = new PrivilegeStatusResourceResponse();
    resourceResponse.setData(targetResource1);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testPrivilegeTypeResourceResponse() {
    PrivilegeTypeResourceResponse response = new PrivilegeTypeResourceResponse();

    PrivilegeTypeResource type = new PrivilegeTypeResource();
    type.setId("id1");
    type.setName("name1");

    PrivilegeTypePropertyResource property = new PrivilegeTypePropertyResource();
    property.setId("id1");
    property.setName("name1");
    property.setHelpText("help1");

    type.addProperty(property);

    property = new PrivilegeTypePropertyResource();
    property.setId("id2");
    property.setName("name2");
    property.setHelpText("help2");

    type.addProperty(property);

    response.addData(type);

    type = new PrivilegeTypeResource();
    type.setId("id2");
    type.setName("name2");

    property = new PrivilegeTypePropertyResource();
    property.setId("id3");
    property.setName("name3");
    property.setHelpText("help3");

    type.addProperty(property);

    property = new PrivilegeTypePropertyResource();
    property.setId("id4");
    property.setName("name4");
    property.setHelpText("help4");

    type.addProperty(property);

    response.addData(type);

    this.marshalUnmarchalThenCompare(response);
    this.validateXmlHasNoPackageNames(response);
  }

  @Test
  public void testNFCResourceResponse() {
    NFCResourceResponse resourceResponse = new NFCResourceResponse();

    NFCRepositoryResource nfcRepoResource1 = new NFCRepositoryResource();
    nfcRepoResource1.setRepositoryId("repoId1");
    nfcRepoResource1.addNfcPath("path1");
    nfcRepoResource1.addNfcPath("path2");

    NFCStats stats = new NFCStats();
    stats.setHits(1000);
    stats.setMisses(5000);
    stats.setSize(44);
    nfcRepoResource1.setNfcStats(stats);

    NFCRepositoryResource nfcRepoResource2 = new NFCRepositoryResource();
    nfcRepoResource2.setRepositoryId("repoId2");
    nfcRepoResource2.addNfcPath("path3");
    nfcRepoResource2.addNfcPath("path4");

    NFCStats stats2 = new NFCStats();
    stats2.setHits(1000);
    stats2.setMisses(5000);
    stats2.setSize(44);
    nfcRepoResource2.setNfcStats(stats2);

    NFCResource resource = new NFCResource();
    resource.addNfcContent(nfcRepoResource1);
    resource.addNfcContent(nfcRepoResource2);

    resourceResponse.setData(resource);

    // Excluded because our damn json reader doesn't support parsing long values...very very bad
    // this.marshalUnmarchalThenCompare( resourceResponse );
    // TODO: UNCOMMENT this when the json driver is fixed.
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testRepositoryTargetListResourceResponse() {
    RepositoryTargetListResourceResponse resourceResponse = new RepositoryTargetListResourceResponse();

    RepositoryTargetListResource item1 = new RepositoryTargetListResource();
    item1.setContentClass("contentClass1");
    item1.setId("id1");
    item1.setName("name1");
    item1.setResourceURI("resourceURI1");

    RepositoryTargetListResource item2 = new RepositoryTargetListResource();
    item2.setId("Id2");
    item2.setResourceURI("ResourceURI2");
    item2.setContentClass("ContentClass2");
    item2.setName("Name2");

    resourceResponse.addData(item1);
    resourceResponse.addData(item2);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  @Test
  public void testRepositoryTargetResourceResponse() {
    RepositoryTargetResourceResponse resourceResponse = new RepositoryTargetResourceResponse();

    RepositoryTargetResource resource = new RepositoryTargetResource();
    resource.setId("Id");
    resource.setResourceURI("ResourceURI");
    resource.setContentClass("ContentClass");
    resource.setName("Name");
    resource.addPattern("pattern1");
    resource.addPattern("pattern2");
    resourceResponse.setData(resource);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);

  }

  @Test
  public void testRepositoryContentClassListResourceResponse() {
    RepositoryContentClassListResourceResponse resourceResponse = new RepositoryContentClassListResourceResponse();

    RepositoryContentClassListResource item1 = new RepositoryContentClassListResource();
    item1.setContentClass("ContentClass1");
    item1.setName("Name1");

    RepositoryContentClassListResource item2 = new RepositoryContentClassListResource();
    item2.setContentClass("ContentClass2");
    item2.setName("Name2");

    resourceResponse.addData(item1);
    resourceResponse.addData(item2);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);
  }

  public void onHold() {
    ScheduledServiceWeeklyResource scheduledTask = new ScheduledServiceWeeklyResource();
    scheduledTask.setSchedule("weekly");
    scheduledTask.setEnabled(true);
    scheduledTask.setId(null);
    scheduledTask.setName("taskOnce");
    // A future date
    Date startDate = DateUtils.addDays(new Date(), 10);
    startDate = DateUtils.round(startDate, Calendar.DAY_OF_MONTH);
    scheduledTask.setStartDate(String.valueOf(startDate.getTime()));
    scheduledTask.setRecurringTime("03:30");

    // scheduledTask.setRecurringDay( Arrays.asList( new String[] { "monday", "wednesday", "friday" } ) );
    scheduledTask.addRecurringDay("monday");
    scheduledTask.addRecurringDay("wednesday");
    scheduledTask.addRecurringDay("friday");

    scheduledTask.setTypeId("org.sonatype.nexus.tasks.RepairIndexTask");
    scheduledTask.setAlertEmail("foo@bar.org");

    ScheduledServicePropertyResource prop = new ScheduledServicePropertyResource();
    prop.setKey("repositoryId");
    prop.setValue("all_repo");
    scheduledTask.addProperty(prop);

    ScheduledServiceResourceResponse resourceResponse = new ScheduledServiceResourceResponse();
    resourceResponse.setData(scheduledTask);

    // System.out.println( "xml:\n"+ this.xstreamXML.toXML( resourceResponse ) );

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);

  }

  @Test
  public void testUserToRoleResourceRequest() {
    UserToRoleResourceRequest resourceResponse = new UserToRoleResourceRequest();
    UserToRoleResource resource = new UserToRoleResource();
    resourceResponse.setData(resource);

    resource.setUserId("userId");
    resource.setSource("source");
    resource.addRole("role1");
    resource.addRole("role2");

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);

  }

  @Test
  public void testPlexusUserResourceResponse() {
    PlexusUserResourceResponse resourceResponse = new PlexusUserResourceResponse();
    PlexusUserResource resource = new PlexusUserResource();
    resourceResponse.setData(resource);

    resource.setUserId("userId");
    resource.setSource("source");
    resource.setEmail("email");
    PlexusRoleResource role1 = new PlexusRoleResource();
    role1.setName("role1");
    role1.setSource("source1");
    role1.setRoleId("roleId1");
    resource.addRole(role1);

    PlexusRoleResource role2 = new PlexusRoleResource();
    role2.setName("role2");
    role2.setSource("source2");
    role2.setRoleId("roleId2");
    resource.addRole(role2);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);

  }

  @Test
  public void testPlexusUserListResourceResponse() {
    PlexusUserListResourceResponse resourceResponse = new PlexusUserListResourceResponse();
    PlexusUserResource resource1 = new PlexusUserResource();
    resourceResponse.addData(resource1);

    resource1.setUserId("userId");
    resource1.setSource("source");
    resource1.setEmail("email");
    PlexusRoleResource role1 = new PlexusRoleResource();
    role1.setName("role1");
    role1.setSource("source1");
    role1.setRoleId("roleId1");
    resource1.addRole(role1);

    PlexusRoleResource role2 = new PlexusRoleResource();
    role1.setName("role2");
    role1.setSource("source2");
    role1.setRoleId("roleId2");
    resource1.addRole(role2);

    PlexusUserResource resource2 = new PlexusUserResource();
    resourceResponse.addData(resource2);

    resource2.setUserId("userId");
    resource2.setSource("source");
    resource2.setEmail("email");
    PlexusRoleResource role3 = new PlexusRoleResource();
    role3.setName("role1");
    role3.setSource("source1");
    role3.setRoleId("roleId1");
    resource2.addRole(role3);

    PlexusRoleResource role4 = new PlexusRoleResource();
    role4.setName("role2");
    role4.setSource("source2");
    role4.setRoleId("roleId2");
    resource2.addRole(role4);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);

  }

  @Test
  public void testMirrorResourceListResponse() {
    MirrorResourceListResponse response = new MirrorResourceListResponse();

    MirrorResource data = new MirrorResource();
    data.setId("id");
    data.setUrl("url");

    response.addData(data);

    MirrorResource data2 = new MirrorResource();
    data2.setId("id2");
    data2.setUrl("url2");

    response.addData(data2);

    this.marshalUnmarchalThenCompare(response);
    this.validateXmlHasNoPackageNames(response);
  }

  @Test
  public void testMirrorResourceListRequest() {
    MirrorResourceListRequest response = new MirrorResourceListRequest();

    MirrorResource data = new MirrorResource();
    data.setId("id");
    data.setUrl("url");

    response.addData(data);

    MirrorResource data2 = new MirrorResource();
    data2.setId("id2");
    data2.setUrl("url2");

    response.addData(data2);

    this.marshalUnmarchalThenCompare(response);
    this.validateXmlHasNoPackageNames(response);
  }

  @Test
  public void testMirrorStatusResourceListResponse() {
    MirrorStatusResourceListResponse response = new MirrorStatusResourceListResponse();

    MirrorStatusResource data = new MirrorStatusResource();
    data.setId("id");
    data.setUrl("url");
    data.setStatus("status");

    response.addData(data);

    MirrorStatusResource data2 = new MirrorStatusResource();
    data2.setId("id2");
    data2.setUrl("url2");
    data2.setStatus("status2");

    response.addData(data2);

    this.marshalUnmarchalThenCompare(response);
    this.validateXmlHasNoPackageNames(response);
  }

  @Test
  public void testSmtpSettingsResourceRequest() {
    SmtpSettingsResourceRequest request = new SmtpSettingsResourceRequest();

    SmtpSettingsResource resource = new SmtpSettingsResource();
    resource.setHost("host");
    resource.setPassword("password");
    resource.setPort(55);
    resource.setSslEnabled(true);
    resource.setSystemEmailAddress("systememail");
    resource.setTestEmail("testemail");
    resource.setTlsEnabled(true);
    resource.setUsername("username");

    request.setData(resource);

    this.marshalUnmarchalThenCompare(request);
    this.validateXmlHasNoPackageNames(request);
  }

  @Test
  public void testArtifactResolveResourceResponse() {
    ArtifactResolveResourceResponse response = new ArtifactResolveResourceResponse();

    ArtifactResolveResource data = new ArtifactResolveResource();
    data.setArtifactId("artifactId");
    data.setBaseVersion("baseversion");
    data.setClassifier("classifier");
    data.setExtension("extension");
    data.setFileName("filename");
    data.setGroupId("groupid");
    data.setRepositoryPath("repopath");
    data.setSha1("sha1");
    data.setSnapshot(true);
    data.setSnapshotBuildNumber(100);
    data.setSnapshotTimeStamp(12345);
    data.setVersion("version");

    response.setData(data);

    // Exclude because json reader doesn't properly handle long values...
    // this.marshalUnmarchalThenCompare( response );
    // TODO: UNCOMMENT ME WHEN JSON DRIVER IS FIXED
    this.validateXmlHasNoPackageNames(response);
  }

  @Test
  public void testNexusRepositoryTypeListResourceResponse() {
    NexusRepositoryTypeListResourceResponse response = new NexusRepositoryTypeListResourceResponse();

    NexusRepositoryTypeListResource resource = new NexusRepositoryTypeListResource();
    resource.setDescription("description");
    resource.setFormat("format");
    resource.setProvider("provider");
    resource.setProviderRole("providerRole");

    response.addData(resource);

    NexusRepositoryTypeListResource resource2 = new NexusRepositoryTypeListResource();
    resource2.setDescription("description2");
    resource2.setFormat("format2");
    resource2.setProvider("provider2");
    resource2.setProviderRole("providerRole2");

    response.addData(resource2);

    this.marshalUnmarchalThenCompare(response);
    this.validateXmlHasNoPackageNames(response);
  }

  @Test
  public void testPlexusComponentListResourceResponse() {
    PlexusComponentListResourceResponse resourceResponse = new PlexusComponentListResourceResponse();

    PlexusComponentListResource resource1 = new PlexusComponentListResource();
    resource1.setDescription("description1");
    resource1.setRoleHint("role-hint1");
    resourceResponse.addData(resource1);

    PlexusComponentListResource resource2 = new PlexusComponentListResource();
    resource2.setDescription("description2");
    resource2.setRoleHint("role-hint2");
    resourceResponse.addData(resource2);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);

  }

  @Test
  public void testExternalRoleMappingResourceResponse() {
    ExternalRoleMappingListResourceResponse resourceResponse = new ExternalRoleMappingListResourceResponse();

    ExternalRoleMappingResource resource1 = new ExternalRoleMappingResource();
    resourceResponse.addData(resource1);

    PlexusRoleResource role1 = new PlexusRoleResource();
    role1.setName("role1");
    role1.setSource("source1");
    role1.setRoleId("roleId1");
    resource1.setDefaultRole(role1);

    PlexusRoleResource role2 = new PlexusRoleResource();
    role2.setName("role2");
    role2.setSource("source2");
    role2.setRoleId("roleId2");
    resource1.addMappedRole(role2);

    PlexusRoleResource role3 = new PlexusRoleResource();
    role3.setName("role3");
    role3.setSource("source3");
    role3.setRoleId("roleId3");
    resource1.addMappedRole(role3);

    ExternalRoleMappingResource resource2 = new ExternalRoleMappingResource();
    resourceResponse.addData(resource2);

    PlexusRoleResource role4 = new PlexusRoleResource();
    role4.setName("role4");
    role4.setSource("source4");
    role4.setRoleId("roleId4");
    resource2.setDefaultRole(role4);

    PlexusRoleResource role5 = new PlexusRoleResource();
    role5.setName("role5");
    role5.setSource("source5");
    role5.setRoleId("roleId5");
    resource2.addMappedRole(role5);

    PlexusRoleResource role6 = new PlexusRoleResource();
    role6.setName("role6");
    role6.setSource("source6");
    role6.setRoleId("roleId6");
    resource2.addMappedRole(role6);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);

  }

  @Test
  public void testPlexusRoleListPlexusResource() {
    PlexusRoleListResourceResponse resourceResponse = new PlexusRoleListResourceResponse();

    PlexusRoleResource role1 = new PlexusRoleResource();
    role1.setName("role1");
    role1.setSource("source1");
    role1.setRoleId("roleId1");
    resourceResponse.addData(role1);

    PlexusRoleResource role2 = new PlexusRoleResource();
    role2.setName("role2");
    role2.setSource("source2");
    role2.setRoleId("roleId2");
    resourceResponse.addData(role2);

    this.marshalUnmarchalThenCompare(resourceResponse);
    this.validateXmlHasNoPackageNames(resourceResponse);

  }

  protected void marshalUnmarchalThenCompare(Object obj) {
    // do xml
    String xml = this.xstreamXML.toXML(obj);

    System.out.println("xml: \n" + xml);
    this.compareObjects(obj, xstreamXML.fromXML(xml));

    // do json
    String json =
        new StringBuilder("{ \"").append(obj.getClass().getName()).append("\" : ").append(this.xstreamJSON.toXML(obj))
            .append(" }").toString();
    System.out.println("json:\n " + json);
    try {
      this.compareObjects(obj, xstreamJSON.fromXML(json, obj.getClass().newInstance()));
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage() + "\nJSON:\n" + json);
    }
  }

  protected void marshalUnmarchalThenCompare(Object obj, XStream xstream) {
    String xml = xstream.toXML(obj);
    this.compareObjects(obj, xstream.fromXML(xml));
  }

  protected void validateXmlHasNoPackageNames(Object obj) {
    String xml = this.xstreamXML.toXML(obj);

    // quick way of looking for the class="org attribute
    // i don't want to parse a dom to figure this out

    int totalCount = StringUtils.countMatches(xml, "org.sonatype");
    int attributeCount = StringUtils.countMatches(xml, "\"org.sonatype");

    // check the counts
    Assert.assertFalse("Found package name in XML:\n" + xml, totalCount > 0);

    // // print out each type of method, so i can rafb it
    // System.out.println( "\n\nClass: "+ obj.getClass() +"\n" );
    // System.out.println( xml+"\n" );
    //
    // Assert.assertFalse( "Found <string> XML: " + obj.getClass() + "\n" + xml, xml.contains( "<string>" ) );

    // also check for modelEncoding
    Assert.assertFalse(xml.contains("modelEncoding"));
  }

  private String toDebugString(Object obj) {
    return ToStringBuilder.reflectionToString(obj, ToStringStyle.MULTI_LINE_STYLE);
  }

  private void compareObjects(Object expected, Object actual) {
    // ignore the modelEncoding field
    String[] ignoredFields = {"modelEncoding"};

    if (!DeepEqualsBuilder.reflectionDeepEquals(expected, actual, false, null, ignoredFields)) {
      // Print out the objects so we can compare them if it fails.
      Assert.fail("Expected objects to be equal: \nExpected:\n" + this.toDebugString(expected)
          + "\n\nActual:\n" + this.toDebugString(actual) + "\n\nExpected XML: "
          + this.xstreamXML.toXML(expected) + "\n\nActual:\n" + this.xstreamXML.toXML(actual));
    }

  }

  public static void main(String[] args) {
    Class clazz = RepositoryContentClassListResource.class;

    Method[] methods = clazz.getMethods();

    String suffix = "2";
    String varName = "item" + suffix;

    System.out.println(clazz.getSimpleName() + " " + varName + " = new " + clazz.getSimpleName() + "();");

    for (int ii = 0; ii < methods.length; ii++) {
      Method method = methods[ii];

      if (method.getName().startsWith("set") && !method.getName().equals("setModelEncoding")) {
        String name = method.getName().substring(3);

        System.out.println(varName + "." + method.getName() + "( \"" + name + suffix + "\" );");
      }

    }

  }

}
