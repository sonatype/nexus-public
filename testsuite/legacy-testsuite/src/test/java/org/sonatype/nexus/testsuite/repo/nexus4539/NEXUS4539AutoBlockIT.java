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
package org.sonatype.nexus.testsuite.repo.nexus4539;

import org.junit.Test;

/**
 * Make sure normal case works, that is, a proxy whose remote is timing out blocks, and then unblocks when the remote
 * is
 * available. It does take around 2 minutes to run.
 */
public class NEXUS4539AutoBlockIT
    extends AutoBlockITSupport
{

  @Test
  public void autoBlock()
      throws Exception
  {
    // initial status, no timing out
    autoUnblockNexus();

    // block Nexus
    autoBlockNexus();

    // it must unblock auto magically
    autoUnblockNexus();

    // let's see if it will block again
    autoBlockNexus();
    // let is sit on ice for 30s
    Thread.sleep(30 * 1000);

    // it must auto unblock again
    autoUnblockNexus();
  }

}
