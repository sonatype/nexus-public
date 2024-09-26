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

import javax.inject.Provider;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.BlobStoreDescriptor;
import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.api.tasks.BlobStoreTaskService;
import org.sonatype.nexus.common.app.FreezeService;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.stateguard.InvalidStateException;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.repository.blobstore.BlobStoreConfigurationStore;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.replication.ReplicationBlobStoreStatusManager;
import org.sonatype.nexus.security.UserIdHelper;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.sonatype.nexus.blobstore.api.BlobStoreManager.DEFAULT_BLOBSTORE_NAME;

public class BlobStoreManagerImplTest
    extends TestSupport
{
  private static final String SECRET_FIELD_KEY = "secretAccessKey";

  private static final String SECRET_FIELD_VALUE = "secretAccessKeyValue";

  private static final String TEST_USER = "test-user";

  private static final String SECRET_ID = "_1";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock
  private EventManager eventManager;

  @Mock
  private BlobStoreConfigurationStore store;

  @Mock
  private BlobStoreDescriptor descriptor;

  @Mock
  private Provider<BlobStore> provider;

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

  BlobStoreManagerImpl underTest;

  @Before
  public void setup() {
    userIdHelperMockedStatic = mockStatic(UserIdHelper.class);
    userIdHelperMockedStatic.when(UserIdHelper::get).thenReturn(TEST_USER);
    when(store.newConfiguration()).thenReturn(new MockBlobStoreConfiguration());
    underTest = newBlobStoreManager(false);
  }

  @After
  public void destroy() throws Exception {
    userIdHelperMockedStatic.close();
  }

  private BlobStoreManagerImpl newBlobStoreManager(Boolean provisionDefaults) {
    Map<String, BlobStoreDescriptor> descriptors = new HashMap<>();
    descriptors.put("test", descriptor);
    descriptors.put("File", descriptor);
    Map<String, Provider<BlobStore>> providers = new HashMap<>();
    providers.put("test", provider);
    providers.put("File", provider);
    return spy(new BlobStoreManagerImpl(eventManager, store,
        descriptors,
        providers,
        freezeService, () -> repositoryManager,
        nodeAccess, provisionDefaults,
        new DefaultFileBlobStoreProvider(),
        blobStoreTaskService,
        blobStoreOverrideProvider,
        replicationBlobStoreStatusManager,
        secretsService));
  }

  @Test
  public void canStartWithNothingConfigured() throws Exception {
    underTest = newBlobStoreManager(true);

    ArgumentCaptor<BlobStoreConfiguration> configurationArgumentCaptor = forClass(BlobStoreConfiguration.class);
    when(store.list()).thenReturn(Collections.emptyList());
    underTest.doStart();
    assert !underTest.browse().iterator().hasNext();

    verify(store).create(configurationArgumentCaptor.capture());
    assertThat(configurationArgumentCaptor.getValue().getName(), is(DEFAULT_BLOBSTORE_NAME));
  }

  @Test
  public void canStartWithNothingConfiguredAndDoesNotCreateDefaultWhenClustered() throws Exception {
    when(nodeAccess.isClustered()).thenReturn(true);
    when(store.list()).thenReturn(Collections.emptyList());
    underTest.doStart();

    verify(store, times(0)).create(any(BlobStoreConfiguration.class));
  }

  @Test
  public void canStartWithNothingConfiguredAndDoesCreateDefaultWhenClusteredIfProvisionDefaultsIsTrue()
      throws Exception
  {
    underTest = newBlobStoreManager(true);

    ArgumentCaptor<BlobStoreConfiguration> configurationArgumentCaptor = forClass(BlobStoreConfiguration.class);
    when(nodeAccess.isClustered()).thenReturn(true);

    when(store.list()).thenReturn(Collections.emptyList());
    underTest.doStart();

    verify(store).create(configurationArgumentCaptor.capture());
    assertThat(configurationArgumentCaptor.getValue().getName(), is(DEFAULT_BLOBSTORE_NAME));
  }

  @Test
  public void canSkipCreatingDefaultBlobstoreWhenNonClusteredIfProvisionDefaultsIsFalse() throws Exception {
    underTest = newBlobStoreManager(false);

    when(nodeAccess.isClustered()).thenReturn(false);
    when(store.list()).thenReturn(Collections.emptyList());
    underTest.doStart();

    verify(store, times(0)).create(any(BlobStoreConfiguration.class));
  }

  @Test
  public void canStartWithExistingConfiguration() throws Exception {
    BlobStore blobStore = mock(BlobStore.class);
    when(provider.get()).thenReturn(blobStore);
    when(store.list()).thenReturn(Collections.singletonList(createConfig("test")));

    underTest.doStart();

    //assert underTest.browse().toList().equals(List.of(blobStore));

    assert StreamSupport.stream(underTest.browse().spliterator(), false)
        .collect(Collectors.toList())
        .equals(Collections.singletonList(blobStore));
  }

  @Test
  public void nameCanBeDuplicateRegardlessOfCase() throws Exception {
    BlobStore blobStore = mock(BlobStore.class);
    when(provider.get()).thenReturn(blobStore);
    when(store.list()).thenReturn(Collections.singletonList(createConfig("test")));

    underTest.doStart();

    assert !underTest.exists("unique");
    assert underTest.exists("test");
    assert underTest.exists("TEST");
  }

  @Test
  public void canCreateABlobStore() throws Exception {
    BlobStore blobStore = mock(BlobStore.class);
    when(provider.get()).thenReturn(blobStore);
    BlobStoreConfiguration configuration = createConfig("test");

    BlobStore createdBlobStore = underTest.create(configuration);

    assert createdBlobStore == blobStore;
    verify(store).create(configuration);
    verify(blobStore).start();

    assert StreamSupport.stream(underTest.browse().spliterator(), false)
        .collect(Collectors.toList())
        .equals(Collections.singletonList(blobStore));
    assert underTest.get("test") == blobStore;
  }

  @Test
  public void canCreateBlobStoreAndEncryptSensitiveValues() throws Exception {
    when(descriptor.getSensitiveConfigurationFields()).thenReturn(Collections.singletonList(SECRET_FIELD_KEY));
    BlobStore blobStore = mock(BlobStore.class);
    when(provider.get()).thenReturn(blobStore);
    Secret secret = mock(Secret.class);
    when(secret.getId()).thenReturn(SECRET_ID);
    when(secretsService.encryptMaven(BlobStoreManagerImpl.BLOBSTORE_CONFIG, SECRET_FIELD_VALUE.toCharArray(), TEST_USER))
        .thenReturn(secret);
    Map<String, Map<String, Object>> blobStoreAttributes = new HashMap<>();
    Map<String, Object> blobConfigMap = new HashMap<>();
    blobConfigMap.put(SECRET_FIELD_KEY, SECRET_FIELD_VALUE);
    blobStoreAttributes.put("test", blobConfigMap);
    blobStoreAttributes.put("file", Collections.singletonMap("path", "foo"));
    BlobStoreConfiguration configuration = createConfig("test", blobStoreAttributes);

    BlobStore createdBlobStore = underTest.create(configuration);

    assertThat(configuration.getAttributes().get("test").get(SECRET_FIELD_KEY), is(SECRET_ID));
    assertThat(createdBlobStore, is(createdBlobStore));
    verify(secretsService).encryptMaven(BlobStoreManagerImpl.BLOBSTORE_CONFIG, SECRET_FIELD_VALUE.toCharArray(),
        TEST_USER);
    verify(store).create(configuration);
    verify(blobStore).start();
  }

  @Test
  public void canDeleteAnExistingBlobStore() throws Exception {
    when(descriptor.getSensitiveConfigurationFields()).thenReturn(Collections.singletonList(SECRET_FIELD_KEY));
    BlobStore blobStore = mock(BlobStore.class);
    Map<String, Map<String, Object>> blobStoreAttributes = new HashMap<>();
    Map<String, Object> blobConfigMap = new HashMap<>();
    blobConfigMap.put(SECRET_FIELD_KEY, SECRET_ID);
    blobStoreAttributes.put("test", blobConfigMap);
    blobStoreAttributes.put("file", Collections.singletonMap("path", "foo"));
    BlobStoreConfiguration configuration = createConfig("test", blobStoreAttributes);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(configuration);
    doReturn(blobStore).when(underTest).blobStore("test");
    when(store.list()).thenReturn(Collections.singletonList(configuration));
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
    BlobStore blobStore = mock(BlobStore.class);
    doReturn(blobStore).when(underTest).blobStore("test");
    doThrow(InvalidStateException.class).when(blobStore).stop();
    when(store.list()).thenReturn(Collections.singletonList(configuration));
    when(blobStore.getBlobStoreConfiguration()).thenReturn(configuration);

    underTest.delete(configuration.getName());

    verify(blobStore).shutdown();
    verify(store).delete(configuration);
    verify(freezeService).checkWritable("Unable to delete a BlobStore while database is frozen.");
  }

  @Test
  public void canDeleteAnExistingBlobStoreThatFailsOnRemove() throws Exception {
    BlobStoreConfiguration configuration = createConfig("test");
    BlobStore blobStore = mock(BlobStore.class);
    doReturn(blobStore).when(underTest).blobStore("test");
    when(store.list()).thenReturn(Collections.singletonList(configuration));
    when(blobStore.getBlobStoreConfiguration()).thenReturn(configuration);
    doThrow(BlobStoreException.class).when(blobStore).remove();

    underTest.delete(configuration.getName());

    verify(blobStore).shutdown();
    verify(blobStore).remove();
    verify(store).delete(configuration);
    verify(freezeService).checkWritable("Unable to delete a BlobStore while database is frozen.");
  }

  @Test(expected = IllegalStateException.class)
  public void canNotDeleteAnExistingBlobStoreUsedInAMoveTask() throws Exception {
    underTest.doStart();

    BlobStoreConfiguration configuration = createConfig("test");
    BlobStore blobStore = mock(BlobStore.class);
    doReturn(blobStore).when(underTest).blobStore("test");
    doThrow(InvalidStateException.class).when(blobStore).stop();
    when(store.list()).thenReturn(Collections.singletonList(configuration));
    when(blobStore.getBlobStoreConfiguration()).thenReturn(configuration);
    when(blobStoreTaskService.isAnyTaskInUseForBlobStore("test")).thenReturn(true);

    underTest.delete(configuration.getName());
  }

  @Test
  public void allBlobStoresAreStoppedWithTheManagerIsStopped() throws Exception {
    BlobStore blobStore = mock(BlobStore.class);
    when(provider.get()).thenReturn(blobStore);
    BlobStoreConfiguration configuration = createConfig("test");
    underTest.create(configuration);

    underTest.stop();

    verify(blobStore).stop();
  }

  @Test
  public void blobStoreNotCreatedForInvalidConfiguration() {
    when(provider.get()).thenThrow(new IllegalArgumentException());

    BlobStoreConfiguration configuration = createConfig("test");

    try {
      underTest.create(configuration);
      fail();
    }
    catch (Exception e) {
      // expected
    }

    assert !underTest.browse().iterator().hasNext();
  }

  @Test
  public void canSuccessfullyCreateNewBlobStoresConcurrently() throws Exception {
    Map<String, BlobStoreDescriptor> descriptors = new HashMap<>();
    descriptors.put("test", descriptor);
    descriptors.put("File", descriptor);
    Map<String, Provider<BlobStore>> providers = new HashMap<>();
    providers.put("test", provider);
    providers.put("File", provider);
    underTest = new BlobStoreManagerImpl(eventManager, store,
        descriptors,
        providers,
        freezeService, () -> repositoryManager,
        nodeAccess, true,
        new DefaultFileBlobStoreProvider(),
        blobStoreTaskService,
        blobStoreOverrideProvider,
        replicationBlobStoreStatusManager,
        secretsService);

    BlobStore blobStore = mock(BlobStore.class);
    when(provider.get()).thenReturn(blobStore);

    underTest.create(createConfig("concurrency-test-1"));
    underTest.create(createConfig("concurrency-test-2"));

    Iterator<Entry<String, BlobStore>> storesIterator = underTest.getByName().entrySet().iterator();
    storesIterator.next();

    underTest.create(createConfig("concurrency-test-3"));
    storesIterator.next();
  }

  @Test(expected = BlobStoreException.class)
  public void inUseBlobstoreCannotBeDeleted() throws Exception {
    BlobStore used = mock(BlobStore.class);
    BlobStore unused = mock(BlobStore.class);
    when(used.getBlobStoreConfiguration()).thenReturn(createConfig("used"));
    when(unused.getBlobStoreConfiguration()).thenReturn(createConfig("unused"));
    underTest.track("used", used);
    underTest.track("unused", unused);
    when(repositoryManager.isBlobstoreUsed("used")).thenReturn(true);
    when(repositoryManager.isBlobstoreUsed("unused")).thenReturn(false);

    try {
      underTest.delete("unused");
      underTest.delete("used");
    }
    finally {
      verify(unused, times(1)).remove();
      verify(used, times(0)).remove();
    }
  }

  @Test
  public void itIsConvertableWhenTheStoreFindsNoParentsAndTheBlobStoreIsGroupable() {
    String blobStoreName = "child";
    BlobStore blobStore = mock(BlobStore.class);
    underTest.track(blobStoreName, blobStore);
    when(blobStore.isGroupable()).thenReturn(true);
    when(blobStore.isWritable()).thenReturn(true);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(new MockBlobStoreConfiguration(blobStoreName, "test"));
    when(store.findParent(blobStoreName)).thenReturn(Optional.empty());
    assert underTest.isConvertable(blobStoreName);
  }

  @Test
  public void itIsNotConvertableWhenTheStoreFindsParents() {
    String blobStoreName = "child";
    BlobStore blobStore = mock(BlobStore.class);
    when(blobStore.isGroupable()).thenReturn(true);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(new MockBlobStoreConfiguration(blobStoreName, "test"));
    when(store.findParent(blobStoreName)).thenReturn(Optional.of(new MockBlobStoreConfiguration()));
    assert !underTest.isConvertable(blobStoreName);
  }

  @Test
  public void itIsNotConvertableWhenTheStoreIsNotGroupable() {
    String blobStoreName = "child";
    BlobStore blobStore = mock(BlobStore.class);
    when(blobStore.isGroupable()).thenReturn(false);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(new MockBlobStoreConfiguration(blobStoreName, "test"));
    when(store.findParent(blobStoreName)).thenReturn(Optional.empty());
    assert !underTest.isConvertable(blobStoreName);
  }

  @Test
  public void itIsNotConvertableWhenTheStoreIsInUseByATask() throws Exception {
    underTest.doStart();

    String blobStoreName = "child";
    BlobStore blobStore = mock(BlobStore.class);
    underTest.track(blobStoreName, blobStore);
    when(blobStoreTaskService.isAnyTaskInUseForBlobStore("child")).thenReturn(true);
    when(blobStore.isGroupable()).thenReturn(true);
    when(blobStore.isWritable()).thenReturn(true);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(new MockBlobStoreConfiguration(blobStoreName, "test"));
    when(store.findParent(blobStoreName)).thenReturn(Optional.empty());
    assert !underTest.isConvertable(blobStoreName);
  }

  @Test
  public void canStartWhenABlobStoreFailsToRestore() throws Exception {
    BlobStore blobStore = mock(BlobStore.class);
    doThrow(new IllegalStateException()).when(blobStore).init(any(BlobStoreConfiguration.class));
    when(provider.get()).thenReturn(blobStore);
    when(store.list()).thenReturn(Collections.singletonList(createConfig("test")));

    underTest.doStart();
    assertThat("blob store manager should still track blob stores that failed on startup", underTest.get("test"),
        notNullValue());
  }

  @Test
  public void canStartWhenABlobStoreFailsToStart() throws Exception {
    BlobStore blobStore = mock(BlobStore.class);
    doThrow(new IllegalStateException()).when(blobStore).start();
    when(provider.get()).thenReturn(blobStore);
    when(store.list()).thenReturn(Collections.singletonList(createConfig("test")));
    underTest.doStart();
    //assert underTest.browse().toList().equals(List.of(blobStore));
    assert StreamSupport.stream(underTest.browse().spliterator(), false)
        .collect(Collectors.toList())
        .equals(Collections.singletonList(blobStore));
  }

  @Test
  public void canUpdateBlobStoreFromNewConfig() throws Exception {
    when(descriptor.getSensitiveConfigurationFields()).thenReturn(Collections.singletonList(SECRET_FIELD_KEY));
    BlobStore blobStore = mock(BlobStore.class);
    Map<String, Map<String, Object>> oldBlobStoreAttributes = new HashMap<>();
    Map<String, Object> oldBlobConfigMap = new HashMap<>();
    oldBlobConfigMap.put(SECRET_FIELD_KEY, SECRET_ID);
    oldBlobStoreAttributes.put("test", oldBlobConfigMap);
    oldBlobStoreAttributes.put("file", Collections.singletonMap("path", "foo"));
    BlobStoreConfiguration oldBlobStoreConfig = createConfig("test", oldBlobStoreAttributes);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(oldBlobStoreConfig);
    Secret oldSecret = mock(Secret.class);
    when(secretsService.from(SECRET_ID)).thenReturn(oldSecret);
    when(oldSecret.decrypt()).thenReturn(SECRET_FIELD_VALUE.toCharArray());

    Secret newSecret = mock(Secret.class);
    when(newSecret.getId()).thenReturn("_2");
    Map<String, Map<String, Object>> updatedBlobStoreAttributes = new HashMap<>();
    Map<String, Object> newBlobConfigMap = new HashMap<>();
    newBlobConfigMap.put(SECRET_FIELD_KEY, SECRET_FIELD_VALUE);
    updatedBlobStoreAttributes.put("test", newBlobConfigMap);
    updatedBlobStoreAttributes.put("file", Collections.singletonMap("path", "foo"));
    BlobStoreConfiguration newBlobStoreConfig = createConfig("test", updatedBlobStoreAttributes);
    when(secretsService.encryptMaven(BlobStoreManagerImpl.BLOBSTORE_CONFIG, SECRET_FIELD_VALUE.toCharArray(),
        TEST_USER)).thenReturn(newSecret);

    underTest.track("test", blobStore);

    underTest.update(newBlobStoreConfig);

    verify(secretsService).remove(oldSecret);
    verify(secretsService).encryptMaven(BlobStoreManagerImpl.BLOBSTORE_CONFIG, SECRET_FIELD_VALUE.toCharArray(),
        TEST_USER);
    verify(store, times(1)).update(newBlobStoreConfig);
    verify(store, times(0)).update(oldBlobStoreConfig);
  }

  @Test(expected = BlobStoreException.class)
  public void cannotUpdateBlobStoreFromNewConfig() throws Exception {
    when(descriptor.getSensitiveConfigurationFields()).thenReturn(Collections.singletonList(SECRET_FIELD_KEY));
    BlobStore blobStore = mock(BlobStore.class);
    Map<String, Map<String, Object>> oldBlobStoreAttributes = new HashMap<>();
    Map<String, Object> oldBlobConfigMap = new HashMap<>();
    oldBlobConfigMap.put(SECRET_FIELD_KEY, SECRET_FIELD_VALUE);
    oldBlobStoreAttributes.put("test", oldBlobConfigMap);
    oldBlobStoreAttributes.put("file", Collections.singletonMap("path", "foo"));
    BlobStoreConfiguration oldBlobStoreConfig = createConfig("test", oldBlobStoreAttributes);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(oldBlobStoreConfig);
    BlobId blobId = new BlobId("testBlobId");
    Secret oldSecret = mock(Secret.class);
    when(secretsService.from(SECRET_FIELD_VALUE)).thenReturn(oldSecret);
    when(oldSecret.decrypt()).thenReturn(SECRET_FIELD_VALUE.toCharArray());

    Secret newSecret = mock(Secret.class);
    when(newSecret.getId()).thenReturn("_2");
    Map<String, Map<String, Object>> updatedBlobStoreAttributes = new HashMap<>();
    Map<String, Object> newBlobConfigMap = new HashMap<>();
    newBlobConfigMap.put(SECRET_FIELD_KEY, SECRET_FIELD_VALUE);
    oldBlobStoreAttributes.put("test", newBlobConfigMap);
    updatedBlobStoreAttributes.put("file", Collections.singletonMap("path", "foo"));
    BlobStoreConfiguration newBlobStoreConfig = createConfig("test", updatedBlobStoreAttributes);
    when(secretsService.encryptMaven(BlobStoreManagerImpl.BLOBSTORE_CONFIG, SECRET_FIELD_VALUE.toCharArray(),
        TEST_USER)).thenReturn(newSecret);

    doThrow(new BlobStoreException("Cannot start blobstore with new config", blobId)).when(blobStore).start();

    underTest.track("test", blobStore);

    underTest.update(newBlobStoreConfig);

    verify(store, times(1)).update(newBlobStoreConfig);
    verify(secretsService).encryptMaven(BlobStoreManagerImpl.BLOBSTORE_CONFIG, SECRET_FIELD_VALUE.toCharArray(),
        TEST_USER);
    // old blobstore config should be put back in the database and new secret should be deleted
    verify(secretsService).remove(oldSecret);
    verify(store, times(1)).update(oldBlobStoreConfig);
  }

  private BlobStoreConfiguration createConfig(String name) {
    Map<String, Map<String, Object>> fileAttributes = new HashMap<>();
    fileAttributes.put("file", Collections.singletonMap("path", "baz"));
    return createConfig(name, fileAttributes);
  }

  private BlobStoreConfiguration createConfig(String name, Map<String, Map<String, Object>> fileAttributes) {
    MockBlobStoreConfiguration config = new MockBlobStoreConfiguration();
    config.setName(name);
    config.setType("test");
    config.setAttributes(fileAttributes);
    return config;
  }
}
