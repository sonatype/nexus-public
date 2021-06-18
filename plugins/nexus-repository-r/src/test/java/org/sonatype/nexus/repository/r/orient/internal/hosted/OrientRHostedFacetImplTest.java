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
package org.sonatype.nexus.repository.r.orient.internal.hosted;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.repository.r.orient.OrientRFacet;
import org.sonatype.nexus.repository.r.orient.internal.OrientRepositoryFacetTestSupport;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_DEPENDS;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_IMPORTS;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_LICENSE;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_LINKINGTO;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_NEEDS_COMPILATION;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_PACKAGE;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_SUGGESTS;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_VERSION;
import static org.sonatype.nexus.repository.r.internal.util.RDescriptionUtils.extractDescriptionFromArchive;
import static org.sonatype.nexus.repository.r.internal.util.RMetadataUtils.HASH_ALGORITHMS;

public class OrientRHostedFacetImplTest
    extends OrientRepositoryFacetTestSupport<OrientRHostedFacetImpl>
{
  static final String PACKAGES_GZ = "PACKAGES.gz";

  static final String PACKAGE_NAME = "package.gz";

  static final String REAL_PACKAGE = "r-package.zip";

  static final String BASE_PATH = "packages/base/path";

  static final String PACKAGE_PATH = BASE_PATH + "/" + PACKAGE_NAME;

  static final String REAL_PACKAGE_PATH = BASE_PATH + "/" + REAL_PACKAGE;

  static final String PACKAGES_GZ_PATH = BASE_PATH + "/" + PACKAGES_GZ;

  static final String VERSION = "1.0.0";

  static final String DEPENDS = "a,b,c";

  static final String IMPORTS = "d,e,f";

  static final String SUGGESTS = "g,h,i";

  static final String LINKINGTO = "Rcpp";

  static final String LICENSE = "MIT";

  static final String NEEDS_COMPILATION = "true";

  @Mock
  TempBlob tempBlob;

  @Mock
  Content payload;

  @Mock
  AssetBlob assetBlob;

  @Mock
  Component component;

  @Mock
  OrientRFacet rFacet;

  @Override
  protected OrientRHostedFacetImpl initialiseSystemUnderTest() {
    return new OrientRHostedFacetImpl();
  }

  @Before
  public void setup() {
    when(formatAttributes.get(P_PACKAGE, String.class)).thenReturn(PACKAGE_NAME);
    when(formatAttributes.get(P_VERSION, String.class)).thenReturn(VERSION);
    when(formatAttributes.get(P_DEPENDS, String.class)).thenReturn(DEPENDS);
    when(formatAttributes.get(P_IMPORTS, String.class)).thenReturn(IMPORTS);
    when(formatAttributes.get(P_SUGGESTS, String.class)).thenReturn(SUGGESTS);
    when(formatAttributes.get(P_LINKINGTO, String.class)).thenReturn(LINKINGTO);
    when(formatAttributes.get(P_LICENSE, String.class)).thenReturn(LICENSE);
    when(formatAttributes.get(P_NEEDS_COMPILATION, String.class)).thenReturn(NEEDS_COMPILATION);
    when(asset.name()).thenReturn(PACKAGE_PATH);
    when(repository.facet(OrientRFacet.class)).thenReturn(rFacet);
    when(rFacet.findOrCreateAsset(any(), any())).thenReturn(asset);
    when(rFacet.findOrCreateAsset(any(), any(), any(), any())).thenReturn(asset);
  }

  @Test
  public void getContentReturnsCorrectContentType() throws Exception {
    when(asset.requireContentType()).thenReturn("application/x-gzip");
    Content packages = underTest.getStoredContent(PACKAGE_PATH);
    assertThat(packages.getContentType(), is(equalTo("application/x-gzip")));
  }

  @Test
  public void getStoredContent() throws Exception {
    Content archive = underTest.getStoredContent(PACKAGE_PATH);
    assertThat(archive, is(notNullValue()));
  }

  @Test(expected = NullPointerException.class)
  public void failFastOnGetContentWithNull() throws Exception {
    underTest.getStoredContent(null);
  }

  @Test
  public void nullWhenAssetNullOnGetContent() throws Exception {
    when(storageTx.findAssetWithProperty(anyString(), anyString(), any(Bucket.class))).thenReturn(null);
    Content archive = underTest.getStoredContent(PACKAGES_GZ_PATH);
    assertThat(archive, is(nullValue()));
  }

  @Test
  public void markAssetAsDownloadedAndSaveOnGetContent() throws Exception {
    when(asset.markAsDownloaded()).thenReturn(true);
    underTest.getStoredContent(PACKAGES_GZ_PATH);
    verify(storageTx).saveAsset(asset);
  }

  @Test
  public void doNotSaveAssetWhenContentNotMarkedAsDownloaded() throws Exception {
    when(asset.markAsDownloaded()).thenReturn(false);
    underTest.getStoredContent(PACKAGES_GZ_PATH);
    verify(storageTx, never()).saveAsset(asset);
  }

  @Test
  public void putArchive() throws Exception {
    List<Component> list = ImmutableList.of(component);
    when(tempBlob.get()).thenReturn(getClass().getResourceAsStream(
        "/org/sonatype/nexus/repository/r/internal/" + REAL_PACKAGE));
    when(asset.name()).thenReturn(REAL_PACKAGE_PATH);
    when(assetBlob.getBlob())
        .thenReturn(blob);
    doReturn(assetBlob)
        .when(storageTx).setBlob(any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        anyBoolean());
    when(storageTx.findComponents(any(), any()))
        .thenReturn(list);
    when(rFacet.findOrCreateComponent(any(storageTx.getClass()), anyString(), anyMapOf(String.class, String.class)))
        .thenReturn(component);
    when(rFacet.findOrCreateAsset(any(storageTx.getClass()), any(component.getClass()), eq(REAL_PACKAGE_PATH),
        anyMapOf(String.class, String.class)))
        .thenReturn(asset);

    underTest.doPutArchive(REAL_PACKAGE_PATH, tempBlob, payload);
    verify(storageTx).saveAsset(asset);
  }

  @Test
  public void shouldBuildPackagesGz() throws Exception {
    when(tempBlob.get()).thenReturn(getClass().getResourceAsStream(PACKAGES_GZ));
    when(asset.name()).thenReturn(PACKAGES_GZ_PATH);
    when(assetBlob.getBlob())
        .thenReturn(blob);
    doReturn(assetBlob)
        .when(storageTx).setBlob(any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        anyBoolean());
    when(storageFacet.createTempBlob(any(InputStream.class), eq(HASH_ALGORITHMS))).thenAnswer(
        invocation -> {
          InputStream is = (InputStream) invocation.getArguments()[0];
          byte[] content = ByteStreams.toByteArray(is);
          when(tempBlob.get()).thenAnswer(i -> new ByteArrayInputStream(content));
          return tempBlob;
        });
    assets.add(asset);
    underTest.buildAndPutPackagesGz(BASE_PATH);
    try (InputStream in = tempBlob.get()) {
      Map<String, String> attributes = extractDescriptionFromArchive(PACKAGE_NAME, in);
      assertThat(attributes.get(P_PACKAGE), is(equalTo(PACKAGE_NAME)));
      assertThat(attributes.get(P_VERSION), is(equalTo(VERSION)));
      assertThat(attributes.get(P_DEPENDS), is(equalTo(DEPENDS)));
      assertThat(attributes.get(P_IMPORTS), is(equalTo(IMPORTS)));
      assertThat(attributes.get(P_SUGGESTS), is(equalTo(SUGGESTS)));
      assertThat(attributes.get(P_LINKINGTO), is(equalTo(LINKINGTO)));
      assertThat(attributes.get(P_LICENSE), is(equalTo(LICENSE)));
      assertThat(attributes.get(P_NEEDS_COMPILATION), is(equalTo(NEEDS_COMPILATION)));
    }
    verify(storageTx).saveAsset(asset);
  }
}
