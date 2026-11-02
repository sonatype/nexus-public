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
package org.sonatype.nexus.repository.httpclient.internal;

import java.util.Collection;
import java.util.Set;

import org.sonatype.nexus.repository.httpclient.AutoBlockConfiguration;

/**
 * An {@link AutoBlockConfiguration} backed by an explicit per-repository list of status codes. Used to override
 * the format-scoped default when a repository declares its own set of auto-block status codes.
 */
public class ConfigurableAutoBlockConfiguration
    implements AutoBlockConfiguration
{
  private final Set<Integer> statusCodes;

  public ConfigurableAutoBlockConfiguration(final Collection<Integer> statusCodes) {
    if (statusCodes == null) {
      throw new IllegalArgumentException("statusCodes must not be null");
    }
    for (Integer statusCode : statusCodes) {
      if (statusCode == null) {
        throw new IllegalArgumentException("autoBlockStatusCodes must not contain null status codes");
      }
    }
    this.statusCodes = Set.copyOf(statusCodes);
  }

  @Override
  public boolean shouldBlock(final int statusCode) {
    return statusCodes.contains(statusCode);
  }
}
