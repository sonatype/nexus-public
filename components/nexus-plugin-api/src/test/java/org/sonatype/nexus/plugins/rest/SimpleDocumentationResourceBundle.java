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
package org.sonatype.nexus.plugins.rest;

import java.net.URL;
import java.net.URLClassLoader;

import org.sonatype.nexus.mime.DefaultMimeSupport;
import org.sonatype.nexus.plugin.support.AbstractDocumentationResourceBundle;

import org.eclipse.sisu.space.URLClassSpace;

public class SimpleDocumentationResourceBundle
    extends AbstractDocumentationResourceBundle
{
  public SimpleDocumentationResourceBundle() {
    super(new DefaultMimeSupport(), new URLClassSpace(new URLClassLoader(
        new URL[] { SimpleDocumentationResourceBundle.class.getResource("/docs.zip") })));
  }

  @Override
  public String getPluginId() {
    return "test";
  }

  @Override
  public String getDescription() {
    return "Simple Test";
  }

  @Override
  public String getPathPrefix() {
    return "test";
  }
}