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
package org.sonatype.nexus.extender.modules;

import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.webresources.UrlWebResource;
import org.sonatype.nexus.webresources.WebResource;
import org.sonatype.nexus.webresources.WebResourceBundle;

import org.eclipse.sisu.space.ClassSpace;

/**
 * {@link WebResourceBundle} that exposes any bundle resources found under /static.
 * 
 * @since 3.0
 */
@Singleton
public class StaticWebResourceBundle
    implements WebResourceBundle
{
  // ----------------------------------------------------------------------
  // Implementation fields
  // ----------------------------------------------------------------------

  private final List<WebResource> staticResources = new ArrayList<WebResource>();

  @Inject
  public StaticWebResourceBundle(final ClassSpace space, final MimeSupport mimeSupport) {
    for (Enumeration<URL> e = space.findEntries("static", "*", true); e.hasMoreElements();) {
      final URL url = e.nextElement();
      final String path = getPublishedPath(url);
      if (path != null && !path.endsWith("/")) {
        staticResources.add(new UrlWebResource(url, path, mimeSupport.guessMimeTypeFromPath(path)));
      }
    }
  }

  // ----------------------------------------------------------------------
  // Public methods
  // ----------------------------------------------------------------------

  public List<WebResource> getResources() {
    return staticResources;
  }

  // ----------------------------------------------------------------------
  // Implementation methods
  // ----------------------------------------------------------------------

  private static String getPublishedPath(final URL resourceURL) {
    final String path = resourceURL.toExternalForm();
    int index = path.indexOf("jar!/");
    if (index > 0) {
      return path.substring(index + 4);
    }
    index = path.indexOf("/static/");
    if (index > 0) {
      return path.substring(index);
    }
    return null;
  }
}
