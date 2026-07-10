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
package org.sonatype.nexus.repository.search.normalize;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SemVerVersionNormalizerTest
{
  private final SemVerVersionNormalizer underTest = new SemVerVersionNormalizer();

  @Test
  public void testBlankInputs() {
    assertThat(underTest.getNormalizedVersion(null)).isEqualTo("");
    assertThat(underTest.getNormalizedVersion("")).isEqualTo("");
    assertThat(underTest.getNormalizedVersion("   ")).isEqualTo("");
  }

  @Test
  public void testReleaseVersions() {
    assertThat(underTest.getNormalizedVersion("1.0.0")).endsWith(".c");
    assertThat(underTest.getNormalizedVersion("2.5.10")).endsWith(".c");
  }

  @Test
  public void testPreReleaseVersions() {
    assertThat(underTest.getNormalizedVersion("1.0.0-alpha")).contains(".a.");
    assertThat(underTest.getNormalizedVersion("1.0.0-beta.1")).contains(".a.");
    assertThat(underTest.getNormalizedVersion("1.0.0-rc.1")).contains(".a.");
  }

  @Test
  public void testBuildMetadataStripped() {
    String withMeta = underTest.getNormalizedVersion("1.0.0+build.123");
    String withoutMeta = underTest.getNormalizedVersion("1.0.0");
    assertThat(withMeta).isEqualTo(withoutMeta);
  }

  @Test
  public void testPreReleaseSortsBeforeRelease() {
    String preRelease = underTest.getNormalizedVersion("1.0.0-alpha");
    String release = underTest.getNormalizedVersion("1.0.0");
    assertThat(preRelease).isLessThan(release);
  }

  @Test
  public void testNumericPreReleaseSortsBeforeAlpha() {
    String numeric = underTest.getNormalizedVersion("1.0.0-1");
    String alpha = underTest.getNormalizedVersion("1.0.0-alpha");
    assertThat(numeric).isLessThan(alpha);
  }

  @Test
  public void testHigherVersionSortsAfter() {
    String v1 = underTest.getNormalizedVersion("1.0.0");
    String v2 = underTest.getNormalizedVersion("2.0.0");
    assertThat(v1).isLessThan(v2);
  }

  @Test
  public void testPreReleaseOrdering() {
    String alpha = underTest.getNormalizedVersion("1.0.0-alpha");
    String beta = underTest.getNormalizedVersion("1.0.0-beta");
    String rc = underTest.getNormalizedVersion("1.0.0-rc");
    assertThat(alpha).isLessThan(beta);
    assertThat(beta).isLessThan(rc);
  }

  @Test
  public void testTrimsWhitespace() {
    assertThat(underTest.getNormalizedVersion("  1.0.0  "))
        .isEqualTo(underTest.getNormalizedVersion("1.0.0"));
  }
}
