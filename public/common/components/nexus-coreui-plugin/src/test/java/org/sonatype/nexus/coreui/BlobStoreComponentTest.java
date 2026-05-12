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

package org.sonatype.nexus.coreui;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.blobstore.BlobStoreDescriptor;
import org.sonatype.nexus.blobstore.BlobStoreDescriptorProvider;
import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.api.tasks.BlobStoreTaskService;
import org.sonatype.nexus.blobstore.group.BlobStoreGroup;
import org.sonatype.nexus.bootstrap.entrypoint.configuration.ApplicationDirectories;
import org.sonatype.nexus.rapture.PasswordPlaceholder;
import org.sonatype.nexus.repository.blobstore.BlobStoreConfigurationStore;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static java.lang.Math.pow;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class BlobStoreComponentTest
{
  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private BlobStoreConfigurationStore store;

  @Mock
  private BlobStoreDescriptorProvider blobStoreDescriptorProvider;

  @Mock
  private ApplicationDirectories applicationDirectories;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private RepositoryPermissionChecker permissionChecker;

  @Mock
  private BlobStoreTaskService blobStoreTaskService;

  private BlobStoreComponent underTest;

  @BeforeEach
  void setup() {
    underTest = new BlobStoreComponent(blobStoreManager, store, blobStoreDescriptorProvider, List.of(),
        applicationDirectories, repositoryManager, permissionChecker, blobStoreTaskService);
  }

  @Test
  void testReadTypesReturnsDescriptorData() {
    BlobStoreDescriptor descriptor = mock(BlobStoreDescriptor.class);
    when(descriptor.getName()).thenReturn("MyType");
    when(descriptor.getFormFields()).thenReturn(Collections.emptyList());
    Map<String, BlobStoreDescriptor> blobStoreDescriptors = Map.of("MyType", descriptor);
    when(blobStoreDescriptorProvider.get()).thenReturn(blobStoreDescriptors);

    List<BlobStoreTypeXO> types = underTest.readTypes();

    assertThat(types, containsInAnyOrder(
        allOf(
            hasProperty("id", is("MyType")),
            hasProperty("name", is("MyType")),
            hasProperty("formFields", is(empty()))),
        allOf(
            hasProperty("id", is("")),
            hasProperty("name", is("")),
            hasProperty("formFields", is(nullValue())))));
  }

  @Test
  void testCreateBlobstoreCreatesAndReturnsNewBlobstore() throws Exception {
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    Map<String, Object> fileAttributes = new HashMap<>();
    fileAttributes.put("path", "path/to/blobs/myblobs");
    attributes.put("file", fileAttributes);
    BlobStoreXO blobStoreXO =
        new BlobStoreXO()
            .withName("myblobs")
            .withType("File")
            .withIsQuotaEnabled(true)
            .withQuotaType("spaceUsedQuota")
            .withQuotaLimit(10L)
            .withAttributes(attributes);
    BlobStoreConfiguration expectedConfig = new MockBlobStoreConfiguration().withName("myblobs")
        .withType("File")
        .withAttributes(
            Map.of("file", Map.of("path", "path/to/blobs/myblobs"), "blobStoreQuotaConfig",
                Map.of("quotaType", "spaceUsedQuota", "quotaLimit", 10L)));

    BlobStore blobStore = mock(BlobStore.class);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(expectedConfig);

    when(blobStoreManager.create(any(BlobStoreConfiguration.class))).thenReturn(blobStore);
    when(blobStoreManager.newConfiguration()).thenReturn(mock(BlobStoreConfiguration.class));
    when(blobStoreManager.getByName()).thenReturn(Map.of("myblobs", blobStore));

    BlobStoreXO createdXO = underTest.create(blobStoreXO);

    verify(blobStoreManager).create(any(BlobStoreConfiguration.class));
    assertThat(createdXO.getName(), is(expectedConfig.getName()));
    assertThat(createdXO.getType(), is(expectedConfig.getType()));
    assertThat(createdXO.getAttributes(), is(expectedConfig.getAttributes()));
  }

  @Test
  void testRemoveBlobstoreOnlyRemovesUnusedBlobstores() throws Exception {
    when(repositoryManager.isBlobstoreUsed("not-used")).thenReturn(false);

    underTest.remove("not-used");

    verify(blobStoreManager).delete("not-used");

    when(repositoryManager.isBlobstoreUsed("used")).thenReturn(true);
    assertThrows(BlobStoreException.class, () -> underTest.remove("used"));
    verify(blobStoreManager, never()).delete("used");
  }

  @Test
  void testDefaultWorkDirectoryReturnsTheBlobsDirectory() {
    File blobDirectory = new File("path/to/blobs");
    when(applicationDirectories.getWorkDirectory("blobs")).thenReturn(blobDirectory);

    PathSeparatorXO defaultWorkDirectory = underTest.defaultWorkDirectory();

    assertThat(new File(defaultWorkDirectory.getPath()), is(blobDirectory));
    assertThat(defaultWorkDirectory.getFileSeparator(), is(File.separator));
  }

  @Test
  void testCreateBlobStoreXOWithQuota() {
    long quotaLimitBytes = (long) (10 * pow(10, 6));
    MockBlobStoreConfiguration config = mockConfig("test", quotaLimitBytes);

    BlobStore blobStore = mock(BlobStore.class);
    when(blobStoreManager.getByName()).thenReturn(Map.of("test", blobStore));

    BlobStoreXO blobStoreXO = underTest.asBlobStoreXO(config);

    assertThat(blobStoreXO.isQuotaEnabled(), is(true));
    assertThat(blobStoreXO.getQuotaType(), is("spaceUsedQuota"));
    assertThat(blobStoreXO.getQuotaLimit(), is(10L));

    config = mockConfig("test", 1);
    blobStoreXO = underTest.asBlobStoreXO(config);
    assertThat(blobStoreXO.getQuotaLimit(), is(0L));
  }

  @Test
  void testCreateBlobStoreConfigWithQuota() {
    BlobStoreConfiguration blobStoreConfig = mock(BlobStoreConfiguration.class);
    BlobStoreXO blobStoreXO = mock(BlobStoreXO.class);
    when(blobStoreXO.getName()).thenReturn("xoTest");
    when(blobStoreXO.getType()).thenReturn("type");
    when(blobStoreXO.isQuotaEnabled()).thenReturn(true);
    when(blobStoreXO.getQuotaLimit()).thenReturn(10L);
    when(blobStoreXO.getQuotaType()).thenReturn("properType");
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    Map<String, Object> quotaConfig = new HashMap<>();
    quotaConfig.put("quotaType", "shouldBeClobbered");
    quotaConfig.put("quotaLimitBytes", 7);
    attributes.put("blobStoreQuotaConfig", quotaConfig);
    when(blobStoreXO.getAttributes()).thenReturn(attributes);
    when(blobStoreManager.newConfiguration()).thenReturn(blobStoreConfig);

    underTest.asConfiguration(blobStoreXO);

    verify(blobStoreConfig).setName("xoTest");
    verify(blobStoreConfig).setType("type");
    verify(blobStoreConfig).setAttributes(
        Map.of("blobStoreQuotaConfig", Map.of("quotaType", "properType", "quotaLimitBytes", (long) (10 * pow(10, 6)))));
  }

  @Test
  void testRequestingBlobstoreNamesOnlyDoesNotSetOtherProperties() {
    MockBlobStoreConfiguration config = new MockBlobStoreConfiguration().withName("test")
        .withAttributes(
            Map.of("file", Map.of("path", "path"), "blobStoreQuotaConfig",
                Map.of("quotaType", "spaceUsedQuota", "quotaLimitBytes", 7)));
    BlobStore blobStore = mock(BlobStore.class);
    when(blobStoreManager.getByName()).thenReturn(Map.of("test", blobStore));

    BlobStoreXO blobStoreXO = underTest.asBlobStoreXO(config, Collections.emptyList());

    assertThat(blobStoreXO.getName(), is("test"));
    assertThat(blobStoreXO.getType(), is(nullValue()));
    verify(blobStore, never()).getMetrics();
  }

  @Test
  void testUpdatingS3BlobstoreWithPasswordPlaceholderDoesNotAlterSecretAccessKey() throws Exception {
    ArgumentCaptor<BlobStoreConfiguration> blobStoreConfigCaptor =
        ArgumentCaptor.forClass(BlobStoreConfiguration.class);

    Map<String, Map<String, Object>> attributes = new HashMap<>();
    Map<String, Object> s3Attributes = new HashMap<>();
    s3Attributes.put("access", "test");
    s3Attributes.put("secretAccessKey", PasswordPlaceholder.get());
    attributes.put("s3", s3Attributes);

    String originalSecret = "hello";
    BlobStoreXO blobStoreXO = new BlobStoreXO().withName("myblobs")
        .withType("S3")
        .withAttributes(Map.of("s3", s3Attributes));
    Map<String, Map<String, Object>> existingAttributes = new HashMap<>();
    Map<String, Object> existingS3 = new HashMap<>();
    existingS3.put("accessKeyId", "test");
    existingS3.put("secretAccessKey", originalSecret);
    existingAttributes.put("s3", existingS3);
    MockBlobStoreConfiguration existingConfig = new MockBlobStoreConfiguration().withName("myblobs")
        .withType("S3")
        .withAttributes(existingAttributes);

    BlobStore blobStore = mock(BlobStore.class);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(existingConfig);

    when(blobStoreManager.get("myblobs")).thenReturn(blobStore);
    when(blobStoreManager.newConfiguration()).thenReturn(new MockBlobStoreConfiguration());
    when(blobStoreManager.getByName()).thenReturn(Map.of("myblobs", blobStore));
    when(blobStoreManager.update(any(BlobStoreConfiguration.class))).thenReturn(blobStore);

    BlobStoreXO updatedXO = underTest.update(blobStoreXO);

    verify(blobStoreManager).update(blobStoreConfigCaptor.capture());
    BlobStoreConfiguration capturedConfig = blobStoreConfigCaptor.getValue();
    assertThat(capturedConfig.getAttributes().get("s3").get("secretAccessKey"), is(originalSecret));
    assertThat(updatedXO.getAttributes().get("s3").get("secretAccessKey"), is(PasswordPlaceholder.get()));
  }

  @Test
  void testUpdatingAzureBlobstoreWithPasswordPlaceholderDoesNotAlterAccountKey() throws Exception {
    ArgumentCaptor<BlobStoreConfiguration> blobStoreConfigCaptor =
        ArgumentCaptor.forClass(BlobStoreConfiguration.class);

    String originalSecret = "hello";
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    Map<String, Object> azureAttributes = new HashMap<>();
    azureAttributes.put("accountKey", PasswordPlaceholder.get());
    attributes.put("azure cloud storage", azureAttributes);
    BlobStoreXO blobStoreXO = new BlobStoreXO().withName("myblobs")
        .withType("Azure Cloud Storage")
        .withAttributes(attributes);
    Map<String, Map<String, Object>> existingAttributes = new HashMap<>();
    Map<String, Object> existingAzure = new HashMap<>();
    existingAzure.put("accountKey", originalSecret);
    existingAttributes.put("azure cloud storage", existingAzure);
    MockBlobStoreConfiguration existingConfig = new MockBlobStoreConfiguration().withName("myblobs")
        .withType("Azure Cloud Storage")
        .withAttributes(existingAttributes);
    BlobStore blobStore = mock(BlobStore.class);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(existingConfig);

    when(blobStoreManager.get("myblobs")).thenReturn(blobStore);
    when(blobStoreManager.newConfiguration()).thenReturn(new MockBlobStoreConfiguration());
    when(blobStoreManager.getByName()).thenReturn(Map.of("myblobs", blobStore));
    when(blobStoreManager.update(any(BlobStoreConfiguration.class))).thenReturn(blobStore);

    BlobStoreXO updatedXO = underTest.update(blobStoreXO);

    verify(blobStoreManager).update(blobStoreConfigCaptor.capture());
    BlobStoreConfiguration capturedConfig = blobStoreConfigCaptor.getValue();
    assertThat(capturedConfig.getAttributes().get("azure cloud storage").get("accountKey"), is(originalSecret));
    assertThat(updatedXO.getAttributes().get("azure cloud storage").get("accountKey"), is(PasswordPlaceholder.get()));
  }

  @Test
  void testRemoveBlobstoreDoesNotRemoveBlobstoresPartOfMoveRepositoryTask() throws Exception {
    when(blobStoreTaskService.countTasksInUseForBlobStore("used_in_move")).thenReturn(2);

    try {
      underTest.remove("used_in_move");
    }
    catch (BlobStoreException e) {
      assertThat(e, is(instanceOf(BlobStoreException.class)));
    }

    verify(blobStoreManager, never()).delete("used_in_move");
  }

  @Test
  void testReadReturnsAllBlobStores() {
    MockBlobStoreConfiguration config1 = new MockBlobStoreConfiguration().withName("store1")
        .withType("File")
        .withAttributes(new HashMap<>(Map.of("file", new HashMap<>(Map.of("path", "path1")))));
    MockBlobStoreConfiguration config2 = new MockBlobStoreConfiguration().withName("store2")
        .withType("File")
        .withAttributes(new HashMap<>(Map.of("file", new HashMap<>(Map.of("path", "path2")))));

    when(store.list()).thenReturn(List.of(config1, config2));
    when(blobStoreManager.browse()).thenReturn(Collections.emptyList());

    BlobStore bs1 = mock(BlobStore.class);
    BlobStore bs2 = mock(BlobStore.class);
    when(blobStoreManager.getByName()).thenReturn(Map.of("store1", bs1, "store2", bs2));

    List<BlobStoreXO> result = underTest.read();
    assertThat(result, hasSize(2));
  }

  @Test
  void testReadWithAllAddsAllEntry() {
    MockBlobStoreConfiguration config1 = new MockBlobStoreConfiguration().withName("store1")
        .withType("File")
        .withAttributes(new HashMap<>(Map.of("file", new HashMap<>(Map.of("path", "path1")))));

    when(store.list()).thenReturn(List.of(config1));
    when(blobStoreManager.browse()).thenReturn(Collections.emptyList());

    BlobStore bs1 = mock(BlobStore.class);
    when(blobStoreManager.getByName()).thenReturn(Map.of("store1", bs1));

    List<BlobStoreXO> result = underTest.readWithAll();
    assertThat(result, hasSize(2));
    assertThat(result.get(result.size() - 1).getName(), is("(All Blob Stores)"));
  }

  @Test
  void testReadNamesReturnsOnlyNames() {
    MockBlobStoreConfiguration config1 = new MockBlobStoreConfiguration().withName("store1")
        .withType("File")
        .withAttributes(Map.of("file", Map.of("path", "path1")));
    MockBlobStoreConfiguration config2 = new MockBlobStoreConfiguration().withName("store2")
        .withType("S3")
        .withAttributes(Map.of("s3", Map.of("bucket", "bucket1")));

    when(store.list()).thenReturn(List.of(config1, config2));

    List<BlobStoreXO> result = underTest.readNames();
    assertThat(result, hasSize(2));
    assertThat(result.get(0).getName(), is("store1"));
    assertThat(result.get(1).getName(), is("store2"));
  }

  @Test
  void testReadGroupsReturnsOnlyGroupTypes() {
    MockBlobStoreConfiguration groupConfig = new MockBlobStoreConfiguration().withName("group1")
        .withType(BlobStoreGroup.TYPE)
        .withAttributes(new HashMap<>(Map.of("group", new HashMap<>(Map.of("members", "store1,store2")))));
    MockBlobStoreConfiguration fileConfig = new MockBlobStoreConfiguration().withName("store1")
        .withType("File")
        .withAttributes(new HashMap<>(Map.of("file", new HashMap<>(Map.of("path", "path1")))));

    when(store.list()).thenReturn(List.of(groupConfig, fileConfig));

    BlobStore bs = mock(BlobStore.class);
    when(blobStoreManager.getByName()).thenReturn(Map.of("group1", bs));

    List<BlobStoreXO> result = underTest.readGroups();
    assertThat(result, hasSize(1));
    assertThat(result.get(0).getName(), is("group1"));
  }

  @Test
  void testReadNoneGroupEntriesIncludingEntryForAll() {
    MockBlobStoreConfiguration groupConfig = new MockBlobStoreConfiguration().withName("group1")
        .withType(BlobStoreGroup.TYPE)
        .withAttributes(new HashMap<>(Map.of("group", new HashMap<>(Map.of("members", "store1")))));
    MockBlobStoreConfiguration fileConfig = new MockBlobStoreConfiguration().withName("store1")
        .withType("File")
        .withAttributes(new HashMap<>(Map.of("file", new HashMap<>(Map.of("path", "path1")))));

    when(store.list()).thenReturn(List.of(groupConfig, fileConfig));

    BlobStore bs = mock(BlobStore.class);
    when(blobStoreManager.getByName()).thenReturn(Map.of("store1", bs));

    List<BlobStoreXO> result = underTest.readNoneGroupEntriesIncludingEntryForAll();
    // Should include store1 (non-group) plus the (All Blob Stores) entry, NOT group1
    assertThat(result, hasSize(2));
    assertThat(result.get(result.size() - 1).getName(), is("(All Blob Stores)"));
    assertThat(result.get(0).getName(), is("store1"));
  }

  @Test
  void testAsConfigurationWithoutQuota() {
    BlobStoreConfiguration blobStoreConfig = mock(BlobStoreConfiguration.class);
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    attributes.put("file", Map.of("path", "path/to/blobs"));
    BlobStoreXO blobStoreXO = new BlobStoreXO()
        .withName("test")
        .withType("File")
        .withIsQuotaEnabled(false)
        .withAttributes(attributes);
    when(blobStoreManager.newConfiguration()).thenReturn(blobStoreConfig);

    underTest.asConfiguration(blobStoreXO);

    verify(blobStoreConfig).setName("test");
    verify(blobStoreConfig).setType("File");
    verify(blobStoreConfig).setAttributes(Map.of("file", Map.of("path", "path/to/blobs")));
  }

  @Test
  void testAsBlobStoreXO_withStartedBlobStoreMetrics() {
    MockBlobStoreConfiguration config = new MockBlobStoreConfiguration()
        .withName("test-store")
        .withType("File")
        .withAttributes(new HashMap<>(Map.of("file", new HashMap<>(Map.of("path", "path")))));

    BlobStore blobStore = mock(BlobStore.class);
    when(blobStore.isStarted()).thenReturn(true);
    BlobStoreMetrics metrics = mock(BlobStoreMetrics.class);
    when(metrics.getBlobCount()).thenReturn(100L);
    when(metrics.getTotalSize()).thenReturn(50000L);
    when(metrics.getAvailableSpace()).thenReturn(1000000L);
    when(metrics.isUnlimited()).thenReturn(false);
    when(metrics.isUnavailable()).thenReturn(false);
    when(blobStore.getMetrics()).thenReturn(metrics);

    when(blobStoreManager.getByName()).thenReturn(Map.of("test-store", blobStore));

    BlobStoreXO result = underTest.asBlobStoreXO(config);

    assertThat(result.getBlobCount(), is(100L));
    assertThat(result.getTotalSize(), is(50000L));
    assertThat(result.getAvailableSpace(), is(1000000L));
    assertThat(result.isUnlimited(), is(false));
    assertThat(result.isUnavailable(), is(false));
  }

  @Test
  void testAsBlobStoreXO_withStoppedBlobStoreIsUnavailable() {
    MockBlobStoreConfiguration config = new MockBlobStoreConfiguration()
        .withName("stopped-store")
        .withType("File")
        .withAttributes(new HashMap<>(Map.of("file", new HashMap<>(Map.of("path", "path")))));

    BlobStore blobStore = mock(BlobStore.class);
    when(blobStore.isStarted()).thenReturn(false);
    when(blobStoreManager.getByName()).thenReturn(Map.of("stopped-store", blobStore));

    BlobStoreXO result = underTest.asBlobStoreXO(config);

    assertThat(result.isUnavailable(), is(true));
  }

  @Test
  void testAsBlobStoreXO_withNoBlobStoreInManagerIsUnavailable() {
    MockBlobStoreConfiguration config = new MockBlobStoreConfiguration()
        .withName("missing-store")
        .withType("File")
        .withAttributes(new HashMap<>(Map.of("file", new HashMap<>(Map.of("path", "path")))));

    when(blobStoreManager.getByName()).thenReturn(Collections.emptyMap());

    BlobStoreXO result = underTest.asBlobStoreXO(config);

    assertThat(result.isUnavailable(), is(true));
  }

  @Test
  void testAsBlobStoreXO_withNoQuotaAttributes() {
    MockBlobStoreConfiguration config = new MockBlobStoreConfiguration()
        .withName("no-quota")
        .withType("File")
        .withAttributes(new HashMap<>(Map.of("file", new HashMap<>(Map.of("path", "path")))));

    when(blobStoreManager.getByName()).thenReturn(Collections.emptyMap());

    BlobStoreXO result = underTest.asBlobStoreXO(config);

    assertThat(result.isQuotaEnabled(), is(false));
    assertThat(result.getQuotaType(), is(nullValue()));
    assertThat(result.getQuotaLimit(), is(nullValue()));
  }

  @Test
  void testFilterAttributes_s3SecretAccessKey() {
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    Map<String, Object> s3Attrs = new HashMap<>();
    s3Attrs.put("secretAccessKey", "my-secret-key");
    s3Attrs.put("bucket", "my-bucket");
    attributes.put("s3", s3Attrs);

    MockBlobStoreConfiguration config = new MockBlobStoreConfiguration()
        .withName("s3-store")
        .withType("S3")
        .withAttributes(attributes);

    when(blobStoreManager.getByName()).thenReturn(Collections.emptyMap());

    BlobStoreXO result = underTest.asBlobStoreXO(config);

    assertThat(result.getAttributes().get("s3").get("secretAccessKey"), is(PasswordPlaceholder.get()));
    assertThat(result.getAttributes().get("s3").get("bucket"), is("my-bucket"));
  }

  @Test
  void testFilterAttributes_azureAccountKey() {
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    Map<String, Object> azureAttrs = new HashMap<>();
    azureAttrs.put("accountKey", "my-azure-key");
    azureAttrs.put("containerName", "my-container");
    attributes.put("azure cloud storage", azureAttrs);

    MockBlobStoreConfiguration config = new MockBlobStoreConfiguration()
        .withName("azure-store")
        .withType("Azure Cloud Storage")
        .withAttributes(attributes);

    when(blobStoreManager.getByName()).thenReturn(Collections.emptyMap());

    BlobStoreXO result = underTest.asBlobStoreXO(config);

    assertThat(result.getAttributes().get("azure cloud storage").get("accountKey"), is(PasswordPlaceholder.get()));
    assertThat(result.getAttributes().get("azure cloud storage").get("containerName"), is("my-container"));
  }

  @Test
  void testFilterAttributes_noSecretsPresent() {
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    Map<String, Object> fileAttrs = new HashMap<>();
    fileAttrs.put("path", "path/to/blobs");
    attributes.put("file", fileAttrs);

    MockBlobStoreConfiguration config = new MockBlobStoreConfiguration()
        .withName("file-store")
        .withType("File")
        .withAttributes(attributes);

    when(blobStoreManager.getByName()).thenReturn(Collections.emptyMap());

    BlobStoreXO result = underTest.asBlobStoreXO(config);

    assertThat(result.getAttributes().get("file").get("path"), is("path/to/blobs"));
  }

  @Test
  void testReadQuotaTypes_empty() {
    // underTest is constructed with empty quota list
    List<BlobStoreQuotaTypeXO> quotaTypes = underTest.readQuotaTypes();
    assertThat(quotaTypes, is(empty()));
  }

  @Test
  void testAsBlobStoreXO_withGroupMembership() {
    MockBlobStoreConfiguration memberConfig = new MockBlobStoreConfiguration()
        .withName("member-store")
        .withType("File")
        .withAttributes(new HashMap<>(Map.of("file", new HashMap<>(Map.of("path", "path")))));

    BlobStoreGroup group = mock(BlobStoreGroup.class);
    MockBlobStoreConfiguration groupConfig = new MockBlobStoreConfiguration()
        .withName("test-group")
        .withType(BlobStoreGroup.TYPE);
    when(group.getBlobStoreConfiguration()).thenReturn(groupConfig);

    BlobStore memberBlobStore = mock(BlobStore.class);
    when(memberBlobStore.getBlobStoreConfiguration()).thenReturn(memberConfig);
    when(group.getMembers()).thenReturn(List.of(memberBlobStore));

    when(blobStoreManager.getByName()).thenReturn(Collections.emptyMap());

    BlobStoreXO result = underTest.asBlobStoreXO(memberConfig, List.of(group));

    assertThat(result.getGroupName(), is("test-group"));
  }

  @Test
  void testAsBlobStoreXO_notInAnyGroup() {
    MockBlobStoreConfiguration storeConfig = new MockBlobStoreConfiguration()
        .withName("standalone-store")
        .withType("File")
        .withAttributes(new HashMap<>(Map.of("file", new HashMap<>(Map.of("path", "path")))));

    BlobStoreGroup group = mock(BlobStoreGroup.class);

    MockBlobStoreConfiguration otherMemberConfig = new MockBlobStoreConfiguration()
        .withName("other-store")
        .withType("File");
    BlobStore otherMember = mock(BlobStore.class);
    when(otherMember.getBlobStoreConfiguration()).thenReturn(otherMemberConfig);
    when(group.getMembers()).thenReturn(List.of(otherMember));

    when(blobStoreManager.getByName()).thenReturn(Collections.emptyMap());

    BlobStoreXO result = underTest.asBlobStoreXO(storeConfig, List.of(group));

    assertThat(result.getGroupName(), is(nullValue()));
  }

  @Test
  void testUpdateWithNoPasswordChange_S3() throws Exception {
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    Map<String, Object> s3Attributes = new HashMap<>();
    s3Attributes.put("secretAccessKey", "new-actual-secret");
    attributes.put("s3", s3Attributes);

    BlobStoreXO blobStoreXO = new BlobStoreXO().withName("s3store")
        .withType("S3")
        .withAttributes(attributes);

    MockBlobStoreConfiguration existingConfig = new MockBlobStoreConfiguration().withName("s3store")
        .withType("S3")
        .withAttributes(new HashMap<>(Map.of("s3", new HashMap<>(Map.of("secretAccessKey", "existing-secret")))));

    BlobStore blobStore = mock(BlobStore.class);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(existingConfig);

    when(blobStoreManager.get("s3store")).thenReturn(blobStore);
    when(blobStoreManager.newConfiguration()).thenReturn(new MockBlobStoreConfiguration());
    when(blobStoreManager.getByName()).thenReturn(Map.of("s3store", blobStore));
    when(blobStoreManager.update(any(BlobStoreConfiguration.class))).thenReturn(blobStore);

    underTest.update(blobStoreXO);

    // When the password is NOT a placeholder, it should be passed through as-is
    ArgumentCaptor<BlobStoreConfiguration> captor = ArgumentCaptor.forClass(BlobStoreConfiguration.class);
    verify(blobStoreManager).update(captor.capture());
    assertThat(captor.getValue().getAttributes().get("s3").get("secretAccessKey"), is("new-actual-secret"));
  }

  private static MockBlobStoreConfiguration mockConfig(final String name, final long quotaLimitBytes) {
    return new MockBlobStoreConfiguration()
        .withName(name)
        .withAttributes(Map.of("file", Map.of("path", "path"), "blobStoreQuotaConfig",
            Map.of("quotaType", "spaceUsedQuota", "quotaLimitBytes", quotaLimitBytes)));
  }
}
