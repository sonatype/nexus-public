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
package org.sonatype.nexus.repository.npm.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.common.Time;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.goodies.testsupport.concurrent.ConcurrentRunner;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.io.CooperationFactory;
import org.sonatype.nexus.common.io.LocalCooperationFactory;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.MissingAssetBlobException;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher.State;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.transaction.UnitOfWork;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atMost;
import static org.sonatype.nexus.repository.npm.internal.NpmHandlers.T_PACKAGE_NAME;
import static org.sonatype.nexus.repository.npm.internal.NpmJsonUtils.bytes;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.VERSIONS;

public class NpmGroupFacetTest
    extends TestSupport
{
  private static final NpmPackageId A = NpmPackageId.parse("a");

  private static final NpmPackageId SCOPED = NpmPackageId.parse("@scope/a");

  private static final Time MAJOR_TIMEOUT = Time.seconds(60);

  private static final Time MINOR_TIMEOUT = Time.seconds(60);

  @Mock
  private ConstraintViolationFactory constraintViolationFactory;

  @Mock
  private Response proxyResponse, hostedResponse;

  @Mock
  private Repository proxyRepository, hostedRepository, groupRepository;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private Request request;

  @Mock
  private Context context;

  @Mock
  private State state;

  @Mock
  private StorageFacet storageFacet;

  @Mock
  private StorageTx storageTx;

  @Mock
  private Blob blob;

  @Mock
  private BlobRef blobRef;

  @Mock
  private BlobStore blobStore;

  @Mock
  private Asset packageRootAsset;

  @Mock
  private AssetBlob assetBlob;

  @Mock
  private Configuration configuration;

  @Mock
  private MissingAssetBlobException missingAssetBlobException;

  @Mock
  private Asset asset;

  @Mock
  private ConfigurationFacet configurationFacet;

  private CooperationFactory cooperationFactory = new LocalCooperationFactory();

  private NpmGroupFacet underTest;

  @Before
  public void setUp() throws Exception {
    BaseUrlHolder.set("http://localhost:8080/");

    setupNpmGroupFacet();
    underTest.attach(groupRepository);

    when(state.getTokens()).thenReturn(ImmutableMap.of(T_PACKAGE_NAME, "test"));

    AttributesMap attributesMap = new AttributesMap();
    attributesMap.set(TokenMatcher.State.class, state);

    when(context.getAttributes()).thenReturn(attributesMap);
    when(context.getRepository()).thenReturn(groupRepository);
    when(context.getRequest()).thenReturn(request);
    when(request.getPath()).thenReturn("/simple");

    when(groupRepository.getName()).thenReturn(NpmGroupFacetTest.class.getSimpleName() + "-group");
    when(groupRepository.getFormat()).thenReturn(new NpmFormat());
    when(groupRepository.facet(ConfigurationFacet.class)).thenReturn(configurationFacet);
    when(groupRepository.facet(StorageFacet.class)).thenReturn(storageFacet);

    when(packageRootAsset.formatAttributes()).thenReturn(new NestedAttributesMap("metadata", new HashMap<>()));
    when(packageRootAsset.attributes()).thenReturn(new NestedAttributesMap("content", new HashMap<>()));
    when(packageRootAsset.name(any())).thenReturn(packageRootAsset);
    when(packageRootAsset.requireBlobRef()).thenReturn(blobRef);

    when(storageFacet.txSupplier()).thenReturn(() -> storageTx);
    when(storageFacet.blobStore()).thenReturn(blobStore);

    when(blobStore.get(any())).thenReturn(blob);

    when(storageTx.createAsset(any(), any(NpmFormat.class))).thenReturn(packageRootAsset);
    when(storageTx.createBlob(any(), any(), any(), any(), any(), anyBoolean())).thenReturn(assetBlob);
    when(storageTx.requireBlob(blobRef)).thenReturn(blob);

    when(asset.blobRef()).thenReturn(blobRef);
    when(missingAssetBlobException.getAsset()).thenReturn(asset);

    underTest.doInit(configuration);

    UnitOfWork.beginBatch(storageTx);
  }

  @After
  public void tearDown() {
    UnitOfWork.end();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldRunConcurrentWithoutCooperation() throws Exception {
    int invocationsCount = concurrentlyBuildPackageRoot(false);
    verify(underTest, times(invocationsCount)).buildMergedPackageRoot(anyMap(), eq(context));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldRunConcurrentWithCooperation() throws Exception {
    int invocationsCount = concurrentlyBuildPackageRoot(true);

    // we should have at least have used cooperation once
    verify(underTest, atMost(invocationsCount - 1)).buildMergedPackageRoot(anyMap(), eq(context));
  }

  @Test
  public void whenOnlyOneResponseUpdatePackageRoot() throws IOException {
    when(proxyResponse.getPayload()).thenReturn(toContent(createSimplePackageRoot("1.0")));

    buildMergedPackageRoot(ImmutableMap.of(proxyRepository, proxyResponse));

    assertMergedSimplePackageRoot(captureGroupStoredBlobInputStream(), "1.0");
  }

  @Test
  public void whenMultipleSameResponsesBuildNewMergedPackageRoot() throws IOException {
    NestedAttributesMap packageRoot = createSimplePackageRoot("1.0");
    when(proxyResponse.getPayload()).thenReturn(toContent(packageRoot));
    when(hostedResponse.getPayload()).thenReturn(toContent(packageRoot));

    buildMergedPackageRoot(ImmutableMap.of(proxyRepository, proxyResponse, hostedRepository, hostedResponse));

    assertMergedSimplePackageRoot(captureGroupStoredBlobInputStream(), "1.0");
  }

  @Test
  public void whenMultipleDifferentResponsesBuildNewMergedPackageRoot() throws IOException {
    when(proxyResponse.getPayload()).thenReturn(toContent(createSimplePackageRoot("1.0")));
    when(hostedResponse.getPayload()).thenReturn(toContent(createSimplePackageRoot("2.0")));

    buildMergedPackageRoot(ImmutableMap.of(proxyRepository, proxyResponse, hostedRepository, hostedResponse));

    assertMergedSimplePackageRoot(captureGroupStoredBlobInputStream(), "1.0", "2.0");
  }

  @Test
  public void whenMultipleDifferentResponsesWithDisabledMergeUpdatePackageRoot() throws Exception {
    setupMergeDisabledNpmGroupFacet();
    underTest.attach(groupRepository);
    underTest.doInit(configuration);

    when(proxyResponse.getPayload()).thenReturn(toContent(createSimplePackageRoot("1.0")));
    when(hostedResponse.getPayload()).thenReturn(toContent(createSimplePackageRoot("2.0")));

    buildMergedPackageRoot(ImmutableMap.of(proxyRepository, proxyResponse, hostedRepository, hostedResponse));

    assertMergedSimplePackageRoot(captureGroupStoredBlobInputStream(), "1.0");
  }

  @Test
  public void whenSingleOrUnScopedResultShouldNotMerge() {
    assertThat(underTest.shouldServeFirstResult(createRandomMaps(1), A), equalTo(true));
    assertThat(underTest.shouldServeFirstResult(createRandomMaps(1), SCOPED), equalTo(true));

    setupMergeDisabledNpmGroupFacet();
    assertThat(underTest.shouldServeFirstResult(createRandomMaps(1), A), equalTo(true));
    assertThat(underTest.shouldServeFirstResult(createRandomMaps(2), A), equalTo(true));
  }

  @Test
  public void whenMultipleResultShouldMerge() {
    assertThat(underTest.shouldServeFirstResult(createRandomMaps(2), A), equalTo(false));
    assertThat(underTest.shouldServeFirstResult(createRandomMaps(2), SCOPED), equalTo(false));

    setupMergeDisabledNpmGroupFacet();
    assertThat(underTest.shouldServeFirstResult(createRandomMaps(2), SCOPED), equalTo(false));
  }

  @Test
  public void whenBuildMergedPackageRootOnMissingBlob_Should_DeleteAsset_And_GetBlobInputStream() throws IOException {
    when(proxyResponse.getPayload()).thenReturn(toContent(createSimplePackageRoot("1.0")));
    when(hostedResponse.getPayload()).thenReturn(toContent(createSimplePackageRoot("2.0")));

    Map<Repository, Response> responses = ImmutableMap
        .of(proxyRepository, proxyResponse, hostedRepository, hostedResponse);

    underTest.buildMergedPackageRootOnMissingBlob(responses, context, missingAssetBlobException);

    verify(storageTx).deleteAsset(eq(asset), eq(false));
    verify(blob).getInputStream();
  }

  @Test
  public void whenBuildMergedPackageRootOnMissingBlob_Should_DeleteAsset_And_GetErrorStream() throws IOException {

    try (InputStream inputStream =
             underTest.buildMergedPackageRootOnMissingBlob(newHashMap(), context, missingAssetBlobException)) {
      NestedAttributesMap map = NpmJsonUtils.parse(() -> inputStream);
      assertThat(map.get("success"), equalTo(false));
      assertThat(map.get("error"), equalTo(
          "Failed to stream response due to: Unable to retrieve merged package root on recovery for missing blob"));
    }

    verify(storageTx).deleteAsset(eq(asset), eq(false));
    verify(blob, never()).getInputStream();
  }

  private void setupNpmGroupFacet() {
    underTest = spy(new NpmGroupFacet(true, repositoryManager, constraintViolationFactory, new GroupType()));
  }

  private void setupMergeDisabledNpmGroupFacet() {
    underTest = spy(new NpmGroupFacet(false, repositoryManager, constraintViolationFactory, new GroupType()));
  }

  private int concurrentlyBuildPackageRoot(final boolean cooperationEnabled) throws Exception
  {
    int instancesCount = 100;

    setupCooperation(cooperationEnabled, instancesCount);

    final int iterations = 2;
    final int iterationTimeoutSeconds = 60;

    ConcurrentRunner runner = new ConcurrentRunner(iterations, iterationTimeoutSeconds);
    runner.addTask(instancesCount, () -> {
      UnitOfWork.beginBatch(storageTx);
      try {
        underTest.buildPackageRoot(newHashMap(), context);
      }
      finally {
        UnitOfWork.end();
      }
    });
    runner.go();

    int invocationsCount = runner.getTaskCount() * runner.getIterations();
    assertThat(runner.getRunInvocations(), is(invocationsCount));
    return invocationsCount;
  }


  private void setupCooperation(final boolean cooperationEnabled, final int instancesCount) {
    underTest.configureCooperation(
        cooperationFactory,
        cooperationEnabled,
        MAJOR_TIMEOUT,
        MINOR_TIMEOUT,
        2 * instancesCount);

    underTest.buildCooperation();
  }

  private Content toContent(final NestedAttributesMap packageRoot) {
    Content content = new Content(new BytesPayload(bytes(packageRoot), ContentTypes.APPLICATION_JSON));
    content.getAttributes().set(NestedAttributesMap.class, packageRoot);
    return content;
  }

  private Content buildMergedPackageRoot(final Map<Repository, Response> responses) throws IOException {
    Content mergedPackageRoot = underTest.buildMergedPackageRoot(responses, context);

    assertThat(mergedPackageRoot, notNullValue());
    verify(storageTx).createAsset(any(), any(NpmFormat.class));

    return mergedPackageRoot;
  }

  private NestedAttributesMap createSimplePackageRoot(final String version) {
    NestedAttributesMap packageRoot = new NestedAttributesMap("simple", newHashMap());
    packageRoot.set("_id", "id");
    packageRoot.set("_rev", "1");

    NestedAttributesMap versionAttributes = packageRoot.child(VERSIONS).child(version);
    versionAttributes.set("version", version);
    versionAttributes.set("name", "package");
    versionAttributes.child("dist").set("tarball", "http://example.com/path/package-" + version + ".tgz");

    return packageRoot;
  }

  private void assertMergedSimplePackageRoot(final Supplier<InputStream> supplier, final String... versions)
      throws IOException
  {
    NestedAttributesMap result = NpmJsonUtils.parse(supplier);
    assertThat(result, notNullValue());
    assertThat(result.backing(), not(hasKey("_id")));
    assertThat(result.backing(), not(hasKey("_rev")));

    for (String version : versions) {
      assertThat(result.child(VERSIONS).child(version).child("dist").get("tarball", String.class),
          equalTo(format("%s/repository/%s/package/-/package-%s.tgz",
              BaseUrlHolder.get(), groupRepository.getName(), version)));
    }
  }

  private List<NestedAttributesMap> createRandomMaps(final int number) {
    List<NestedAttributesMap> list = new ArrayList<>();
    for (int i = 0; i < number; i++) {
      list.add(new NestedAttributesMap("foo", newHashMap()));
    }
    return list;
  }

  @SuppressWarnings("unchecked")
  private Supplier<InputStream> captureGroupStoredBlobInputStream() throws IOException {
    ArgumentCaptor argumentCaptor = ArgumentCaptor.forClass(Supplier.class);

    verify(storageTx).createBlob(any(),
        (Supplier<InputStream>) argumentCaptor.capture()
        , any(), any(), any(), anyBoolean());


    return (Supplier<InputStream>) argumentCaptor.getValue();
  }
}
