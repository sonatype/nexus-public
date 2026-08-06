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
package org.sonatype.nexus.cleanup.internal.storage;

import java.util.Map;

import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.common.entity.HasName;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link CleanupPolicyData}.
 */
public class CleanupPolicyDataTest
{
  private final CleanupPolicyData underTest = new CleanupPolicyData();

  @Test
  public void testNameGetterAndSetter() {
    assertThat(underTest.getName(), is(nullValue()));

    underTest.setName("my-policy");
    assertThat(underTest.getName(), is("my-policy"));

    underTest.setName(null);
    assertThat(underTest.getName(), is(nullValue()));
  }

  @Test
  public void testNotesGetterAndSetter() {
    assertThat(underTest.getNotes(), is(nullValue()));

    underTest.setNotes("some notes");
    assertThat(underTest.getNotes(), is("some notes"));

    underTest.setNotes(null);
    assertThat(underTest.getNotes(), is(nullValue()));
  }

  @Test
  public void testModeGetterAndSetter() {
    assertThat(underTest.getMode(), is(nullValue()));

    underTest.setMode("delete");
    assertThat(underTest.getMode(), is("delete"));

    underTest.setMode(null);
    assertThat(underTest.getMode(), is(nullValue()));
  }

  @Test
  public void testCriteriaGetterAndSetter() {
    assertThat(underTest.getCriteria(), is(nullValue()));

    Map<String, String> criteria = ImmutableMap.of("regex", "*.json", "lastDownloaded", "100");
    underTest.setCriteria(criteria);
    assertThat(underTest.getCriteria(), is(criteria));

    underTest.setCriteria(null);
    assertThat(underTest.getCriteria(), is(nullValue()));
  }

  @Test
  public void testFormatGetterAndSetterWithSpecificFormat() {
    assertThat(underTest.getFormat(), is(nullValue()));

    underTest.setFormat("npm");
    assertThat(underTest.getFormat(), is("npm"));
  }

  @Test
  public void testSetFormatAllFormatsIsStoredAsAllCleanupPolicyFormat() {
    underTest.setFormat(CleanupPolicy.ALL_FORMATS);

    // "*" is translated to the ALL_CLEANUP_POLICY_FORMAT constant ("ALL_FORMATS"), not stored verbatim
    assertThat(underTest.getFormat(), is(CleanupPolicy.ALL_CLEANUP_POLICY_FORMAT));
    assertThat(underTest.getFormat(), is(not(CleanupPolicy.ALL_FORMATS)));
  }

  @Test
  public void testSetFormatNonAllFormatIsStoredUnchanged() {
    underTest.setFormat("maven2");
    assertThat(underTest.getFormat(), is("maven2"));

    // null does not equal ALL_FORMATS, so it is stored unchanged
    underTest.setFormat(null);
    assertThat(underTest.getFormat(), is(nullValue()));
  }

  @Test
  public void testToString() {
    underTest.setName("my-policy");
    underTest.setFormat("npm");
    underTest.setMode("delete");
    Map<String, String> criteria = ImmutableMap.of("regex", "*.json");
    underTest.setCriteria(criteria);

    String result = underTest.toString();

    assertThat(result, is("CleanupPolicy{name='my-policy', format='npm', mode='delete', criteria=" + criteria + "}"));
    assertThat(result, containsString("name='my-policy'"));
    assertThat(result, containsString("format='npm'"));
    assertThat(result, containsString("mode='delete'"));
    assertThat(result, containsString("criteria=" + criteria));
  }

  @Test
  public void testToStringWithNullValues() {
    assertThat(underTest.toString(),
        is("CleanupPolicy{name='null', format='null', mode='null', criteria=null}"));
  }

  @Test
  public void testSettersAreNotFluentButMutateState() {
    CleanupPolicyData data = new CleanupPolicyData();
    data.setName("name");
    data.setNotes("notes");
    data.setFormat("docker");
    data.setMode("delete");
    Map<String, String> criteria = ImmutableMap.of("key", "value");
    data.setCriteria(criteria);

    assertThat(data.getName(), is("name"));
    assertThat(data.getNotes(), is("notes"));
    assertThat(data.getFormat(), is("docker"));
    assertThat(data.getMode(), is("delete"));
    assertThat(data.getCriteria(), sameInstance(criteria));
  }

  @Test
  public void testSetFormatEmptyStringIsStoredUnchanged() {
    // an empty string does not equal ALL_FORMATS ("*"), so it is stored verbatim
    underTest.setFormat("");
    assertThat(underTest.getFormat(), is(""));
  }

  @Test
  public void testSetFormatAllCleanupPolicyFormatConstantIsStoredUnchanged() {
    // only the ALL_FORMATS wildcard ("*") triggers translation; the already-translated
    // value "ALL_FORMATS" does not equal "*" and is therefore stored verbatim
    underTest.setFormat(CleanupPolicy.ALL_CLEANUP_POLICY_FORMAT);
    assertThat(underTest.getFormat(), is(CleanupPolicy.ALL_CLEANUP_POLICY_FORMAT));
  }

  @Test
  public void testCriteriaGetterReturnsSameInstanceAndEmptyMapRoundTrips() {
    Map<String, String> empty = ImmutableMap.of();
    underTest.setCriteria(empty);
    assertThat(underTest.getCriteria(), sameInstance(empty));

    Map<String, String> criteria = ImmutableMap.of("regex", ".*");
    underTest.setCriteria(criteria);
    // the getter exposes the exact instance that was set (no defensive copy)
    assertThat(underTest.getCriteria(), sameInstance(criteria));
  }

  @Test
  public void testImplementsHasNameAndCleanupPolicy() {
    assertThat(underTest, instanceOf(HasName.class));
    assertThat(underTest, instanceOf(CleanupPolicy.class));

    // getName/setName are honoured through the HasName mix-in view
    HasName asHasName = underTest;
    asHasName.setName("named-via-has-name");
    assertThat(asHasName.getName(), is("named-via-has-name"));
    assertThat(((CleanupPolicy) underTest).getName(), is("named-via-has-name"));
  }

  @Test
  public void testNotesAreNotIncludedInToString() {
    underTest.setName("my-policy");
    underTest.setNotes("these notes must not leak into toString");

    String result = underTest.toString();

    assertThat(result, is("CleanupPolicy{name='my-policy', format='null', mode='null', criteria=null}"));
    assertThat(result, not(containsString("notes")));
    assertThat(result, not(containsString("these notes must not leak into toString")));
  }
}
