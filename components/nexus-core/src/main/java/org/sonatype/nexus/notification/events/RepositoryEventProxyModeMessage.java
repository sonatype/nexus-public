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
package org.sonatype.nexus.notification.events;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import org.sonatype.nexus.notification.NotificationMessage;
import org.sonatype.nexus.proxy.events.RepositoryEventProxyMode;
import org.sonatype.nexus.proxy.repository.ProxyMode;
import org.sonatype.security.usermanagement.User;

import org.apache.commons.lang.time.DurationFormatUtils;

public class RepositoryEventProxyModeMessage
    implements NotificationMessage
{
  private final RepositoryEventProxyMode repositoryEventProxyMode;

  private final User user;

  private final String title;

  private final String body;

  public RepositoryEventProxyModeMessage(RepositoryEventProxyMode revt, User user) {
    this.repositoryEventProxyMode = revt;

    this.user = user;

    // we will reuse this
    StringBuilder sb = null;

    // -- Title
    sb = new StringBuilder("Proxy repository  \"");

    sb.append(revt.getRepository().getName());

    sb.append("\" (repoId=").append(revt.getRepository().getId()).append(") was ");

    if (ProxyMode.ALLOW.equals(revt.getNewProxyMode())) {
      sb.append("unblocked.");
    }
    else if (ProxyMode.BLOCKED_AUTO.equals(revt.getNewProxyMode())) {
      sb.append("auto-blocked.");
    }
    else if (ProxyMode.BLOCKED_MANUAL.equals(revt.getNewProxyMode())) {
      sb.append("blocked.");
    }
    else {
      sb.append(revt.getRepository().getProxyMode().toString()).append(".");
    }

    this.title = sb.toString();

    // -- Body

    sb = new StringBuilder("Howdy,\n\n");

    sb.append("the proxy mode of repository \"");

    sb.append(revt.getRepository().getName());

    sb.append("\" (repoId=").append(revt.getRepository().getId());

    sb.append(", remoteUrl=");

    sb.append(revt.getRepository().getRemoteUrl());

    sb.append(") was set to \n\n");

    if (ProxyMode.ALLOW.equals(revt.getNewProxyMode())) {
      sb.append("Allow.");
    }
    else if (ProxyMode.BLOCKED_AUTO.equals(revt.getNewProxyMode())) {
      sb.append("Blocked (automatically by Nexus). Next attempt to check remote peer health will occur in ");

      sb.append(DurationFormatUtils.formatDurationWords(revt.getRepository().getCurrentRemoteStatusRetainTime(),
          true, true)
          + ".");
    }
    else if (ProxyMode.BLOCKED_MANUAL.equals(revt.getNewProxyMode())) {
      sb.append("Blocked (by a user).");
    }
    else {
      sb.append(revt.getNewProxyMode().toString()).append(".");
    }

    sb.append("\n\nThe previous state was \n\n");

    if (ProxyMode.ALLOW.equals(revt.getOldProxyMode())) {
      sb.append("Allow.");
    }
    else if (ProxyMode.BLOCKED_AUTO.equals(revt.getOldProxyMode())) {
      sb.append("Blocked (automatically by Nexus).");
    }
    else if (ProxyMode.BLOCKED_MANUAL.equals(revt.getOldProxyMode())) {
      sb.append("Blocked (by a user).");
    }
    else {
      sb.append(revt.getOldProxyMode().toString()).append(".");
    }

    if (revt.getCause() != null) {
      sb.append("\n\n\nLast detected transport error was:\n\n");

      final Writer result = new StringWriter();

      final PrintWriter printWriter = new PrintWriter(result);

      revt.getCause().printStackTrace(printWriter);

      sb.append(result.toString());
    }

    this.body = sb.toString();
  }

  public RepositoryEventProxyMode getRepositoryEventProxyMode() {
    return repositoryEventProxyMode;
  }

  public User getUser() {
    return user;
  }

  public String getMessageTitle() {
    return title;
  }

  public String getMessageBody() {
    return body;
  }

}
