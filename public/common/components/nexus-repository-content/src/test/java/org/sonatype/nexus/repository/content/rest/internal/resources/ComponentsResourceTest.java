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
package org.sonatype.nexus.repository.content.rest.internal.resources;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.content.fluent.FluentContinuation;
import org.sonatype.nexus.repository.content.fluent.internal.FluentAssetImpl;
import org.sonatype.nexus.repository.content.fluent.internal.FluentComponentImpl;
import org.sonatype.nexus.repository.content.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.content.rest.ComponentsResourceExtension;
import org.sonatype.nexus.repository.content.store.AssetBlobData;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.rest.api.ComponentXO;
import org.sonatype.nexus.repository.rest.api.ComponentXOFactory;
import org.sonatype.nexus.repository.rest.api.DefaultComponentXO;
import org.sonatype.nexus.repository.rest.api.RepositoryItemIDXO;
import org.sonatype.nexus.repository.rest.api.RepositoryManagerRESTAdapter;
import org.sonatype.nexus.repository.selector.ContentAuthHelper;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.upload.UploadManager;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.rest.Page;
import org.sonatype.nexus.rest.WebApplicationMessageException;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;

import static java.util.Base64.getUrlEncoder;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.content.rest.internal.resources.AssetsResourceSupport.PAGE_SIZE_LIMIT;
import static org.sonatype.nexus.repository.content.store.InternalIds.toExternalId;

@ExtendWith({MockitoExtension.class, AuthenticationExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
@WithUser(permissions = {"nexus:uploader-metadata:read"})
class ComponentsResourceTest
{
  private static final String REPOSITORY_NAME = "test-repo";

  private static final String REPOSITORY_URL = "http://localhost:8081/repository/" + REPOSITORY_NAME;

  private static final String FORMAT_VALUE = "maven2";

  private static final int COMPONENT_ID = 42;

  private static final String COMPONENT_NAME = "junit";

  private static final String COMPONENT_NAMESPACE = "org.junit";

  private static final String COMPONENT_VERSION = "4.12";

  private ComponentsResource underTest;

  @Mock
  private Format format;

  @Mock
  private Repository repository;

  @Mock
  private RepositoryManagerRESTAdapter repositoryManagerRESTAdapter;

  @Mock
  private MaintenanceService maintenanceService;

  @Mock
  private UploadManager uploadManager;

  @Mock
  private ComponentXOFactory componentXOFactory;

  @Mock
  private ContentAuthHelper contentAuthHelper;

  @Mock
  private ContentFacet contentFacet;

  @Mock
  private ContentFacetSupport contentFacetSupport;

  @Mock
  private FluentComponents fluentComponents;

  @Mock
  private Continuation<FluentComponent> componentContinuation;

  @Spy
  private ComponentsResourceExtension componentsResourceExtension = new TestComponentsResourceExtension();

  @BeforeEach
  void setUp() {
    configureMockedRepository();

    underTest = new ComponentsResource(
        repositoryManagerRESTAdapter,
        maintenanceService,
        uploadManager,
        componentXOFactory,
        contentAuthHelper,
        ImmutableSet.of(componentsResourceExtension),
        null);
  }

  // --- getComponents tests ---

  @Test
  void getComponents_returnsEmptyPageWhenNoComponents() {
    when(componentContinuation.isEmpty()).thenReturn(true);

    Page<ComponentXO> page = underTest.getComponents(null, REPOSITORY_NAME);

    assertThat(page, is(notNullValue()));
    assertThat(page.getItems(), is(empty()));
    assertThat(page.getContinuationToken(), is(nullValue()));
  }

  @Test
  void getComponents_returnsPageWithComponents() {
    when(componentXOFactory.createComponentXO()).thenReturn(new DefaultComponentXO());
    when(contentAuthHelper.checkPathPermissions(COMPONENT_NAME, FORMAT_VALUE, REPOSITORY_NAME)).thenReturn(true);
    when(contentAuthHelper.checkPathPermissions(any(), eq(FORMAT_VALUE), eq(REPOSITORY_NAME))).thenReturn(true);
    when(repositoryManagerRESTAdapter.findContainingGroups(REPOSITORY_NAME)).thenReturn(emptySet());

    List<FluentComponent> componentList = List.of(aFluentComponent());
    when(componentContinuation.isEmpty()).thenReturn(false).thenReturn(true);
    when(componentContinuation.iterator()).thenReturn(componentList.iterator());

    Page<ComponentXO> page = underTest.getComponents(null, REPOSITORY_NAME);

    assertThat(page, is(notNullValue()));
    assertThat(page.getItems(), hasSize(1));

    ComponentXO xo = page.getItems().iterator().next();
    assertThat(xo.getName(), is(COMPONENT_NAME));
    assertThat(xo.getGroup(), is(COMPONENT_NAMESPACE));
    assertThat(xo.getVersion(), is(COMPONENT_VERSION));
    assertThat(xo.getRepository(), is(REPOSITORY_NAME));
    assertThat(xo.getFormat(), is(FORMAT_VALUE));
  }

  @Test
  void getComponents_continuationTokenIsNullWhenFewerThanPageLimit() {
    when(componentXOFactory.createComponentXO()).thenReturn(new DefaultComponentXO());
    when(contentAuthHelper.checkPathPermissions(any(), eq(FORMAT_VALUE), eq(REPOSITORY_NAME))).thenReturn(true);
    when(repositoryManagerRESTAdapter.findContainingGroups(REPOSITORY_NAME)).thenReturn(emptySet());

    List<FluentComponent> componentList = List.of(aFluentComponent());
    when(componentContinuation.isEmpty()).thenReturn(false).thenReturn(true);
    when(componentContinuation.iterator()).thenReturn(componentList.iterator());

    Page<ComponentXO> page = underTest.getComponents(null, REPOSITORY_NAME);

    // Fewer than PAGE_SIZE_LIMIT, so token should be null
    assertThat(page.getContinuationToken(), is(nullValue()));
  }

  @Test
  void getComponents_continuationTokenIsSetWhenExactlyPageLimit() {
    when(componentXOFactory.createComponentXO()).thenReturn(new DefaultComponentXO());
    when(contentAuthHelper.checkPathPermissions(any(), eq(FORMAT_VALUE), eq(REPOSITORY_NAME))).thenReturn(true);
    when(repositoryManagerRESTAdapter.findContainingGroups(REPOSITORY_NAME)).thenReturn(emptySet());

    // Create exactly PAGE_SIZE_LIMIT components
    List<FluentComponent> componentList =
        range(0, PAGE_SIZE_LIMIT).mapToObj(i -> aFluentComponent(COMPONENT_ID + i)).collect(toList());
    when(componentContinuation.isEmpty()).thenReturn(false);
    when(componentContinuation.iterator()).thenReturn(componentList.iterator());

    Page<ComponentXO> page = underTest.getComponents(null, REPOSITORY_NAME);

    assertThat(page.getItems(), hasSize(PAGE_SIZE_LIMIT));
    // When exactly PAGE_SIZE_LIMIT results, a continuation token is returned
    assertThat(page.getContinuationToken(), is(notNullValue()));
  }

  @Test
  void getComponents_invokesExtensions() {
    when(componentXOFactory.createComponentXO()).thenReturn(new DefaultComponentXO());
    when(contentAuthHelper.checkPathPermissions(any(), eq(FORMAT_VALUE), eq(REPOSITORY_NAME))).thenReturn(true);
    when(repositoryManagerRESTAdapter.findContainingGroups(REPOSITORY_NAME)).thenReturn(emptySet());

    List<FluentComponent> componentList = List.of(aFluentComponent());
    when(componentContinuation.isEmpty()).thenReturn(false).thenReturn(true);
    when(componentContinuation.iterator()).thenReturn(componentList.iterator());

    underTest.getComponents(null, REPOSITORY_NAME);

    verify(componentsResourceExtension).updateComponentXO(any(ComponentXO.class), any(FluentComponent.class));
  }

  @Test
  void getComponents_filtersAssetsByPermission() {
    when(componentXOFactory.createComponentXO()).thenReturn(new DefaultComponentXO());
    when(contentAuthHelper.checkPathPermissions(COMPONENT_NAME, FORMAT_VALUE, REPOSITORY_NAME)).thenReturn(true);
    // Deny asset-level permission
    when(contentAuthHelper.checkPathPermissions(any(), eq(FORMAT_VALUE), eq(REPOSITORY_NAME))).thenReturn(false);
    // But allow component-level permission
    when(contentAuthHelper.checkPathPermissions(eq(COMPONENT_NAME), eq(FORMAT_VALUE), eq(REPOSITORY_NAME)))
        .thenReturn(true);
    when(repositoryManagerRESTAdapter.findContainingGroups(REPOSITORY_NAME)).thenReturn(emptySet());

    FluentComponent componentWithAsset = aFluentComponentWithAsset();
    List<FluentComponent> componentList = List.of(componentWithAsset);
    when(componentContinuation.isEmpty()).thenReturn(false).thenReturn(true);
    when(componentContinuation.iterator()).thenReturn(componentList.iterator());

    Page<ComponentXO> page = underTest.getComponents(null, REPOSITORY_NAME);

    assertThat(page.getItems(), hasSize(1));
    // Assets should be filtered out because permission denied
    assertThat(page.getItems().iterator().next().getAssets(), is(empty()));
  }

  // --- getComponentById tests ---

  @Test
  void getComponentById_returnsComponent() {
    when(componentXOFactory.createComponentXO()).thenReturn(new DefaultComponentXO());
    when(contentAuthHelper.checkPathPermissions(COMPONENT_NAME, FORMAT_VALUE, REPOSITORY_NAME)).thenReturn(true);
    when(repositoryManagerRESTAdapter.findContainingGroups(REPOSITORY_NAME)).thenReturn(emptySet());

    String externalId = toExternalId(COMPONENT_ID).getValue();
    when(fluentComponents.find(new DetachedEntityId(externalId)))
        .thenReturn(Optional.of(aFluentComponent()));

    String encodedId = encodeId(REPOSITORY_NAME, externalId);

    ComponentXO result = underTest.getComponentById(encodedId);

    assertThat(result, is(notNullValue()));
    assertThat(result.getName(), is(COMPONENT_NAME));
    assertThat(result.getGroup(), is(COMPONENT_NAMESPACE));
    assertThat(result.getVersion(), is(COMPONENT_VERSION));
    assertThat(result.getRepository(), is(REPOSITORY_NAME));
    assertThat(result.getFormat(), is(FORMAT_VALUE));
  }

  @Test
  void getComponentById_throwsNotFoundWhenComponentDoesNotExist() {
    String externalId = toExternalId(COMPONENT_ID).getValue();
    when(fluentComponents.find(new DetachedEntityId(externalId)))
        .thenReturn(Optional.empty());

    String encodedId = encodeId(REPOSITORY_NAME, externalId);

    assertThrows(NotFoundException.class, () -> underTest.getComponentById(encodedId));
  }

  @Test
  void getComponentById_throwsNotFoundWhenNotPermitted() {
    when(contentAuthHelper.checkPathPermissions(COMPONENT_NAME, FORMAT_VALUE, REPOSITORY_NAME)).thenReturn(false);

    String externalId = toExternalId(COMPONENT_ID).getValue();
    when(fluentComponents.find(new DetachedEntityId(externalId)))
        .thenReturn(Optional.of(aFluentComponent()));

    String encodedId = encodeId(REPOSITORY_NAME, externalId);

    assertThrows(NotFoundException.class, () -> underTest.getComponentById(encodedId));
  }

  @Test
  void getComponentById_throwsUnprocessableEntityOnIllegalArgument() {
    String externalId = toExternalId(COMPONENT_ID).getValue();
    when(fluentComponents.find(new DetachedEntityId(externalId)))
        .thenThrow(new IllegalArgumentException("bad id"));

    String encodedId = encodeId(REPOSITORY_NAME, externalId);

    try {
      underTest.getComponentById(encodedId);
      fail("Expected WebApplicationException");
    }
    catch (WebApplicationException e) {
      assertThat(e.getResponse().getStatus(), is(422));
    }
  }

  @Test
  void getComponentById_setsIdCorrectly() {
    when(componentXOFactory.createComponentXO()).thenReturn(new DefaultComponentXO());
    when(contentAuthHelper.checkPathPermissions(COMPONENT_NAME, FORMAT_VALUE, REPOSITORY_NAME)).thenReturn(true);
    when(repositoryManagerRESTAdapter.findContainingGroups(REPOSITORY_NAME)).thenReturn(emptySet());

    String externalId = toExternalId(COMPONENT_ID).getValue();
    when(fluentComponents.find(new DetachedEntityId(externalId)))
        .thenReturn(Optional.of(aFluentComponent()));

    String encodedId = encodeId(REPOSITORY_NAME, externalId);

    ComponentXO result = underTest.getComponentById(encodedId);

    String expectedId = new RepositoryItemIDXO(REPOSITORY_NAME, externalId).getValue();
    assertThat(result.getId(), is(expectedId));
  }

  // --- deleteComponent tests ---

  @Test
  void deleteComponent_deletesExistingComponent() {
    when(contentAuthHelper.checkPathPermissions(COMPONENT_NAME, FORMAT_VALUE, REPOSITORY_NAME)).thenReturn(true);

    String externalId = toExternalId(COMPONENT_ID).getValue();
    FluentComponent component = aFluentComponent();
    when(fluentComponents.find(new DetachedEntityId(externalId)))
        .thenReturn(Optional.of(component));

    String encodedId = encodeId(REPOSITORY_NAME, externalId);

    underTest.deleteComponent(encodedId);

    verify(maintenanceService).deleteComponent(repository, component);
  }

  @Test
  void deleteComponent_throwsNotFoundWhenComponentDoesNotExist() {
    String externalId = toExternalId(COMPONENT_ID).getValue();
    when(fluentComponents.find(new DetachedEntityId(externalId)))
        .thenReturn(Optional.empty());

    String encodedId = encodeId(REPOSITORY_NAME, externalId);

    assertThrows(NotFoundException.class, () -> underTest.deleteComponent(encodedId));
  }

  @Test
  void deleteComponent_throwsNotFoundWhenNotPermitted() {
    when(contentAuthHelper.checkPathPermissions(COMPONENT_NAME, FORMAT_VALUE, REPOSITORY_NAME)).thenReturn(false);

    String externalId = toExternalId(COMPONENT_ID).getValue();
    when(fluentComponents.find(new DetachedEntityId(externalId)))
        .thenReturn(Optional.of(aFluentComponent()));

    String encodedId = encodeId(REPOSITORY_NAME, externalId);

    assertThrows(NotFoundException.class, () -> underTest.deleteComponent(encodedId));
  }

  @Test
  void deleteComponent_doesNotDeleteWhenComponentNotFound() {
    String externalId = toExternalId(COMPONENT_ID).getValue();
    when(fluentComponents.find(new DetachedEntityId(externalId)))
        .thenReturn(Optional.empty());

    String encodedId = encodeId(REPOSITORY_NAME, externalId);

    try {
      underTest.deleteComponent(encodedId);
    }
    catch (NotFoundException e) {
      // expected
    }

    verify(maintenanceService, never()).deleteComponent(any(), any());
  }

  // --- uploadComponent tests ---

  @Test
  void uploadComponent_successfulUpload() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getContentType()).thenReturn(MediaType.MULTIPART_FORM_DATA);

    UploadResponse uploadResponse = new UploadResponse(emptyList());
    when(uploadManager.handle(repository, request)).thenReturn(uploadResponse);

    underTest.uploadComponent(REPOSITORY_NAME, request);

    verify(uploadManager).handle(repository, request);
  }

  @Test
  void uploadComponent_acceptsMultipartWithBoundary() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getContentType()).thenReturn("multipart/form-data; boundary=----WebKitFormBoundary");

    UploadResponse uploadResponse = new UploadResponse(emptyList());
    when(uploadManager.handle(repository, request)).thenReturn(uploadResponse);

    underTest.uploadComponent(REPOSITORY_NAME, request);

    verify(uploadManager).handle(repository, request);
  }

  @Test
  void uploadComponent_throwsBadRequestWhenContentTypeIsNull() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getContentType()).thenReturn(null);

    try {
      underTest.uploadComponent(REPOSITORY_NAME, request);
      fail("Expected WebApplicationMessageException");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(400));
    }
  }

  @Test
  void uploadComponent_throwsBadRequestWhenContentTypeIsNotMultipart() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getContentType()).thenReturn(MediaType.APPLICATION_JSON);

    try {
      underTest.uploadComponent(REPOSITORY_NAME, request);
      fail("Expected WebApplicationMessageException");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(400));
    }
  }

  @Test
  void uploadComponent_throwsBadRequestOnIllegalOperationException() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getContentType()).thenReturn(MediaType.MULTIPART_FORM_DATA);
    when(uploadManager.handle(repository, request))
        .thenThrow(new IllegalOperationException("Repository does not allow upload"));

    try {
      underTest.uploadComponent(REPOSITORY_NAME, request);
      fail("Expected WebApplicationMessageException");
    }
    catch (WebApplicationMessageException e) {
      assertThat(e.getResponse().getStatus(), is(400));
    }
  }

  @Test
  void uploadComponent_propagatesIOException() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getContentType()).thenReturn(MediaType.MULTIPART_FORM_DATA);
    when(uploadManager.handle(repository, request)).thenThrow(new IOException("Upload failed"));

    try {
      underTest.uploadComponent(REPOSITORY_NAME, request);
      fail("Expected IOException");
    }
    catch (IOException e) {
      assertThat(e.getMessage(), is("Upload failed"));
    }
  }

  // --- fromComponent / toComponentXOs tests (exercised through getComponentById) ---

  @Test
  void fromComponent_populatesAllFieldsCorrectly() {
    when(componentXOFactory.createComponentXO()).thenReturn(new DefaultComponentXO());
    when(contentAuthHelper.checkPathPermissions(COMPONENT_NAME, FORMAT_VALUE, REPOSITORY_NAME)).thenReturn(true);
    when(repositoryManagerRESTAdapter.findContainingGroups(REPOSITORY_NAME)).thenReturn(emptySet());

    String externalId = toExternalId(COMPONENT_ID).getValue();
    when(fluentComponents.find(new DetachedEntityId(externalId)))
        .thenReturn(Optional.of(aFluentComponent()));

    String encodedId = encodeId(REPOSITORY_NAME, externalId);

    ComponentXO result = underTest.getComponentById(encodedId);

    assertThat(result.getName(), is(COMPONENT_NAME));
    assertThat(result.getGroup(), is(COMPONENT_NAMESPACE));
    assertThat(result.getVersion(), is(COMPONENT_VERSION));
    assertThat(result.getRepository(), is(REPOSITORY_NAME));
    assertThat(result.getFormat(), is(FORMAT_VALUE));
    assertThat(result.getAssets(), is(notNullValue()));
    assertThat(result.getAssets(), is(empty()));
    assertThat(result.getId(), is(new RepositoryItemIDXO(REPOSITORY_NAME, externalId).getValue()));
  }

  @Test
  void fromComponent_invokesMultipleExtensions() {
    ComponentsResourceExtension secondExtension = mock(ComponentsResourceExtension.class);
    when(secondExtension.updateComponentXO(any(), any())).thenAnswer(inv -> inv.getArgument(0));

    ComponentsResource resourceWithMultipleExtensions = new ComponentsResource(
        repositoryManagerRESTAdapter,
        maintenanceService,
        uploadManager,
        componentXOFactory,
        contentAuthHelper,
        ImmutableSet.of(componentsResourceExtension, secondExtension),
        null);

    when(componentXOFactory.createComponentXO()).thenReturn(new DefaultComponentXO());
    when(contentAuthHelper.checkPathPermissions(COMPONENT_NAME, FORMAT_VALUE, REPOSITORY_NAME)).thenReturn(true);
    when(repositoryManagerRESTAdapter.findContainingGroups(REPOSITORY_NAME)).thenReturn(emptySet());

    String externalId = toExternalId(COMPONENT_ID).getValue();
    when(fluentComponents.find(new DetachedEntityId(externalId)))
        .thenReturn(Optional.of(aFluentComponent()));

    String encodedId = encodeId(REPOSITORY_NAME, externalId);

    resourceWithMultipleExtensions.getComponentById(encodedId);

    verify(componentsResourceExtension).updateComponentXO(any(ComponentXO.class), any(FluentComponent.class));
    verify(secondExtension).updateComponentXO(any(ComponentXO.class), any(FluentComponent.class));
  }

  @Test
  void fromComponent_worksWithNoExtensions() {
    ComponentsResource resourceWithNoExtensions = new ComponentsResource(
        repositoryManagerRESTAdapter,
        maintenanceService,
        uploadManager,
        componentXOFactory,
        contentAuthHelper,
        emptySet(),
        null);

    when(componentXOFactory.createComponentXO()).thenReturn(new DefaultComponentXO());
    when(contentAuthHelper.checkPathPermissions(COMPONENT_NAME, FORMAT_VALUE, REPOSITORY_NAME)).thenReturn(true);
    when(repositoryManagerRESTAdapter.findContainingGroups(REPOSITORY_NAME)).thenReturn(emptySet());

    String externalId = toExternalId(COMPONENT_ID).getValue();
    when(fluentComponents.find(new DetachedEntityId(externalId)))
        .thenReturn(Optional.of(aFluentComponent()));

    String encodedId = encodeId(REPOSITORY_NAME, externalId);

    ComponentXO result = resourceWithNoExtensions.getComponentById(encodedId);

    assertThat(result, is(notNullValue()));
    assertThat(result.getName(), is(COMPONENT_NAME));
  }

  // --- getComponent (private) error handling tested through getComponentById ---

  @Test
  void getComponent_throwsUnprocessableEntityForInvalidIdFormat() {
    // When find throws IllegalArgumentException, the resource should wrap it as 422
    String externalId = toExternalId(COMPONENT_ID).getValue();
    when(fluentComponents.find(any(DetachedEntityId.class)))
        .thenThrow(new IllegalArgumentException("Invalid component id"));

    String encodedId = encodeId(REPOSITORY_NAME, externalId);

    try {
      underTest.getComponentById(encodedId);
      fail("Expected WebApplicationException");
    }
    catch (WebApplicationException e) {
      assertThat(e.getResponse().getStatus(), is(422));
    }
  }

  // --- Helper methods ---

  private void configureMockedRepository() {
    when(repositoryManagerRESTAdapter.getRepository(REPOSITORY_NAME)).thenReturn(repository);
    when(repository.getUrl()).thenReturn(REPOSITORY_URL);
    when(repository.getName()).thenReturn(REPOSITORY_NAME);
    when(repository.getType()).thenReturn(new HostedType());
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn(FORMAT_VALUE);

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.components()).thenReturn(fluentComponents);
    when(fluentComponents.browse(PAGE_SIZE_LIMIT, null))
        .thenReturn(new FluentContinuation<>(componentContinuation, c -> c));
  }

  private FluentComponent aFluentComponent() {
    return aFluentComponent(COMPONENT_ID);
  }

  private FluentComponent aFluentComponent(final int componentId) {
    ComponentData componentData = new ComponentData();
    componentData.setComponentId(componentId);
    componentData.setName(COMPONENT_NAME);
    componentData.setNamespace(COMPONENT_NAMESPACE);
    componentData.setVersion(COMPONENT_VERSION);
    return new FluentComponentImpl(contentFacetSupport, componentData, Collections.emptyList());
  }

  private FluentComponent aFluentComponentWithAsset() {
    AssetData assetData = new AssetData();
    assetData.setAssetId(1);
    assetData.setPath("/org/junit/junit/4.12/junit-4.12.jar");
    assetData.setCreated(OffsetDateTime.now());
    AssetBlobData assetBlobData = new AssetBlobData();
    assetBlobData.setAssetBlobId(1);
    assetBlobData.setBlobCreated(OffsetDateTime.now());
    assetData.setAssetBlob(assetBlobData);

    FluentAsset fluentAsset = new FluentAssetImpl(contentFacetSupport, assetData);

    ComponentData componentData = new ComponentData();
    componentData.setComponentId(COMPONENT_ID);
    componentData.setName(COMPONENT_NAME);
    componentData.setNamespace(COMPONENT_NAMESPACE);
    componentData.setVersion(COMPONENT_VERSION);

    return new FluentComponentImpl(contentFacetSupport, componentData, List.of(fluentAsset));
  }

  private String encodeId(final String repositoryName, final String id) {
    return getUrlEncoder().withoutPadding().encodeToString((repositoryName + ":" + id).getBytes());
  }

  private static class TestComponentsResourceExtension
      implements ComponentsResourceExtension
  {
    @Override
    public ComponentXO updateComponentXO(final ComponentXO componentXO, final FluentComponent component) {
      return componentXO;
    }
  }
}
