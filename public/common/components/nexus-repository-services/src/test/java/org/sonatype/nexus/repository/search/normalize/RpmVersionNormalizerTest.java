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

public class RpmVersionNormalizerTest
{
  private final RpmVersionNormalizer underTest = new RpmVersionNormalizer();

  @Test
  public void testBlankInputs() {
    assertThat(underTest.getNormalizedVersion(null)).isEqualTo("");
    assertThat(underTest.getNormalizedVersion("")).isEqualTo("");
    assertThat(underTest.getNormalizedVersion("   ")).isEqualTo("");
  }

  @Test
  public void testSimpleVersionRelease() {
    String result = underTest.getNormalizedVersion("1.2.3-1");
    assertThat(result).startsWith("000000000."); // default epoch
    assertThat(result).contains(".c.");
  }

  @Test
  public void testEpoch() {
    String withEpoch = underTest.getNormalizedVersion("3:1.2.3-1");
    String withoutEpoch = underTest.getNormalizedVersion("1.2.3-1");
    assertThat(withEpoch).startsWith("000000003.");
    assertThat(withoutEpoch).isLessThan(withEpoch);
  }

  @Test
  public void testInvalidEpochDefaultsToZero() {
    String result = underTest.getNormalizedVersion("abc:1.2.3-1");
    assertThat(result).startsWith("000000000.");
  }

  @Test
  public void testTildePreRelease() {
    String preRelease = underTest.getNormalizedVersion("1.0~rc1-1");
    String release = underTest.getNormalizedVersion("1.0-1");
    assertThat(preRelease).contains(".a.");
    assertThat(preRelease).isLessThan(release);
  }

  @Test
  public void testReleaseFieldZeroPrefixIndicatesPreRelease() {
    String preRelease = underTest.getNormalizedVersion("1.0-0.1.rc1");
    String release = underTest.getNormalizedVersion("1.0-1");
    assertThat(preRelease).contains(".a.");
    assertThat(preRelease).isLessThan(release);
  }

  @Test
  public void testNoReleaseField() {
    String result = underTest.getNormalizedVersion("1.0");
    assertThat(result).startsWith("000000000.");
  }

  @Test
  public void testHigherVersionSortsAfter() {
    assertThat(underTest.getNormalizedVersion("1.0-1"))
        .isLessThan(underTest.getNormalizedVersion("2.0-1"));
  }

  /**
   * Regression: a bare trailing tilde (e.g. "1.0~") must not produce a sort key ending in ".".
   * Prior to the empty-pre-release guard in normalizeWithTilde(), VersionNumberExpander.expand("")
   * returned "" and left a trailing "." in the key. The guard substitutes "0" so the key remains
   * well-formed and orders consistently against other tilde pre-releases (e.g. "1.0~" sorts
   * before "1.0~1").
   */
  @Test
  public void testTrailingTildeProducesWellFormedKey() {
    String key = underTest.getNormalizedVersion("1.0~");
    assertThat(key).doesNotEndWith(".");
    assertThat(key).contains(".a.");
    assertThat(key).isLessThan(underTest.getNormalizedVersion("1.0~1"));
  }
}
