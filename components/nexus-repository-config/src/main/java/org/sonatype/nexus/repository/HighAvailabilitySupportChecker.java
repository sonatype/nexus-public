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
package org.sonatype.nexus.repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.sonatype.nexus.common.node.NodeAccess;

import static java.lang.String.format;
import static org.sonatype.nexus.common.property.SystemPropertiesHelper.getBoolean;

/**
 * Verifies availability of the repository dependent on clustering mode of NXRM and the clustering configuration
 * property of the repository.
 *
 * @since 3.17
 */
public abstract class HighAvailabilitySupportChecker
{
  private final boolean isNexusClustered;

  private final Map<String, Boolean> formatHAStates = new ConcurrentHashMap<>();

  public HighAvailabilitySupportChecker(final NodeAccess nodeAccess) {
    isNexusClustered = nodeAccess.isClustered();

    // Formats enabled by default
    getEnabledFormats().forEach(format -> formatHAStates.put(format, queryFormat(format, true)));
  }

  /**
   * Get formats enabled by default.
   *
   * @return a list of formats which are enabled by default in the cluster mode.
   */
  protected abstract List<String> getEnabledFormats();

  public boolean isSupported(final String formatName) {
    boolean formatEnabled = formatHAStates.computeIfAbsent(formatName, f -> queryFormat(formatName, false));
    return !isNexusClustered || formatEnabled;
  }

  private boolean queryFormat(final String formatName, final boolean defaultValue) {
    return getBoolean(format("nexus.%s.ha.supported", formatName), defaultValue);
  }
}
