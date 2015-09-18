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
package org.sonatype.nexus.security.privilege;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.sonatype.nexus.security.privilege.PrivilegeDescriptorSupport.ALL;
import static org.sonatype.nexus.security.privilege.PrivilegeDescriptorSupport.humanizeActions;
import static org.sonatype.nexus.security.privilege.PrivilegeDescriptorSupport.humanizeName;

/**
 * Tests for {@link PrivilegeDescriptorSupport}.
 */
public class PrivilegeDescriptorSupportTest
    extends TestSupport
{
  @Test
  public void humanizeNameTest() throws Exception {
    assertThat(humanizeName(ALL, ALL), equalTo("all"));
    assertThat(humanizeName(ALL, "bar"), equalTo("all 'bar'-format"));
    assertThat(humanizeName("foo", "bar"), equalTo("foo"));
  }

  @Test
  public void humanizeActionsTest() throws Exception {
    assertThat(humanizeActions(ALL), equalTo("All privileges"));
    assertThat(humanizeActions("FOO"), equalTo("Foo privilege"));
    assertThat(humanizeActions("foo"), equalTo("Foo privilege"));
    assertThat(humanizeActions("FOO", "BAR", "BAZ"), equalTo("Foo, Bar, Baz privileges"));
    assertThat(humanizeActions("foo", "bar", "baz"), equalTo("Foo, Bar, Baz privileges"));
  }
}