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

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.BlobStoreDescriptor;
import org.sonatype.nexus.blobstore.BlobStoreDescriptorProvider;
import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.api.tasks.BlobStoreTaskService;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuota;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.rapture.PasswordPlaceholder;
import org.sonatype.nexus.repository.blobstore.BlobStoreConfigurationStore;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.pow;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BlobStoreComponentTest
    extends TestSupport
{
  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private BlobStoreConfigurationStore store;

  @Mock
  private BlobStoreDescriptorProvider blobStoreDescriptorProvider;

  private Map<String, BlobStoreQuota> quotaFactories = new HashMap<>();

  @Mock
  private ApplicationDirectories applicationDirectories;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private RepositoryPermissionChecker permissionChecker;

  @Mock
  private BlobStoreTaskService blobStoreTaskService;

  private BlobStoreComponent underTest;

  @Before
  public void setup() {
    underTest = new BlobStoreComponent(blobStoreManager, store, blobStoreDescriptorProvider, quotaFactories,
        applicationDirectories, repositoryManager, permissionChecker, blobStoreTaskService);
  }

  @Test
  public void testReadTypesReturnsDescriptorData() {
    BlobStoreDescriptor descriptor = mock(BlobStoreDescriptor.class);
    when(descriptor.getName()).thenReturn("MyType");
    when(descriptor.getFormFields()).thenReturn(Collections.emptyList());
    Map<String, BlobStoreDescriptor> blobStoreDescriptors = Collections.singletonMap("MyType", descriptor);
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
  public void testCreateBlobstoreCreatesAndReturnsNewBlobstore() throws Exception {
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
    when(blobStore.getMetrics()).thenReturn(mock(BlobStoreMetrics.class));

    when(blobStoreManager.create(any(BlobStoreConfiguration.class))).thenReturn(blobStore);
    when(blobStoreManager.newConfiguration()).thenReturn(mock(BlobStoreConfiguration.class));
    when(blobStoreManager.getByName()).thenReturn(Collections.singletonMap("myblobs", blobStore));

    BlobStoreXO createdXO = underTest.create(blobStoreXO);

    verify(blobStoreManager).create(any(BlobStoreConfiguration.class));
    assertThat(createdXO.getName(), is(expectedConfig.getName()));
    assertThat(createdXO.getType(), is(expectedConfig.getType()));
    assertThat(createdXO.getAttributes(), is(expectedConfig.getAttributes()));
  }

  @Test
  public void testRemoveBlobstoreOnlyRemovesUnusedBlobstores() throws Exception {
    when(repositoryManager.isBlobstoreUsed("not-used")).thenReturn(false);

    underTest.remove("not-used");

    verify(blobStoreManager).delete("not-used");

    when(repositoryManager.isBlobstoreUsed("used")).thenReturn(true);
    assertThrows(BlobStoreException.class, () -> underTest.remove("used"));
    verify(blobStoreManager, never()).delete("used");
  }

  @Test
  public void testDefaultWorkDirectoryReturnsTheBlobsDirectory() {
    File blobDirectory = new File("path/to/blobs");
    when(applicationDirectories.getWorkDirectory("blobs")).thenReturn(blobDirectory);

    PathSeparatorXO defaultWorkDirectory = underTest.defaultWorkDirectory();

    assertThat(new File(defaultWorkDirectory.getPath()), is(blobDirectory));
    assertThat(defaultWorkDirectory.getFileSeparator(), is(File.separator));
  }

  @Test
  public void testCreateBlobStoreXOWithQuota() {
    long quotaLimitBytes = (long) (10 * pow(10, 6));
    MockBlobStoreConfiguration config = mockConfig(quotaLimitBytes);

    BlobStore blobStore = mock(BlobStore.class);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(config);
    when(blobStore.getMetrics()).thenReturn(mock(BlobStoreMetrics.class));
    when(blobStoreManager.getByName()).thenReturn(Collections.singletonMap("test", blobStore));

    BlobStoreXO blobStoreXO = underTest.asBlobStoreXO(config);

    assertThat(blobStoreXO.isQuotaEnabled(), is(true));
    assertThat(blobStoreXO.getQuotaType(), is("spaceUsedQuota"));
    assertThat(blobStoreXO.getQuotaLimit(), is(10L));

    config = mockConfig(1);
    blobStoreXO = underTest.asBlobStoreXO(config);
    assertThat(blobStoreXO.getQuotaLimit(), is(0L));
  }

  @Test
  public void testCreateBlobStoreConfigWithQuota() {
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
  public void testRequestingBlobstoreNamesOnlyDoesNotSetOtherProperties() {
    MockBlobStoreConfiguration config = new MockBlobStoreConfiguration().withName("test")
        .withAttributes(
            Map.of("file", Map.of("path", "path"), "blobStoreQuotaConfig",
                Map.of("quotaType", "spaceUsedQuota", "quotaLimitBytes", 7)));
    BlobStore blobStore = mock(BlobStore.class);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(config);
    when(blobStoreManager.getByName()).thenReturn(Collections.singletonMap("test", blobStore));

    BlobStoreXO blobStoreXO = underTest.asBlobStoreXO(config, Collections.emptyList());

    assertThat(blobStoreXO.getName(), is("test"));
    assertThat(blobStoreXO.getType(), is(nullValue()));
    verify(blobStore, never()).getMetrics();
  }

  @Test
  public void testUpdatingS3BlobstoreWithPasswordPlaceholderDoesNotAlterSecretAccessKey() throws Exception {
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
    when(blobStore.getMetrics()).thenReturn(mock(BlobStoreMetrics.class));

    when(blobStoreManager.get("myblobs")).thenReturn(blobStore);
    when(blobStoreManager.newConfiguration()).thenReturn(new MockBlobStoreConfiguration());
    when(blobStoreManager.getByName()).thenReturn(Collections.singletonMap("myblobs", blobStore));
    when(blobStoreManager.update(any(BlobStoreConfiguration.class))).thenReturn(blobStore);

    BlobStoreXO updatedXO = underTest.update(blobStoreXO);

    verify(blobStoreManager).update(blobStoreConfigCaptor.capture());
    BlobStoreConfiguration capturedConfig = blobStoreConfigCaptor.getValue();
    assertThat(capturedConfig.getAttributes().get("s3").get("secretAccessKey"), is(originalSecret));
    assertThat(updatedXO.getAttributes().get("s3").get("secretAccessKey"), is(PasswordPlaceholder.get()));
  }

  @Test
  public void testUpdatingAzureBlobstoreWithPasswordPlaceholderDoesNotAlterAccountKey() throws Exception {
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
    when(blobStore.getMetrics()).thenReturn(mock(BlobStoreMetrics.class));

    when(blobStoreManager.get("myblobs")).thenReturn(blobStore);
    when(blobStoreManager.newConfiguration()).thenReturn(new MockBlobStoreConfiguration());
    when(blobStoreManager.getByName()).thenReturn(Collections.singletonMap("myblobs", blobStore));
    when(blobStoreManager.update(any(BlobStoreConfiguration.class))).thenReturn(blobStore);

    BlobStoreXO updatedXO = underTest.update(blobStoreXO);

    verify(blobStoreManager).update(blobStoreConfigCaptor.capture());
    BlobStoreConfiguration capturedConfig = blobStoreConfigCaptor.getValue();
    assertThat(capturedConfig.getAttributes().get("azure cloud storage").get("accountKey"), is(originalSecret));
    assertThat(updatedXO.getAttributes().get("azure cloud storage").get("accountKey"), is(PasswordPlaceholder.get()));
  }

  @Test
  public void testRemoveBlobstoreDoesNotRemoveBlobstoresPartOfMoveRepositoryTask() throws Exception {
    when(blobStoreTaskService.countTasksInUseForBlobStore("used_in_move")).thenReturn(2);

    try {
      underTest.remove("used_in_move");
    }
    catch (BlobStoreException e) {
      assertThat(e, is(instanceOf(BlobStoreException.class)));
    }

    verify(blobStoreManager, never()).delete("used_in_move");
  }

  private static MockBlobStoreConfiguration mockConfig(final long quotaLimitBytes) {
    return new MockBlobStoreConfiguration().withAttributes(
        Map.of("file", Map.of("path", "path"), "blobStoreQuotaConfig",
            Map.of("quotaType", "spaceUsedQuota", "quotaLimitBytes", quotaLimitBytes)));
  }
}
