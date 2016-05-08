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

import org.apache.shiro.subject.Subject;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.UserIdHelper.UNKNOWN;

/**
 * Tests for {@link UserIdHelper}.
 */
public class UserIdHelperTest
  extends TestSupport
{
  private Subject subject(final Object principal) {
    Subject subject = mock(Subject.class);
    when(subject.getPrincipal()).thenReturn(principal);
    return subject;
  }

  @Test
  public void get_subject() {
    assertThat(UserIdHelper.get(subject("test")), is("test"));
  }

  @Test
  public void get_nullSubject() {
    assertThat(UserIdHelper.get(null), is(UNKNOWN));
  }

  @Test
  public void get_nullPrincipal() {
    assertThat(UserIdHelper.get(subject(null)), is(UNKNOWN));
  }
}
