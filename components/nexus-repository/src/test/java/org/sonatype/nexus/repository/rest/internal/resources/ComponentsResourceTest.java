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
package org.sonatype.nexus.repository.rest.internal.resources;

import java.util.Arrays;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import org.sonatype.nexus.common.entity.ContinuationTokenHelper;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.repository.browse.BrowseResult;
import org.sonatype.nexus.repository.browse.QueryOptions;
import org.sonatype.nexus.repository.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.rest.ComponentUploadExtension;
import org.sonatype.nexus.repository.rest.ComponentsResourceExtension;
import org.sonatype.nexus.repository.rest.api.ComponentXO;
import org.sonatype.nexus.repository.rest.api.ComponentXOFactory;
import org.sonatype.nexus.repository.rest.internal.api.RepositoryItemIDXO;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.repository.storage.internal.ComponentContinuationTokenHelper;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadConfiguration;
import org.sonatype.nexus.repository.upload.UploadManager;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.rest.Page;

import com.google.common.collect.ImmutableSet;
import com.orientechnologies.orient.core.id.ORID;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_ACCEPTABLE;
import static org.sonatype.nexus.repository.http.HttpStatus.UNPROCESSABLE_ENTITY;

public class ComponentsResourceTest
    extends RepositoryResourceTestSupport
{
  private static final String COMPONENT_ID = "newId";

  private ComponentsResource underTest;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Mock
  private ComponentEntityAdapter componentEntityAdapter;

  @Mock
  private Component componentOne;

  @Mock
  private ORID componentOneORID;

  @Mock
  private EntityMetadata componentOneEntityMetadata;

  @Mock
  private EntityId componentOneEntityId;

  @Mock
  private BrowseResult<Asset> componentOneBrowseResults;

  @Mock
  private Component componentTwo;

  @Mock
  private ORID componentTwoORID;

  @Mock
  private EntityMetadata componentTwoEntityMetadata;

  @Mock
  private EntityId componentTwoEntityId;

  @Mock
  private BrowseResult<Asset> componentTwoBrowseResults;

  @Captor
  private ArgumentCaptor<QueryOptions> queryOptionsCaptor;

  @Mock
  private BrowseResult<Component> componentBrowseResult;

  @Captor
  private ArgumentCaptor<DetachedEntityId> detachedEntityIdCaptor;

  @Mock
  private MaintenanceService maintenanceService;

  @Mock
  private UploadManager uploadManager;

  @Mock
  private UploadConfiguration uploadConfiguration;

  @Captor
  private ArgumentCaptor<ComponentUpload> componentUploadCaptor;

  @Mock
  private ComponentUploadExtension componentUploadExtension;

  @Spy
  private ComponentsResourceExtension componentsResourceExtension = new TestComponentsResourceExtension();

  private Asset assetOne;

  private Asset assetTwo;

  private void configureComponent(final Component component, final String group, final String name, final String version) {
    when(component.group()).thenReturn(group);
    when(component.name()).thenReturn(name);
    when(component.version()).thenReturn(version);
  }

  @Test
  public void checkPath() {
    assertThat(ComponentsResource.RESOURCE_URI, is("/v1/components"));
  }

  @Before
  public void setUp() throws Exception {
    assetOne = getMockedAsset("assetOne", "assetOneId");
    assetTwo = getMockedAsset("assetTwo", "assetTwoId");

    configureComponent(componentOne, "component-one-group", "component-one-name", "1.0.0");

    when(componentOne.getEntityMetadata()).thenReturn(componentOneEntityMetadata);
    when(componentOneEntityMetadata.getId()).thenReturn(componentOneEntityId);
    when(componentOneEntityId.toString()).thenReturn("componentOneORID");
    when(componentOneEntityId.getValue()).thenReturn("componentOne");

    when(browseService.browseComponentAssets(mavenReleases, "componentOne")).thenReturn(componentOneBrowseResults);
    when(componentOneBrowseResults.getResults()).thenReturn(Collections.singletonList(assetOne));

    configureComponent(componentTwo, null, "component-two-name", null);

    when(componentTwo.getEntityMetadata()).thenReturn(componentTwoEntityMetadata);
    when(componentTwoEntityMetadata.getId()).thenReturn(componentTwoEntityId);
    when(componentTwoEntityId.toString()).thenReturn("componentTwoORID");
    when(componentTwoEntityId.getValue()).thenReturn("88491cd1d185dd1318fdba4364e78406");

    when(browseService.browseComponentAssets(mavenReleases, "88491cd1d185dd1318fdba4364e78406"))
        .thenReturn(componentOneBrowseResults);
    when(componentTwoBrowseResults.getResults()).thenReturn(Collections.singletonList(assetTwo));

    ContinuationTokenHelper continuationTokenHelper = new ComponentContinuationTokenHelper(componentEntityAdapter);

    when(uploadConfiguration.isEnabled()).thenReturn(true);

    underTest = new ComponentsResource(repositoryManagerRESTAdapter, browseService, componentEntityAdapter,
        maintenanceService, continuationTokenHelper, uploadManager, uploadConfiguration,
        new ComponentXOFactory(emptySet()), ImmutableSet.of(componentsResourceExtension));
  }

  @Test
  public void getComponents_firstPage() throws Exception {
    when(componentBrowseResult.getTotal()).thenReturn(10L);
    when(componentBrowseResult.getResults()).thenReturn(Arrays.asList(componentOne, componentTwo));

    when(browseService.browseComponents(eq(mavenReleases), queryOptionsCaptor.capture()))
        .thenReturn(componentBrowseResult);

    Page<ComponentXO> componentXOPage = underTest.getComponents(null, mavenReleasesId);

    assertThat(componentXOPage.getItems(), hasSize(2));
    assertThat(componentXOPage.getContinuationToken(), is("88491cd1d185dd1318fdba4364e78406"));
    assertThat(queryOptionsCaptor.getValue().getLastId(), nullValue());
    verify(componentsResourceExtension, times(2)).updateComponentXO(any(ComponentXO.class), any(Component.class));

    componentXOPage.getItems().stream().forEach(componentXO -> assertThat(componentXO.getAssets(), hasSize(1)));
  }

  @Test(expected = NotFoundException.class)
  public void getComponentById_notFound() throws Exception {
    RepositoryItemIDXO repositoryItemXOID = getRepositoryItemIdXO(null);

    underTest.getComponentById(repositoryItemXOID.getValue());
  }

  @Test
  public void getComponentById_illegalArgumentException() throws Exception {
    RepositoryItemIDXO repositoryItemXOID = new RepositoryItemIDXO("maven-releases",
        "f10bd0593de3b5e4b377049bcaa80d3e");

    //IllegalArgumentException is thrown when an id for a different entity type is supplied
    doThrow(new IllegalArgumentException()).when(componentEntityAdapter)
        .recordIdentity(new DetachedEntityId(repositoryItemXOID.getId()));

    thrown.expect(hasProperty("response", hasProperty("status", is(UNPROCESSABLE_ENTITY))));
    underTest.getComponentById(repositoryItemXOID.getValue());
  }

  @Test
  public void deleteComponent() throws Exception {
    RepositoryItemIDXO repositoryItemXOID = getRepositoryItemIdXO(componentOne);

    underTest.deleteComponent(repositoryItemXOID.getValue());

    verify(maintenanceService).deleteComponent(mavenReleases, componentOne);
  }

  @Test(expected = NotFoundException.class)
  public void deleteComponent_notFound() throws Exception {
    RepositoryItemIDXO repositoryItemXOID = getRepositoryItemIdXO(null);

    underTest.deleteComponent(repositoryItemXOID.getValue());
  }

  @Test
  public void invalidContinuationTokenReturnsNotAcceptable() {
    doThrow(new IllegalArgumentException()).when(componentEntityAdapter)
        .recordIdentity(detachedEntityIdCaptor.capture());

    thrown.expect(hasProperty("response", hasProperty("status", is(NOT_ACCEPTABLE))));
    underTest.getComponents("whatever", mavenReleasesId);
  }

  private RepositoryItemIDXO getRepositoryItemIdXO(final Component resultComponent) {
    RepositoryItemIDXO repositoryItemXOID = new RepositoryItemIDXO("maven-releases",
        "f10bd0593de3b5e4b377049bcaa80d3e");

    when(componentEntityAdapter.recordIdentity(new DetachedEntityId(repositoryItemXOID.getId())))
        .thenReturn(componentOneORID);
    when(browseService.getComponentById(componentOneORID, mavenReleases)).thenReturn(resultComponent);

    return repositoryItemXOID;
  }

  @Test
  public void uploadComponent() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);

    UploadResponse uploadResponse = new UploadResponse(new DetachedEntityId(COMPONENT_ID), emptyList());
    when(uploadManager.handle(mavenReleases, request)).thenReturn(uploadResponse);
    underTest.uploadComponent(mavenReleasesId, request);
  }

  @Test
  public void uploadComponentIsHiddenWhenFeatureFlagIsNotEnabled() throws Exception {
    when(uploadConfiguration.isEnabled()).thenReturn(false);

    try {
      underTest.uploadComponent(mavenReleasesId, null);
      fail("Expected a WebApplicationException(NOT_FOUND) to be thrown");
    }
    catch (WebApplicationException e) {
      assertThat(e.getResponse(), is(CoreMatchers.notNullValue()));
      assertThat(e.getResponse().getStatus(), is(404));
    }
  }

  @Test
  public void getComponents_lastPage() throws Exception {
    when(componentBrowseResult.getTotal()).thenReturn(2L);
    when(componentBrowseResult.getResults()).thenReturn(Arrays.asList(componentOne, componentTwo));

    when(browseService.browseComponents(eq(mavenReleases), queryOptionsCaptor.capture()))
        .thenReturn(componentBrowseResult);

    when(componentEntityAdapter.recordIdentity(detachedEntityIdCaptor.capture())).thenReturn(componentTwoORID);

    Page<ComponentXO> componentXOPage = underTest.getComponents("88491cd1d185dd1318fdba4364e78406", mavenReleasesId);

    assertThat(componentXOPage.getItems(), hasSize(2));
    assertThat(componentXOPage.getContinuationToken(), isEmptyOrNullString());
    assertThat(queryOptionsCaptor.getValue().getLastId(), notNullValue());
    assertThat(detachedEntityIdCaptor.getValue(), notNullValue());
    verify(componentsResourceExtension, times(2)).updateComponentXO(any(ComponentXO.class), any(Component.class));

    componentXOPage.getItems().stream().forEach(componentXO -> assertThat(componentXO.getAssets(), hasSize(1)));
  }

  @Test
  public void getComponentById() throws Exception {
    RepositoryItemIDXO repositoryItemXOID = getRepositoryItemIdXO(componentOne);

    ComponentXO componentXO = underTest.getComponentById(repositoryItemXOID.getValue());

    assertThat(componentXO, notNullValue());
    assertThat(componentXO.getGroup(), is("component-one-group"));
    assertThat(componentXO.getName(), is("component-one-name"));
    assertThat(componentXO.getVersion(), is("1.0.0"));
    assertThat(componentXO.getAssets(), hasSize(1));

    verify(componentsResourceExtension).updateComponentXO(any(ComponentXO.class), any(Component.class));
  }

  private class TestComponentsResourceExtension
      implements ComponentsResourceExtension
  {
    @Override
    public ComponentXO updateComponentXO(final ComponentXO componentXO, final Component component) {
      return componentXO;
    }
  }
}
