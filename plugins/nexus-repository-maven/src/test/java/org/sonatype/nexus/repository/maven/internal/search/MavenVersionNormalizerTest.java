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
package org.sonatype.nexus.repository.maven.internal.search;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MavenVersionNormalizerTest
{
  private final MavenVersionNormalizer underTest = new MavenVersionNormalizer();

  @Test
  public void testNullEmpty() {
    assertEquals("", underTest.getNormalizedValue(null));
    assertEquals("", underTest.getNormalizedValue(""));
    assertEquals("", underTest.getNormalizedValue(" "));
  }

  @Test
  public void testVersionExpansion()
  {
    assertVersionsEqual("1", "1.0");
    assertVersionsEqual("1", "1.0.0");
    assertVersionsEqual("1.0", "1.0.0");
  }

  @Test
  public void testAliases() {
    assertVersionsEqual( "1ga", "1" );
    assertVersionsEqual( "1release", "1" );
    assertVersionsEqual( "1final", "1" );
    assertVersionsEqual( "1cr", "1rc" );
    assertVersionsEqual( "1a1", "1-alpha-1" );
    assertVersionsEqual( "1b2", "1-beta-2" );
    assertVersionsEqual( "1m3", "1-milestone-3" );
  }

  @Test
  public void testCaseInsensitive() {
    assertVersionsEqual( "1X", "1x" );
    assertVersionsEqual( "1A", "1a" );
    assertVersionsEqual( "1B", "1b" );
    assertVersionsEqual( "1M", "1m" );
    assertVersionsEqual( "1Ga", "1" );
    assertVersionsEqual( "1GA", "1" );
    assertVersionsEqual( "1RELEASE", "1" );
    assertVersionsEqual( "1release", "1" );
    assertVersionsEqual( "1RELeaSE", "1" );
    assertVersionsEqual( "1Final", "1" );
    assertVersionsEqual( "1FinaL", "1" );
    assertVersionsEqual( "1FINAL", "1" );
    assertVersionsEqual( "1Cr", "1Rc" );
    assertVersionsEqual( "1cR", "1rC" );
    assertVersionsEqual( "1m3", "1Milestone3" );
    assertVersionsEqual( "1m3", "1MileStone3" );
    assertVersionsEqual( "1m3", "1MILESTONE3" );
  }

  @Test
  public void testNoSeparator() {
    assertVersionsEqual( "1a", "1-a" );
    assertVersionsEqual( "1a", "1.0-a" );
    assertVersionsEqual( "1a", "1.0.0-a" );
    assertVersionsEqual( "1.0a", "1-a" );
    assertVersionsEqual( "1.0.0a", "1-a" );
    assertVersionsEqual( "1x", "1-x" );
    assertVersionsEqual( "1x", "1.0-x" );
    assertVersionsEqual( "1x", "1.0.0-x" );
    assertVersionsEqual( "1.0x", "1-x" );
    assertVersionsEqual( "1.0.0x", "1-x" );
    // NEXUS-31494
    assertVersionsEqual( "1.0-", "1" );
  }

  @Test
  public void testOrder() {
    assertInOrder("1-alpha2", "1-alpha-123", "1-beta-2", "1-beta123", "1-m2", "1-m11", "1-rc", "1-cr2", "1-rc123",
        "1-sp2", "1-1", "1-2", "1-123");
    assertInOrder("2.0", "2.0.a", "2.0.2", "2.0.123", "2.1.0", "2.1-a", "2.1b", "2.1-c", "2.1-1", "2.2", "2.123");
    assertInOrder("11.a2", "11.a11", "11.b2", "11.b11", "11.m2", "11.m11", "11", "11.a", "11b", "11c", "11m");

    assertInOrder("1", "2");
    assertInOrder("1.5", "2");
    assertInOrder("1", "2.5");
    assertInOrder("1.0", "1.1");
    assertInOrder("1.1", "1.2");
    assertInOrder("1.0.0", "1.1");
    assertInOrder("1.0.1", "1.1");
    assertInOrder("1.1", "1.2.0");

    assertInOrder("1.0-alpha-1", "1.0");
    assertInOrder("1.0-alpha-1", "1.0-alpha-2");
    assertInOrder("1.0-alpha-1", "1.0-beta-1");

    assertInOrder("1.0", "1.0-1");
    assertInOrder("1.0-1", "1.0-2");
    assertInOrder("1.0.0", "1.0-1");

    assertInOrder("2.0-1", "2.0.1");
    assertInOrder("2.0.1-klm", "2.0.1-lmn");
    assertInOrder("2.0.1", "2.0.1-xyz");

    assertInOrder("2.0.1", "2.0.1-123");
    assertInOrder("2.0.1-xyz", "2.0.1-123");
  }

  @Test
  public void testSnapshotOrder() {
    assertInOrder("1-20211022.164208-1", "1");
    assertInOrder("1.0-beta-1", "1.0-20230122.854921-1");
    assertInOrder("2.1-20190801.333981-1", "2.1.0");
    assertInOrder("3-beta-1", "3.0.0-20100505.190345-1");
  }

  @Test
  public void testUnrecognizedVersionDefaultsToOriginalLogic() {
    assertEquals("asparagus.schoolbus", underTest.getNormalizedValue("asparagus.schoolbus"));
    assertEquals("000000001.000000002.000000003.000000004.000000005", underTest.getNormalizedValue("1.2.3.4.5"));
    assertEquals("develop-020211201.000171404-000000903", underTest.getNormalizedValue("develop-20211201.171404-903"));

    assertInOrder("develop-20211130.182421-895", "develop-20211130.203249-896", "develop-20211201.111154-898",
        "develop-20211202.180605-904");
  }

  private void assertInOrder(String... versions) {
    for (int i = 1; i < versions.length; i++) {
      String v1 = versions[i - 1];
      String n1 = underTest.getNormalizedValue(v1);
      for (int j = i; j < versions.length; j++) {
        String v2 = versions[j];
        String n2 = underTest.getNormalizedValue(v2);
        assertTrue(String.format("expected %s (%s) < %s (%s)", v1, n1, v2, n2), n1.compareTo(n2) < 0);
        assertTrue(String.format("expected %s (%s) > %s (%s)", v2, n2, v1, n1), n2.compareTo(n1) > 0);
      }
    }
  }

  private void assertVersionsEqual(String v1, String v2) {
    assertEquals(underTest.getNormalizedValue(v1), underTest.getNormalizedValue(v2));
  }
}
