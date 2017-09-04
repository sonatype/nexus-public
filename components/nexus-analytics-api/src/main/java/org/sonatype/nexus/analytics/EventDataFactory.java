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
package org.sonatype.nexus.analytics;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.node.NodeAccess;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper to build {@link EventData} instances.
 *
 * @since 3.next
 */
@Named
@Singleton
public class EventDataFactory
{
  private final RollingCounter counter = new RollingCounter(999_999_999_999_999L);

  private final NodeAccess nodeAccess;

  @Inject
  public EventDataFactory(final NodeAccess nodeAccess) {
    this.nodeAccess = checkNotNull(nodeAccess);
  }

  public EventData create(final String type) {
    EventData data = new EventData();
    data.setType(type);
    data.setTimestamp(System.currentTimeMillis());
    data.setSequence(counter.next());

    // capture the user and session ids if we can
    Subject subject = SecurityUtils.getSubject();
    if (subject != null) {
      Object principal = subject.getPrincipal();
      if (principal != null) {
        data.setUserId(principal.toString());
      }

      Session session = subject.getSession(false);
      if (session != null) {
        data.setSessionId(session.getId().toString());
      }
    }

    // capture node id
    if (nodeAccess.getId() != null) {
      data.getAttributes().put("nodeId", nodeAccess.getId());
    }

    return data;
  }
}
