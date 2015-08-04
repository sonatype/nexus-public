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

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.micromailer.Address;
import org.sonatype.micromailer.MailRequest;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The default nexus post office.
 *
 * @author Alin Dreghiciu
 */
@Named
@Singleton
public class DefaultNexusPostOffice
    implements NexusPostOffice
{
  private final NexusEmailer nexusEmailer;

  @Inject
  public DefaultNexusPostOffice(final NexusEmailer nexusEmailer) {
    this.nexusEmailer = checkNotNull(nexusEmailer);
  }

  /**
   * {@inheritDoc}
   */
  public void sendNexusTaskFailure(final String email, final String taskId, final String taskName,
                                   final Throwable cause)
  {
    final StringBuilder body = new StringBuilder();

    if (taskId != null) {
      body.append(String.format("Task ID: %s", taskId)).append("\n");
    }

    if (taskName != null) {
      body.append(String.format("Task Name: %s", taskName)).append("\n");
    }

    if (cause != null) {
      final StringWriter sw = new StringWriter();
      final PrintWriter pw = new PrintWriter(sw);
      cause.printStackTrace(pw);
      body.append("Stack trace: ").append("\n").append(sw.toString());
    }

    MailRequest request = nexusEmailer.getDefaultMailRequest("Nexus: Task execution failure", body.toString());

    request.getToAddresses().add(new Address(email));

    nexusEmailer.sendMail(request);
  }

}