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
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.r.orient.internal.hosted.OrientRPackagesBuilder;
import org.sonatype.nexus.repository.storage.Asset;

import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
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

/**
 * {@link OrientRPackagesBuilder} unit tests.
 */
public class OrientRPackagesBuilderTest
    extends TestSupport
{

  @Test
  public void shouldAppendPackages() {
    OrientRPackagesBuilder underTest = new OrientRPackagesBuilder();
    underTest.append(createAsset("/foo/bar/b-4", "b", "4.0.0"));
    underTest.append(createAsset("/foo/bar/a-1", "a", "1.0.0"));
    underTest.append(createAsset("/foo/bar/a-3", "a", "3.0.0"));
    underTest.append(createAsset("/foo/bar/a-2", "a", "2.0.0"));

    Map<String, Map<String, String>> packageInformation = underTest.getPackageInformation();
    assertThat(packageInformation.keySet(), contains("a", "b"));

    Map<String, String> packageA = packageInformation.get("a");
    assertThat(packageA.get(P_PACKAGE), is("a"));
    assertThat(packageA.get(P_VERSION), is("3.0.0"));
    assertThat(packageA.get(P_DEPENDS), is("Depends:/foo/bar/a-3"));
    assertThat(packageA.get(P_IMPORTS), is("Imports:/foo/bar/a-3"));
    assertThat(packageA.get(P_SUGGESTS), is("Suggests:/foo/bar/a-3"));
    assertThat(packageA.get(P_LINKINGTO), is("LinkingTo:/foo/bar/a-3"));
    assertThat(packageA.get(P_LICENSE), is("License:/foo/bar/a-3"));
    assertThat(packageA.get(P_NEEDS_COMPILATION), is("NeedsCompilation:/foo/bar/a-3"));

    Map<String, String> packageB = packageInformation.get("b");
    assertThat(packageB.get(P_PACKAGE), is("b"));
    assertThat(packageB.get(P_VERSION), is("4.0.0"));
    assertThat(packageB.get(P_DEPENDS), is("Depends:/foo/bar/b-4"));
    assertThat(packageB.get(P_IMPORTS), is("Imports:/foo/bar/b-4"));
    assertThat(packageB.get(P_SUGGESTS), is("Suggests:/foo/bar/b-4"));
    assertThat(packageB.get(P_LINKINGTO), is("LinkingTo:/foo/bar/b-4"));
    assertThat(packageB.get(P_LICENSE), is("License:/foo/bar/b-4"));
    assertThat(packageB.get(P_NEEDS_COMPILATION), is("NeedsCompilation:/foo/bar/b-4"));
  }

  @Test
  public void shouldBuildPackages() throws IOException {
    OrientRPackagesBuilder underTest = new OrientRPackagesBuilder();
    underTest.append(createAsset("/foo/bar/a-3", "a", "3.0.0"));

    Map<String, Map<String, String>> packageInformation = underTest.getPackageInformation();
    assertThat(packageInformation.keySet(), contains("a"));

    try (InputStream in = new ByteArrayInputStream(underTest.buildPackagesGz())) {
      Map<String, String> attributes = extractDescriptionFromArchive("PACKAGES.gz", in);
      assertThat(attributes.get(P_PACKAGE), is("a"));
      assertThat(attributes.get(P_VERSION), is("3.0.0"));
      assertThat(attributes.get(P_DEPENDS), is("Depends:/foo/bar/a-3"));
      assertThat(attributes.get(P_IMPORTS), is("Imports:/foo/bar/a-3"));
      assertThat(attributes.get(P_SUGGESTS), is("Suggests:/foo/bar/a-3"));
      assertThat(attributes.get(P_LINKINGTO), is("LinkingTo:/foo/bar/a-3"));
      assertThat(attributes.get(P_LICENSE), is("License:/foo/bar/a-3"));
      assertThat(attributes.get(P_NEEDS_COMPILATION), is("NeedsCompilation:/foo/bar/a-3"));
    }
  }

  private Asset createAsset(final String assetName,
                            final String packageName,
                            final String packageVersion)
  {
    NestedAttributesMap formatAttributes = mock(NestedAttributesMap.class);
    when(formatAttributes.get(P_PACKAGE, String.class)).thenReturn(packageName);
    when(formatAttributes.get(P_VERSION, String.class)).thenReturn(packageVersion);
    when(formatAttributes.get(P_DEPENDS, String.class)).thenReturn("Depends:" + assetName);
    when(formatAttributes.get(P_IMPORTS, String.class)).thenReturn("Imports:" + assetName);
    when(formatAttributes.get(P_SUGGESTS, String.class)).thenReturn("Suggests:" + assetName);
    when(formatAttributes.get(P_LINKINGTO, String.class)).thenReturn("LinkingTo:" + assetName);
    when(formatAttributes.get(P_LICENSE, String.class)).thenReturn("License:" + assetName);
    when(formatAttributes.get(P_NEEDS_COMPILATION, String.class)).thenReturn("NeedsCompilation:" + assetName);

    Asset asset = mock(Asset.class);
    when(asset.formatAttributes()).thenReturn(formatAttributes);
    when(asset.name()).thenReturn(assetName);
    return asset;
  }
}
