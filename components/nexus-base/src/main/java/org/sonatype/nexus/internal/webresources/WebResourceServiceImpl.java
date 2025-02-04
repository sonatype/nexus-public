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
package org.sonatype.nexus.internal.webresources;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.webresources.FileWebResource;
import org.sonatype.nexus.webresources.WebResource;
import org.sonatype.nexus.webresources.WebResourceBundle;
import org.sonatype.nexus.webresources.WebResourceService;

import com.google.common.collect.Maps;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.Mediator;

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
  private final DevModeResources devModeResources;

  private final MimeSupport mimeSupport;

  private final Map<String, WebResource> resourcePaths;

  @Inject
  public WebResourceServiceImpl(
      final DevModeResources devModeResources,
      final MimeSupport mimeSupport)
  {
    this.devModeResources = checkNotNull(devModeResources);
    this.mimeSupport = checkNotNull(mimeSupport);
    this.resourcePaths = Maps.newHashMap();

    // make it clear we have DEV mode enabled
    List<File> locations = devModeResources.getResourceLocations();
    if (locations != null) {
      log.warn("DEV mode resources is ENABLED");
      // spit out the locations where we will look for resources
      for (File file : locations) {
        log.info("  {}", file);
      }
    }
  }

  private void addResource(final WebResource resource) {
    String path = resource.getPath();
    log.trace("Adding resource: {} -> {}", path, resource);
    final WebResource old = resourcePaths.put(path, resource);
    if (old != null) {
      // complain if any resources overlap
      log.warn("Overlapping resources on path {}: old={}, new={}", path, old, resource);
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
    File file = devModeResources.getFileIfOnFileSystem(path);
    if (file != null) {
      resource = new FileWebResource(file, path, mimeSupport.guessMimeTypeFromPath(file.getName()), false);
      log.trace("Found dev-mode resource: {}", resource);
    }

    // 2) second, look at "ordinary" resources, but only if devResource did not hit anything
    if (resource == null) {
      resource = resourcePaths.get(path);
      if (resource != null) {
        log.trace("Found bound resource: {}", resource);
      }
    }

    return resource;
  }

  @Named
  static class ResourceBundleMediator
      implements Mediator<Named, WebResourceBundle, WebResourceServiceImpl>
  {
    @Override
    public void add(BeanEntry<Named, WebResourceBundle> entry, WebResourceServiceImpl watcher) throws Exception {
      List<WebResource> resources = entry.getValue().getResources();
      if (resources != null) {
        for (WebResource resource : resources) {
          watcher.addResource(resource);
        }
      }
    }

    @Override
    public void remove(BeanEntry<Named, WebResourceBundle> entry, WebResourceServiceImpl watcher) throws Exception {
      // no-op
    }
  }

  @Named
  static class ResourceMediator
      implements Mediator<Named, WebResource, WebResourceServiceImpl>
  {
    @Override
    public void add(BeanEntry<Named, WebResource> entry, WebResourceServiceImpl watcher) throws Exception {
      watcher.addResource(entry.getValue());
    }

    @Override
    public void remove(BeanEntry<Named, WebResource> entry, WebResourceServiceImpl watcher) throws Exception {
      // no-op
    }
  }
}
