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

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GoVersionNormalizerTest
{
  private GoVersionNormalizer underTest;

  @Before
  public void setup() {
    underTest = new GoVersionNormalizer(new SemVerVersionNormalizer());
  }

  @Test
  public void testBlankInputs() {
    assertThat(underTest.getNormalizedVersion(null)).isEqualTo("");
    assertThat(underTest.getNormalizedVersion("")).isEqualTo("");
    assertThat(underTest.getNormalizedVersion(" ")).isEqualTo("");
  }

  @Test
  public void testStripsLeadingV() {
    String withV = underTest.getNormalizedVersion("v1.2.3");
    String withoutV = underTest.getNormalizedVersion("1.2.3");
    assertThat(withV).isEqualTo(withoutV);
  }

  @Test
  public void testStripsLeadingVUppercase() {
    assertThat(underTest.getNormalizedVersion("V1.2.3"))
        .isEqualTo(underTest.getNormalizedVersion("1.2.3"));
  }

  @Test
  public void testReleaseVersion() {
    String result = underTest.getNormalizedVersion("v1.2.3");
    assertThat(result).endsWith(".c");
  }

  @Test
  public void testPreReleaseVersion() {
    String preRelease = underTest.getNormalizedVersion("v1.2.3-rc1");
    String release = underTest.getNormalizedVersion("v1.2.3");
    assertThat(preRelease).isLessThan(release);
  }

  @Test
  public void testPseudoVersion() {
    String result = underTest.getNormalizedVersion("v0.0.0-20210101120000-abcdef123456");
    assertThat(result).contains(".a.");
    assertThat(result).contains("20210101120000");
  }

  @Test
  public void testPseudoVersionTimestampOrdering() {
    String earlier = underTest.getNormalizedVersion("v0.0.0-20200101120000-abcdef123456");
    String later = underTest.getNormalizedVersion("v0.0.0-20210101120000-abcdef123456");
    assertThat(earlier).isLessThan(later);
  }

  @Test
  public void testJustVPrefix() {
    assertThat(underTest.getNormalizedVersion("v")).isEqualTo("");
  }

  @Test
  public void testHigherVersionSortsAfter() {
    assertThat(underTest.getNormalizedVersion("v1.0.0"))
        .isLessThan(underTest.getNormalizedVersion("v2.0.0"));
  }
}
