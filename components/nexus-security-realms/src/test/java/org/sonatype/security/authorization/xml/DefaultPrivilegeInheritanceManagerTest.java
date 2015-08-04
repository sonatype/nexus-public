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
package org.sonatype.security.authorization.xml;

import java.util.List;

import org.eclipse.sisu.launch.InjectedTestCase;

public class DefaultPrivilegeInheritanceManagerTest
    extends InjectedTestCase
{
  private DefaultPrivilegeInheritanceManager manager;

  protected void setUp()
      throws Exception
  {
    super.setUp();

    manager = (DefaultPrivilegeInheritanceManager) this.lookup(PrivilegeInheritanceManager.class);
  }

  public void testCreateInherit()
      throws Exception
  {
    List<String> methods = manager.getInheritedMethods("create");

    assertTrue(methods.size() == 2);
    assertTrue(methods.contains("read"));
    assertTrue(methods.contains("create"));
  }

  public void testReadInherit()
      throws Exception
  {
    List<String> methods = manager.getInheritedMethods("read");

    assertTrue(methods.size() == 1);
    assertTrue(methods.contains("read"));
  }

  public void testUpdateInherit()
      throws Exception
  {
    List<String> methods = manager.getInheritedMethods("update");

    assertTrue(methods.size() == 2);
    assertTrue(methods.contains("read"));
    assertTrue(methods.contains("update"));
  }

  public void testDeleteInherit()
      throws Exception
  {
    List<String> methods = manager.getInheritedMethods("delete");

    assertTrue(methods.size() == 2);
    assertTrue(methods.contains("read"));
    assertTrue(methods.contains("delete"));
  }
}
