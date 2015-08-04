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
package org.sonatype.nexus.test.utils;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailUtil
{
  public static final int EMAIL_SERVER_PORT;

  private static final Logger log = LoggerFactory.getLogger(EmailUtil.class);

  public static final String USER_USERNAME = "smtp-username";

  public static final String USER_PASSWORD = "smtp-password";

  public static final String USER_EMAIL = "system@nexus.org";

  static {
    String port = TestProperties.getString("email.server.port");
    EMAIL_SERVER_PORT = new Integer(port);
  }

  private static GreenMail server;

  public static synchronized GreenMail startEmailServer() {
    if (server == null) {
      // ServerSetup smtp = new ServerSetup( 1234, null, ServerSetup.PROTOCOL_SMTP );
      ServerSetup smtp = new ServerSetup(EMAIL_SERVER_PORT, null, ServerSetup.PROTOCOL_SMTP);

      server = new GreenMail(smtp);
      server.setUser(USER_EMAIL, USER_USERNAME, USER_PASSWORD);
      log.debug("Starting e-mail server");
      server.start();
    }
    return server;
  }

  public static synchronized void stopEmailServer() {
    log.debug("Stoping e-mail server");
    server.stop();
    server = null;
  }

}
