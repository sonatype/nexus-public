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
package org.sonatype.nexus.audit.protect;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Posted on the event bus when Protect-related configuration changes (firewall, health check, cleanup task).
 */
public class ProtectConfigChangedEvent
{
  private final String domain;

  private final String type;

  private final String context;

  private final String fromValue;

  private final String toValue;

  public ProtectConfigChangedEvent(
      final String domain,
      final String type,
      final String context,
      final String fromValue,
      final String toValue)
  {
    this.domain = checkNotNull(domain);
    this.type = checkNotNull(type);
    this.context = checkNotNull(context);
    this.fromValue = checkNotNull(fromValue);
    this.toValue = checkNotNull(toValue);
  }

  public String getDomain() {
    return domain;
  }

  public String getType() {
    return type;
  }

  public String getContext() {
    return context;
  }

  public String getFromValue() {
    return fromValue;
  }

  public String getToValue() {
    return toValue;
  }
}
