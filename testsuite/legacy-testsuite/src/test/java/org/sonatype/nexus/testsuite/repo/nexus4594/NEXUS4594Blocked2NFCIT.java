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
package org.sonatype.nexus.testsuite.repo.nexus4594;

import org.junit.Test;

public class NEXUS4594Blocked2NFCIT
    extends Blocked2NFCITSupport
{

  @Test
  public void whileNexusIsAutoBlockedItDoesNotAddPathsToNFC()
      throws Exception
  {
    // auto block Nexus
    autoBlockNexus();

    // make a request to an arbitrary artifact and verify that Nexus did not went remote (repository is blocked)
    // Nexus should not add it to NFC, but that will see later while re-requesting the artifact with Nexus unblocked
    downloadArtifact("foo", "bar", "5.0");
    verifyNexusDidNotWentRemote();

    // unblock Nexus so we can request again the arbitrary artifact
    autoUnblockNexus();

    // make a request and check that Nexus went remote (so is not in NFC)
    downloadArtifact("foo", "bar", "5.0");
    verifyNexusWentRemote();
  }

}
