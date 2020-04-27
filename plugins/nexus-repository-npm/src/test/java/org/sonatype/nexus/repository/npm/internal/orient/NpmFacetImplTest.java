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
package org.sonatype.nexus.repository.npm.internal.orient;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.npm.internal.NpmFormat;
import org.sonatype.nexus.repository.npm.internal.NpmPackageId;
import org.sonatype.nexus.repository.npm.internal.NpmPackageParser;
import org.sonatype.nexus.repository.npm.internal.search.legacy.NpmSearchIndexInvalidatedEvent;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.transaction.UnitOfWork;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;

import static java.util.Collections.singletonMap;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.npm.internal.orient.NpmFacetUtils.REPOSITORY_ROOT_ASSET;

public class NpmFacetImplTest
    extends TestSupport
{
  private static final String ASSET_KIND_KEY = "asset_kind";

  private static final String ASSET_KIND_VALUE = "TARBALL";

  private static final String FORMAT_ATTRIBUTE_KEY = "name";

  private static final String FORMAT_ATTRIBUTE_VALUE = "foo";

  private final Map<String, Object> FORMAT_ATTRIBUTES = singletonMap(FORMAT_ATTRIBUTE_KEY, FORMAT_ATTRIBUTE_VALUE);

  private NpmFacetImpl underTest;

  private NpmPackageId packageId;

  @Mock
  private NpmPackageParser npmPackageParser;

  @Mock
  private EventManager eventManager;

  @Mock
  private Repository repository;

  @Mock
  private NpmFormat format;

  @Mock
  private StorageTx tx;

  @Mock
  private Iterable<Component> iterable;

  @Mock
  private Iterator<Component> iterator;

  @Mock
  private Bucket bucket;

  @Mock
  private AssetBlob assetBlob;

  @Mock
  private BlobRef blobRef;

  @Mock
  private Blob blob;

  @Mock
  private Component component;

  @Mock
  private Asset asset;

  @Mock
  private NestedAttributesMap attributesMap;

  @Mock
  private NestedAttributesMap formatAttributes;

  @Mock
  private InputStream inputStream;

  @Before
  public void setUp() throws Exception {
    underTest = new NpmFacetImpl(npmPackageParser);
    underTest.installDependencies(eventManager);
    underTest.attach(repository);

    packageId = new NpmPackageId(null, "query-string");

    UnitOfWork.beginBatch(tx);

    when(repository.getFormat()).thenReturn(format);
    when(tx.findBucket(repository)).thenReturn(bucket);
    when(tx.createAsset(any(), any(Component.class))).thenReturn(asset);
    when(tx.createAsset(any(), any(Format.class))).thenReturn(asset);
    when(tx.createComponent(eq(bucket), any())).thenReturn(component);
    when(tx.findComponents(any(), any())).thenReturn(iterable);
    when(tx.requireBlob(blobRef)).thenReturn(blob);
    when(iterable.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(false);
    when(component.group(any())).thenReturn(component);
    when(component.name(any())).thenReturn(component);
    when(component.version(any())).thenReturn(component);
    when(asset.formatAttributes()).thenReturn(formatAttributes);
    when(asset.attributes()).thenReturn(attributesMap);
    when(attributesMap.child(any())).thenReturn(attributesMap);
    when(asset.name(any())).thenReturn(asset);
    when(assetBlob.getBlobRef()).thenReturn(blobRef);
    when(blob.getInputStream()).thenReturn(inputStream);
    when(npmPackageParser.parsePackageJson(any(Supplier.class))).thenReturn(FORMAT_ATTRIBUTES);
  }

  @After
  public void tearDown() throws Exception {
    UnitOfWork.end();
  }

  @Test
  public void testPutPackageRoot() throws Exception {
    underTest.putPackageRoot(packageId.id(), assetBlob, null);

    verifyAssetCreated(packageId.id());
  }

  @Test
  public void testPutRepositoryRoot() throws Exception {
    underTest.putRepositoryRoot(assetBlob, null);

    verifyAssetCreated(REPOSITORY_ROOT_ASSET);
    verify(eventManager).post(any(NpmSearchIndexInvalidatedEvent.class));
  }

  @Test
  public void testPutTarball() throws Exception {
    String tarball = "query-string.tgz";
    underTest.putTarball(packageId.id(), tarball, assetBlob, null);

    verify(tx).createAsset(eq(bucket), eq(component));
    verify(tx).saveAsset(asset);

    verifyTarballAndVersion(packageId, tarball, "");
  }

  @Test
  public void testReleaseVersion() throws Exception {
    String version = "1.0.0";
    String tarball = "query-string-" + version + ".tgz";

    underTest.putTarball(packageId.id(), tarball, assetBlob, null);

    verifyTarballAndVersion(packageId, tarball, version);
  }

  @Test
  public void testReleaseVersionWithMultiDigit() throws Exception {
    String version = "12.345.6789-beta.15";
    String tarball = "query-string-" + version + ".tgz";

    underTest.putTarball(packageId.id(), tarball, assetBlob, null);

    verifyTarballAndVersion(packageId, tarball, version);
  }

  @Test
  public void testPrereleaseVersion() throws Exception {
    NpmPackageId packageId = new NpmPackageId(null, "query-string");
    String version = "1.0.0-alpha." + System.currentTimeMillis() + ".500-beta";
    String tarball = "query-string-" + version + ".tgz";

    underTest.putTarball(packageId.id(), tarball, assetBlob, null);

    verifyTarballAndVersion(packageId, tarball, version);
  }

  @Test
  public void testVersionWithMetadata() throws Exception {
    NpmPackageId packageId = new NpmPackageId(null, "query-string");
    String version = "1.0.0-alpha." + System.currentTimeMillis() + ".500-beta+meta.data";
    String tarball = "query-string-" + version + ".tgz";

    underTest.putTarball(packageId.id(), tarball, assetBlob, null);

    verifyTarballAndVersion(packageId, tarball, version);
  }

  private void verifyAssetCreated(final String name) throws Exception {
    verify(tx).createAsset(eq(bucket), eq(format));
    verify(tx).saveAsset(asset);
    verify(asset).name(eq(name));
  }

  private void verifyTarballAndVersion(final NpmPackageId packageId, final String tarball, final String version) throws Exception {
    verify(tx).createComponent(bucket, repository.getFormat());
    verify(component).name(eq(packageId.name()));
    verify(component).version(eq(version));

    verify(asset).name(Matchers.eq(NpmFacetUtils.tarballAssetName(packageId, tarball)));
    verify(formatAttributes).set(ASSET_KIND_KEY, ASSET_KIND_VALUE);
    verify(formatAttributes).set(FORMAT_ATTRIBUTE_KEY, FORMAT_ATTRIBUTE_VALUE);
  }
}
