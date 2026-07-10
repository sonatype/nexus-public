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

public class DebianVersionNormalizerTest
{
  private final DebianVersionNormalizer underTest = new DebianVersionNormalizer();

  @Test
  public void testBlankInputs() {
    assertThat(underTest.getNormalizedVersion(null)).isEqualTo("");
    assertThat(underTest.getNormalizedVersion("")).isEqualTo("");
    assertThat(underTest.getNormalizedVersion("  ")).isEqualTo("");
  }

  @Test
  public void testSimpleRelease() {
    String result = underTest.getNormalizedVersion("1.2.3");
    assertThat(result).startsWith("000000000."); // default epoch
    assertThat(result).contains(".c");
  }

  @Test
  public void testEpochParsing() {
    String withEpoch = underTest.getNormalizedVersion("2:1.2.3");
    String withoutEpoch = underTest.getNormalizedVersion("1.2.3");
    assertThat(withEpoch).startsWith("000000002.");
    assertThat(withoutEpoch).startsWith("000000000.");
    assertThat(withoutEpoch).isLessThan(withEpoch);
  }

  @Test
  public void testInvalidEpochFallsBackToZero() {
    String result = underTest.getNormalizedVersion("abc:1.2.3");
    assertThat(result).startsWith("000000000.");
  }

  @Test
  public void testTildePreRelease() {
    String preRelease = underTest.getNormalizedVersion("1.0~rc1");
    String release = underTest.getNormalizedVersion("1.0");
    assertThat(preRelease).contains(".a.");
    assertThat(preRelease).isLessThan(release);
  }

  @Test
  public void testPlusPostRelease() {
    String postRelease = underTest.getNormalizedVersion("1.0+deb1");
    String release = underTest.getNormalizedVersion("1.0");
    assertThat(postRelease).contains(".d.");
    assertThat(release).isLessThan(postRelease);
  }

  @Test
  public void testDebianRevisionAppended() {
    String withRev = underTest.getNormalizedVersion("1.2.3-1");
    String withoutRev = underTest.getNormalizedVersion("1.2.3");
    assertThat(withRev).isNotEqualTo(withoutRev);
    assertThat(withRev).contains("000000001");
  }

  @Test
  public void testFullDebianFormat() {
    String result = underTest.getNormalizedVersion("3:1.2.3~rc1-2deb1");
    assertThat(result).startsWith("000000003.");
    assertThat(result).contains(".a.");
  }

  @Test
  public void testHigherVersionSortsAfter() {
    assertThat(underTest.getNormalizedVersion("1.0"))
        .isLessThan(underTest.getNormalizedVersion("2.0"));
  }

  /**
   * Regression: a bare trailing tilde (e.g. "1.0~") must not produce a sort key ending in ".".
   * Prior to the empty-pre-release guard in normalizeWithTildeAndPlus(),
   * VersionNumberExpander.expand("") returned "" and left a trailing "." in the key. The guard
   * substitutes "0" so the key remains well-formed and still sorts before the corresponding
   * release.
   */
  @Test
  public void testTrailingTildeProducesWellFormedKey() {
    String key = underTest.getNormalizedVersion("1.0~");
    assertThat(key).doesNotEndWith(".");
    assertThat(key).contains(".a.");
    assertThat(key).isLessThan(underTest.getNormalizedVersion("1.0~1"));
  }

  /**
   * Regression: same empty-suffix guard for a bare trailing plus (e.g. "1.0+"). The key must not
   * end in "." and must order consistently against other plus post-releases (e.g. "1.0+" sorts
   * before "1.0+1").
   */
  @Test
  public void testTrailingPlusProducesWellFormedKey() {
    String key = underTest.getNormalizedVersion("1.0+");
    assertThat(key).doesNotEndWith(".");
    assertThat(key).contains(".d.");
    assertThat(key).isLessThan(underTest.getNormalizedVersion("1.0+1"));
  }
}
