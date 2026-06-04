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
package org.sonatype.nexus.blobstore.group;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.cache.Cache;
import javax.cache.configuration.MutableConfiguration;

import org.sonatype.nexus.common.time.Time;
import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.OperationMetrics;
import org.sonatype.nexus.blobstore.api.OperationType;
import org.sonatype.nexus.blobstore.group.internal.WriteToFirstMemberFillPolicy;
import org.sonatype.nexus.cache.CacheHelper;
import org.sonatype.nexus.common.QualifierUtil;

import com.google.common.hash.HashCode;
import jakarta.inject.Provider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.api.OperationType.DOWNLOAD;
import static org.sonatype.nexus.blobstore.api.OperationType.UPLOAD;

@ExtendWith(MockitoExtension.class)
class BlobStoreGroupTest
{
  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private OperationMetrics operationMetrics;

  @Mock
  private FillPolicy testFillPolicy;

  @Mock
  private Cache<BlobId, String> cache;

  @Mock
  private Provider<CacheHelper> cacheHelperProvider;

  @Mock
  private CacheHelper cacheHelper;

  @Mock
  private Blob blobOne;

  @Mock
  private Blob blobTwo;

  @Mock
  private BlobStore one;

  @Mock
  private BlobStore two;

  MockedStatic<QualifierUtil> mockedStatic;

  private BlobStoreGroup blobStore;

  private final BlobStoreConfiguration config = new MockBlobStoreConfiguration();

  private final WriteToFirstMemberFillPolicy writeToFirstMemberFillPolicy = new WriteToFirstMemberFillPolicy();

  @BeforeEach
  void setUp() {
    mockedStatic = mockStatic(QualifierUtil.class);

    lenient().when(cacheHelperProvider.get()).thenReturn(cacheHelper);
    lenient().when(cacheHelper.maybeCreateCache(anyString(), any(MutableConfiguration.class))).thenReturn(cache);
    when(one.getBlobStoreConfiguration()).thenReturn(mock(BlobStoreConfiguration.class));
    when(two.getBlobStoreConfiguration()).thenReturn(mock(BlobStoreConfiguration.class));
    lenient().when(one.getBlobStoreConfiguration().getName()).thenReturn("one");
    lenient().when(two.getBlobStoreConfiguration().getName()).thenReturn("two");
    lenient().when(blobStoreManager.get("one")).thenReturn(one);
    lenient().when(blobStoreManager.get("two")).thenReturn(two);

    Map<String, Provider<FillPolicy>> fillPolicyFactories = new HashMap<>();
    fillPolicyFactories.put("writeToFirst", () -> writeToFirstMemberFillPolicy);
    fillPolicyFactories.put("test", () -> testFillPolicy);
    when(QualifierUtil.buildQualifierBeanMap(Mockito.<List<Provider<FillPolicy>>>any()))
        .thenReturn(fillPolicyFactories);
    blobStore =
        new BlobStoreGroup(blobStoreManager, List.of(), cacheHelperProvider, new Time(2, TimeUnit.DAYS));

    Map<OperationType, OperationMetrics> operationMetricsMap =
        Map.of(DOWNLOAD, operationMetrics, UPLOAD, operationMetrics);
    lenient().when(one.getOperationMetricsDelta()).thenReturn(operationMetricsMap);
  }

  @AfterEach
  void tearDown() {
    mockedStatic.close();
  }

  private static Map<String, Map<String, Object>> buildAttributes(
      final List<String> memberNames,
      final String fillPolicyName)
  {
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    Map<String, Object> group = new HashMap<>();
    group.put("members", memberNames);
    group.put("fillPolicy", fillPolicyName);
    attributes.put("group", group);
    return attributes;
  }

  @Test
  void getWithNoMembers() throws Exception {
    config.setAttributes(buildAttributes(emptyList(), "test"));
    blobStore.init(config);
    blobStore.start();

    Blob foundBlob = blobStore.get(new BlobId("doesntexist"));

    assertThat(foundBlob, nullValue());
  }

  @Test
  void getWithTwoMembers() throws Exception {
    config.setAttributes(buildAttributes(Arrays.asList("one", "two"), "test"));
    blobStore.init(config);
    blobStore.start();
    when(one.exists(any())).thenAnswer(invocation -> invocation.getArgument(0).equals(new BlobId("in_one")));
    when(two.exists(any())).thenAnswer(invocation -> invocation.getArgument(0).equals(new BlobId("in_two")));
    when(one.get(new BlobId("in_one"), false)).thenReturn(blobOne);
    when(two.get(new BlobId("in_two"), false)).thenReturn(blobTwo);

    Blob foundBlob = blobStore.get(new BlobId("in_one"));
    assertThat(foundBlob, is(blobOne));

    foundBlob = blobStore.get(new BlobId("in_two"));
    assertThat(foundBlob, is(blobTwo));

    foundBlob = blobStore.get(new BlobId("doesntexist"));
    assertThat(foundBlob, nullValue());
  }

  @Test
  void twoParamGet() throws Exception {
    config.setAttributes(buildAttributes(Arrays.asList("one", "two"), "test"));
    blobStore.init(config);
    blobStore.start();
    when(one.exists(any())).thenAnswer(invocation -> invocation.getArgument(0).equals(new BlobId("in_one")));
    when(two.exists(any())).thenAnswer(invocation -> invocation.getArgument(0).equals(new BlobId("in_two")));
    when(one.get(new BlobId("in_one"), false)).thenReturn(blobOne);
    when(two.get(new BlobId("in_two"), false)).thenReturn(blobTwo);

    assertBlobStoreGet("doesntexists", false, null);
    assertBlobStoreGet("in_one", false, blobOne);
    assertBlobStoreGet("in_two", false, blobTwo);
  }

  @Test
  void twoParamGetIncludeDeleted() throws Exception {
    config.setAttributes(buildAttributes(Arrays.asList("one", "two"), "test"));
    blobStore.init(config);
    blobStore.start();
    when(one.exists(any())).thenAnswer(invocation -> invocation.getArgument(0).equals(new BlobId("in_one")));
    when(two.exists(any())).thenAnswer(invocation -> {
      BlobId id = invocation.getArgument(0);
      return id.equals(new BlobId("in_two")) || id.equals(new BlobId("deleted_in_two"));
    });
    lenient().when(one.get(new BlobId("in_one"), false)).thenReturn(blobOne);
    lenient().when(two.get(new BlobId("in_two"), false)).thenReturn(blobTwo);
    lenient().when(one.get(new BlobId("in_one"), true)).thenReturn(blobOne);
    lenient().when(two.get(new BlobId("in_two"), true)).thenReturn(blobTwo);
    lenient().when(two.get(new BlobId("deleted_in_two"), true)).thenReturn(blobTwo);

    assertBlobStoreGet("doesntexists", false, null);
    assertBlobStoreGet("in_one", false, blobOne);
    assertBlobStoreGet("in_two", false, blobTwo);
    assertBlobStoreGet("deleted_in_two", false, null);
    assertBlobStoreGet("doesntexists", true, null);
    assertBlobStoreGet("in_one", true, blobOne);
    assertBlobStoreGet("in_two", true, blobTwo);
    assertBlobStoreGet("deleted_in_two", true, blobTwo);
  }

  private void assertBlobStoreGet(final String blobId, final boolean includeDeleted, final Blob expectedBlob) {
    Blob foundBlob = blobStore.get(new BlobId(blobId), includeDeleted);
    assertThat(foundBlob, is(expectedBlob));
  }

  @Test
  void createWithStreamDelegatesToMemberChosenByFillPolicy() throws Exception {
    config.setAttributes(buildAttributes(Arrays.asList("one", "two"), "test"));
    blobStore.init(config);
    blobStore.start();
    ByteArrayInputStream byteStream = new ByteArrayInputStream("".getBytes());
    Blob blob = mock(Blob.class);

    when(testFillPolicy.chooseBlobStore(blobStore, new HashMap<>())).thenReturn(two);
    when(two.create(byteStream, new HashMap<>(), null)).thenReturn(blob);
    when(blob.getId()).thenReturn(new BlobId("created"));

    blobStore.create(byteStream, new HashMap<>());

    verify(testFillPolicy).chooseBlobStore(blobStore, new HashMap<>());
    verify(two).create(byteStream, new HashMap<>(), null);
    verify(one, never()).create(any(), any(), any());
  }

  @Test
  void createWithPathDelegatesToMemberChosenByFillPolicy() throws Exception {
    config.setAttributes(buildAttributes(Arrays.asList("one", "two"), "test"));
    blobStore.init(config);
    blobStore.start();
    Path path = new File(".").toPath();
    long size = 0L;
    HashCode hashCode = HashCode.fromInt(0);
    Blob blob = mock(Blob.class);

    when(testFillPolicy.chooseBlobStore(blobStore, new HashMap<>())).thenReturn(two);
    when(two.create(path, new HashMap<>(), size, hashCode)).thenReturn(blob);
    when(blob.getId()).thenReturn(new BlobId("created"));

    blobStore.create(path, new HashMap<>(), size, hashCode);

    verify(testFillPolicy).chooseBlobStore(blobStore, new HashMap<>());
    verify(two).create(path, new HashMap<>(), size, hashCode);
    verify(one, never()).create(any(), any());
  }

  @Test
  void getBlobStreamIdWithTwoBlobstores() throws Exception {
    config.setAttributes(buildAttributes(Arrays.asList("one", "two"), "test"));
    blobStore.init(config);
    blobStore.start();
    when(one.getBlobIdStream()).thenReturn(Stream.of(new BlobId("a"), new BlobId("b"), new BlobId("c")));
    when(two.getBlobIdStream()).thenReturn(Stream.of(new BlobId("d"), new BlobId("e"), new BlobId("f")));

    Stream<BlobId> stream = blobStore.getBlobIdStream();

    List<String> result = stream.map(BlobId::toString).collect(Collectors.toList());
    assertThat(result, is(Arrays.asList("a", "b", "c", "d", "e", "f")));
  }

  @Test
  void deleteWithTwoMembers() throws Exception {
    config.setAttributes(buildAttributes(Arrays.asList("one", "two"), "test"));
    blobStore.init(config);
    blobStore.start();
    when(one.exists(any())).thenAnswer(invocation -> {
      BlobId id = invocation.getArgument(0);
      return id.equals(new BlobId("in_one")) || id.equals(new BlobId("in_both"));
    });
    when(two.exists(any())).thenAnswer(invocation -> {
      BlobId id = invocation.getArgument(0);
      return id.equals(new BlobId("in_two")) || id.equals(new BlobId("in_both"));
    });
    when(one.delete(eq(new BlobId("in_one")), anyString())).thenReturn(true);
    when(one.delete(eq(new BlobId("in_both")), anyString())).thenReturn(true);
    when(two.delete(eq(new BlobId("in_two")), anyString())).thenReturn(true);
    when(two.delete(eq(new BlobId("in_both")), anyString())).thenReturn(false);

    assertBlobStoreDelete("doesntexists", false);
    assertBlobStoreDelete("in_one", true);
    assertBlobStoreDelete("in_two", true);
    assertBlobStoreDelete("in_both", false);
  }

  @Test
  void deleteHardWithTwoMembers() throws Exception {
    config.setAttributes(buildAttributes(Arrays.asList("one", "two"), "test"));
    blobStore.init(config);
    blobStore.start();
    when(one.exists(any())).thenAnswer(invocation -> {
      BlobId id = invocation.getArgument(0);
      return id.equals(new BlobId("in_one")) || id.equals(new BlobId("in_both"));
    });
    when(two.exists(any())).thenAnswer(invocation -> {
      BlobId id = invocation.getArgument(0);
      return id.equals(new BlobId("in_two")) || id.equals(new BlobId("in_both"));
    });
    when(one.deleteHard(new BlobId("in_one"))).thenReturn(true);
    when(one.deleteHard(new BlobId("in_both"))).thenReturn(true);
    when(two.deleteHard(new BlobId("in_two"))).thenReturn(true);
    when(two.deleteHard(new BlobId("in_both"))).thenReturn(false);

    assertBlobStoreDeleteHard("doesntexists", false);
    assertBlobStoreDeleteHard("in_one", true);
    assertBlobStoreDeleteHard("in_two", true);
    assertBlobStoreDeleteHard("in_both", false);
  }

  private void assertBlobStoreDeleteHard(final String blobId, final boolean expectedDeleted) {
    boolean deleted = blobStore.deleteHard(new BlobId(blobId));
    assertThat(deleted, is(expectedDeleted));
  }

  private void assertBlobStoreDelete(final String blobId, final boolean expectedDeleted) {
    boolean deleted = blobStore.delete(new BlobId(blobId), "just because");
    assertThat(deleted, is(expectedDeleted));
  }

  @Test
  void fallBackOnDefaultFillPolicyIfNamedPolicyNotFound() {
    config.setAttributes(buildAttributes(Arrays.asList("one", "two"), "nonExistentPolicy"));

    blobStore.init(config);

    assertThat(blobStore.fillPolicy, is(writeToFirstMemberFillPolicy));
  }

  @Test
  void itWillSearchWritableBlobStoresFirst() throws Exception {
    config.setAttributes(buildAttributes(Arrays.asList("writableMember", "nonWritableMember"), "test"));
    blobStore.init(config);
    blobStore.start();
    when(blobStoreManager.get("writableMember")).thenReturn(one);
    when(blobStoreManager.get("nonWritableMember")).thenReturn(two);
    when(one.isWritable()).thenReturn(true);
    when(two.isWritable()).thenReturn(false);

    BlobId blobId = new BlobId("BLOB_ID_VALUE");

    when(one.exists(blobId)).thenReturn(true);

    Optional<BlobStore> locatedMember = blobStore.locateBlobStore(blobId);

    verify(one).exists(blobId);
    verify(two, never()).exists(blobId);
    assertThat(locatedMember.get(), is(one));
    verify(cache).put(blobId, "one");
  }

  @Test
  void itWillOnlyCacheBlobIdsOfWritableBlobStores() throws Exception {
    config.setAttributes(buildAttributes(Arrays.asList("writableMember", "nonWritableMember"), "test"));
    blobStore.init(config);
    blobStore.start();
    when(blobStoreManager.get("writableMember")).thenReturn(one);
    when(blobStoreManager.get("nonWritableMember")).thenReturn(two);
    when(one.isWritable()).thenReturn(true);
    when(two.isWritable()).thenReturn(false);

    BlobId blobId = new BlobId("BLOB_ID_VALUE");

    when(one.exists(blobId)).thenReturn(false);
    when(two.exists(blobId)).thenReturn(true);

    Optional<BlobStore> locatedMember = blobStore.locateBlobStore(blobId);

    verify(one).exists(blobId);
    verify(two).exists(blobId);
    assertThat(locatedMember.get(), is(two));
    verify(cache, never()).put(any(), any());
  }

  @Test
  void locateBlobStore_withValidCacheEntry_returnsCachedMemberWithoutSearch() throws Exception {
    config.setAttributes(buildAttributes(Arrays.asList("one", "two"), "test"));
    blobStore.init(config);
    blobStore.start();

    BlobId blobId = new BlobId("BLOB_ID_VALUE");

    when(cache.get(blobId)).thenReturn("one");
    when(blobStoreManager.get("one")).thenReturn(one);

    Optional<BlobStore> result = blobStore.locateBlobStore(blobId);

    assertThat(result.get(), is(one));
    verify(one, never()).exists(any());
    verify(two, never()).exists(any());
  }

  @Test
  void locateBlobStore_withMemberGoneFromManager_evictsAndSearchesOtherMembers() throws Exception {
    config.setAttributes(buildAttributes(Arrays.asList("one", "two"), "test"));
    blobStore.init(config);
    blobStore.start();

    BlobId blobId = new BlobId("BLOB_ID_VALUE");

    when(cache.get(blobId)).thenReturn("removed-member");
    when(blobStoreManager.get("removed-member")).thenReturn(null);
    when(one.exists(blobId)).thenReturn(true);
    when(one.isWritable()).thenReturn(true);

    Optional<BlobStore> locatedMember = blobStore.locateBlobStore(blobId);

    assertThat(locatedMember.get(), is(one));
    verify(cache).remove(blobId);
    verify(cache).put(blobId, "one");
  }

  @Test
  void locateBlobStore_blobNotFoundInAnyMember_returnsEmpty() throws Exception {
    config.setAttributes(buildAttributes(Arrays.asList("one", "two"), "test"));
    blobStore.init(config);
    blobStore.start();

    BlobId blobId = new BlobId("BLOB_ID_VALUE");
    when(one.exists(blobId)).thenReturn(false);
    when(two.exists(blobId)).thenReturn(false);

    Optional<BlobStore> result = blobStore.locateBlobStore(blobId);

    assertThat(result.isPresent(), is(false));
    verify(cache, never()).put(any(), any());
  }

  @Test
  void locateBlob_withValidCacheEntry_returnsBlobWithoutSearch() throws Exception {
    config.setAttributes(buildAttributes(Arrays.asList("one", "two"), "test"));
    blobStore.init(config);
    blobStore.start();

    BlobId blobId = new BlobId("BLOB_ID_VALUE");
    Blob blob = mock(Blob.class);

    when(cache.get(blobId)).thenReturn("one");
    when(blobStoreManager.get("one")).thenReturn(one);
    when(one.get(blobId, false)).thenReturn(blob);

    Optional<Blob> result = blobStore.locateBlob(blobId, false);

    assertThat(result.get(), is(blob));
    verify(two, never()).exists(any());
    verify(two, never()).get(any(), anyBoolean());
  }

  @Test
  void locateBlob_withStaleCacheEntry_evictsAndSearchesOtherMembers() throws Exception {
    config.setAttributes(buildAttributes(Arrays.asList("one", "two"), "test"));
    blobStore.init(config);
    blobStore.start();

    BlobId blobId = new BlobId("BLOB_ID_VALUE");
    Blob blob = mock(Blob.class);

    when(cache.get(blobId)).thenReturn("one");
    when(blobStoreManager.get("one")).thenReturn(one);
    when(one.get(blobId, false)).thenReturn(null);
    when(one.exists(blobId)).thenReturn(false);
    when(two.exists(blobId)).thenReturn(true);
    when(two.get(blobId, false)).thenReturn(blob);
    when(two.isWritable()).thenReturn(true);

    Optional<Blob> result = blobStore.locateBlob(blobId, false);

    assertThat(result.get(), is(blob));
    verify(cache).remove(blobId);
    verify(cache).put(blobId, "two");
  }

  @Test
  void locateBlob_withStaleCacheEntry_foundInNonWritableMember_doesNotCache() throws Exception {
    config.setAttributes(buildAttributes(Arrays.asList("one", "two"), "test"));
    blobStore.init(config);
    blobStore.start();

    BlobId blobId = new BlobId("BLOB_ID_VALUE");
    Blob blob = mock(Blob.class);

    when(cache.get(blobId)).thenReturn("one");
    when(blobStoreManager.get("one")).thenReturn(one);
    when(one.get(blobId, false)).thenReturn(null);
    when(one.exists(blobId)).thenReturn(false);
    when(two.exists(blobId)).thenReturn(true);
    when(two.get(blobId, false)).thenReturn(blob);
    when(two.isWritable()).thenReturn(false);

    Optional<Blob> result = blobStore.locateBlob(blobId, false);

    assertThat(result.get(), is(blob));
    verify(cache).remove(blobId);
    verify(cache, never()).put(any(), any());
  }

  @Test
  void locateBlob_withSoftDeletedBlob_doesNotEvictCacheOrSearch() throws Exception {
    config.setAttributes(buildAttributes(Arrays.asList("one", "two"), "test"));
    blobStore.init(config);
    blobStore.start();

    BlobId blobId = new BlobId("BLOB_ID_VALUE");

    when(cache.get(blobId)).thenReturn("one");
    when(blobStoreManager.get("one")).thenReturn(one);
    // get() returns null because the blob is soft-deleted, but exists() is true
    when(one.get(blobId, false)).thenReturn(null);
    when(one.exists(blobId)).thenReturn(true);

    Optional<Blob> result = blobStore.locateBlob(blobId, false);

    assertThat(result.isPresent(), is(false));
    verify(cache, never()).remove(blobId);
    verify(two, never()).exists(any());
  }

  @Test
  void locateBlob_blobNotFoundInAnyMember_returnsEmpty() throws Exception {
    config.setAttributes(buildAttributes(Arrays.asList("one", "two"), "test"));
    blobStore.init(config);
    blobStore.start();

    BlobId blobId = new BlobId("BLOB_ID_VALUE");
    when(one.exists(blobId)).thenReturn(false);
    when(two.exists(blobId)).thenReturn(false);

    Optional<Blob> result = blobStore.locateBlob(blobId, false);

    assertThat(result.isPresent(), is(false));
    verify(cache, never()).put(any(), any());
  }

  @Test
  void invalidateCachedLocation_removesEntryFromCache() throws Exception {
    config.setAttributes(buildAttributes(Arrays.asList("one", "two"), "test"));
    blobStore.init(config);
    blobStore.start();

    BlobId blobId = new BlobId("BLOB_ID_VALUE");

    blobStore.invalidateCachedLocation(blobId);

    verify(cache).remove(blobId);
  }

  @Test
  void testMakeBlobPermanent() throws Exception {
    config.setAttributes(buildAttributes(Arrays.asList("one", "two"), "test"));
    blobStore.init(config);
    blobStore.start();

    // The blobstore that doesn't own the blob returns null
    Blob response = mock(Blob.class);
    when(one.isOwner(any())).thenReturn(false);
    when(two.isOwner(any())).thenReturn(true);
    when(two.makeBlobPermanent(any(), any())).thenReturn(response);

    Blob result = blobStore.makeBlobPermanent(response, Map.of());

    assertThat(result, not(nullValue()));
    verify(one).isOwner(any());
    verify(two).isOwner(any());
    verify(two).makeBlobPermanent(any(), any());

    // We want to be sure copy is never called for cloud blobstores
    verify(one, never()).copy(any(), any());
    verify(two, never()).copy(any(), any());
  }

  @Test
  void testDeleteIfTemp() throws Exception {
    config.setAttributes(buildAttributes(Arrays.asList("one", "two"), "test"));
    blobStore.init(config);
    blobStore.start();
    when(one.deleteIfTemp(any())).thenReturn(true);

    assertThat(blobStore.deleteIfTemp(mock(Blob.class)), is(true));
    verify(one).deleteIfTemp(any());
    verify(two, never()).deleteIfTemp(any());
  }

  @Test
  void testDeleteIfTemp_missing() throws Exception {
    config.setAttributes(buildAttributes(Arrays.asList("one", "two"), "test"));
    blobStore.init(config);
    blobStore.start();

    assertThat(blobStore.deleteIfTemp(mock(Blob.class)), is(false));
    verify(one).deleteIfTemp(any());
    verify(two).deleteIfTemp(any());
  }
}
