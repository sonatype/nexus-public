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
package org.sonatype.nexus.testsuite.security.nexus395;

import javax.mail.internet.MimeMessage;

import org.sonatype.nexus.integrationtests.AbstractEmailServerNexusIT;

import com.icegreen.greenmail.util.GreenMailUtil;
import org.junit.Assert;

/**
 * @author juven
 */
public abstract class AbstractForgotUserNameIT
    extends AbstractEmailServerNexusIT
{
  protected void assertRecoveredUserName(String expectedUserName)
      throws Exception
  {
    // Need 1 message
    waitForMail(1);

    MimeMessage[] msgs = server.getReceivedMessages();

    for (MimeMessage msg : msgs) {
      log.debug("Mail Title:\n" + GreenMailUtil.getHeaders(msg));
      log.debug("Mail Body:\n" + GreenMailUtil.getBody(msg));
    }

    String username = null;
    // Sample body: Your password has been reset. Your new password is: c1r6g4p8l7
    String body = GreenMailUtil.getBody(msgs[0]);

    int index = body.indexOf(" - \"");
    int usernameStartIndex = index + " - \"".length();
    if (index != -1) {
      username = body.substring(usernameStartIndex, body.indexOf('\"', usernameStartIndex)).trim();
    }

    Assert.assertEquals(username, expectedUserName);
  }
}
