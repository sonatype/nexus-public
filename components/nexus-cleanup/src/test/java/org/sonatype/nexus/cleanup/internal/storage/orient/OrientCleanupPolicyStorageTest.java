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
package org.sonatype.nexus.cleanup.internal.storage.orient;

import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.cleanup.storage.event.CleanupPolicyCreatedEvent;
import org.sonatype.nexus.cleanup.storage.event.CleanupPolicyDeletedEvent;
import org.sonatype.nexus.cleanup.storage.event.CleanupPolicyEvent;
import org.sonatype.nexus.cleanup.storage.event.CleanupPolicyUpdatedEvent;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.orient.HexRecordIdObfuscator;
import org.sonatype.nexus.orient.entity.EntityHook;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;
import org.sonatype.nexus.repository.Format;

import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.exception.OValidationException;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sonatype.nexus.cleanup.storage.CleanupPolicy.ALL_CLEANUP_POLICY_FORMAT;

public class OrientCleanupPolicyStorageTest
    extends TestSupport
{

  private static final int VERSION = 0;

  private static class TestFormat extends Format {
    public TestFormat() {
      super("TestFormat");
    }
  }

  private static final String NAME = "name";

  private static final String NOTES = "notes for cleanup";

  private static final String FORMAT = "TestFormat";

  private static final String MODE = "TestMode";

  private static final String KEY = "key";

  private static final String VALUE = "value";

  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  private EntityHook entityHook;

  @Mock
  private EventManager eventManager;

  private CleanupPolicy item;

  private OrientCleanupPolicyStorage underTest;

  @Before
  public void setUp() throws Exception {
    entityHook = new EntityHook(eventManager);

    Orient.instance().addDbLifecycleListener(entityHook);

    OrientCleanupPolicyEntityAdapter entityAdapter = new OrientCleanupPolicyEntityAdapter();
    entityAdapter.enableObfuscation(new HexRecordIdObfuscator());
    entityAdapter.enableEntityHook(entityHook);

    underTest = new OrientCleanupPolicyStorage(database.getInstanceProvider(), entityAdapter);
    underTest.start();

    item = createPolicy();
  }

  @After
  public void tearDown() throws Exception {
    if(underTest != null) {
      underTest.stop();
      underTest = null;
    }
  }

  @Test
  public void getAllIsEmpty() {
    List<CleanupPolicy> cleanupPolicies = underTest.getAll();
    assertThat(cleanupPolicies, notNullValue());
    assertThat(cleanupPolicies.isEmpty(), is(true));
  }

  @Test
  public void canAdd() {
    CleanupPolicy cleanupPolicy = underTest.add(item);

    List<CleanupPolicy> cleanupPolicies = underTest.getAll();
    assertThat(cleanupPolicies, hasSize(1));

    CleanupPolicy firstCleanupPolicy = cleanupPolicies.get(0);
    assertCleanupPolicy(firstCleanupPolicy, MODE);

    assertThat(cleanupPolicy.getName(), equalTo(firstCleanupPolicy.getName()));
    assertThat(cleanupPolicy.getMode(), equalTo(firstCleanupPolicy.getMode()));
  }

  @Test
  public void canGetSingleItem() throws Exception {
    CleanupPolicy otherItem = createPolicy("otherName");

    underTest.add(item);
    underTest.add(otherItem);

    assertCleanupPolicy(underTest.get(item.getName()), MODE);
    assertCleanupPolicy(underTest.get(otherItem.getName()), MODE, "otherName");
  }

  @Test
  public void canUpdate() {
    String otherMode = "othermode";
    CleanupPolicy cleanupPolicy = underTest.add(item);

    item.setMode("othermode");
    CleanupPolicy updatedCleanupPolicy = underTest.update(item);
    CleanupPolicy latestCleanupPolicy = underTest.get(item.getName());

    assertCleanupPolicy(latestCleanupPolicy, "othermode");
    assertCleanupPolicy(updatedCleanupPolicy, "othermode");

    assertThat(cleanupPolicy.getName(), equalTo(updatedCleanupPolicy.getName()));
    assertThat(cleanupPolicy.getName(), equalTo(latestCleanupPolicy.getName()));
  }

  @Test
  public void canDelete() {
    underTest.add(item);
    assertThat(item.getName(), is(notNullValue()));

    underTest.remove(item);

    assertThat(underTest.getAll(), hasSize(0));
  }

  @Test
  public void canGetByFormat() throws Exception {
    underTest.add(item);

    CleanupPolicy otherMode = createPolicy("modeName");
    otherMode.setMode("otherMode");
    underTest.add(otherMode);

    CleanupPolicy otherFormat = createPolicy("formatName");
    otherFormat.setFormat("otherFormat");
    underTest.add(otherFormat);

    CleanupPolicy allFormatsName = createPolicy("allFormatsName");
    otherFormat.setFormat(ALL_CLEANUP_POLICY_FORMAT);
    underTest.add(allFormatsName);

    List<CleanupPolicy> allByFormat = underTest.getAllByFormat(FORMAT);

    assertThat(allByFormat, hasSize(3));
    assertThatCleanupPoliciesContains(allByFormat, item, otherMode, allFormatsName);
  }

  @Test(expected = ORecordDuplicatedException.class)
  public void whenUsingDuplicateNameShouldNotBeCreated() throws Exception {
    underTest.add(item);

    underTest.add(
        new CleanupPolicy(item.getName(), "abcd", "OtherFormat","OtherMode",
            newHashMap()));
  }

  @Test(expected = OValidationException.class)
  public void whenFormatIsNull() throws Exception {
    item.setFormat(null);
    underTest.add(item);
  }

  @Test
  public void addingAnItemSendsAnAddEvent() throws Exception {
    underTest.add(item);

    ArgumentCaptor<?> eventCaptor = ArgumentCaptor.forClass(Object.class);
    verify(eventManager, times(1)).post(eventCaptor.capture());

    Object[] events = eventCaptor.getAllValues().toArray();
    assertThat(events[0], instanceOf(CleanupPolicyCreatedEvent.class));
    checkEventDetails((CleanupPolicyEvent) events[0], underTest.get(item.getName()));
  }

  @Test
  public void updatingAnItemSendsAnUpdateEvent() throws Exception {
    underTest.add(item);

    item.setMode("othermode");
    underTest.update(item);

    ArgumentCaptor<?> eventCaptor = ArgumentCaptor.forClass(Object.class);
    verify(eventManager, times(2)).post(eventCaptor.capture());

    Object[] events = eventCaptor.getAllValues().toArray();
    assertThat(events[0], instanceOf(CleanupPolicyCreatedEvent.class));
    assertThat(events[1], instanceOf(CleanupPolicyUpdatedEvent.class));
    checkEventDetails((CleanupPolicyEvent) events[1], underTest.get(item.getName()));
  }

  @Test
  public void deletingAnItemSendsADeleteEvent() throws Exception {
    underTest.add(item);
    underTest.remove(item);

    ArgumentCaptor<?> eventCaptor = ArgumentCaptor.forClass(Object.class);
    verify(eventManager, times(2)).post(eventCaptor.capture());

    Object[] events = eventCaptor.getAllValues().toArray();
    assertThat(events[0], instanceOf(CleanupPolicyCreatedEvent.class));
    assertThat(events[1], instanceOf(CleanupPolicyDeletedEvent.class));
  }

  @Test(expected = OValidationException.class)
  public void whenNameIsLongerThanAllowedLengthShouldThrowException() throws Exception {
    String stringLimit = StringUtils.repeat("a", 256);
    item.setName(stringLimit);

    underTest.add(item);
  }

  @Test
  public void whenNameIsAllowedLengthShouldAdd() throws Exception {
    String stringLimit = StringUtils.repeat("a", 255);
    item.setName(stringLimit);

    underTest.add(item);
    assertThat(underTest.get(stringLimit), is(notNullValue()));
  }

  @Test
  public void existenceByNameIgnoringCase() {
    assertThat(underTest.exists(item.getName()), is(false));

    underTest.add(item);

    assertThat(underTest.exists(item.getName()), is(true));
    assertThat(underTest.exists(item.getName().toLowerCase()), is(true));
    assertThat(underTest.exists(item.getName().toUpperCase()), is(true));
  }

  private void checkEventDetails(final CleanupPolicyEvent event,
                                 final CleanupPolicy item)
  {
    CleanupPolicy eventItem = event.getCleanupPolicy();
    assertThat(eventItem.getName(), is(item.getName()));
    assertThat(eventItem.getNotes(), is(item.getNotes()));
    assertThat(eventItem.getFormat(), is(item.getFormat()));
    assertThat(eventItem.getCriteria(), is(item.getCriteria()));
  }

  private void assertCleanupPolicy(final CleanupPolicy actual, final String mode) {
    assertCleanupPolicy(actual, mode, NAME);
  }

  private void assertCleanupPolicy(final CleanupPolicy actual, final String mode, final String name) {
    assertThat(actual.getName(), is(name));
    assertThat(actual.getNotes(), is(NOTES));
    assertThat(actual.getFormat(), is(FORMAT));
    assertThat(actual.getMode(), is(mode));
    assertThat(actual.getCriteria().get(KEY), is(VALUE));
  }

  private CleanupPolicy createPolicy() {
    return createPolicy(NAME);
  }

  private CleanupPolicy createPolicy(final String name) {
    Map<String, String> criteria = newHashMap();
    criteria.put(KEY, VALUE);
    return new CleanupPolicy(name, NOTES, FORMAT, MODE, criteria);
  }

  private void assertThatCleanupPoliciesContains(List<CleanupPolicy> cleanupPolicies, CleanupPolicy... expectedCleanupPolicies) {
    for(CleanupPolicy cleanupPolicy: expectedCleanupPolicies) {
      assertThat(cleanupPolicies.stream().anyMatch(c -> c.getName().equals(cleanupPolicy.getName())), is(true));
    }
  }

}
