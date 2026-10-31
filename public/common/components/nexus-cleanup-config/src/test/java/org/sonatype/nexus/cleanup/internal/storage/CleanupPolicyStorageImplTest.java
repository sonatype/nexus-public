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

import java.util.List;
import java.util.Map;

import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.cleanup.storage.event.CleanupPolicyDeletedEvent;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseExtension;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DatabaseExtension.class)
class CleanupPolicyStorageImplTest
{
  @DataSessionConfiguration(daos = {CleanupPolicyDAO.class})
  TestDataSessionSupplier sessionSupplier;

  private EventManager eventManager;

  private CleanupPolicyStorageImpl underTest;

  @BeforeEach
  void setUp() {
    eventManager = mock(EventManager.class);
    underTest = new CleanupPolicyStorageImpl(sessionSupplier, eventManager);
  }

  @DatabaseTest
  void constructorRejectsNullEventManager() {
    assertThrows(NullPointerException.class, () -> new CleanupPolicyStorageImpl(sessionSupplier, null));
  }

  @DatabaseTest
  void newCleanupPolicyReturnsCleanupPolicyData() {
    CleanupPolicy policy = underTest.newCleanupPolicy();

    assertThat(policy, is(notNullValue()));
    assertThat(policy, is(instanceOf(CleanupPolicyData.class)));
    // a fresh, empty instance is returned
    assertThat(policy.getName(), is(nullValue()));
    assertThat(policy.getFormat(), is(nullValue()));
    assertThat(policy.getCriteria(), is(nullValue()));
    // each invocation returns a distinct instance
    assertThat(underTest.newCleanupPolicy(), is(not(sameInstance(policy))));
  }

  @DatabaseTest
  void addReturnsPolicyAndPersistsIt() {
    CleanupPolicy policy = policy("foo", "some text", "maven2", "deletion", Map.of("bar", "one"));

    CleanupPolicy added = underTest.add(policy);

    assertThat(added, is(sameInstance(policy)));

    CleanupPolicy read = underTest.get("foo");
    assertThat(read, is(notNullValue()));
    assertThat(read.getName(), is("foo"));
    assertThat(read.getNotes(), is("some text"));
    assertThat(read.getFormat(), is("maven2"));
    assertThat(read.getMode(), is("deletion"));
    assertThat(read.getCriteria(), is(Map.of("bar", "one")));
  }

  @DatabaseTest
  void getReturnsNullForMissingPolicy() {
    assertThat(underTest.get("missing"), is(nullValue()));
  }

  @DatabaseTest
  void existsReflectsPresence() {
    underTest.add(policy("foo", "some text", "maven2", "deletion", Map.of("bar", "one")));

    assertThat(underTest.exists("foo"), is(true));
    assertThat(underTest.exists("missing"), is(false));
  }

  @DatabaseTest
  void getAllReturnsAllPolicies() {
    assertThat(underTest.getAll(), hasSize(0));

    underTest.add(policy("foo", "some text", "maven2", "deletion", Map.of("bar", "one")));
    underTest.add(policy("bar", "more text", "npm", "deletion", Map.of("baz", "two")));

    List<CleanupPolicy> all = underTest.getAll();

    assertThat(all, hasSize(2));
    assertThat(all.stream().map(CleanupPolicy::getName).toList(), containsInAnyOrder("foo", "bar"));
  }

  @DatabaseTest
  void getAllByFormatFiltersByFormat() {
    underTest.add(policy("foo", "some text", "maven2", "deletion", Map.of("bar", "one")));
    underTest.add(policy("bar", "more text", "npm", "deletion", Map.of("baz", "two")));

    List<CleanupPolicy> mavenPolicies = underTest.getAllByFormat("maven2");

    assertThat(mavenPolicies, hasSize(1));
    assertThat(mavenPolicies.get(0).getName(), is("foo"));

    assertThat(underTest.getAllByFormat("nuget"), hasSize(0));
  }

  @DatabaseTest
  void getAllByFormatIncludesAllFormatsPolicies() {
    // a policy registered for ALL_FORMATS ("*") is stored under ALL_CLEANUP_POLICY_FORMAT
    CleanupPolicy allFormats = policy("all", "any format", CleanupPolicy.ALL_FORMATS, "deletion", Map.of("k", "v"));
    assertThat(allFormats.getFormat(), is(CleanupPolicy.ALL_CLEANUP_POLICY_FORMAT));
    underTest.add(allFormats);
    underTest.add(policy("foo", "some text", "maven2", "deletion", Map.of("bar", "one")));

    // a concrete format query returns both the matching policy and the all-formats policy
    assertThat(underTest.getAllByFormat("maven2").stream().map(CleanupPolicy::getName).toList(),
        containsInAnyOrder("foo", "all"));

    // an unrelated format query still returns the all-formats policy
    assertThat(underTest.getAllByFormat("npm").stream().map(CleanupPolicy::getName).toList(),
        contains("all"));
  }

  @DatabaseTest
  void countReflectsNumberOfPolicies() {
    assertThat(underTest.count(), is(0L));

    underTest.add(policy("foo", "some text", "maven2", "deletion", Map.of("bar", "one")));
    underTest.add(policy("bar", "more text", "npm", "deletion", Map.of("baz", "two")));

    assertThat(underTest.count(), is(2L));
  }

  @DatabaseTest
  void updateReturnsPolicyAndPersistsChanges() {
    CleanupPolicy policy = policy("foo", "some text", "maven2", "deletion", Map.of("bar", "one"));
    underTest.add(policy);

    policy.setNotes("some other text");
    policy.setMode("other");

    CleanupPolicy updated = underTest.update(policy);

    assertThat(updated, is(sameInstance(policy)));

    CleanupPolicy read = underTest.get("foo");
    assertThat(read.getNotes(), is("some other text"));
    assertThat(read.getMode(), is("other"));
    // untouched fields remain persisted
    assertThat(read.getFormat(), is("maven2"));
    assertThat(read.getCriteria(), is(Map.of("bar", "one")));
  }

  @DatabaseTest
  void removeDeletesPolicyAndPostsDeletedEvent() {
    CleanupPolicy policy = policy("foo", "some text", "maven2", "deletion", Map.of("bar", "one"));
    underTest.add(policy);
    assertThat(underTest.count(), is(1L));

    underTest.remove(policy);

    assertThat(underTest.count(), is(0L));
    assertThat(underTest.get("foo"), is(nullValue()));
    assertThat(underTest.exists("foo"), is(false));

    ArgumentCaptor<CleanupPolicyDeletedEvent> captor = ArgumentCaptor.forClass(CleanupPolicyDeletedEvent.class);
    verify(eventManager).post(captor.capture());

    CleanupPolicyDeletedEvent event = captor.getValue();
    assertThat(event.isLocal(), is(true));
    assertThat(event.getCleanupPolicy(), is(sameInstance(policy)));
  }

  private static CleanupPolicyData policy(
      final String name,
      final String notes,
      final String format,
      final String mode,
      final Map<String, String> criteria)
  {
    CleanupPolicyData policy = new CleanupPolicyData();
    policy.setName(name);
    policy.setNotes(notes);
    policy.setFormat(format);
    policy.setMode(mode);
    policy.setCriteria(criteria);
    return policy;
  }
}
