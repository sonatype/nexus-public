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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
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
import static org.mockito.Mockito.times;
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
  public void canCreateBlobStoreWithExistingSecretIds() throws Exception {
    // Test that existing secret IDs (from configuration import) are not re-encrypted
    when(descriptor.getSensitiveConfigurationFields()).thenReturn(List.of(SECRET_FIELD_KEY));
    when(provider.get()).thenReturn(blobStore);

    // Mock an existing secret with ID "_1"
    Secret existingSecret = mock(Secret.class);
    when(secretsService.from(SECRET_ID)).thenReturn(existingSecret);

    Map<String, Map<String, Object>> blobStoreAttributes = new HashMap<>();
    Map<String, Object> blobConfigMap = new HashMap<>();
    blobConfigMap.put(SECRET_FIELD_KEY, SECRET_ID); // Use existing secret ID
    blobStoreAttributes.put("test", blobConfigMap);
    blobStoreAttributes.put("file", Map.of("path", "foo"));
    BlobStoreConfiguration configuration = createConfig("test", blobStoreAttributes);

    BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig);
    BlobStore createdBlobStore = underTest.create(configuration);

    // Verify the secret ID is preserved (not changed)
    assertThat(configuration.getAttributes().get("test").get(SECRET_FIELD_KEY), is(SECRET_ID));
    assertThat(createdBlobStore, is(blobStore));

    // Verify secretsService.from() was called to retrieve the existing secret
    verify(secretsService).from(SECRET_ID);

    // Verify encryptMaven was NOT called (no re-encryption)
    verify(secretsService, never()).encryptMaven(any(), any(), any());

    verify(store).create(configuration);
    verify(blobStore).start();
  }

  @Test
  public void canCreateBlobStoreWithMultipleExistingSecretIds() throws Exception {
    // Test handling multiple existing secret IDs
    String sessionTokenKey = "sessionToken";
    String sessionTokenSecretId = "_2";

    when(descriptor.getSensitiveConfigurationFields()).thenReturn(List.of(SECRET_FIELD_KEY, sessionTokenKey));
    when(provider.get()).thenReturn(blobStore);

    Secret secretAccessKeySecret = mock(Secret.class);
    when(secretsService.from(SECRET_ID)).thenReturn(secretAccessKeySecret);

    Secret sessionTokenSecret = mock(Secret.class);
    when(secretsService.from(sessionTokenSecretId)).thenReturn(sessionTokenSecret);

    Map<String, Map<String, Object>> blobStoreAttributes = new HashMap<>();
    Map<String, Object> blobConfigMap = new HashMap<>();
    blobConfigMap.put(SECRET_FIELD_KEY, SECRET_ID);
    blobConfigMap.put(sessionTokenKey, sessionTokenSecretId);
    blobStoreAttributes.put("test", blobConfigMap);
    blobStoreAttributes.put("file", Map.of("path", "foo"));
    BlobStoreConfiguration configuration = createConfig("test", blobStoreAttributes);

    BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig);
    BlobStore createdBlobStore = underTest.create(configuration);

    // Verify both secret IDs are preserved
    assertThat(configuration.getAttributes().get("test").get(SECRET_FIELD_KEY), is(SECRET_ID));
    assertThat(configuration.getAttributes().get("test").get(sessionTokenKey), is(sessionTokenSecretId));
    assertThat(createdBlobStore, is(blobStore));

    // Verify both secrets were retrieved but not re-encrypted
    verify(secretsService).from(SECRET_ID);
    verify(secretsService).from(sessionTokenSecretId);
    verify(secretsService, never()).encryptMaven(any(), any(), any());

    verify(store).create(configuration);
    verify(blobStore).start();
  }

  @Test
  public void canCreateBlobStoreWithMixOfSecretIdsAndPlaintext() throws Exception {
    // Test handling mix of existing secret IDs and plaintext values
    String sessionTokenKey = "sessionToken";
    String sessionTokenPlaintext = "newSessionTokenValue";

    when(descriptor.getSensitiveConfigurationFields()).thenReturn(List.of(SECRET_FIELD_KEY, sessionTokenKey));
    when(provider.get()).thenReturn(blobStore);

    // Mock existing secret for secretAccessKey
    Secret existingSecret = mock(Secret.class);
    when(secretsService.from(SECRET_ID)).thenReturn(existingSecret);

    // Mock new secret for sessionToken
    Secret newSecret = mock(Secret.class);
    when(newSecret.getId()).thenReturn("_2");
    when(secretsService.encryptMaven(BaseBlobStoreManager.BLOBSTORE_CONFIG, sessionTokenPlaintext.toCharArray(),
        TEST_USER))
            .thenReturn(newSecret);

    Map<String, Map<String, Object>> blobStoreAttributes = new HashMap<>();
    Map<String, Object> blobConfigMap = new HashMap<>();
    blobConfigMap.put(SECRET_FIELD_KEY, SECRET_ID); // Existing secret ID
    blobConfigMap.put(sessionTokenKey, sessionTokenPlaintext); // New plaintext value
    blobStoreAttributes.put("test", blobConfigMap);
    blobStoreAttributes.put("file", Map.of("path", "foo"));
    BlobStoreConfiguration configuration = createConfig("test", blobStoreAttributes);

    BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig);
    BlobStore createdBlobStore = underTest.create(configuration);

    // Verify existing secret ID is preserved
    assertThat(configuration.getAttributes().get("test").get(SECRET_FIELD_KEY), is(SECRET_ID));
    // Verify plaintext value was encrypted and replaced with new secret ID
    assertThat(configuration.getAttributes().get("test").get(sessionTokenKey), is("_2"));
    assertThat(createdBlobStore, is(blobStore));

    // Verify existing secret was retrieved but not re-encrypted
    verify(secretsService).from(SECRET_ID);
    // Verify plaintext value was encrypted
    verify(secretsService).encryptMaven(BaseBlobStoreManager.BLOBSTORE_CONFIG, sessionTokenPlaintext.toCharArray(),
        TEST_USER);

    verify(store).create(configuration);
    verify(blobStore).start();
  }

  @Test
  public void doesNotTreatNonSecretIdPatternsAsSecretIds() throws Exception {
    // Test that values starting with "_" but not matching secret ID pattern are treated as plaintext
    when(descriptor.getSensitiveConfigurationFields()).thenReturn(List.of(SECRET_FIELD_KEY));
    when(provider.get()).thenReturn(blobStore);

    Secret newSecret = mock(Secret.class);
    when(newSecret.getId()).thenReturn(SECRET_ID);

    // Test various non-secret-ID patterns
    String[] nonSecretIdValues = {
        "_abc", // underscore followed by letters
        "_", // just underscore
        "__123", // double underscore
        "_xyz123", // underscore followed by letters then numbers
        "plaintext" // no underscore at all
    };

    for (String value : nonSecretIdValues) {
      when(secretsService.encryptMaven(BaseBlobStoreManager.BLOBSTORE_CONFIG, value.toCharArray(), TEST_USER))
          .thenReturn(newSecret);

      Map<String, Map<String, Object>> blobStoreAttributes = new HashMap<>();
      Map<String, Object> blobConfigMap = new HashMap<>();
      blobConfigMap.put(SECRET_FIELD_KEY, value);
      blobStoreAttributes.put("test", blobConfigMap);
      blobStoreAttributes.put("file", Map.of("path", "foo"));
      BlobStoreConfiguration configuration = createConfig("test-" + value.replace("_", "u"), blobStoreAttributes);

      BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig);
      underTest.create(configuration);

      // Verify the value was encrypted (treated as plaintext, not as secret ID)
      verify(secretsService).encryptMaven(BaseBlobStoreManager.BLOBSTORE_CONFIG, value.toCharArray(), TEST_USER);
    }
  }

  @Test
  public void recognizesVariousSecretIdPatterns() throws Exception {
    // Test that various valid secret ID patterns are correctly recognized
    when(descriptor.getSensitiveConfigurationFields()).thenReturn(List.of(SECRET_FIELD_KEY));
    when(provider.get()).thenReturn(blobStore);

    // Test various valid secret ID patterns
    String[] validSecretIds = {
        "_1",
        "_2",
        "_10",
        "_999",
        "_12345"
    };

    for (String secretId : validSecretIds) {
      Secret existingSecret = mock(Secret.class);
      when(secretsService.from(secretId)).thenReturn(existingSecret);

      Map<String, Map<String, Object>> blobStoreAttributes = new HashMap<>();
      Map<String, Object> blobConfigMap = new HashMap<>();
      blobConfigMap.put(SECRET_FIELD_KEY, secretId);
      blobStoreAttributes.put("test", blobConfigMap);
      blobStoreAttributes.put("file", Map.of("path", "foo"));
      BlobStoreConfiguration configuration = createConfig("test-" + secretId.substring(1), blobStoreAttributes);

      BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig);
      underTest.create(configuration);

      // Verify secretsService.from() was called (secret ID was recognized)
      verify(secretsService).from(secretId);
      // Verify the secret ID is preserved in the configuration
      assertThat(configuration.getAttributes().get("test").get(SECRET_FIELD_KEY), is(secretId));
    }

    // Verify encryptMaven was never called (all were treated as existing secret IDs)
    verify(secretsService, never()).encryptMaven(any(), any(), any());
  }

  @Test
  public void updateBlobStorePreservesExistingSecretIds() throws Exception {
    // Test that updating a blob store with unchanged secret IDs skips re-encryption
    when(descriptor.getSensitiveConfigurationFields()).thenReturn(List.of(SECRET_FIELD_KEY));

    Map<String, Map<String, Object>> oldBlobStoreAttributes = new HashMap<>();
    Map<String, Object> oldBlobConfigMap = new HashMap<>();
    oldBlobConfigMap.put(SECRET_FIELD_KEY, SECRET_ID);
    oldBlobStoreAttributes.put("test", oldBlobConfigMap);
    oldBlobStoreAttributes.put("file", Map.of("path", "foo"));
    BlobStoreConfiguration oldBlobStoreConfig = createConfig("test", oldBlobStoreAttributes);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(oldBlobStoreConfig);

    Secret existingSecret = mock(Secret.class);
    when(secretsService.from(SECRET_ID)).thenReturn(existingSecret);

    // Create new config with same secret ID (unchanged)
    Map<String, Map<String, Object>> newBlobStoreAttributes = new HashMap<>();
    Map<String, Object> newBlobConfigMap = new HashMap<>();
    newBlobConfigMap.put(SECRET_FIELD_KEY, SECRET_ID);
    newBlobStoreAttributes.put("test", newBlobConfigMap);
    newBlobStoreAttributes.put("file", Map.of("path", "bar")); // Changed something else
    BlobStoreConfiguration newBlobStoreConfig = createConfig("test", newBlobStoreAttributes);

    BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig);
    underTest.track("test", blobStore);

    underTest.update(newBlobStoreConfig);

    // When secret ID is unchanged, it should retrieve it but NOT re-encrypt
    verify(secretsService).from(SECRET_ID);
    verify(secretsService, never()).encryptMaven(any(), any(), any());

    // The secret should NOT be removed since the value is unchanged
    verify(secretsService, never()).remove(any());
    verify(store).update(newBlobStoreConfig);
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

  // Tests for moveBlob retry logic on temp file race condition

  @Test
  void moveBlobSucceedsOnFirstAttempt() throws Exception {
    // Setup
    BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig);
    BlobStore srcBlobStore = mock(BlobStore.class);
    BlobStore destBlobStore = mock(BlobStore.class);
    BlobId blobId = new BlobId("test-blob-id", OffsetDateTime.now());
    Blob srcBlob = mock(Blob.class);
    Blob destBlob = mock(Blob.class);
    BlobAttributes blobAttributes = mock(BlobAttributes.class);
    InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
    Map<String, String> headers = Map.of("test-header", "test-value");
    BlobStoreConfiguration srcConfig = mock(BlobStoreConfiguration.class);
    BlobStoreConfiguration destConfig = mock(BlobStoreConfiguration.class);

    when(srcBlobStore.getBlobStoreConfiguration()).thenReturn(srcConfig);
    when(destBlobStore.getBlobStoreConfiguration()).thenReturn(destConfig);
    when(srcConfig.getName()).thenReturn("src-store");
    when(destConfig.getName()).thenReturn("dest-store");
    when(srcBlobStore.getBlobAttributes(blobId)).thenReturn(blobAttributes);
    when(blobAttributes.isDeleted()).thenReturn(false);
    when(blobAttributes.getHeaders()).thenReturn(headers);
    when(srcBlobStore.isInternalMoveSupported(destBlobStore)).thenReturn(false);
    when(srcBlobStore.get(blobId, true)).thenReturn(srcBlob);
    when(srcBlob.getInputStream()).thenReturn(inputStream);
    when(destBlobStore.create(any(InputStream.class), any(), any())).thenReturn(destBlob);

    // Execute
    Blob result = underTest.moveBlob(blobId, srcBlobStore, destBlobStore);

    // Verify
    assertThat(result, is(destBlob));
    verify(destBlobStore, times(1)).create(any(InputStream.class), any(), any());
    verify(srcBlobStore).deleteHard(blobId);
  }

  @Test
  void moveBlobRetriesOnTempFileNoSuchFileException() throws Exception {
    // Setup
    BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig);
    BlobStore srcBlobStore = mock(BlobStore.class);
    BlobStore destBlobStore = mock(BlobStore.class);
    BlobId blobId = new BlobId("test-blob-id", OffsetDateTime.now());
    Blob srcBlob = mock(Blob.class);
    Blob destBlob = mock(Blob.class);
    BlobAttributes blobAttributes = mock(BlobAttributes.class);
    Map<String, String> headers = Map.of("test-header", "test-value");
    BlobStoreConfiguration srcConfig = mock(BlobStoreConfiguration.class);
    BlobStoreConfiguration destConfig = mock(BlobStoreConfiguration.class);

    when(srcBlobStore.getBlobStoreConfiguration()).thenReturn(srcConfig);
    when(destBlobStore.getBlobStoreConfiguration()).thenReturn(destConfig);
    when(srcConfig.getName()).thenReturn("src-store");
    when(destConfig.getName()).thenReturn("dest-store");
    when(srcBlobStore.getBlobAttributes(blobId)).thenReturn(blobAttributes);
    when(blobAttributes.isDeleted()).thenReturn(false);
    when(blobAttributes.getHeaders()).thenReturn(headers);
    when(srcBlobStore.isInternalMoveSupported(destBlobStore)).thenReturn(false);
    when(srcBlobStore.get(blobId, true)).thenReturn(srcBlob);
    // Return new input stream on each call
    when(srcBlob.getInputStream())
        .thenReturn(new ByteArrayInputStream("test content".getBytes()))
        .thenReturn(new ByteArrayInputStream("test content".getBytes()));

    // First call throws NoSuchFileException for temp file, second succeeds
    NoSuchFileException tempFileException = new NoSuchFileException("/nexus-data/blobs/content/tmp/test.bytes");
    when(destBlobStore.create(any(InputStream.class), any(), any()))
        .thenThrow(new BlobStoreException(tempFileException, blobId))
        .thenReturn(destBlob);

    // Execute
    Blob result = underTest.moveBlob(blobId, srcBlobStore, destBlobStore);

    // Verify - should succeed after retry
    assertThat(result, is(destBlob));
    verify(destBlobStore, times(2)).create(any(InputStream.class), any(), any());
    verify(srcBlobStore, times(2)).get(blobId, true);
    verify(srcBlobStore).deleteHard(blobId);
  }

  @Test
  void moveBlobRetriesOnWindowsTempFilePath() throws Exception {
    // Setup
    BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig);
    BlobStore srcBlobStore = mock(BlobStore.class);
    BlobStore destBlobStore = mock(BlobStore.class);
    BlobId blobId = new BlobId("test-blob-id", OffsetDateTime.now());
    Blob srcBlob = mock(Blob.class);
    Blob destBlob = mock(Blob.class);
    BlobAttributes blobAttributes = mock(BlobAttributes.class);
    Map<String, String> headers = Map.of("test-header", "test-value");
    BlobStoreConfiguration srcConfig = mock(BlobStoreConfiguration.class);
    BlobStoreConfiguration destConfig = mock(BlobStoreConfiguration.class);

    when(srcBlobStore.getBlobStoreConfiguration()).thenReturn(srcConfig);
    when(destBlobStore.getBlobStoreConfiguration()).thenReturn(destConfig);
    when(srcConfig.getName()).thenReturn("src-store");
    when(destConfig.getName()).thenReturn("dest-store");
    when(srcBlobStore.getBlobAttributes(blobId)).thenReturn(blobAttributes);
    when(blobAttributes.isDeleted()).thenReturn(false);
    when(blobAttributes.getHeaders()).thenReturn(headers);
    when(srcBlobStore.isInternalMoveSupported(destBlobStore)).thenReturn(false);
    when(srcBlobStore.get(blobId, true)).thenReturn(srcBlob);
    when(srcBlob.getInputStream())
        .thenReturn(new ByteArrayInputStream("test content".getBytes()))
        .thenReturn(new ByteArrayInputStream("test content".getBytes()));

    // Windows-style path with backslashes
    NoSuchFileException tempFileException = new NoSuchFileException("C:\\nexus-data\\blobs\\content\\tmp\\test.bytes");
    when(destBlobStore.create(any(InputStream.class), any(), any()))
        .thenThrow(new BlobStoreException(tempFileException, blobId))
        .thenReturn(destBlob);

    // Execute
    Blob result = underTest.moveBlob(blobId, srcBlobStore, destBlobStore);

    // Verify - should succeed after retry with Windows path
    assertThat(result, is(destBlob));
    verify(destBlobStore, times(2)).create(any(InputStream.class), any(), any());
  }

  @Test
  void moveBlobDoesNotRetryOnNonTempFileException() throws Exception {
    // Setup
    BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig);
    BlobStore srcBlobStore = mock(BlobStore.class);
    BlobStore destBlobStore = mock(BlobStore.class);
    BlobId blobId = new BlobId("test-blob-id", OffsetDateTime.now());
    Blob srcBlob = mock(Blob.class);
    BlobAttributes blobAttributes = mock(BlobAttributes.class);
    Map<String, String> headers = Map.of("test-header", "test-value");
    BlobStoreConfiguration srcConfig = mock(BlobStoreConfiguration.class);
    BlobStoreConfiguration destConfig = mock(BlobStoreConfiguration.class);

    // Use lenient() for stubs that may not be called due to exception
    lenient().when(srcBlobStore.getBlobStoreConfiguration()).thenReturn(srcConfig);
    lenient().when(destBlobStore.getBlobStoreConfiguration()).thenReturn(destConfig);
    lenient().when(srcConfig.getName()).thenReturn("src-store");
    lenient().when(destConfig.getName()).thenReturn("dest-store");
    when(srcBlobStore.getBlobAttributes(blobId)).thenReturn(blobAttributes);
    when(blobAttributes.isDeleted()).thenReturn(false);
    when(blobAttributes.getHeaders()).thenReturn(headers);
    when(srcBlobStore.isInternalMoveSupported(destBlobStore)).thenReturn(false);
    when(srcBlobStore.get(blobId, true)).thenReturn(srcBlob);
    when(srcBlob.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));

    // NoSuchFileException for non-temp file path - should NOT retry
    NoSuchFileException nonTempFileException = new NoSuchFileException("/nexus-data/blobs/content/vol-01/blob.bytes");
    when(destBlobStore.create(any(InputStream.class), any(), any()))
        .thenThrow(new BlobStoreException(nonTempFileException, blobId));

    // Execute & Verify - should throw immediately without retry
    assertThrows(BlobStoreException.class, () -> underTest.moveBlob(blobId, srcBlobStore, destBlobStore));
    verify(destBlobStore, times(1)).create(any(InputStream.class), any(), any());
  }

  @Test
  void moveBlobFailsAfterMaxRetries() throws Exception {
    // Setup
    BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig);
    BlobStore srcBlobStore = mock(BlobStore.class);
    BlobStore destBlobStore = mock(BlobStore.class);
    BlobId blobId = new BlobId("test-blob-id", OffsetDateTime.now());
    Blob srcBlob = mock(Blob.class);
    BlobAttributes blobAttributes = mock(BlobAttributes.class);
    Map<String, String> headers = Map.of("test-header", "test-value");
    BlobStoreConfiguration srcConfig = mock(BlobStoreConfiguration.class);
    BlobStoreConfiguration destConfig = mock(BlobStoreConfiguration.class);

    // Use lenient() for stubs that may not be called due to exception
    lenient().when(srcBlobStore.getBlobStoreConfiguration()).thenReturn(srcConfig);
    lenient().when(destBlobStore.getBlobStoreConfiguration()).thenReturn(destConfig);
    lenient().when(srcConfig.getName()).thenReturn("src-store");
    lenient().when(destConfig.getName()).thenReturn("dest-store");
    when(srcBlobStore.getBlobAttributes(blobId)).thenReturn(blobAttributes);
    when(blobAttributes.isDeleted()).thenReturn(false);
    when(blobAttributes.getHeaders()).thenReturn(headers);
    when(srcBlobStore.isInternalMoveSupported(destBlobStore)).thenReturn(false);
    when(srcBlobStore.get(blobId, true)).thenReturn(srcBlob);
    when(srcBlob.getInputStream())
        .thenReturn(new ByteArrayInputStream("test content".getBytes()))
        .thenReturn(new ByteArrayInputStream("test content".getBytes()))
        .thenReturn(new ByteArrayInputStream("test content".getBytes()));

    // Always throw temp file exception - should fail after 3 attempts
    NoSuchFileException tempFileException = new NoSuchFileException("/nexus-data/blobs/content/tmp/test.bytes");
    when(destBlobStore.create(any(InputStream.class), any(), any()))
        .thenThrow(new BlobStoreException(tempFileException, blobId));

    // Execute & Verify - should fail after max retries (3 attempts)
    assertThrows(BlobStoreException.class, () -> underTest.moveBlob(blobId, srcBlobStore, destBlobStore));
    verify(destBlobStore, times(3)).create(any(InputStream.class), any(), any());
    verify(srcBlobStore, times(3)).get(blobId, true);
  }

  @Test
  void moveBlobDoesNotRetryOnOtherBlobStoreException() throws Exception {
    // Setup
    BaseBlobStoreManager underTest = newBlobStoreManager(true, this::getBlobStoreConfig);
    BlobStore srcBlobStore = mock(BlobStore.class);
    BlobStore destBlobStore = mock(BlobStore.class);
    BlobId blobId = new BlobId("test-blob-id", OffsetDateTime.now());
    Blob srcBlob = mock(Blob.class);
    BlobAttributes blobAttributes = mock(BlobAttributes.class);
    Map<String, String> headers = Map.of("test-header", "test-value");
    BlobStoreConfiguration srcConfig = mock(BlobStoreConfiguration.class);
    BlobStoreConfiguration destConfig = mock(BlobStoreConfiguration.class);

    // Use lenient() for stubs that may not be called due to exception
    lenient().when(srcBlobStore.getBlobStoreConfiguration()).thenReturn(srcConfig);
    lenient().when(destBlobStore.getBlobStoreConfiguration()).thenReturn(destConfig);
    lenient().when(srcConfig.getName()).thenReturn("src-store");
    lenient().when(destConfig.getName()).thenReturn("dest-store");
    when(srcBlobStore.getBlobAttributes(blobId)).thenReturn(blobAttributes);
    when(blobAttributes.isDeleted()).thenReturn(false);
    when(blobAttributes.getHeaders()).thenReturn(headers);
    when(srcBlobStore.isInternalMoveSupported(destBlobStore)).thenReturn(false);
    when(srcBlobStore.get(blobId, true)).thenReturn(srcBlob);
    when(srcBlob.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));

    // BlobStoreException without NoSuchFileException cause - should NOT retry
    when(destBlobStore.create(any(InputStream.class), any(), any()))
        .thenThrow(new BlobStoreException("Disk full", blobId));

    // Execute & Verify - should throw immediately without retry
    assertThrows(BlobStoreException.class, () -> underTest.moveBlob(blobId, srcBlobStore, destBlobStore));
    verify(destBlobStore, times(1)).create(any(InputStream.class), any(), any());
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
        () -> repositoryManager, nodeAccess, provisionDefaults, blobStoreConfigProvider,
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
