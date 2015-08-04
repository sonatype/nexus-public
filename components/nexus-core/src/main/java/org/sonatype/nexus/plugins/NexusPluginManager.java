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
package org.sonatype.nexus.plugins;

import java.util.Collection;
import java.util.Map;

import org.sonatype.plugin.metadata.GAVCoordinate;

/**
 * Manages Nexus plugins, including both system and user types.
 *
 * @deprecated Avoid usage, will be removed and/or replaced in future versions.
 */
@Deprecated
public interface NexusPluginManager
{
  /**
   * Queries for plugin responses to recent actions/requests.
   *
   * @return Map of plugin responses to recent requests
   */
  Map<GAVCoordinate, PluginResponse> getPluginResponses();

  /**
   * Attempts to activate all installed plugins.
   *
   * @return Responses to the activation request
   */
  Collection<PluginManagerResponse> activateInstalledPlugins();
}
