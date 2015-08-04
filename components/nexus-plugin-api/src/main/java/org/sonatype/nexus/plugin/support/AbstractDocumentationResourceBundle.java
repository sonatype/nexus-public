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

import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.web.WebResource;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.annotations.VisibleForTesting;
import org.eclipse.sisu.space.ClassSpace;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @deprecated Replace usage with {@link DocumentationBundleSupport}.
 */
@Deprecated
public abstract class AbstractDocumentationResourceBundle
    extends ComponentSupport
    implements DocumentationBundle
{
  private MimeSupport mimeSupport;

  private ClassSpace space;

  protected AbstractDocumentationResourceBundle() {
    // empty
  }

  @VisibleForTesting
  protected AbstractDocumentationResourceBundle(final MimeSupport mimeSupport, final ClassSpace space) {
    this.mimeSupport = mimeSupport;
    this.space = space;
  }

  @Inject
  public void setMimeSupport(final MimeSupport mimeSupport, final ClassSpace space) {
    this.mimeSupport = checkNotNull(mimeSupport);
    this.space = checkNotNull(space);
  }

  public List<WebResource> getResources() {
    List<WebResource> resources = new LinkedList<WebResource>();

    if (space != null) {
      for (Enumeration<URL> e = space.findEntries("docs", "*", true); e.hasMoreElements();) {
        URL url = e.nextElement();

        String name = url.getPath();
        name = name.substring(1 + name.lastIndexOf("/docs/"), name.length());

        // to lessen clash possibilities, this way only within single plugin may be clashes, but one
        // plugin is usually developed by one team or one user so this is okay
        // system-wide clashes are much harder to resolve
        String path = "/" + getPluginId() + "/" + getPathPrefix() + "/" + name;

        resources.add(new UrlWebResource(url, path, mimeSupport.guessMimeTypeFromPath(name)));
      }
    }

    if (log.isTraceEnabled()) {
      log.trace("Discovered documentation for: {}", getPluginId());
      for (WebResource resource : resources) {
        log.trace("  {}", resource);
      }
    }

    return resources;
  }

  public String getPathPrefix() {
    return "default";
  }

  public abstract String getDescription();

  public abstract String getPluginId();
}
