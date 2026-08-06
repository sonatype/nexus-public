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
package org.sonatype.nexus.cleanup.content;

import org.sonatype.nexus.cleanup.internal.storage.CleanupPolicyData;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Tests for {@link CleanupPolicyEvent} and its concrete subclasses.
 */
public class CleanupPolicyEventTest
{
  private static final String POLICY_NAME = "test-cleanup-policy";

  private CleanupPolicyData cleanupPolicy;

  @Before
  public void setUp() {
    cleanupPolicy = new CleanupPolicyData();
    cleanupPolicy.setName(POLICY_NAME);
  }

  @Test
  public void testCreatedEventStoresPolicy() {
    CleanupPolicyCreatedEvent event = new CleanupPolicyCreatedEvent(cleanupPolicy);

    assertThat(event.getCleanupPolicy(), is(sameInstance(cleanupPolicy)));
  }

  @Test
  public void testUpdatedEventStoresPolicy() {
    CleanupPolicyUpdatedEvent event = new CleanupPolicyUpdatedEvent(cleanupPolicy);

    assertThat(event.getCleanupPolicy(), is(sameInstance(cleanupPolicy)));
  }

  @Test
  public void testDeletedEventStoresPolicy() {
    CleanupPolicyDeletedEvent event = new CleanupPolicyDeletedEvent(cleanupPolicy);

    assertThat(event.getCleanupPolicy(), is(sameInstance(cleanupPolicy)));
  }

  @Test
  public void testCreatedEventToString() {
    CleanupPolicyCreatedEvent event = new CleanupPolicyCreatedEvent(cleanupPolicy);

    String result = event.toString();
    assertThat(result, containsString("CleanupPolicyCreatedEvent"));
    assertThat(result, containsString("cleanupPolicy="));
    assertThat(result, containsString(cleanupPolicy.toString()));
  }

  @Test
  public void testUpdatedEventToString() {
    CleanupPolicyUpdatedEvent event = new CleanupPolicyUpdatedEvent(cleanupPolicy);

    String result = event.toString();
    assertThat(result, containsString("CleanupPolicyUpdatedEvent"));
    assertThat(result, containsString("cleanupPolicy="));
    assertThat(result, containsString(cleanupPolicy.toString()));
  }

  @Test
  public void testDeletedEventToString() {
    CleanupPolicyDeletedEvent event = new CleanupPolicyDeletedEvent(cleanupPolicy);

    String result = event.toString();
    assertThat(result, containsString("CleanupPolicyDeletedEvent"));
    assertThat(result, containsString("cleanupPolicy="));
    assertThat(result, containsString(cleanupPolicy.toString()));
  }

  @Test(expected = NullPointerException.class)
  public void testCreatedEventRejectsNullPolicy() {
    new CleanupPolicyCreatedEvent(null);
  }

  @Test(expected = NullPointerException.class)
  public void testUpdatedEventRejectsNullPolicy() {
    new CleanupPolicyUpdatedEvent(null);
  }

  @Test(expected = NullPointerException.class)
  public void testDeletedEventRejectsNullPolicy() {
    new CleanupPolicyDeletedEvent(null);
  }
}
