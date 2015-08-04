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
package org.sonatype.nexus.testsuite.p2.meclipse0465x02;

import org.sonatype.nexus.testsuite.p2.AbstractNexusProxyP2SecureIT;

import org.junit.Assert;
import org.junit.Test;

public class MECLIPSE0465x02ProxyAuthenticatedP2RepoBadPasswordIT
    extends AbstractNexusProxyP2SecureIT
{

  public MECLIPSE0465x02ProxyAuthenticatedP2RepoBadPasswordIT() {
    super("meclipse0465x02");
  }

  @Test
  public void test()
      throws Exception
  {
    try {
      installAndVerifyP2Feature();
      Assert.fail("Expected P2 Exception containing text: [Unable to load repositories.]");
    }
    catch (final Exception e) {
      if (!e.getMessage().contains("Unable to load repositories.")) {
        throw e;
      }
    }
  }

}
