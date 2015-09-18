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
package org.sonatype.nexus.analytics;

import org.sonatype.goodies.testsupport.TestSupport;

import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;
import org.apache.shiro.util.ThreadState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link EventDataBuilder}.
 */
public class EventDataBuilderTest
    extends TestSupport
{
  private ThreadState threadState;

  @Mock
  private Subject subject;

  @Before
  public void setUp() throws Exception {
    threadState = new SubjectThreadState(subject);
    threadState.bind();
  }

  @After
  public void tearDown() throws Exception {
    if (threadState != null) {
      threadState.clear();
      threadState = null;
    }
  }

  @Test
  public void basics() throws Exception {
    when(subject.getPrincipal()).thenReturn("foo");
    Session session = mock(Session.class);
    when(subject.getSession(false)).thenReturn(session);
    when(session.getId()).thenReturn("1234");

    EventData event = new EventDataBuilder("TEST").build();
    assertThat(event, notNullValue());
    assertThat(event.getType(), is("TEST"));
    assertThat(event.getTimestamp(), notNullValue());
    assertThat(event.getSequence(), notNullValue());
    assertThat(event.getUserId(), is("foo"));
    assertThat(event.getSessionId(), is("1234"));
    assertThat(event.getDuration(), notNullValue());
  }

  @Test
  public void subjectWithNullPrincipal() throws Exception {
    when(subject.getPrincipal()).thenReturn(null);

    EventData event = new EventDataBuilder("TEST").build();
    assertThat(event, notNullValue());
    assertThat(event.getUserId(), nullValue());
  }

  @Test
  public void subjectWithNullSession() throws Exception {
    when(subject.getSession(false)).thenReturn(null);

    EventData event = new EventDataBuilder("TEST").build();
    assertThat(event, notNullValue());
    assertThat(event.getSessionId(), nullValue());
  }

  @Test
  public void setAttributes() throws Exception {
    EventData event = new EventDataBuilder("TEST")
        .set("foo", "bar")
        .set("baz", null)
        .build();

    assertThat(event, notNullValue());
    assertThat(event.getAttributes().entrySet(), hasSize(2));
    assertThat(event.getAttributes(), hasEntry("foo", (Object) "bar"));
    assertThat(event.getAttributes(), hasEntry("baz", null));
  }
}
