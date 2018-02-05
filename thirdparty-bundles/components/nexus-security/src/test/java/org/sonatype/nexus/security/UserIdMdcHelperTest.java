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
package org.sonatype.nexus.security;

import org.sonatype.goodies.testsupport.TestSupport;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.UserIdHelper.UNKNOWN;
import static org.sonatype.nexus.security.UserIdMdcHelper.KEY;

/**
 * Tests for {@link UserIdMdcHelper}.
 */
public class UserIdMdcHelperTest
  extends TestSupport
{
  private void reset() {
    MDC.remove(KEY);
    ThreadContext.unbindSubject();
    ThreadContext.unbindSecurityManager();
  }

  @Before
  public void setUp() throws Exception {
    reset();
  }

  @After
  public void tearDown() throws Exception {
    reset();
  }

  private Subject subject(final Object principal) {
    Subject subject = mock(Subject.class);
    when(subject.getPrincipal()).thenReturn(principal);
    return subject;
  }

  @Test
  public void isSet() {
    MDC.put(KEY, "test");
    assertThat(UserIdMdcHelper.isSet(), is(true));
  }

  @Test
  public void isSet_withNull() {
    String value = MDC.get(KEY);
    assertThat(value, nullValue());
    assertThat(UserIdMdcHelper.isSet(), is(false));
  }

  @Test
  public void isSet_withBlank() {
    MDC.put(KEY, "");
    assertThat(UserIdMdcHelper.isSet(), is(false));
  }

  @Test
  public void isSet_withUnknown() {
    MDC.put(KEY, UNKNOWN);
    assertThat(UserIdMdcHelper.isSet(), is(false));
  }

  @Test(expected = NullPointerException.class)
  public void set_withNull() {
    UserIdMdcHelper.set(null);
  }

  @Test
  public void set_withSubject() {
    UserIdMdcHelper.set(subject("test"));

    assertThat(UserIdMdcHelper.isSet(), is(true));
    assertThat(MDC.get(KEY), is("test"));
  }

  @Test
  public void set_notSet() {
    ThreadContext.bind(subject("test"));

    UserIdMdcHelper.set();

    assertThat(UserIdMdcHelper.isSet(), is(true));
    assertThat(MDC.get(KEY), is("test"));
  }

  @Test
  public void set_notSet_withoutSubject() {
    ThreadContext.bind(mock(SecurityManager.class));

    UserIdMdcHelper.set();

    assertThat(UserIdMdcHelper.isSet(), is(false));
    assertThat(MDC.get(KEY), is(UNKNOWN));
  }

  @Test
  public void set_alreadySet() {
    MDC.put(KEY, "foo");

    ThreadContext.bind(subject("test"));

    UserIdMdcHelper.set();

    assertThat(UserIdMdcHelper.isSet(), is(true));
    assertThat(MDC.get(KEY), is("test"));
  }

  @Test
  public void setIfNeeded_notSet() {
    ThreadContext.bind(subject("test"));

    UserIdMdcHelper.setIfNeeded();

    assertThat(UserIdMdcHelper.isSet(), is(true));
    assertThat(MDC.get(KEY), is("test"));
  }

  @Test
  public void setIfNeeded_alreadySet() {
    MDC.put(KEY, "foo");

    ThreadContext.bind(subject("test"));

    UserIdMdcHelper.setIfNeeded();

    assertThat(UserIdMdcHelper.isSet(), is(true));
    assertThat(MDC.get(KEY), is("foo"));
  }
}
