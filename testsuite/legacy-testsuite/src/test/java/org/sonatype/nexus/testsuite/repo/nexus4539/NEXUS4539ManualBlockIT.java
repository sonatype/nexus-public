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

import org.sonatype.nexus.proxy.repository.ProxyMode;
import org.sonatype.nexus.proxy.repository.RemoteStatus;

import org.junit.Before;
import org.junit.Test;

/**
 * while a proxy is auto-blocked move the proxy repository state to manually blocked. make sure it is still manually
 * blocked after the auto-block thread wakes to recheck the status
 */
public class NEXUS4539ManualBlockIT
    extends AutoBlockITSupport
{

  @Before
  public void setTimeout() {
    super.sleepTime = 100;
  }

  @Test
  public void manualBlock()
      throws Exception
  {
    // initial status, timing out
    waitFor(RemoteStatus.UNAVAILABLE, ProxyMode.BLOCKED_AUTO, false);

    // set manual block
    repoUtil.setBlockProxy(REPO, true);
    assertRepositoryStatus(repoUtil.getStatus(REPO), RemoteStatus.UNAVAILABLE, ProxyMode.BLOCKED_MANUAL);

    // server back to normal
    super.sleepTime = -1;

    // nexus shall not unblock
    Thread.sleep(15 * 1000);
    assertRepositoryStatus(repoUtil.getStatus(REPO), RemoteStatus.UNAVAILABLE, ProxyMode.BLOCKED_MANUAL);

    // must still be manual blocked
    shakeNexus();
    assertRepositoryStatus(repoUtil.getStatus(REPO), RemoteStatus.UNAVAILABLE, ProxyMode.BLOCKED_MANUAL);
  }


}
