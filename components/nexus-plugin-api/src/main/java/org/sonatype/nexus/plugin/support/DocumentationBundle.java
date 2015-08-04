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
package org.sonatype.nexus.plugin.support;

import org.sonatype.nexus.web.WebResourceBundle;

/**
 * A special resource bundle that holds static (preferably static HTML) documentation.
 */
public interface DocumentationBundle
    extends WebResourceBundle
{
  /**
   * Returns the plugin ID (artifactId?) of the plugin containing this resource.
   */
  String getPluginId();

  /**
   * Returns the "url snippet". It makes possible to do a deeper "partition" within plugin documentation URIs.
   */
  String getPathPrefix();

  /**
   * Returns human description of the documentation bundle.
   */
  String getDescription();
}
