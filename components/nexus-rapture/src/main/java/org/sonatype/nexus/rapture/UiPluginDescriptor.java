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
package org.sonatype.nexus.rapture;

import javax.annotation.Nullable;

/**
 * Rapture UI plugin descriptor.
 *
 * Using component priority to determine inclusion order.  Ordering is important to properly load the UI.
 *
 * Plugins are required to define these resources for "debug" mode:
 * <ul>
 *   <li>/static/rapture/resources/<em>plugin-id</em>-debug.css</li>
 * </ul>
 *
 * ... and for "prod" mode:
 * <ul>
 *   <li>/static/rapture/<em>plugin-id</em>-prod.js</li>
 *   <li>/static/rapture/resources/<em>plugin-id</em>-prod.css</li>
 * </ul>
 *
 * @since 3.0
 */
public interface UiPluginDescriptor
{
  /**
   * The plugin identifier.  This is normally the POM artifactId.
   * This is used to generate references to Javascript and CSS sources.
   */
  String getPluginId();

  boolean hasStyle();

  boolean hasScript();

  /**
   * Extjs application plugin namespace.
   */
  @Nullable
  String getNamespace();

  /**
   * The Extjs class name of the {@code PluginConfig} for the plugin.
   */
  @Nullable
  String getConfigClassName();
}
