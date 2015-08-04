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
package org.sonatype.nexus.testsuite.config.nexus1806;

import java.io.IOException;

import javax.mail.internet.MimeMessage;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.SmtpSettingsResource;
import org.sonatype.nexus.test.utils.EmailUtil;
import org.sonatype.nexus.test.utils.SettingsMessageUtil;
import org.sonatype.nexus.test.utils.TestProperties;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.Status;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.test.utils.EmailUtil.USER_EMAIL;
import static org.sonatype.nexus.test.utils.EmailUtil.USER_PASSWORD;
import static org.sonatype.nexus.test.utils.EmailUtil.USER_USERNAME;
import static org.sonatype.nexus.test.utils.NexusRequestMatchers.hasStatusCode;
import static org.sonatype.nexus.test.utils.NexusRequestMatchers.isSuccess;

public class Nexus1806ValidateSmtpConfigurationIT
    extends AbstractNexusIntegrationTest
{

  private GreenMail changedServer;

  private int port;

  private GreenMail originalServer;

  @Before
  public void init() {
    port = TestProperties.getInteger("webproxy-server-port");
    // it is necessary to change port to make sure it worked
    ServerSetup smtp = new ServerSetup(port, null, ServerSetup.PROTOCOL_SMTP);

    changedServer = new GreenMail(smtp);
    changedServer.setUser(USER_EMAIL, USER_USERNAME, USER_PASSWORD);
    log.debug("Starting e-mail server");
    changedServer.start();

    originalServer = EmailUtil.startEmailServer();
  }

  @Test
  public void validateChangedSmtp()
      throws Exception
  {
    run(port, changedServer);
  }

  @Test
  public void validateOriginalSmtp()
      throws Exception
  {
    run(EmailUtil.EMAIL_SERVER_PORT, originalServer);
  }

  @Test
  public void invalidServer()
      throws Exception
  {
    SmtpSettingsResource smtpSettings = new SmtpSettingsResource();
    smtpSettings.setHost("not:a:server:90854322");
    smtpSettings.setPort(1234);
    smtpSettings.setUsername(EmailUtil.USER_USERNAME);
    smtpSettings.setPassword(EmailUtil.USER_PASSWORD);
    smtpSettings.setSystemEmailAddress(EmailUtil.USER_EMAIL);
    smtpSettings.setTestEmail("test_user@sonatype.org");
    Status status = SettingsMessageUtil.save(smtpSettings);
    assertThat(status, hasStatusCode(400));
  }

  @Test
  public void invalidUsername()
      throws Exception
  {
    if (true) {
      // greenmail doesn't allow authentication
      printKnownErrorButDoNotFail(getClass(), "invalidUsername()");
      return;
    }

    String login = "invaliduser_test";
    String email = "invaliduser_test@sonatype.org";
    changedServer.setUser(email, login, "%^$@invalidUserPW**");

    SmtpSettingsResource smtpSettings = new SmtpSettingsResource();
    smtpSettings.setHost("localhost");
    smtpSettings.setPort(port);
    smtpSettings.setUsername(login);
    smtpSettings.setPassword(USER_PASSWORD);
    smtpSettings.setSystemEmailAddress(email);
    smtpSettings.setTestEmail("test_user@sonatype.org");
    Status status = SettingsMessageUtil.save(smtpSettings);
    assertThat(status, hasStatusCode(400));
  }

  private void run(int port, GreenMail server)
      throws IOException, InterruptedException
  {
    SmtpSettingsResource smtpSettings = new SmtpSettingsResource();
    smtpSettings.setHost("localhost");
    smtpSettings.setPort(port);
    smtpSettings.setUsername(EmailUtil.USER_USERNAME);
    smtpSettings.setPassword(EmailUtil.USER_PASSWORD);
    smtpSettings.setSystemEmailAddress(EmailUtil.USER_EMAIL);
    smtpSettings.setTestEmail("test_user@sonatype.org");

    Status status = SettingsMessageUtil.save(smtpSettings);
    assertThat(status, isSuccess());

    server.waitForIncomingEmail(5000, 1);

    MimeMessage[] msgs = server.getReceivedMessages();
    Assert.assertEquals(1, msgs.length);

    MimeMessage msg = msgs[0];
    String body = GreenMailUtil.getBody(msg);

    Assert.assertNotNull(body, "Missing message");
    Assert.assertFalse("Got empty message", body.trim().length() == 0);
  }

  @After
  public void stop() {
    if (originalServer != null) {
      EmailUtil.stopEmailServer();

      originalServer = null;
    }
    if (changedServer != null) {
      changedServer.stop();

      changedServer = null;
    }
  }
}
