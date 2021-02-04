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
package org.sonatype.repository.conan.internal.orient.hosted;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.repository.conan.internal.hosted.ConanHostedHelper;
import org.sonatype.repository.conan.internal.metadata.ConanCoords;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.Spy;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.repository.conan.internal.AssetKind.CONAN_INFO;
import static org.sonatype.repository.conan.internal.AssetKind.CONAN_MANIFEST;
import static org.sonatype.repository.conan.internal.AssetKind.CONAN_PACKAGE;

public class ConanHostedMetadataFacetSupportTest
    extends TestSupport
{
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String path = "should be ignored";

  private static final String group = "vthiery";

  private static final String project = "jsonformoderncpp";

  private static final String version = "3.7.0";

  private static final String channel = "stable";

  private static final String sha = "5ab84d6acfe1f23c4fae0ab88f26e3a396351ac9";

  @Spy
  private ConanHostedMetadataFacetSupport conanHostedMetadataFacetSupport = new ConanHostedMetadataFacetSupport();

  @Test
  public void generateAssetPackagesDownloadUrls() throws Exception {
    String assetPath =
        String.format("%s%s/%s/%s/%s/packages/%s", ConanHostedHelper.CONAN_HOSTED_PREFIX, group, project, version,
            channel, sha);

    Mockito.doReturn("some_hash_1").when(conanHostedMetadataFacetSupport)
        .getHash(String.format("%s/%s", assetPath, CONAN_INFO.getFilename()));
    Mockito.doReturn("some_hash_2").when(conanHostedMetadataFacetSupport)
        .getHash(String.format("%s/%s", assetPath, CONAN_PACKAGE.getFilename()));
    Mockito.doReturn("some_hash_3").when(conanHostedMetadataFacetSupport)
        .getHash(String.format("%s/%s", assetPath, CONAN_MANIFEST.getFilename()));

    String expected = MAPPER.writeValueAsString(ImmutableMap.of(
        CONAN_INFO.getFilename(), "some_hash_1",
        CONAN_PACKAGE.getFilename(), "some_hash_2",
        CONAN_MANIFEST.getFilename(), "some_hash_3"
    ));

    ConanCoords conanCoords = new ConanCoords(path, group, project, version, channel, sha);
    String actual = conanHostedMetadataFacetSupport.generatePackageSnapshotAsJson(conanCoords);

    assertThat(actual, is(expected));
  }

  @Test
  public void generateAssetPackagesDownloadUrlsHashNotFound() throws Exception {
    String assetPath =
        String.format("%s%s/%s/%s/%s/packages/%s", ConanHostedHelper.CONAN_HOSTED_PREFIX, group, project, version,
            channel, sha);

    Mockito.doReturn(null).when(conanHostedMetadataFacetSupport)
        .getHash(String.format("%s/%s", assetPath, CONAN_INFO.getFilename()));
    Mockito.doReturn(null).when(conanHostedMetadataFacetSupport)
        .getHash(String.format("%s/%s", assetPath, CONAN_PACKAGE.getFilename()));
    Mockito.doReturn(null).when(conanHostedMetadataFacetSupport)
        .getHash(String.format("%s/%s", assetPath, CONAN_MANIFEST.getFilename()));

    ConanCoords conanCoords = new ConanCoords(path, group, project, version, channel, sha);
    String actual = conanHostedMetadataFacetSupport.generatePackageSnapshotAsJson(conanCoords);

    assertThat(actual, nullValue());
  }
}
