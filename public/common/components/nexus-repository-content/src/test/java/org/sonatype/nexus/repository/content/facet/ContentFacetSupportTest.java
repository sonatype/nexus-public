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
package org.sonatype.nexus.repository.content.facet;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.app.softdelete.SoftDeletedAssetRef;
import org.sonatype.nexus.common.app.softdelete.TrashIndexRecorder;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.datastore.api.SerializedAccessException;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.RedeployDisabledException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.config.WritePolicy;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.ContentRepository;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport.Config;
import org.sonatype.nexus.repository.content.store.AssetBlobStore;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.content.store.ComponentStore;
import org.sonatype.nexus.repository.content.store.ContentRepositoryData;
import org.sonatype.nexus.repository.content.store.ContentRepositoryStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.security.ClientInfo;
import org.sonatype.nexus.security.ClientInfoProvider;
import org.sonatype.nexus.transaction.Transaction;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentFacetSupportTest
{
  @Mock
  Configuration configuration;

  @Mock
  Repository repository;

  @Mock
  FormatStoreManager formatStoreManager;

  @Mock
  ContentFacetDependencies dependencies;

  @Mock
  BlobStoreManager blobStoreManager;

  @Mock
  ContentRepositoryStore contentRepositoryStore;

  @Mock
  AssetStore assetStore;

  @Mock
  ComponentStore componentStore;

  @Mock
  AssetBlobStore assetBlobStore;

  @Mock
  TrashIndexRecorder trashIndexRecorder;

  ContentFacetSupport underTest;

  @Mock
  DataSessionSupplier dataSessionSupplier;

  @BeforeEach
  void setup() throws Exception {
    lenient().when(dependencies.getBlobStoreManager()).thenReturn(blobStoreManager);
    lenient().when(dependencies.getDataSessionSupplier()).thenReturn(dataSessionSupplier);

    DataSession<?> mockSession = mock(DataSession.class);
    Transaction mockTransaction = mock(Transaction.class);
    lenient().when(mockTransaction.allowRetry(any())).thenReturn(false);
    lenient().doReturn(mockTransaction).when(mockSession).getTransaction();
    lenient().doReturn(mockSession).when(dataSessionSupplier).openSession(any());

    lenient().doReturn(contentRepositoryStore).when(formatStoreManager).contentRepositoryStore(any());
    lenient().doReturn(componentStore).when(formatStoreManager).componentStore(any());
    lenient().doReturn(assetStore).when(formatStoreManager).assetStore(any());
    lenient().doReturn(assetBlobStore).when(formatStoreManager).assetBlobStore(any());

    underTest = new ContentFacetSupport(formatStoreManager)
    {
      // nothing to add
    };
    underTest.setDependencies(dependencies);
    underTest.attach(repository);
  }

  @Test
  void testDoValidate() throws Exception {
    ConstraintViolationFactory factory = mock(Answers.RETURNS_MOCKS);
    when(dependencies.getConstraintViolationFactory()).thenReturn(factory);

    Type type = mock();
    when(repository.getType()).thenReturn(type);

    ConfigurationFacet conf = mock();
    when(repository.facet(ConfigurationFacet.class)).thenReturn(conf);

    Config config = new Config();
    when(conf.readSection(any(), any(), eq(Config.class))).thenReturn(config);

    // Simple case a blobstore that exists
    BlobStore blobstore = mock();
    when(blobStoreManager.get("default")).thenReturn(blobstore);
    config.blobStoreName = "default";
    assertDoesNotThrow(() -> underTest.doValidate(configuration));

    // Specified blobstore is a member group member
    when(blobStoreManager.getParent("default")).thenReturn(Optional.of("parent"));
    assertThrows(ConstraintViolationException.class, () -> underTest.doValidate(configuration));
    verify(factory).createViolation("storage.blobStoreName",
        "Blob Store 'default' is a member of Blob Store Group 'parent' and cannot be set as storage");

    reset(factory);

    // Specified blobstore does not exist - should NOT throw exception (allow repo to load on startup)
    config.blobStoreName = "missing-blobstore";
    when(blobStoreManager.get("missing-blobstore")).thenReturn(null);
    assertDoesNotThrow(() -> underTest.doValidate(configuration));
    // Existence validation is now handled by BaseRepositoryManager.validateConfiguration() during create/update
  }

  /**
   * Test doConfigure sets up stores and fluent APIs from configuration.
   */
  @Test
  void testDoConfigure_setsUpStoresAndFluentApis() throws Exception {
    ConfigurationFacet conf = mock();
    when(repository.facet(ConfigurationFacet.class)).thenReturn(conf);

    Config config = new Config();
    config.blobStoreName = "test-blobstore";
    config.dataStoreName = "test-datastore";
    config.writePolicy = WritePolicy.ALLOW;
    config.strictContentTypeValidation = true;
    when(conf.readSection(any(), any(), eq(Config.class))).thenReturn(config);

    underTest.doConfigure(configuration);

    // Verify stores were created for the configured datastore name
    verify(formatStoreManager).contentRepositoryStore("test-datastore");
    verify(formatStoreManager).componentStore("test-datastore");
    verify(formatStoreManager).assetStore("test-datastore");
    verify(formatStoreManager).assetBlobStore("test-datastore");

    // Verify fluent APIs are accessible after configuration
    assertThat(underTest.blobs(), is(notNullValue()));
    assertThat(underTest.components(), is(notNullValue()));
    assertThat(underTest.assets(), is(notNullValue()));
    assertThat(underTest.stores(), is(notNullValue()));
    assertThat(underTest.stores().blobStoreName, is("test-blobstore"));
    assertThat(underTest.stores().contentStoreName, is("test-datastore"));
  }

  /**
   * Test doStart with an existing content repository reads its ID.
   */
  @Test
  void testDoStart_existingContentRepository() throws Exception {
    // Setup: configure first to populate stores
    configureFacet(WritePolicy.ALLOW);

    EntityId configRepoId = mock(EntityId.class);
    when(repository.getConfiguration()).thenReturn(configuration);
    when(configuration.getRepositoryId()).thenReturn(configRepoId);

    ContentRepository existingRepo = mock(ContentRepository.class);
    when(existingRepo.contentRepositoryId()).thenReturn(42);
    when(contentRepositoryStore.readContentRepository(configRepoId)).thenReturn(Optional.of(existingRepo));

    AssetBlobValidators validators = mock();
    AssetBlobValidator validator = mock();
    when(dependencies.getAssetBlobValidators()).thenReturn(validators);
    when(validators.selectValidator(repository)).thenReturn(validator);

    underTest.doStart();

    assertThat(underTest.configRepositoryId(), is(configRepoId));
    assertThat(underTest.contentRepositoryId(), is(42));
    // Verify it did NOT create a new content repository since one already existed
    verify(contentRepositoryStore, never()).createContentRepository(any());
  }

  /**
   * Test doStart creates a new content repository when none exists.
   */
  @Test
  void testDoStart_createsNewContentRepository() throws Exception {
    configureFacet(WritePolicy.ALLOW);

    EntityId configRepoId = mock(EntityId.class);
    when(repository.getConfiguration()).thenReturn(configuration);
    when(configuration.getRepositoryId()).thenReturn(configRepoId);

    // No existing content repo
    when(contentRepositoryStore.readContentRepository(configRepoId)).thenReturn(Optional.empty());

    // The createContentRepository call should succeed; we need to simulate the store
    // setting the contentRepositoryId on the data object (MyBatis behavior)
    ContentRepositoryData[] createdData = new ContentRepositoryData[1];
    when(contentRepositoryStore.readContentRepository(configRepoId)).thenReturn(Optional.empty());
    org.mockito.stubbing.Answer<Void> createAnswer = invocation -> {
      ContentRepositoryData data = invocation.getArgument(0);
      createdData[0] = data;
      // Simulate MyBatis setting the repository id
      try {
        java.lang.reflect.Field field = data.getClass().getSuperclass().getDeclaredField("repositoryId");
        field.setAccessible(true);
        field.set(data, 99);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
      return null;
    };
    org.mockito.Mockito.doAnswer(createAnswer).when(contentRepositoryStore).createContentRepository(any());

    AssetBlobValidators validators = mock();
    AssetBlobValidator validator = mock();
    when(dependencies.getAssetBlobValidators()).thenReturn(validators);
    when(validators.selectValidator(repository)).thenReturn(validator);

    underTest.doStart();

    // Verify a content repository was created
    verify(contentRepositoryStore).createContentRepository(any(ContentRepositoryData.class));
    assertThat(underTest.configRepositoryId(), is(configRepoId));
    // Verify the created repository had configRepositoryId set
    assertThat(createdData[0], is(notNullValue()));
    assertThat(createdData[0].configRepositoryId(), is(configRepoId));
  }

  /**
   * Fix: under the HA create->use race a peer may insert the shared content-repository row between our read
   * and our insert. On the losing insert we must absorb the DuplicateKeyException and re-read the peer's row
   * so the facet still starts, rather than failing its INITIALISED -> STARTED transition.
   */
  @Test
  void testDoStart_toleratesConcurrentCreate_duplicateKeyReReadsPeerRow() throws Exception {
    configureFacet(WritePolicy.ALLOW);

    EntityId configRepoId = mock(EntityId.class);
    when(repository.getConfiguration()).thenReturn(configuration);
    when(configuration.getRepositoryId()).thenReturn(configRepoId);

    ContentRepository peerRepo = mock(ContentRepository.class);
    when(peerRepo.contentRepositoryId()).thenReturn(55);
    // absent on our first read (we attempt to create); after losing the insert race the peer's row is present
    when(contentRepositoryStore.readContentRepository(configRepoId))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(peerRepo));
    doThrow(new DuplicateKeyException(new RuntimeException("dup")))
        .when(contentRepositoryStore)
        .createContentRepository(any());

    AssetBlobValidators validators = mock();
    AssetBlobValidator validator = mock();
    when(dependencies.getAssetBlobValidators()).thenReturn(validators);
    when(validators.selectValidator(repository)).thenReturn(validator);

    assertDoesNotThrow(() -> underTest.doStart());

    assertThat(underTest.contentRepositoryId(), is(55));
    verify(contentRepositoryStore).createContentRepository(any(ContentRepositoryData.class));
  }

  /**
   * Test doStart fails with IllegalStateException when configRepositoryId is null.
   */
  @Test
  void testDoStart_failsWhenConfigRepositoryIdNull() throws Exception {
    configureFacet(WritePolicy.ALLOW);

    when(repository.getConfiguration()).thenReturn(configuration);
    when(configuration.getRepositoryId()).thenReturn(null);

    assertThrows(IllegalStateException.class, () -> underTest.doStart());
  }

  /**
   * Test doDelete deletes assets, components, and content repository when configRepositoryId is set.
   */
  @Test
  void testDoDelete_deletesAllData() throws Exception {
    configureFacet(WritePolicy.ALLOW);
    startFacet(77);

    underTest.doDelete();

    verify(assetStore).deleteAssets(77);
    verify(componentStore).deleteComponents(77);
    verify(contentRepositoryStore).deleteContentRepository(any(EntityId.class));
  }

  /**
   * Test doDelete does nothing when configRepositoryId is null (configured but never started).
   */
  @Test
  void testDoDelete_doesNothingWhenNotStarted() throws Exception {
    // Configure but don't start - configRepositoryId remains null
    configureFacet(WritePolicy.ALLOW);

    underTest.doDelete();

    verify(assetStore, never()).deleteAssets(any(Integer.class));
    verify(componentStore, never()).deleteComponents(any(Integer.class));
    verify(contentRepositoryStore, never()).deleteContentRepository(any(EntityId.class));
  }

  /**
   * Deleting a repository issues a transactional DELETE of the shared content-repository rows that can deadlock
   * against the asynchronous browse/search flush of the same repository's content. The deadlock is transient, so
   * doDelete must be annotated to retry on {@link SerializedAccessException}; otherwise the STOPPED -> DELETED
   * transition fails with an HTTP 500 and the repository is stranded (NEXUS-54229).
   */
  @Test
  void testDoDelete_retriesOnTransientSerializationFailure() throws Exception {
    Method doDelete = ContentFacetSupport.class.getDeclaredMethod("doDelete");
    Transactional transactional = doDelete.getAnnotation(Transactional.class);

    assertThat("doDelete must be @Transactional", transactional, is(notNullValue()));
    assertThat(
        "doDelete must retry on a transient serialization failure / deadlock so a delete racing the async "
            + "browse/search flush does not strand the repository",
        asList(transactional.retryOn()), hasItem(SerializedAccessException.class));
  }

  /**
   * Test doDestroy clears the config field.
   */
  @Test
  void testDoDestroy_clearsConfig() throws Exception {
    configureFacet(WritePolicy.ALLOW_ONCE);

    // After configure, writePolicy should return ALLOW_ONCE
    assertThat(underTest.getConfiguredWritePolicy(), is(WritePolicy.ALLOW_ONCE));

    underTest.doDestroy();

    // After destroy, config is null so getConfiguredWritePolicy should return ALLOW (the default)
    assertThat(underTest.getConfiguredWritePolicy(), is(WritePolicy.ALLOW));
  }

  /**
   * Test getConfiguredWritePolicy returns ALLOW when config is null (before doConfigure).
   */
  @Test
  void testGetConfiguredWritePolicy_returnsAllowWhenConfigIsNull() {
    // Config hasn't been set (no doConfigure called)
    assertThat(underTest.getConfiguredWritePolicy(), is(WritePolicy.ALLOW));
  }

  /**
   * Test getConfiguredWritePolicy returns the configured write policy.
   */
  @Test
  void testGetConfiguredWritePolicy_returnsConfiguredPolicy() throws Exception {
    configureFacet(WritePolicy.DENY);
    assertThat(underTest.getConfiguredWritePolicy(), is(WritePolicy.DENY));
  }

  /**
   * Test checkAttachAllowed for new asset (no blob) with DENY policy blocks create.
   */
  @Test
  void testCheckAttachAllowed_newAsset_denyPolicy_blocksCreate() throws Exception {
    Asset asset = mock(Asset.class);
    when(asset.hasBlob()).thenReturn(false);
    when(asset.path()).thenReturn("/some/path");
    when(repository.getName()).thenReturn("test-repo");

    ContentFacetSupport testFacet = createFacetWithWritePolicy(WritePolicy.DENY);

    IllegalOperationException exception =
        assertThrows(IllegalOperationException.class, () -> testFacet.checkAttachAllowed(asset));

    assertThat(exception.getMessage(), containsString("test-repo"));
    assertThat(exception.getMessage(), containsString("/some/path"));
    assertThat(exception.getMessage(), containsString("is read-only"));
  }

  /**
   * Test checkAttachAllowed for new asset (no blob) with ALLOW policy allows create.
   */
  @Test
  void testCheckAttachAllowed_newAsset_allowPolicy_permitsCreate() throws Exception {
    Asset asset = mock(Asset.class);
    when(asset.hasBlob()).thenReturn(false);

    ContentFacetSupport testFacet = createFacetWithWritePolicy(WritePolicy.ALLOW);

    assertDoesNotThrow(() -> testFacet.checkAttachAllowed(asset));
  }

  /**
   * Test checkAttachAllowed for new asset (no blob) with ALLOW_ONCE policy allows create.
   */
  @Test
  void testCheckAttachAllowed_newAsset_allowOncePolicy_permitsCreate() throws Exception {
    Asset asset = mock(Asset.class);
    when(asset.hasBlob()).thenReturn(false);

    ContentFacetSupport testFacet = createFacetWithWritePolicy(WritePolicy.ALLOW_ONCE);

    assertDoesNotThrow(() -> testFacet.checkAttachAllowed(asset));
  }

  /**
   * Test that error message is enhanced when ALLOW_ONCE policy prevents update.
   */
  @Test
  void testCheckAttachAllowed_withAllowOncePolicy_enhancedErrorMessage() throws Exception {
    // Setup asset with existing blob
    Asset asset = mock(Asset.class);
    when(asset.hasBlob()).thenReturn(true);
    when(asset.path()).thenReturn("/com/example/artifact/1.0.0/artifact-1.0.0.jar");
    when(repository.getName()).thenReturn("maven-releases");

    ContentFacetSupport testFacet = createFacetWithWritePolicy(WritePolicy.ALLOW_ONCE);

    // Execute and verify exception
    RedeployDisabledException exception = assertThrows(RedeployDisabledException.class,
        () -> testFacet.checkAttachAllowed(asset));

    // Verify error message includes enhanced text for ALLOW_ONCE
    assertThat(exception.getMessage(),
        containsString("as asset already exists and redeploy is not allowed"));
  }

  /**
   * Test that error message is NOT enhanced when ALLOW policy is used.
   */
  @Test
  void testCheckAttachAllowed_withAllowPolicy_noEnhancement() throws Exception {
    // Setup asset with existing blob
    Asset asset = mock(Asset.class);
    when(asset.hasBlob()).thenReturn(true);

    ContentFacetSupport testFacet = createFacetWithWritePolicy(WritePolicy.ALLOW);

    // Execute - should NOT throw exception because ALLOW policy allows updates
    assertDoesNotThrow(() -> testFacet.checkAttachAllowed(asset));
  }

  /**
   * Test that error message with DENY policy shows standard message.
   */
  @Test
  void testCheckAttachAllowed_withDenyPolicy_standardErrorMessage() throws Exception {
    // Setup asset with existing blob
    Asset asset = mock(Asset.class);
    when(asset.hasBlob()).thenReturn(true);
    when(asset.path()).thenReturn("/com/example/artifact/1.0.0/artifact-1.0.0.jar");
    when(repository.getName()).thenReturn("maven-releases");

    ContentFacetSupport testFacet = createFacetWithWritePolicy(WritePolicy.DENY);

    // Execute and verify exception
    RedeployDisabledException exception = assertThrows(RedeployDisabledException.class,
        () -> testFacet.checkAttachAllowed(asset));

    // Verify error message is standard (not enhanced) for DENY policy
    assertThat(exception.getMessage(), containsString("cannot be updated"));
  }

  /**
   * Test checkDeleteAllowed with DENY policy on asset that has a blob throws exception.
   */
  @Test
  void testCheckDeleteAllowed_denyPolicy_withBlob_throws() throws Exception {
    Asset asset = mock(Asset.class);
    when(asset.hasBlob()).thenReturn(true);
    when(asset.path()).thenReturn("/path/to/artifact.jar");
    when(repository.getName()).thenReturn("my-repo");

    ContentFacetSupport testFacet = createFacetWithWritePolicy(WritePolicy.DENY);

    IllegalOperationException exception =
        assertThrows(IllegalOperationException.class, () -> testFacet.checkDeleteAllowed(asset));

    assertThat(exception.getMessage(), containsString("my-repo"));
    assertThat(exception.getMessage(), containsString("/path/to/artifact.jar"));
    assertThat(exception.getMessage(), containsString("cannot be deleted"));
  }

  /**
   * Test checkDeleteAllowed with ALLOW policy on asset that has a blob succeeds.
   */
  @Test
  void testCheckDeleteAllowed_allowPolicy_withBlob_succeeds() throws Exception {
    Asset asset = mock(Asset.class);
    when(asset.hasBlob()).thenReturn(true);

    ContentFacetSupport testFacet = createFacetWithWritePolicy(WritePolicy.ALLOW);

    assertDoesNotThrow(() -> testFacet.checkDeleteAllowed(asset));
  }

  /**
   * Test checkDeleteAllowed with ALLOW_ONCE policy on asset that has a blob succeeds (deletes are allowed).
   */
  @Test
  void testCheckDeleteAllowed_allowOncePolicy_withBlob_succeeds() throws Exception {
    Asset asset = mock(Asset.class);
    when(asset.hasBlob()).thenReturn(true);

    ContentFacetSupport testFacet = createFacetWithWritePolicy(WritePolicy.ALLOW_ONCE);

    assertDoesNotThrow(() -> testFacet.checkDeleteAllowed(asset));
  }

  /**
   * Test checkDeleteAllowed on asset without a blob always succeeds regardless of write policy.
   */
  @Test
  void testCheckDeleteAllowed_noBlob_alwaysSucceeds() throws Exception {
    Asset asset = mock(Asset.class);
    when(asset.hasBlob()).thenReturn(false);

    // Even DENY policy should not throw for assets without blobs
    ContentFacetSupport testFacet = createFacetWithWritePolicy(WritePolicy.DENY);

    assertDoesNotThrow(() -> testFacet.checkDeleteAllowed(asset));
  }

  /**
   * Test openSession delegates to dataSessionSupplier with the configured datastore name.
   */
  @Test
  void testOpenSession_delegatesToDataSessionSupplier() throws Exception {
    configureFacet(WritePolicy.ALLOW);

    DataSessionSupplier sessionSupplier = mock();
    DataSession<?> session = mock();
    when(dependencies.getDataSessionSupplier()).thenReturn(sessionSupplier);
    doReturn(session).when(sessionSupplier).openSession("nexus");

    DataSession<?> result = underTest.openSession();

    assertThat(result, is(session));
    verify(sessionSupplier).openSession("nexus");
  }

  /**
   * Test clientInfo returns present Optional when ClientInfoProvider has info.
   */
  @Test
  void testClientInfo_returnsPresent() {
    ClientInfoProvider clientInfoProvider = mock();
    ClientInfo clientInfo = mock();
    when(dependencies.getClientInfoProvider()).thenReturn(clientInfoProvider);
    when(clientInfoProvider.getCurrentThreadClientInfo()).thenReturn(clientInfo);

    Optional<ClientInfo> result = underTest.clientInfo();

    assertThat(result.isPresent(), is(true));
    assertThat(result.get(), is(clientInfo));
  }

  /**
   * Test clientInfo returns empty Optional when no client info is available.
   */
  @Test
  void testClientInfo_returnsEmpty() {
    ClientInfoProvider clientInfoProvider = mock();
    when(dependencies.getClientInfoProvider()).thenReturn(clientInfoProvider);
    when(clientInfoProvider.getCurrentThreadClientInfo()).thenReturn(null);

    Optional<ClientInfo> result = underTest.clientInfo();

    assertThat(result.isPresent(), is(false));
  }

  /**
   * Test checkContentType delegates to assetBlobValidator with strict validation flag from config.
   */
  @Test
  void testCheckContentType_delegatesToValidator() throws Exception {
    configureFacet(WritePolicy.ALLOW);
    startFacet(1);

    Asset asset = mock(Asset.class);
    when(asset.path()).thenReturn("/test.jar");

    Blob blob = mock(Blob.class);
    when(blob.getHeaders()).thenReturn(Map.of("BlobStore.content-type", "application/java-archive"));

    AssetBlobValidators validators = mock();
    AssetBlobValidator validator = mock();
    when(dependencies.getAssetBlobValidators()).thenReturn(validators);
    when(validators.selectValidator(repository)).thenReturn(validator);
    when(validator.determineContentType(eq(true), any(), eq("/test.jar"), eq("application/java-archive")))
        .thenReturn("application/java-archive");

    // Re-start to set the validator
    EntityId configRepoId = mock(EntityId.class);
    when(repository.getConfiguration()).thenReturn(configuration);
    when(configuration.getRepositoryId()).thenReturn(configRepoId);
    ContentRepository existingRepo = mock(ContentRepository.class);
    when(existingRepo.contentRepositoryId()).thenReturn(1);
    when(contentRepositoryStore.readContentRepository(configRepoId)).thenReturn(Optional.of(existingRepo));
    underTest.doStart();

    String result = underTest.checkContentType(asset, blob);

    assertThat(result, is("application/java-archive"));
  }

  /**
   * Test contentRepository throws IllegalStateException when content repository is missing from store.
   */
  @Test
  void testContentRepository_throwsWhenMissing() throws Exception {
    configureFacet(WritePolicy.ALLOW);
    startFacet(1);

    // Now make the store return empty for the second call (contentRepository() method)
    EntityId configRepoId = underTest.configRepositoryId();
    when(contentRepositoryStore.readContentRepository(configRepoId)).thenReturn(Optional.empty());

    IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> underTest.attributes());

    assertThat(exception.getMessage(), containsString("Missing content repository"));
  }

  /**
   * Test Config toString for debugging output.
   */
  @Test
  void testConfigToString() {
    Config config = new Config();
    config.blobStoreName = "my-blob";
    config.dataStoreName = "my-data";
    config.writePolicy = WritePolicy.ALLOW_ONCE;
    config.strictContentTypeValidation = false;

    String result = config.toString();

    assertThat(result, containsString("my-blob"));
    assertThat(result, containsString("my-data"));
    assertThat(result, containsString("ALLOW_ONCE"));
    assertThat(result, containsString("false"));
  }

  /**
   * Test the error message for checkAttachAllowed includes both repo name and asset path.
   */
  @Test
  void testCheckAttachAllowed_errorMessageFormat() throws Exception {
    Asset asset = mock(Asset.class);
    when(asset.hasBlob()).thenReturn(false);
    when(asset.path()).thenReturn("/my/artifact.tar.gz");
    when(repository.getName()).thenReturn("my-hosted-repo");

    ContentFacetSupport testFacet = createFacetWithWritePolicy(WritePolicy.DENY);

    IllegalOperationException exception =
        assertThrows(IllegalOperationException.class,
            () -> testFacet.checkAttachAllowed(asset));

    // Error message should be: "my-hosted-repo/my/artifact.tar.gz is read-only"
    assertThat(exception.getMessage(), is("my-hosted-repo/my/artifact.tar.gz is read-only"));
  }

  /**
   * When a component is revived by re-upload, its stale "umbrella" trash entry is removed and each sibling
   * asset that is still soft-deleted (not part of the re-upload) is re-surfaced as its own entry; live assets
   * are left alone.
   */
  @Test
  void cleanupComponentTrashIndex_removesComponentEntry_andResurfacesSoftDeletedSiblingsOnly() throws Exception {
    configureFacet(WritePolicy.ALLOW);
    startFacet(77);
    setTrashIndexRecorder(trashIndexRecorder);

    when(repository.getName()).thenReturn("repo1");
    Format format = mock(Format.class);
    when(format.getValue()).thenReturn("raw");
    when(repository.getFormat()).thenReturn(format);

    ComponentData component = new ComponentData();
    component.setComponentId(50);

    // The component was in Trash: its umbrella entry is removed (1 row), which triggers the sibling scan.
    when(trashIndexRecorder.removeComponentByExternalId(eq("repo1"), any())).thenReturn(1);

    AssetData deletedSibling = new AssetData();
    deletedSibling.setAssetId(7);
    deletedSibling.setPath("/com/example/demo/1.0/demo-1.0.pom");
    deletedSibling.setKind("pom");
    deletedSibling.setDeletedOn(OffsetDateTime.now());
    deletedSibling.setDeletedBy("admin");

    AssetData liveAsset = new AssetData();
    liveAsset.setAssetId(8);
    liveAsset.setPath("/com/example/demo/1.0/demo-1.0.jar");

    when(assetStore.browseAllComponentAssets(component)).thenReturn(List.of(deletedSibling, liveAsset));

    underTest.cleanupComponentTrashIndex(component);

    String compExternalId = InternalIds.toExternalId(InternalIds.internalComponentId(component)).getValue();
    verify(trashIndexRecorder).removeComponentByExternalId("repo1", compExternalId);

    // Exactly one re-surface call (the still-soft-deleted sibling); the live asset is not re-surfaced.
    ArgumentCaptor<SoftDeletedAssetRef> captor = ArgumentCaptor.forClass(SoftDeletedAssetRef.class);
    verify(trashIndexRecorder).resurfaceSoftDeletedChildAsset(captor.capture());
    SoftDeletedAssetRef ref = captor.getValue();
    assertThat(ref.repositoryName(), is("repo1"));
    assertThat(ref.repositoryId(), is(77));
    assertThat(ref.format(), is("raw"));
    assertThat(ref.componentExternalId(), is(compExternalId));
    assertThat(ref.assetExternalId(),
        is(InternalIds.toExternalId(InternalIds.internalAssetId(deletedSibling)).getValue()));
    assertThat(ref.path(), is("/com/example/demo/1.0/demo-1.0.pom"));
    assertThat(ref.kind(), is("pom"));
    assertThat(ref.deletedBy(), is("admin"));
  }

  /**
   * A component that was in Trash but whose siblings are now all live (fully re-uploaded) clears its umbrella
   * entry but re-surfaces nothing.
   */
  @Test
  void cleanupComponentTrashIndex_liveOnlySiblings_removesComponentEntry_noResurface() throws Exception {
    configureFacet(WritePolicy.ALLOW);
    startFacet(77);
    setTrashIndexRecorder(trashIndexRecorder);

    when(repository.getName()).thenReturn("repo1");
    Format format = mock(Format.class);
    when(format.getValue()).thenReturn("raw");
    when(repository.getFormat()).thenReturn(format);

    ComponentData component = new ComponentData();
    component.setComponentId(50);

    // The component had an umbrella entry (1 row removed), so the sibling scan runs.
    when(trashIndexRecorder.removeComponentByExternalId(eq("repo1"), any())).thenReturn(1);

    AssetData liveAsset = new AssetData();
    liveAsset.setAssetId(8);
    liveAsset.setPath("/live");

    when(assetStore.browseAllComponentAssets(component)).thenReturn(List.of(liveAsset));

    underTest.cleanupComponentTrashIndex(component);

    verify(trashIndexRecorder).removeComponentByExternalId(eq("repo1"), any());
    verify(trashIndexRecorder, never()).resurfaceSoftDeletedChildAsset(any());
  }

  /**
   * With no trash recorder wired (soft-delete plugin absent / OSS build), the hook is a safe no-op.
   */
  @Test
  void cleanupComponentTrashIndex_nullRecorder_isNoOp() throws Exception {
    ComponentData component = new ComponentData();
    component.setComponentId(50);

    assertDoesNotThrow(() -> underTest.cleanupComponentTrashIndex(component));
  }

  /**
   * Regression: the reconciliation must run whenever the recorder is present, regardless of the current
   * {@code nexus.softDelete.enabled} state. A stale trash index row can be left by a period when the flag
   * was ON and then be silently revived by a re-upload while the flag is OFF; if this hook were gated on
   * the flag, that row would survive as an un-restorable ghost (component in Trash while live in the repo).
   */
  @Test
  void cleanupComponentTrashIndex_recorderPresent_removesEntry_evenAfterFlagToggledOff() throws Exception {
    configureFacet(WritePolicy.ALLOW);
    startFacet(77);
    setTrashIndexRecorder(trashIndexRecorder);

    when(repository.getName()).thenReturn("repo1");
    Format format = mock(Format.class);
    when(format.getValue()).thenReturn("raw");
    when(repository.getFormat()).thenReturn(format);

    ComponentData component = new ComponentData();
    component.setComponentId(50);

    // Stale umbrella entry present (1 row removed) -> reconciliation proceeds.
    when(trashIndexRecorder.removeComponentByExternalId(eq("repo1"), any())).thenReturn(1);

    AssetData liveAsset = new AssetData();
    liveAsset.setAssetId(8);
    liveAsset.setPath("/live");

    when(assetStore.browseAllComponentAssets(component)).thenReturn(List.of(liveAsset));

    underTest.cleanupComponentTrashIndex(component);

    // The stale umbrella entry is removed; no re-surface since the only sibling is live.
    verify(trashIndexRecorder).removeComponentByExternalId(eq("repo1"), any());
    verify(trashIndexRecorder, never()).resurfaceSoftDeletedChildAsset(any());
  }

  /**
   * Fast path (Option A): when the component had no trash entry (a brand-new or never-deleted upload -- the
   * common case), the umbrella delete removes 0 rows and the per-asset sibling scan is skipped entirely,
   * keeping the create/upload hot path cheap.
   */
  @Test
  void cleanupComponentTrashIndex_noEntryRemoved_skipsSiblingScan() throws Exception {
    configureFacet(WritePolicy.ALLOW);
    startFacet(77);
    setTrashIndexRecorder(trashIndexRecorder);

    when(repository.getName()).thenReturn("repo1");

    ComponentData component = new ComponentData();
    component.setComponentId(50);

    // No umbrella entry existed -> delete removes 0 rows (Mockito's default for the unstubbed int call).
    underTest.cleanupComponentTrashIndex(component);

    // The delete is always attempted (this is what fixes the toggle-ghost bug), but the sibling scan and any
    // re-surface are skipped because nothing was removed.
    verify(trashIndexRecorder).removeComponentByExternalId(eq("repo1"), any());
    verify(assetStore, never()).browseAllComponentAssets(any());
    verify(trashIndexRecorder, never()).resurfaceSoftDeletedChildAsset(any());
  }

  // --- Helper methods ---

  private void setTrashIndexRecorder(final TrashIndexRecorder recorder) throws Exception {
    java.lang.reflect.Field field = ContentFacetSupport.class.getDeclaredField("trashIndexRecorder");
    field.setAccessible(true);
    field.set(underTest, recorder);
  }

  /**
   * Configures the facet with the given write policy using mock ConfigurationFacet.
   */
  @Test
  void isGlobalDistributionEnabled_returnsFalseWhenAttributesNull() throws Exception {
    configureFacet(WritePolicy.ALLOW);
    // configuration.getAttributes() returns null (Mockito default)
    assertThat(underTest.isGlobalDistributionEnabled(), is(false));
  }

  @Test
  void isGlobalDistributionEnabled_readsBooleanTrueFromStorageGlobalDistribution() throws Exception {
    when(configuration.getAttributes())
        .thenReturn(Map.of(org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE,
            Map.of(org.sonatype.nexus.repository.config.ConfigurationConstants.GLOBAL_DISTRIBUTION_ENABLED,
                Boolean.TRUE)));
    configureFacet(WritePolicy.ALLOW);
    assertThat(underTest.isGlobalDistributionEnabled(), is(true));
  }

  @Test
  void isGlobalDistributionEnabled_readsStringTrueFromStorageGlobalDistribution() throws Exception {
    when(configuration.getAttributes())
        .thenReturn(Map.of(org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE,
            Map.of(org.sonatype.nexus.repository.config.ConfigurationConstants.GLOBAL_DISTRIBUTION_ENABLED, "true")));
    configureFacet(WritePolicy.ALLOW);
    assertThat(underTest.isGlobalDistributionEnabled(), is(true));
  }

  @Test
  void isGlobalDistributionEnabled_readsBooleanFalseFromStorageGlobalDistribution() throws Exception {
    when(configuration.getAttributes())
        .thenReturn(Map.of(org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE,
            Map.of(org.sonatype.nexus.repository.config.ConfigurationConstants.GLOBAL_DISTRIBUTION_ENABLED,
                Boolean.FALSE)));
    configureFacet(WritePolicy.ALLOW);
    assertThat(underTest.isGlobalDistributionEnabled(), is(false));
  }

  private void configureFacet(final WritePolicy writePolicy) throws Exception {
    ConfigurationFacet conf = mock();
    when(repository.facet(ConfigurationFacet.class)).thenReturn(conf);

    Config config = new Config();
    config.blobStoreName = "default";
    config.dataStoreName = "nexus";
    config.writePolicy = writePolicy;
    config.strictContentTypeValidation = true;
    when(conf.readSection(any(), any(), eq(Config.class))).thenReturn(config);

    underTest.doConfigure(configuration);
  }

  /**
   * Starts the facet by setting up config repository ID and content repository.
   */
  private void startFacet(final int contentRepoId) throws Exception {
    EntityId configRepoId = mock(EntityId.class);
    when(repository.getConfiguration()).thenReturn(configuration);
    when(configuration.getRepositoryId()).thenReturn(configRepoId);

    ContentRepository existingRepo = mock(ContentRepository.class);
    when(existingRepo.contentRepositoryId()).thenReturn(contentRepoId);
    when(contentRepositoryStore.readContentRepository(configRepoId)).thenReturn(Optional.of(existingRepo));

    AssetBlobValidators validators = mock();
    AssetBlobValidator validator = mock();
    when(dependencies.getAssetBlobValidators()).thenReturn(validators);
    when(validators.selectValidator(repository)).thenReturn(validator);

    underTest.doStart();
  }

  /**
   * Creates a ContentFacetSupport subclass with a fixed write policy for testing.
   */
  private ContentFacetSupport createFacetWithWritePolicy(final WritePolicy policy) throws Exception {
    ContentFacetSupport testFacet = new ContentFacetSupport(formatStoreManager)
    {
      @Override
      protected WritePolicy writePolicy(final Asset asset) {
        return policy;
      }
    };
    testFacet.setDependencies(dependencies);
    testFacet.attach(repository);
    return testFacet;
  }
}
