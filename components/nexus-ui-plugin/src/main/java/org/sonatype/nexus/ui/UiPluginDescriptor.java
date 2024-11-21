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
package org.sonatype.nexus.ui;

import java.util.List;

/**
 * Rapture UI plugin descriptor.
 *
 * Using component priority to determine inclusion order. Ordering is important to properly load the UI.
 *
 * @since 3.20
 */
public interface UiPluginDescriptor
{
  String getName();

  /**
   * @return a list of script files that should be included on the page (this is used for non-extjs plugins)
   */
  List<String> getScripts(final boolean isDebug);

  /**
   * @return a list of stylesheets that should be included on the page (this is used for non-extjs plugins)
   */
  List<String> getStyles();
}
