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
package org.sonatype.nexus.cleanup.storage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.cleanup.storage.CleanupPolicyReleaseType.PRERELEASES;
import static org.sonatype.nexus.cleanup.storage.CleanupPolicyReleaseType.RELEASES;

/**
 * Tests for {@link CleanupPolicyCriteria}.
 */
public class CleanupPolicyCriteriaTest
{
  @Test
  public void testToMapWithAllFieldsPopulatedAndPrereleases() {
    CleanupPolicyCriteria criteria = new CleanupPolicyCriteria(2, 3, PRERELEASES, "*.json", 5, "version");

    Map<String, String> map = CleanupPolicyCriteria.toMap(criteria);

    // lastBlobUpdated/lastDownloaded are scaled from days to seconds (multiplied by 86400)
    assertThat(map.get("lastBlobUpdated"), is("172800"));
    assertThat(map.get("lastDownloaded"), is("259200"));
    assertThat(map.get("isPrerelease"), is("true"));
    assertThat(map.get("regex"), is("*.json"));
    // retain is NOT scaled on the way out
    assertThat(map.get("retain"), is("5"));
    assertThat(map.get("sortBy"), is("version"));
    assertThat(map.size(), is(6));
  }

  @Test
  public void testToMapWithReleasesReleaseTypeOnly() {
    CleanupPolicyCriteria criteria = new CleanupPolicyCriteria(null, null, RELEASES, null, null, null);

    Map<String, String> map = CleanupPolicyCriteria.toMap(criteria);

    // releaseType present but not PRERELEASES yields "false"; every other (null) field is omitted
    assertThat(map.get("isPrerelease"), is("false"));
    assertThat(map.size(), is(1));
  }

  @Test
  public void testToMapWithAllNullFieldsReturnsEmptyMap() {
    CleanupPolicyCriteria criteria = new CleanupPolicyCriteria(null, null, null, null, null, null);

    Map<String, String> map = CleanupPolicyCriteria.toMap(criteria);

    assertThat(map.isEmpty(), is(true));
  }

  @Test
  public void testFromMapWithAllFieldsPopulatedAndPrerelease() {
    Map<String, String> map = new HashMap<>();
    map.put("lastBlobUpdated", "172800");
    map.put("lastDownloaded", "259200");
    map.put("isPrerelease", "true");
    map.put("regex", "*.json");
    map.put("retain", "5");
    map.put("sortBy", "version");

    CleanupPolicyCriteria criteria = CleanupPolicyCriteria.fromMap(map);

    // seconds are divided back to days (172800 / 86400 = 2, 259200 / 86400 = 3);
    // retain is a plain component count and must NOT be divided -- fromMap reads it as-is
    // isPrerelease=true maps to PRERELEASES
    assertThat(criteria.toString(), is(
        "CleanupPolicyCriteria[lastBlobUpdated=2, lastDownloaded=3, releaseType=PRERELEASES, "
            + "regex='*.json', retain='5', sortBy=version]"));
  }

  @Test
  public void testFromMapWithPrereleaseFalseMapsToReleases() {
    Map<String, String> map = new HashMap<>();
    map.put("isPrerelease", "false");

    CleanupPolicyCriteria criteria = CleanupPolicyCriteria.fromMap(map);

    assertThat(criteria.toString(), is(
        "CleanupPolicyCriteria[lastBlobUpdated=null, lastDownloaded=null, releaseType=RELEASES, "
            + "regex='null', retain='null', sortBy=null]"));
  }

  @Test
  public void testFromMapWithEmptyMapReturnsNullFields() {
    CleanupPolicyCriteria criteria = CleanupPolicyCriteria.fromMap(Collections.emptyMap());

    assertThat(criteria.toString(), is(
        "CleanupPolicyCriteria[lastBlobUpdated=null, lastDownloaded=null, releaseType=null, "
            + "regex='null', retain='null', sortBy=null]"));

    // every field resolved to null, so the round-tripped map is empty
    assertThat(CleanupPolicyCriteria.toMap(criteria).isEmpty(), is(true));
  }

  @Test
  public void testFromMapWithBlankValuesTreatedAsNull() {
    Map<String, String> map = new HashMap<>();
    map.put("lastBlobUpdated", "   ");
    map.put("lastDownloaded", "");
    map.put("isPrerelease", "  ");

    CleanupPolicyCriteria criteria = CleanupPolicyCriteria.fromMap(map);

    // blank / whitespace-only values are treated as not present (null)
    assertThat(CleanupPolicyCriteria.toMap(criteria).isEmpty(), is(true));
  }

  @Test
  public void testFromMapUsesIntegerDivisionFloor() {
    Map<String, String> map = new HashMap<>();
    map.put("lastBlobUpdated", "100");
    map.put("lastDownloaded", "200");

    CleanupPolicyCriteria criteria = CleanupPolicyCriteria.fromMap(map);

    // 100 / 86400 == 0 and 200 / 86400 == 0 due to integer division
    assertThat(criteria.toString(), is(
        "CleanupPolicyCriteria[lastBlobUpdated=0, lastDownloaded=0, releaseType=null, "
            + "regex='null', retain='null', sortBy=null]"));
  }

  @Test
  public void testRetainRoundTripPreservesValue() {
    // retain is a plain component count -- toMap writes it unscaled and fromMap must read it
    // unscaled, so a toMap -> fromMap -> toMap round trip must preserve the original value.
    CleanupPolicyCriteria criteria = new CleanupPolicyCriteria(null, null, null, null, 5, null);

    Map<String, String> map = CleanupPolicyCriteria.toMap(criteria);
    assertThat(map.get("retain"), is("5"));

    CleanupPolicyCriteria roundTrip = CleanupPolicyCriteria.fromMap(map);
    Map<String, String> roundTripMap = CleanupPolicyCriteria.toMap(roundTrip);

    assertThat(roundTripMap.get("retain"), is("5"));
  }

  @Test
  public void testFromMapWithNonBooleanPrereleaseValueMapsToReleases() {
    Map<String, String> map = new HashMap<>();
    map.put("isPrerelease", "notABoolean");

    CleanupPolicyCriteria criteria = CleanupPolicyCriteria.fromMap(map);

    // parseBoolean treats any value other than a case-insensitive "true" as false, yielding RELEASES
    assertThat(criteria.toString(), is(
        "CleanupPolicyCriteria[lastBlobUpdated=null, lastDownloaded=null, releaseType=RELEASES, "
            + "regex='null', retain='null', sortBy=null]"));
  }

  @Test
  public void testFromMapWithUppercaseTruePrereleaseMapsToPrereleases() {
    Map<String, String> map = new HashMap<>();
    map.put("isPrerelease", "TRUE");

    CleanupPolicyCriteria criteria = CleanupPolicyCriteria.fromMap(map);

    // parseBoolean is case-insensitive, so "TRUE" maps to PRERELEASES (not RELEASES)
    assertThat(criteria.toString(), is(
        "CleanupPolicyCriteria[lastBlobUpdated=null, lastDownloaded=null, releaseType=PRERELEASES, "
            + "regex='null', retain='null', sortBy=null]"));
  }

  @Test
  public void testTimeFieldsRoundTripSymmetrically() {
    // Contrast with testRetainRoundTripIsAsymmetric: lastBlobUpdated/lastDownloaded are scaled by
    // DAY_IN_SECONDS on write (toMap) and divided by it on read (fromMap), so they survive a
    // toMap -> fromMap -> toMap round trip unchanged.
    CleanupPolicyCriteria criteria = new CleanupPolicyCriteria(2, 3, null, null, null, null);

    Map<String, String> map = CleanupPolicyCriteria.toMap(criteria);
    assertThat(map.get("lastBlobUpdated"), is("172800"));
    assertThat(map.get("lastDownloaded"), is("259200"));

    CleanupPolicyCriteria roundTrip = CleanupPolicyCriteria.fromMap(map);
    Map<String, String> roundTripMap = CleanupPolicyCriteria.toMap(roundTrip);

    assertThat(roundTripMap.get("lastBlobUpdated"), is("172800"));
    assertThat(roundTripMap.get("lastDownloaded"), is("259200"));
  }

  @Test
  public void testFromMapWithBlankRetainTreatedAsNull() {
    Map<String, String> map = new HashMap<>();
    map.put("retain", "   ");

    CleanupPolicyCriteria criteria = CleanupPolicyCriteria.fromMap(map);

    // blank-only retain is treated as not present (null), so toMap omits it entirely
    assertThat(CleanupPolicyCriteria.toMap(criteria).isEmpty(), is(true));
  }

  @Test
  public void testToString() {
    CleanupPolicyCriteria criteria =
        new CleanupPolicyCriteria(1, 2, PRERELEASES, "regex-value", 3, "sortByValue");

    assertThat(criteria.toString(), is(
        "CleanupPolicyCriteria[lastBlobUpdated=1, lastDownloaded=2, releaseType=PRERELEASES, "
            + "regex='regex-value', retain='3', sortBy=sortByValue]"));
  }
}
