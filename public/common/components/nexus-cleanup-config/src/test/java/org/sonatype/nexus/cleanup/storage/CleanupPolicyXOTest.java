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

import java.util.Map;

import org.sonatype.nexus.cleanup.internal.storage.CleanupPolicyData;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

public class CleanupPolicyXOTest
{
  @Test
  public void testDefaultConstructor() {
    CleanupPolicyXO xo = new CleanupPolicyXO();

    assertThat(xo.getName(), is(nullValue()));
    assertThat(xo.getFormat(), is(nullValue()));
    assertThat(xo.getMode(), is(nullValue()));
    assertThat(xo.getNotes(), is(nullValue()));
    assertThat(xo.getCriteria(), is(nullValue()));
    assertThat(xo.getSortOrder(), is(0));
  }

  @Test
  public void testFullConstructor() {
    CleanupPolicyCriteria criteria =
        new CleanupPolicyCriteria(1, 2, CleanupPolicyReleaseType.RELEASES, "*.json", 3, "name");

    CleanupPolicyXO xo = new CleanupPolicyXO("policy", "maven2", "delete", "some notes", criteria);

    assertThat(xo.getName(), is("policy"));
    assertThat(xo.getFormat(), is("maven2"));
    assertThat(xo.getMode(), is("delete"));
    assertThat(xo.getNotes(), is("some notes"));
    assertThat(xo.getCriteria(), is(sameInstance(criteria)));
    // sortOrder is not part of the full constructor and defaults to 0
    assertThat(xo.getSortOrder(), is(0));
  }

  @Test
  public void testSetters() {
    CleanupPolicyCriteria criteria =
        new CleanupPolicyCriteria(null, null, null, null, null, null);

    CleanupPolicyXO xo = new CleanupPolicyXO();
    xo.setName("policy");
    xo.setFormat("npm");
    xo.setMode("clean");
    xo.setNotes("notes");
    xo.setCriteria(criteria);
    xo.setSortOrder(5);

    assertThat(xo.getName(), is("policy"));
    assertThat(xo.getFormat(), is("npm"));
    assertThat(xo.getMode(), is("clean"));
    assertThat(xo.getNotes(), is("notes"));
    assertThat(xo.getCriteria(), is(sameInstance(criteria)));
    assertThat(xo.getSortOrder(), is(5));
  }

  @Test
  public void testSettersAcceptNull() {
    CleanupPolicyXO xo =
        new CleanupPolicyXO("policy", "maven2", "delete", "notes",
            new CleanupPolicyCriteria(null, null, null, null, null, null));

    xo.setName(null);
    xo.setFormat(null);
    xo.setMode(null);
    xo.setNotes(null);
    xo.setCriteria(null);

    assertThat(xo.getName(), is(nullValue()));
    assertThat(xo.getFormat(), is(nullValue()));
    assertThat(xo.getMode(), is(nullValue()));
    assertThat(xo.getNotes(), is(nullValue()));
    assertThat(xo.getCriteria(), is(nullValue()));
  }

  @Test
  public void testAllFormatsXoConstant() {
    assertThat(CleanupPolicyXO.ALL_CLEANUP_POLICY_XO_FORMAT, is("(All Formats)"));
  }

  @Test
  public void testFromCleanupPolicy() {
    Map<String, String> criteria = ImmutableMap.of("regex", "*.json");
    CleanupPolicy policy = createCleanupPolicy("policy", "maven2", "delete", "notes", criteria);

    CleanupPolicyXO xo = CleanupPolicyXO.fromCleanupPolicy(policy);

    assertThat(xo.getName(), is("policy"));
    assertThat(xo.getFormat(), is("maven2"));
    assertThat(xo.getMode(), is("delete"));
    assertThat(xo.getNotes(), is("notes"));
    assertThat(xo.getCriteria(), is(notNullValue()));
    assertThat(CleanupPolicyCriteria.toMap(xo.getCriteria()), is(criteria));
    // overload without sortOrder leaves the default value
    assertThat(xo.getSortOrder(), is(0));
  }

  @Test
  public void testFromCleanupPolicyWithSortOrder() {
    Map<String, String> criteria = ImmutableMap.of("regex", "*.json");
    CleanupPolicy policy = createCleanupPolicy("policy", "npm", "clean", "notes", criteria);

    CleanupPolicyXO xo = CleanupPolicyXO.fromCleanupPolicy(policy, 7);

    assertThat(xo.getName(), is("policy"));
    assertThat(xo.getFormat(), is("npm"));
    assertThat(xo.getMode(), is("clean"));
    assertThat(xo.getNotes(), is("notes"));
    assertThat(xo.getCriteria(), is(notNullValue()));
    assertThat(xo.getSortOrder(), is(7));
  }

  @Test
  public void testFromCleanupPolicyMapsAllFormatsToXoFormat() {
    Map<String, String> criteria = ImmutableMap.of("regex", "*.json");
    CleanupPolicy policy =
        createCleanupPolicy("policy", CleanupPolicy.ALL_CLEANUP_POLICY_FORMAT, "delete", "notes", criteria);

    CleanupPolicyXO xo = CleanupPolicyXO.fromCleanupPolicy(policy);

    assertThat(xo.getFormat(), is(CleanupPolicyXO.ALL_CLEANUP_POLICY_XO_FORMAT));
  }

  @Test
  public void testFromCleanupPolicyFormatNullPolicy() {
    assertThat(CleanupPolicyXO.fromCleanupPolicyFormat(null), is(nullValue()));
  }

  @Test
  public void testFromCleanupPolicyFormatNullFormat() {
    CleanupPolicy policy = createCleanupPolicy("policy", null, "delete", "notes", null);

    assertThat(CleanupPolicyXO.fromCleanupPolicyFormat(policy), is(nullValue()));
  }

  @Test
  public void testFromCleanupPolicyFormatAllFormats() {
    CleanupPolicy policy =
        createCleanupPolicy("policy", CleanupPolicy.ALL_CLEANUP_POLICY_FORMAT, "delete", "notes", null);

    assertThat(CleanupPolicyXO.fromCleanupPolicyFormat(policy), is(CleanupPolicyXO.ALL_CLEANUP_POLICY_XO_FORMAT));
  }

  @Test
  public void testFromCleanupPolicyFormatAllFormatsIgnoresCase() {
    CleanupPolicy policy = createCleanupPolicy("policy", "all_formats", "delete", "notes", null);

    assertThat(CleanupPolicyXO.fromCleanupPolicyFormat(policy), is(CleanupPolicyXO.ALL_CLEANUP_POLICY_XO_FORMAT));
  }

  @Test
  public void testFromCleanupPolicyFormatRegularFormat() {
    CleanupPolicy policy = createCleanupPolicy("policy", "maven2", "delete", "notes", null);

    assertThat(CleanupPolicyXO.fromCleanupPolicyFormat(policy), is("maven2"));
  }

  @Test(expected = NullPointerException.class)
  public void testFromCleanupPolicyNullCriteriaThrows() {
    // CleanupPolicyCriteria.fromMap dereferences the criteria map, so a null criteria map fails fast
    CleanupPolicy policy = createCleanupPolicy("policy", "maven2", "delete", "notes", null);

    CleanupPolicyXO.fromCleanupPolicy(policy);
  }

  @Test
  public void testFromCleanupPolicyFormatStarIsNormalizedToAllFormatsDisplay() {
    // CleanupPolicyData.setFormat normalizes the "*" ALL_FORMATS sentinel to ALL_CLEANUP_POLICY_FORMAT
    // ("ALL_FORMATS"), which fromCleanupPolicyFormat then maps to the "(All Formats)" display value
    CleanupPolicy policy = createCleanupPolicy("policy", CleanupPolicy.ALL_FORMATS, "delete", "notes", null);

    assertThat(CleanupPolicyXO.fromCleanupPolicyFormat(policy), is(CleanupPolicyXO.ALL_CLEANUP_POLICY_XO_FORMAT));
  }

  @Test
  public void testFromCleanupPolicyFormatAllFormatsMixedCase() {
    CleanupPolicy policy = createCleanupPolicy("policy", "All_Formats", "delete", "notes", null);

    assertThat(CleanupPolicyXO.fromCleanupPolicyFormat(policy), is(CleanupPolicyXO.ALL_CLEANUP_POLICY_XO_FORMAT));
  }

  @Test
  public void testFromCleanupPolicyFormatEmptyStringPassesThrough() {
    // an empty (but non-null) format is not blank-checked here; it is not equal to ALL_FORMATS so it passes through
    CleanupPolicy policy = createCleanupPolicy("policy", "", "delete", "notes", null);

    assertThat(CleanupPolicyXO.fromCleanupPolicyFormat(policy), is(""));
  }

  @Test
  public void testFromCleanupPolicyWithEmptyCriteriaMap() {
    // an empty (non-null) criteria map does not NPE; fromMap yields a non-null criteria with no entries
    Map<String, String> emptyCriteria = ImmutableMap.of();
    CleanupPolicy policy = createCleanupPolicy("policy", "maven2", "delete", "notes", emptyCriteria);

    CleanupPolicyXO xo = CleanupPolicyXO.fromCleanupPolicy(policy);

    assertThat(xo.getCriteria(), is(notNullValue()));
    assertThat(CleanupPolicyCriteria.toMap(xo.getCriteria()), is(emptyCriteria));
  }

  @Test
  public void testFromCleanupPolicyWithNegativeSortOrder() {
    Map<String, String> criteria = ImmutableMap.of("regex", "*.json");
    CleanupPolicy policy = createCleanupPolicy("policy", "npm", "clean", "notes", criteria);

    CleanupPolicyXO xo = CleanupPolicyXO.fromCleanupPolicy(policy, -3);

    // sortOrder is stored verbatim, including negative values
    assertThat(xo.getSortOrder(), is(-3));
  }

  @Test(expected = NullPointerException.class)
  public void testFromCleanupPolicyNullPolicyThrows() {
    // fromCleanupPolicy dereferences the policy (e.g. getName()), so a null policy fails fast
    CleanupPolicyXO.fromCleanupPolicy((CleanupPolicy) null);
  }

  @Test(expected = NullPointerException.class)
  public void testFromCleanupPolicyWithSortOrderNullPolicyThrows() {
    // the sortOrder overload delegates to fromCleanupPolicy first, so a null policy fails fast there too
    CleanupPolicyXO.fromCleanupPolicy((CleanupPolicy) null, 7);
  }

  private CleanupPolicy createCleanupPolicy(
      final String name,
      final String format,
      final String mode,
      final String notes,
      final Map<String, String> criteria)
  {
    CleanupPolicyData policyData = new CleanupPolicyData();
    policyData.setName(name);
    policyData.setFormat(format);
    policyData.setMode(mode);
    policyData.setNotes(notes);
    policyData.setCriteria(criteria);

    return policyData;
  }
}
