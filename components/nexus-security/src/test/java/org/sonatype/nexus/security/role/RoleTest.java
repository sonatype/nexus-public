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
package org.sonatype.nexus.security.role;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link Role}.
 */
public class RoleTest
    extends TestSupport
{
  @Test
  public void testCompareDifferentId() throws Exception {
    Role roleA = new Role();
    roleA.setName("ID1");
    roleA.setRoleId("ID1");
    roleA.setSource("source");

    Role roleB = new Role();
    roleB.setName("ID2");
    roleB.setRoleId("ID2");
    roleB.setSource("source");

    Assert.assertEquals(-1, roleA.compareTo(roleB));
    Assert.assertEquals(1, roleB.compareTo(roleA));
  }

  @Test
  public void testCompareDifferentSource() throws Exception {
    Role roleA = new Role();
    roleA.setName("ID1");
    roleA.setRoleId("ID1");
    roleA.setSource("source1");

    Role roleB = new Role();
    roleB.setName("ID1");
    roleB.setRoleId("ID1");
    roleB.setSource("source2");

    Assert.assertEquals(-1, roleA.compareTo(roleB));
    Assert.assertEquals(1, roleB.compareTo(roleA));
  }
}
