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
package org.sonatype.nexus.webresources.internal;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.ServletContext;

import org.sonatype.nexus.internal.DevModeResources;
import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.plugin.support.FileWebResource;
import org.sonatype.nexus.plugin.support.UrlWebResource;
import org.sonatype.nexus.web.WebResource;
import org.sonatype.nexus.web.WebResourceBundle;
import org.sonatype.nexus.webresources.WebResourceService;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link WebResourceService} implementation.
 *
 * @since 2.8
 */
@Singleton
@Named
public class WebResourceServiceImpl
  extends ComponentSupport
  implements WebResourceService
{
  private final List<WebResourceBundle> bundles;

  private final List<WebResource> resources;

  private final Provider<ServletContext> servletContextProvider;

  private final MimeSupport mimeSupport;

  private final Map<String, WebResource> resourcePaths;

  @Inject
  public WebResourceServiceImpl(final List<WebResourceBundle> bundles,
                                final List<WebResource> resources,
                                final @Named("nexus") Provider<ServletContext> servletContextProvider,
                                final MimeSupport mimeSupport)
  {
    this.bundles = checkNotNull(bundles);
    this.resources = checkNotNull(resources);
    this.servletContextProvider = checkNotNull(servletContextProvider);
    this.mimeSupport = checkNotNull(mimeSupport);
    this.resourcePaths = Maps.newHashMap();

    discoverResources();
  }

  private void discoverResources() {
    // register resources in bundles
    for (WebResourceBundle bundle : bundles) {
      List<WebResource> resources = bundle.getResources();
      if (resources != null) {
        for (WebResource resource : resources) {
          addResource(resource);
        }
      }
    }

    // register standalone resources
    for (WebResource resource : resources) {
      addResource(resource);
    }

    log.info("Discovered {} resources", resourcePaths.size());
    if (log.isDebugEnabled()) {
      List<String> paths = Lists.newArrayList(resourcePaths.keySet());
      Collections.sort(paths);
      for (String path : paths) {
        log.debug("  {}", path);
      }
    }

    // make it clear we have DEV mode enabled
    if (DevModeResources.hasResourceLocations()) {
      log.warn("DEV mode resources is ENABLED");
    }
  }

  private void addResource(final WebResource resource) {
    String path = resource.getPath();
    log.trace("Adding resource: {} -> {}", path, resource);
    final WebResource old = resourcePaths.put(path, resource);
    if (old != null) {
      // FIXME: for now this causes a bit of noise on startup for overlapping icons, for now reduce to DEBUG
      // FIXME: ... we need to sort out a general strategy short/long term for how to handle this issue
      log.debug("Overlapping resources on path {}: old={}, new={}", path, old, resource);
    }
  }

  @Override
  public Collection<String> getPaths() {
    return Collections.unmodifiableCollection(resourcePaths.keySet());
  }

  @Override
  public Collection<WebResource> getResources() {
    return Collections.unmodifiableCollection(resourcePaths.values());
  }

  @Override
  public WebResource getResource(final String path) {
    log.trace("Looking up resource: {}", path);

    WebResource resource = null;

    // 1) first "dev" resources if enabled (to override everything else)
    File file = DevModeResources.getFileIfOnFileSystem(path);
    if (file != null) {
      resource = new FileWebResource(file, path, mimeSupport.guessMimeTypeFromPath(file.getName()), false);
      log.trace("Found dev-mode resource: {}", resource);
      return resource;
    }

    // 2) second, look at "ordinary" resources, but only if devResource did not hit anything
    resource = resourcePaths.get(path);
    if (resource != null) {
      log.trace("Found bound resource: {}", resource);
      return resource;
    }

    // 3) weed out the traversal requests, as the next check will potentially load files from anywhere
    if (path.contains("..")) {
      log.debug("Ignoring request that contains `..`: {}", path);
      return null;
    }

    // 4) finally, look into WAR embedded resources
    URL url;
    try {
      url = servletContextProvider.get().getResource(path);
      if (url != null) {
        resource = new UrlWebResource(url, path, mimeSupport.guessMimeTypeFromPath(path));
        log.trace("Found servlet-context resource: {}", resource);
        return resource;
      }
    }
    catch (MalformedURLException e) {
      throw Throwables.propagate(e);
    }

    return null;
  }
}
