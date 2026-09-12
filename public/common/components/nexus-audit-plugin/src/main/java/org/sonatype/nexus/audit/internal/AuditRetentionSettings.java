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
package org.sonatype.nexus.audit.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Holds the currently configured audit event retention window (in days).
 * Updated by {@link AuditCapability} lifecycle callbacks and read by the
 * scheduled cleanup task at execution time.
 * <p>
 * The upper bound is injected via {@code nexus.audit.retention.max.days} (default 3650)
 * so deployment-specific caps (e.g. 1095 on repository-cloud-aws) can be enforced without
 * plugin code changes.
 */
@Component
public class AuditRetentionSettings
{
  public static final int DEFAULT_RETENTION_DAYS = 90;

  private final int maxRetentionDays;

  private volatile int retentionDays = DEFAULT_RETENTION_DAYS;

  @Autowired
  public AuditRetentionSettings(
      @Value("${nexus.audit.retention.max.days:3650}") final int maxRetentionDays)
  {
    if (maxRetentionDays < 1) {
      throw new IllegalArgumentException(
          "nexus.audit.retention.max.days must be >= 1, got " + maxRetentionDays);
    }
    this.maxRetentionDays = maxRetentionDays;
  }

  public int getMaxRetentionDays() {
    return maxRetentionDays;
  }

  public int getRetentionDays() {
    return retentionDays;
  }

  public void setRetentionDays(final int retentionDays) {
    if (retentionDays < 1 || retentionDays > maxRetentionDays) {
      throw new IllegalArgumentException(
          "retentionDays must be between 1 and " + maxRetentionDays + ", got " + retentionDays);
    }
    this.retentionDays = retentionDays;
  }
}
