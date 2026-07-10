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

public class GemStyleVersionNormalizerTest
{
  private final GemStyleVersionNormalizer underTest = new GemStyleVersionNormalizer();

  @Test
  public void testBlankInputs() {
    assertThat(underTest.getNormalizedVersion(null)).isEqualTo("");
    assertThat(underTest.getNormalizedVersion("")).isEqualTo("");
    assertThat(underTest.getNormalizedVersion("   ")).isEqualTo("");
  }

  @Test
  public void testSimpleNumericRelease() {
    String result = underTest.getNormalizedVersion("1.2.3");
    assertThat(result).endsWith(".c");
  }

  @Test
  public void testPreReleaseWithLetters() {
    String preRelease = underTest.getNormalizedVersion("1.0.0.pre");
    String release = underTest.getNormalizedVersion("1.0.0");
    assertThat(preRelease).contains(".a.");
    assertThat(preRelease).isLessThan(release);
  }

  @Test
  public void testHigherVersionSortsAfter() {
    assertThat(underTest.getNormalizedVersion("1.0.0"))
        .isLessThan(underTest.getNormalizedVersion("2.0.0"));
  }

  @Test
  public void testCaseInsensitivePreRelease() {
    String upper = underTest.getNormalizedVersion("1.0.0.PRE");
    String lower = underTest.getNormalizedVersion("1.0.0.pre");
    assertThat(upper).isEqualTo(lower);
  }

  @Test
  public void testRcPreRelease() {
    String rc = underTest.getNormalizedVersion("1.0.0.rc1");
    String release = underTest.getNormalizedVersion("1.0.0");
    assertThat(rc).isLessThan(release);
  }

  @Test
  public void testPureLetterVersion() {
    String result = underTest.getNormalizedVersion("alpha");
    assertThat(result).isNotEmpty();
  }
}
