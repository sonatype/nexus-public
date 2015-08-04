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
package org.sonatype.nexus.testsuite.misc.nexus421;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.sonatype.nexus.integrationtests.AbstractEmailServerNexusIT;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.proxy.repository.RemoteStatus;
import org.sonatype.nexus.rest.model.GlobalConfigurationResource;
import org.sonatype.nexus.rest.model.RepositoryProxyResource;
import org.sonatype.nexus.rest.model.RepositoryStatusResource;
import org.sonatype.nexus.rest.model.SystemNotificationSettings;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.SettingsMessageUtil;
import org.sonatype.nexus.test.utils.TestProperties;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.MediaType;

public class Nexus421PlainNotificationIT
    extends AbstractEmailServerNexusIT
{

  protected RepositoryMessageUtil repoMessageUtil;

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void testAutoBlockNotification()
      throws Exception
  {
    prepare();

    // make central auto-block itself (point it to bad URL)
    pointCentralToRemoteUrl("http://repo1.maven.org/mavenFooBar/not-here/");

    // we have 3 recipients set
    checkMails(3, 0);

    // make central unblock itself (point it to good URL)
    pointCentralToRemoteUrl("http://repo1.maven.org/maven2/");

    // we have 3 recipients set (but count with 3 "old" mails since Greenmail will _again_ return those too)
    checkMails(3, 3);
  }

  // --

  protected void prepare()
      throws Exception
  {
    // set up repo message util
    this.repoMessageUtil = new RepositoryMessageUtil(this, getXMLXStream(), MediaType.APPLICATION_XML);

    // CONFIG CHANGES (using Nexus factory-defaults!)
    // set up SMTP to use our mailServer
    // set admin role as role to be notified
    // set pipi1@wherever.com and pipi2@wherever.com as external mails to be notified
    // set notification enabled
    // save
    // enable auto-block on central

    GlobalConfigurationResource globalSettings = SettingsMessageUtil.getCurrentSettings();

    // correct SMTP hostname
    globalSettings.getSmtpSettings().setHost("localhost");
    globalSettings.getSmtpSettings().setPort(Integer.valueOf(TestProperties.getString("email.server.port")));

    SystemNotificationSettings notificationSettings = globalSettings.getSystemNotificationSettings();

    // Damian returns null here (already fixed in trunk, remove this!)
    if (notificationSettings == null) {
      notificationSettings = new SystemNotificationSettings();

      globalSettings.setSystemNotificationSettings(notificationSettings);
    }

    // set email addresses
    notificationSettings.setEmailAddresses("pipi1@wherever.com,pipi2@wherever.com");

    // this is ROLE!
    notificationSettings.getRoles().add("nx-admin");

    // enable notification
    notificationSettings.setEnabled(true);

    Assert.assertTrue("On saving global config, response should be success.",
        SettingsMessageUtil.save(globalSettings).isSuccess());

    // make a proxy server to block (do it by taking central, and breaking it's remoteURL)
    RepositoryProxyResource central = (RepositoryProxyResource) repoMessageUtil.getRepository("central");

    // make auto block active
    central.setAutoBlockActive(true);

    repoMessageUtil.updateRepo(central);
  }

  protected void pointCentralToRemoteUrl(String remoteUrl)
      throws IOException, InterruptedException
  {
    // make a proxy server to block (do it by taking central, and breaking it's remoteURL)
    RepositoryProxyResource central = (RepositoryProxyResource) repoMessageUtil.getRepository("central");

    // direct the repo to nonexistent maven2 repo
    central.getRemoteStorage().setRemoteStorageUrl(remoteUrl);

    repoMessageUtil.updateRepo(central);

    // to "ping it" (and wait for all the thread to check remote availability)
    RepositoryStatusResource res = repoMessageUtil.getStatus("central", true);

    while (RemoteStatus.UNKNOWN.name().equals(res.getRemoteStatus())) {
      res = repoMessageUtil.getStatus("central", false);

      Thread.sleep(10000);
    }
  }

  protected void checkMails(int expectedBlockedMails, int expectedUnblockedMails)
      throws InterruptedException, MessagingException
  {
    // expect total 2*count mails: once for auto-block, once for unblock, for admin user, for pipi1 and for pipi2.
    // Mail
    // should be about "unblocked"
    // wait for long, since we really _dont_ know when the mail gonna be sent:
    // See "fibonacci" calculation above!
    for (int retryCount = 0; retryCount < 30; retryCount++) {
      if (waitForMail(expectedBlockedMails + expectedUnblockedMails, 15000)) {
        break;
      }
    }

    MimeMessage[] msgs = server.getReceivedMessages();

    Assert.assertNotNull("Messages array should not be null!", msgs);

    int blockedMails = 0;

    int unblockedMails = 0;

    for (int i = 0; i < msgs.length; i++) {
      MimeMessage msg = msgs[i];

      if (msg.getSubject().toLowerCase().contains("auto-blocked")) {
        blockedMails++;
      }
      else if (msg.getSubject().toLowerCase().contains("unblocked")) {
        unblockedMails++;
      }
    }

    Assert.assertEquals("We should have " + expectedBlockedMails
        + " auto-blocked mails!", blockedMails, expectedBlockedMails);

    Assert.assertEquals("We should have " + expectedUnblockedMails
        + " auto-UNblocked mails!", unblockedMails, expectedUnblockedMails);
  }
}
