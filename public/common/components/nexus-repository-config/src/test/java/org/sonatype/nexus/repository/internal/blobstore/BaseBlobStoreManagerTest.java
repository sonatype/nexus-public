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
package org.sonatype.nexus.repository.internal.blobstore;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.blobstore.BlobStoreDescriptor;
import org.sonatype.nexus.blobstore.BlobSupport;
import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.api.DefaultBlobStoreProvider;
import org.sonatype.nexus.blobstore.api.tasks.BlobStoreTaskService;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.app.FreezeService;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.repository.blobstore.BlobStoreConfigurationStore;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.replication.ReplicationBlobStoreStatusManager;
import org.sonatype.nexus.security.UserIdHelper;

import jakarta.inject.Provider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.api.BlobStoreManager.DEFAULT_BLOBSTORE_NAME;

class BaseBlobStoreManagerTest
    extends Test5Support
{
  private static final String SECRET_FIELD_KEY = "secretAccessKey";

  private static final String SECRET_FIELD_VALUE = "secretAccessKeyValue";

  private static final String TEST_USER = "test-user";

  private static final String SECRET_ID = "_1";

  @Mock
  private EventManager eventManager;

  @Mock
  private BlobStoreConfigurationStore store;

  @Mock
  private BlobStoreDescriptor descriptor;

  @Mock
  private Provider<BlobStore> provider;

  @Mock
  private BlobStore blobStore;

  @Mock
  private FreezeService freezeService;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private NodeAccess nodeAccess;

  @Mock
  private ReplicationBlobStoreStatusManager replicationBlobStoreStatusManager;

  @Mock
  private BlobStoreTaskService blobStoreTaskService;

  @Mock
  private Provider<BlobStoreOverride> blobStoreOverrideProvider;

  @Mock
  private SecretsService secretsService;

  private MockedStatic<UserIdHelper> userIdHelperMockedStatic;

  private MockedStatic<QualifierUtil> qualifierUtilMockedStatic;

  @BeforeEach
  public void setup() throws Exception {
    lenient().when(provider.get()).thenReturn(blobStore);

    userIdHelperMockedStatic = mockStatic(UserIdHelper.class);
    qualifierUtilMockedStatic = mockStatic(QualifierUtil.class);
    userIdHelperMockedStatic.when(UserIdHelper::get).thenReturn(TEST_USER);
    lenient().when(store.newConfiguration()).thenReturn(new MockBlobStoreConfiguration());
  }

  @AfterEach
  public void destroy() throws Exception {
    userIdHelperMockedStatic.close();
    qualifierUtilMockedStatic.close();
  }

  @Test
  public void shouldNotCreateDefaultBlobStoreWhenProviderIsNull() throws Exception {
    newBlobStoreManager(false, null);

    verify(store, never()).create(any(BlobStoreConfiguration.class));
  }

  @Test
  public void canStartWithNothingConfigured() throws Exception {
    BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig);

    ArgumentCaptor<BlobStoreConfiguration> configurationArgumentCaptor = forClass(BlobStoreConfiguration.class);
    assertFalse(underTest.browse().iterator().hasNext());

    verify(store).create(configurationArgumentCaptor.capture());
    assertThat(configurationArgumentCaptor.getValue().getName(), is(DEFAULT_BLOBSTORE_NAME));
  }

  @Test
  public void canStartWithNothingConfiguredAndDoesNotCreateDefaultWhenClustered() throws Exception {
    when(nodeAccess.isClustered()).thenReturn(true);

    newBlobStoreManager(null, this::getBlobStoreConfig);

    verify(store, never()).create(any(BlobStoreConfiguration.class));
  }

  @Test
  public void canStartWithNothingConfiguredAndDoesCreateDefaultWhenClusteredIfProvisionDefaultsIsTrue() throws Exception {
    ArgumentCaptor<BlobStoreConfiguration> configurationArgumentCaptor = forClass(BlobStoreConfiguration.class);

    newBlobStoreManager(true, this::getBlobStoreConfig);

    verify(nodeAccess, never()).isClustered();
    verify(store).create(configurationArgumentCaptor.capture());
    assertThat(configurationArgumentCaptor.getValue().getName(), is(DEFAULT_BLOBSTORE_NAME));
  }

  @Test
  public void canSkipCreatingDefaultBlobstoreWhenNonClusteredIfProvisionDefaultsIsFalse() throws Exception {
    newBlobStoreManager(false, this::getBlobStoreConfig);

    verify(nodeAccess, never()).isClustered();
    verify(store, never()).create(any(BlobStoreConfiguration.class));
  }

  @Test
  public void canStartWithExistingConfiguration() throws Exception {
    BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig, createConfig("test"));

    assertThat(StreamSupport.stream(underTest.browse().spliterator(), false).toList(), is(List.of(blobStore)));
  }

  @Test
  public void nameCanBeDuplicateRegardlessOfCase() throws Exception {
    BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig, createConfig("test"));

    assertFalse(underTest.exists("unique"));
    assertTrue(underTest.exists("test"));
    assertTrue(underTest.exists("TEST"));
  }

  @Test
  public void canCreateABlobStore() throws Exception {
    BlobStoreConfiguration configuration = createConfig("test");
    BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig);

    BlobStore createdBlobStore = underTest.create(configuration);

    assertThat(createdBlobStore, is(blobStore));
    verify(store).create(configuration);
    verify(blobStore).start();

    assertThat(StreamSupport.stream(underTest.browse().spliterator(), false).toList(), is(List.of(blobStore)));
    assertThat(underTest.get("test"), is(blobStore));
  }

  @Test
  public void canCreateBlobStoreAndEncryptSensitiveValues() throws Exception {
    when(descriptor.getSensitiveConfigurationFields()).thenReturn(List.of(SECRET_FIELD_KEY));
    when(provider.get()).thenReturn(blobStore);
    Secret secret = mock(Secret.class);
    when(secret.getId()).thenReturn(SECRET_ID);
    when(
        secretsService.encryptMaven(BaseBlobStoreManager.BLOBSTORE_CONFIG, SECRET_FIELD_VALUE.toCharArray(), TEST_USER))
            .thenReturn(secret);
    Map<String, Map<String, Object>> blobStoreAttributes = new HashMap<>();
    Map<String, Object> blobConfigMap = new HashMap<>();
    blobConfigMap.put(SECRET_FIELD_KEY, SECRET_FIELD_VALUE);
    blobStoreAttributes.put("test", blobConfigMap);
    blobStoreAttributes.put("file", Map.of("path", "foo"));
    BlobStoreConfiguration configuration = createConfig("test", blobStoreAttributes);

    BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig);
    BlobStore createdBlobStore = underTest.create(configuration);

    assertThat(configuration.getAttributes().get("test").get(SECRET_FIELD_KEY), is(SECRET_ID));
    assertThat(createdBlobStore, is(createdBlobStore));
    verify(secretsService).encryptMaven(BaseBlobStoreManager.BLOBSTORE_CONFIG, SECRET_FIELD_VALUE.toCharArray(),
        TEST_USER);
    verify(store).create(configuration);
    verify(blobStore).start();
  }

  @Test
  public void canDeleteAnExistingBlobStore() throws Exception {
    when(descriptor.getSensitiveConfigurationFields()).thenReturn(List.of(SECRET_FIELD_KEY));
    Map<String, Map<String, Object>> blobStoreAttributes = new HashMap<>();
    Map<String, Object> blobConfigMap = new HashMap<>();
    blobConfigMap.put(SECRET_FIELD_KEY, SECRET_ID);
    blobStoreAttributes.put("test", blobConfigMap);
    blobStoreAttributes.put("file", Map.of("path", "foo"));
    BlobStoreConfiguration configuration = createConfig("test", blobStoreAttributes);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(configuration);

    BaseBlobStoreManager underTest = spy(newBlobStoreManager(true, this::getBlobStoreConfig, configuration));

    doReturn(blobStore).when(underTest).blobStore("test");
    Secret secret = mock(Secret.class);
    when(secretsService.from(SECRET_ID)).thenReturn(secret);

    underTest.delete(configuration.getName());

    verify(blobStore).shutdown();
    verify(store).delete(configuration);
    verify(secretsService).remove(secret);
    verify(freezeService).checkWritable("Unable to delete a BlobStore while database is frozen.");
  }

  @Test
  public void canDeleteAnExistingBlobStoreInFailedState() throws Exception {
    BlobStoreConfiguration configuration = createConfig("test");
    BaseBlobStoreManager underTest = spy(newBlobStoreManager(true, this::getBlobStoreConfig, configuration));

    doReturn(blobStore).when(underTest).blobStore("test");
    when(blobStore.getBlobStoreConfiguration()).thenReturn(configuration);

    underTest.delete(configuration.getName());

    verify(blobStore).shutdown();
    verify(store).delete(configuration);
    verify(freezeService).checkWritable("Unable to delete a BlobStore while database is frozen.");
    verify(blobStore, never()).stop();
  }

  @Test
  public void canDeleteAnExistingBlobStoreThatFailsOnRemove() throws Exception {
    BlobStoreConfiguration configuration = createConfig("test");
    BaseBlobStoreManager underTest = spy(newBlobStoreManager(true, this::getBlobStoreConfig, configuration));
    doReturn(blobStore).when(underTest).blobStore("test");
    when(blobStore.getBlobStoreConfiguration()).thenReturn(configuration);
    doThrow(BlobStoreException.class).when(blobStore).remove();

    underTest.delete(configuration.getName());

    verify(blobStore).shutdown();
    verify(blobStore).remove();
    verify(store).delete(configuration);
    verify(freezeService).checkWritable("Unable to delete a BlobStore while database is frozen.");
  }

  @Test
  public void canNotDeleteAnExistingBlobStoreUsedInAMoveTask() throws Exception {
    BlobStoreConfiguration configuration = createConfig("test");
    when(blobStoreTaskService.isAnyTaskInUseForBlobStore("test")).thenReturn(true);
    BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig, configuration);

    assertThrows(IllegalStateException.class, () -> underTest.delete("test"));
    verify(blobStore, never()).stop();
  }

  @Test
  public void allBlobStoresAreStoppedWithTheManagerIsStopped() throws Exception {
    BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig);

    BlobStoreConfiguration configuration = createConfig("test");
    underTest.create(configuration);

    underTest.stop();

    verify(blobStore).stop();
  }

  @Test
  public void blobStoreNotCreatedForInvalidConfiguration() throws Exception {
    when(provider.get()).thenThrow(new IllegalArgumentException());

    BlobStoreConfiguration configuration = createConfig("test");
    BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig, configuration);

    try {
      underTest.create(configuration);
      fail();
    }
    catch (Exception e) {
      // expected
    }

    assertFalse(underTest.browse().iterator().hasNext());
  }

  @Test
  public void canSuccessfullyCreateNewBlobStoresConcurrently() throws Exception {
    BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig);

    underTest.create(createConfig("concurrency-test-1"));
    underTest.create(createConfig("concurrency-test-2"));

    Iterator<Entry<String, BlobStore>> storesIterator = underTest.getByName().entrySet().iterator();
    storesIterator.next();

    underTest.create(createConfig("concurrency-test-3"));
    storesIterator.next();
  }

  @Test
  public void inUseBlobstoreCannotBeDeleted() throws Exception {
    BlobStore used = mock(BlobStore.class);
    BlobStore unused = mock(BlobStore.class);
    when(used.getBlobStoreConfiguration()).thenReturn(createConfig("used"));
    when(unused.getBlobStoreConfiguration()).thenReturn(createConfig("unused"));
    BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig,
        used.getBlobStoreConfiguration(), unused.getBlobStoreConfiguration());
    underTest.track("used", used);
    underTest.track("unused", unused);
    when(repositoryManager.isBlobstoreUsed("used")).thenReturn(true);
    when(repositoryManager.isBlobstoreUsed("unused")).thenReturn(false);

    underTest.delete("unused");
    assertThrows(BlobStoreException.class, () -> underTest.delete("used"));
    verify(unused).remove();
    verify(used, never()).remove();
  }

  @Test
  public void itIsConvertableWhenTheStoreFindsNoParentsAndTheBlobStoreIsGroupable() throws Exception {
    String blobStoreName = "child";
    BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig);
    underTest.track(blobStoreName, blobStore);
    when(blobStore.isGroupable()).thenReturn(true);
    when(blobStore.isWritable()).thenReturn(true);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(new MockBlobStoreConfiguration(blobStoreName, "test"));
    when(store.findParent(blobStoreName)).thenReturn(Optional.empty());
    assertTrue(underTest.isConvertable(blobStoreName));
  }

  @Test
  public void itIsNotConvertableWhenTheStoreFindsParents() throws Exception {
    String blobStoreName = "child";
    when(blobStore.isGroupable()).thenReturn(true);
    when(blobStore.isWritable()).thenReturn(true);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(new MockBlobStoreConfiguration(blobStoreName, "test"));
    when(store.findParent(blobStoreName)).thenReturn(Optional.of(new MockBlobStoreConfiguration()));

    BaseBlobStoreManager underTest =
        newBlobStoreManager(true, this::getBlobStoreConfig, blobStore.getBlobStoreConfiguration());
    assertFalse(underTest.isConvertable(blobStoreName));

    verify(store).findParent(blobStoreName);
  }

  @Test
  public void itIsNotConvertableWhenTheStoreIsNotGroupable() throws Exception {
    String blobStoreName = "child";
    when(blobStore.isGroupable()).thenReturn(false);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(new MockBlobStoreConfiguration(blobStoreName, "test"));

    BaseBlobStoreManager underTest =
        newBlobStoreManager(true, this::getBlobStoreConfig, blobStore.getBlobStoreConfiguration());
    assertFalse(underTest.isConvertable(blobStoreName));
    verify(store, never()).findParent(any());
  }

  @Test
  public void itIsNotConvertableWhenTheStoreIsInUseByATask() throws Exception {
    String blobStoreName = "child";
    BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig);

    underTest.track(blobStoreName, blobStore);
    when(blobStoreTaskService.isAnyTaskInUseForBlobStore("child")).thenReturn(true);
    when(blobStore.isGroupable()).thenReturn(true);
    when(blobStore.isWritable()).thenReturn(true);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(new MockBlobStoreConfiguration(blobStoreName, "test"));
    when(store.findParent(blobStoreName)).thenReturn(Optional.empty());
    assertFalse(underTest.isConvertable(blobStoreName));
  }

  @Test
  public void canStartWhenABlobStoreFailsToRestore() throws Exception {
    doThrow(new IllegalStateException()).when(blobStore).init(any(BlobStoreConfiguration.class));
    when(provider.get()).thenReturn(blobStore);

    BlobStoreConfiguration configuration = createConfig("test");
    BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig, configuration);

    assertThat("blob store manager should still track blob stores that failed on startup", underTest.get("test"),
        notNullValue());
  }

  @Test
  public void canStartWhenABlobStoreFailsToStart() throws Exception {
    doThrow(new IllegalStateException()).when(blobStore).start();

    BlobStoreConfiguration configuration = createConfig("test");
    BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig, configuration);

    // assert underTest.browse().toList().equals(List.of(blobStore));
    assertThat(StreamSupport.stream(underTest.browse().spliterator(), false).toList(), is(List.of(blobStore)));
  }

  @Test
  public void canUpdateBlobStoreFromNewConfig() throws Exception {
    when(descriptor.getSensitiveConfigurationFields()).thenReturn(List.of(SECRET_FIELD_KEY));
    Map<String, Map<String, Object>> oldBlobStoreAttributes = new HashMap<>();
    Map<String, Object> oldBlobConfigMap = new HashMap<>();
    oldBlobConfigMap.put(SECRET_FIELD_KEY, SECRET_ID);
    oldBlobStoreAttributes.put("test", oldBlobConfigMap);
    oldBlobStoreAttributes.put("file", Map.of("path", "foo"));
    BlobStoreConfiguration oldBlobStoreConfig = createConfig("test", oldBlobStoreAttributes);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(oldBlobStoreConfig);
    Secret oldSecret = mock(Secret.class);
    when(secretsService.from(SECRET_ID)).thenReturn(oldSecret);

    Secret newSecret = mock(Secret.class);
    when(newSecret.getId()).thenReturn("_2");
    Map<String, Map<String, Object>> updatedBlobStoreAttributes = new HashMap<>();
    Map<String, Object> newBlobConfigMap = new HashMap<>();
    newBlobConfigMap.put(SECRET_FIELD_KEY, SECRET_FIELD_VALUE);
    updatedBlobStoreAttributes.put("test", newBlobConfigMap);
    updatedBlobStoreAttributes.put("file", Map.of("path", "foo"));
    BlobStoreConfiguration newBlobStoreConfig = createConfig("test", updatedBlobStoreAttributes);
    when(secretsService.encryptMaven(BaseBlobStoreManager.BLOBSTORE_CONFIG, SECRET_FIELD_VALUE.toCharArray(),
        TEST_USER)).thenReturn(newSecret);

    BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig);

    underTest.track("test", blobStore);

    underTest.update(newBlobStoreConfig);

    verify(secretsService).remove(oldSecret);
    verify(secretsService).encryptMaven(BaseBlobStoreManager.BLOBSTORE_CONFIG, SECRET_FIELD_VALUE.toCharArray(),
        TEST_USER);
    verify(store).update(newBlobStoreConfig);
    verify(store, never()).update(oldBlobStoreConfig);
  }

  @Test
  public void cannotUpdateBlobStoreFromNewConfig() throws Exception {
    when(descriptor.getSensitiveConfigurationFields()).thenReturn(List.of(SECRET_FIELD_KEY));
    Map<String, Map<String, Object>> oldBlobStoreAttributes = new HashMap<>();
    Map<String, Object> oldBlobConfigMap = new HashMap<>();
    oldBlobConfigMap.put(SECRET_FIELD_KEY, SECRET_FIELD_VALUE);
    oldBlobStoreAttributes.put("test", oldBlobConfigMap);
    oldBlobStoreAttributes.put("file", Map.of("path", "foo"));
    BlobStoreConfiguration oldBlobStoreConfig = createConfig("test", oldBlobStoreAttributes);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(oldBlobStoreConfig);
    BlobId blobId = new BlobId("testBlobId", OffsetDateTime.now());
    Secret oldSecret = mock(Secret.class);
    when(secretsService.from(SECRET_FIELD_VALUE)).thenReturn(oldSecret);
    when(oldSecret.decrypt()).thenReturn(SECRET_FIELD_VALUE.toCharArray());

    Secret newSecret = mock(Secret.class);
    when(newSecret.getId()).thenReturn("_2");
    Map<String, Map<String, Object>> updatedBlobStoreAttributes = new HashMap<>();
    Map<String, Object> newBlobConfigMap = new HashMap<>();
    newBlobConfigMap.put(SECRET_FIELD_KEY, SECRET_FIELD_VALUE);
    updatedBlobStoreAttributes.put("test", newBlobConfigMap);
    updatedBlobStoreAttributes.put("file", Map.of("path", "foo"));
    BlobStoreConfiguration newBlobStoreConfig = createConfig("test", updatedBlobStoreAttributes);
    when(secretsService.encryptMaven(BaseBlobStoreManager.BLOBSTORE_CONFIG, SECRET_FIELD_VALUE.toCharArray(),
        TEST_USER)).thenReturn(newSecret);

    doThrow(new BlobStoreException("Cannot start blobstore with new config", blobId)).when(blobStore).start();

    BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig, oldBlobStoreConfig);
    when(blobStore.isStarted()).thenReturn(true);

    assertThrows(BlobStoreException.class, () -> underTest.update(newBlobStoreConfig));

    verify(store).update(newBlobStoreConfig);
    verify(secretsService).encryptMaven(BaseBlobStoreManager.BLOBSTORE_CONFIG, SECRET_FIELD_VALUE.toCharArray(),
        TEST_USER);
  }


  @Test
  public void moveBlobMarksDestinationAsStaleWhenSoftDeleted() throws Exception {
    MoveBlobTestContext context = setupMoveBlobTest(true, false);
    context.executeMove();

    // Blob is marked as stale because it is soft-deleted in source
    verify(context.destBlob()).markStale();
    verify(context.destBlobStore()).create(any(InputStream.class), any(Map.class), eq(context.blobId()));
    verify(context.destBlobStore()).setBlobAttributes(eq(context.newBlobId()), any(BlobAttributes.class));
    verify(context.srcBlobStore()).deleteHard(context.blobId());
  }

  @Test
  public void moveBlobDoesNotMarkAsStaleWhenNotDeleted() throws Exception {
    MoveBlobTestContext context = setupMoveBlobTest(false, false);
    context.executeMove();

    // Blob is never marked as stale because it is not soft-deleted
    verify(context.destBlob(), never()).markStale();
    verify(context.destBlobStore()).create(any(InputStream.class), any(Map.class), eq(context.blobId()));
    verify(context.destBlobStore()).setBlobAttributes(eq(context.newBlobId()), any(BlobAttributes.class));
    verify(context.srcBlobStore()).deleteHard(context.blobId());
  }

  @Test
  public void moveBlobHandlesBlobStoreExceptionWhenMarkingStale() throws Exception {
    MoveBlobTestContext context = setupMoveBlobTest(true, true);
    context.executeMove();

    // Exception was thrown while marking stale, but move continued
    verify(context.destBlob(), never()).markStale();
    verify(context.destBlobStore()).create(any(InputStream.class), any(Map.class), eq(context.blobId()));
    verify(context.destBlobStore()).setBlobAttributes(eq(context.newBlobId()), any(BlobAttributes.class));
    verify(context.srcBlobStore()).deleteHard(context.blobId());
  }


  @SuppressWarnings({"unchecked", "rawtypes"})
  private BaseBlobStoreManager newBlobStoreManager(
      final Boolean provisionDefaults,
      final DefaultBlobStoreProvider blobStoreConfigProvider,
      final BlobStoreConfiguration... configurations) throws Exception
  {
    Map<String, BlobStoreDescriptor> descriptors = Map.of("test", descriptor, "File", descriptor);
    Map<String, Provider<BlobStore>> providers = Map.of("test", provider, "File", provider);

    when(QualifierUtil.buildQualifierBeanMap(any())).thenReturn((Map) descriptors, (Map) providers);

    when(store.list()).thenReturn(List.of(configurations));

    BaseBlobStoreManager bbsm = new BaseBlobStoreManager(eventManager, store, List.of(), List.of(),
        freezeService, () -> repositoryManager, nodeAccess, provisionDefaults, blobStoreConfigProvider,
        blobStoreTaskService, blobStoreOverrideProvider, replicationBlobStoreStatusManager, secretsService)
    {
    };
    bbsm.start();

    return bbsm;
  }

  private static BlobStoreConfiguration createConfig(final String name) {
    Map<String, Map<String, Object>> fileAttributes = new HashMap<>();
    fileAttributes.put("file", Map.of("path", "baz"));
    return createConfig(name, fileAttributes);
  }

  private static BlobStoreConfiguration createConfig(
      final String name,
      final Map<String, Map<String, Object>> fileAttributes)
  {
    MockBlobStoreConfiguration config = new MockBlobStoreConfiguration();
    config.setName(name);
    config.setType("test");
    config.setAttributes(fileAttributes);
    return config;
  }

  public BlobStoreConfiguration getBlobStoreConfig(final Supplier<BlobStoreConfiguration> configurationSupplier) {
    final BlobStoreConfiguration configuration = configurationSupplier.get();
    configuration.setName(DEFAULT_BLOBSTORE_NAME);
    configuration.setType(FileBlobStore.TYPE);
    return configuration;
  }

  private MoveBlobTestContext setupMoveBlobTest(
      boolean srcDeleted,
      boolean throwOnGet) throws Exception
  {
    BaseBlobStoreManager manager = newBlobStoreManager(true, this::getBlobStoreConfig);
    BlobStore srcBlobStore = mock(BlobStore.class);
    BlobStore destBlobStore = mock(BlobStore.class);
    BlobId blobId = new BlobId("test-blob");
    BlobId newBlobId = new BlobId("test-blob-new");

    BlobStoreConfiguration srcConfig = mock(BlobStoreConfiguration.class);
    when(srcConfig.getName()).thenReturn("source-blobstore");
    when(srcBlobStore.getBlobStoreConfiguration()).thenReturn(srcConfig);

    BlobStoreConfiguration destConfig = mock(BlobStoreConfiguration.class);
    when(destConfig.getName()).thenReturn("dest-blobstore");
    when(destBlobStore.getBlobStoreConfiguration()).thenReturn(destConfig);

    BlobAttributes srcAttributes = mock(BlobAttributes.class);
    when(srcAttributes.isDeleted()).thenReturn(srcDeleted);
    when(srcAttributes.getHeaders()).thenReturn(Map.of());
    when(srcBlobStore.getBlobAttributes(blobId)).thenReturn(srcAttributes);

    BlobAttributes destAttributes = mock(BlobAttributes.class);
    when(destAttributes.isDeleted()).thenReturn(srcDeleted);
    when(destBlobStore.getBlobAttributes(newBlobId)).thenReturn(destAttributes);

    BlobSupport destBlob = mock(BlobSupport.class);
    if (throwOnGet) {
      lenient().when(destBlobStore.get(newBlobId, true)).thenThrow(new BlobStoreException("Test error", newBlobId));
    }
    else {
      lenient().when(destBlobStore.get(newBlobId, true)).thenReturn(destBlob);
    }

    Blob srcBlob = mock(Blob.class);
    when(srcBlobStore.isInternalMoveSupported(destBlobStore)).thenReturn(false);
    when(srcBlobStore.get(blobId, true)).thenReturn(srcBlob);
    when(srcBlob.getInputStream()).thenReturn(mock(InputStream.class));

    Blob newBlob = mock(Blob.class);
    when(newBlob.getId()).thenReturn(newBlobId);
    when(destBlobStore.create(any(InputStream.class), any(Map.class), any(BlobId.class))).thenReturn(newBlob);

    return new MoveBlobTestContext(manager, srcBlobStore, destBlobStore, blobId, newBlobId, destBlob);
  }

  private record MoveBlobTestContext(
      BaseBlobStoreManager manager,
      BlobStore srcBlobStore,
      BlobStore destBlobStore,
      BlobId blobId,
      BlobId newBlobId,
      BlobSupport destBlob)
  {
    void executeMove() {
      manager.moveBlob(blobId, srcBlobStore, destBlobStore);
    }
  }
}
