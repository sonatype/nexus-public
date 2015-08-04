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
package org.sonatype.nexus.email;

import org.sonatype.micromailer.Address;
import org.sonatype.micromailer.EMailer;
import org.sonatype.micromailer.MailRequest;
import org.sonatype.micromailer.MailRequestStatus;

/**
 * Simple service interface to expose the properly configured MicroMailer and also some helper methods for creating and
 * sending emails. This component will also keep EMailer configuration in sync with Nexus.
 *
 * @author cstamas
 */
public interface NexusEmailer
{
  /**
   * Gets the preconfigured EMailer instance for prepared for using it.
   */
  EMailer getEMailer();

  /**
   * Returns the system-wide default mail type used as default mailType for outgoing mails.
   */
  String getDefaultMailTypeId();

  /**
   * Returns a prepopulated MailRequest. The request only needs to set the To, CC, Bcc (or override any of the
   * defaulted values) and send it.
   *
   * @param subject the string used for subject. May be Velocity template, but the API consumer should take care to
   *                populate the request context then.
   * @param body    the string used for body. May be Velocity template, but the API consumer should take care to
   *                populate
   *                the request context then.
   */
  MailRequest getDefaultMailRequest(String subject, String body);

  /**
   * A shortcut method.
   */
  MailRequestStatus sendMail(MailRequest request);

  // ==

  String getSMTPHostname();

  void setSMTPHostname(String host);

  int getSMTPPort();

  void setSMTPPort(int port);

  boolean isSMTPSslEnabled();

  void setSMTPSslEnabled(boolean val);

  boolean isSMTPTlsEnabled();

  void setSMTPTlsEnabled(boolean val);

  String getSMTPUsername();

  void setSMTPUsername(String username);

  String getSMTPPassword();

  void setSMTPPassword(String password);

  Address getSMTPSystemEmailAddress();

  void setSMTPSystemEmailAddress(Address adr);

  boolean isSMTPDebug();

  void setSMTPDebug(boolean val);
}
