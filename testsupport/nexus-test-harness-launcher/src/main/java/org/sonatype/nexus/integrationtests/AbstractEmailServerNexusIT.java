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
package org.sonatype.nexus.integrationtests;

import org.sonatype.nexus.test.utils.TestProperties;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractEmailServerNexusIT
    extends AbstractNexusIntegrationTest
{

  private static final Logger LOG = LoggerFactory.getLogger(AbstractEmailServerNexusIT.class);

  private static int emailServerPort;

  protected static GreenMail server;

  @BeforeClass
  public static void startEmailServer() {
    String port = TestProperties.getString("email.server.port");
    emailServerPort = new Integer(port);
    // ServerSetup smtp = new ServerSetup( 1234, null, ServerSetup.PROTOCOL_SMTP );
    ServerSetup smtp = new ServerSetup(emailServerPort, null, ServerSetup.PROTOCOL_SMTP);

    server = new GreenMail(smtp);
    server.setUser("system@nexus.org", "smtp-username", "smtp-password");
    LOG.debug("Starting e-mail server");
    server.start();
  }

  @AfterClass
  public static void stopEmailServer() {
    if (server != null) {
      LOG.debug("Stoping e-mail server");
      server.stop();

      server = null;
    }
  }

  protected boolean waitForMail(int count) {
    return waitForMail(count, 5000);
  }

  protected boolean waitForMail(int count, long timeout) {
    try {
      return server.waitForIncomingEmail(timeout, count);
    }
    catch (InterruptedException e) {
      return false;
    }
  }

}
