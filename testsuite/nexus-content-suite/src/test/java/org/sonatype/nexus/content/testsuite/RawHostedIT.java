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
package org.sonatype.nexus.content.testsuite;

import org.sonatype.nexus.content.testsupport.raw.RawClient;
import org.sonatype.nexus.content.testsupport.raw.RawITSupport;

import org.junit.Before;
import org.junit.Test;

public class RawHostedIT
    extends RawITSupport
{
  public static final String HOSTED_REPO = "raw-test-hosted";

  public static final String TEST_CONTENT = "alphabet.txt";

  private RawClient rawClient;

  @Before
  public void createHostedRepository() throws Exception {
    rawClient = rawClient(repos.createRawHosted(HOSTED_REPO));
  }

  @Test
  public void uploadAndDownload() throws Exception {
    uploadAndDownload(rawClient, TEST_CONTENT);
  }

  @Test
  public void redeploy() throws Exception {
    uploadAndDownload(rawClient, TEST_CONTENT);
    uploadAndDownload(rawClient, TEST_CONTENT);
  }
}
