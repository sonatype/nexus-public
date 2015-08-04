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
package org.sonatype.nexus.jsecurity;

import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.security.SecuritySystem;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class DefaultNexusSecurityUpgradeTest
    extends NexusAppTestSupport
{

  private static final String ORG_CONFIG_FILE = "target/test-classes/org/sonatype/nexus/jsecurity/security.xml";

  @Test
  public void testDoUpgrade() throws Exception {
    this.lookup(SecuritySystem.class);
  }

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    // copy the file to a different location because we are going to change it
    FileUtils.copyFileToDirectory(util.resolveFile(ORG_CONFIG_FILE), getConfHomeDir(), false);
  }
}
