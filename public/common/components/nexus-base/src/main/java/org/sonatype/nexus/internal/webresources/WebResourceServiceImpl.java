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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.sonatype.nexus.webresources.WebResource;
import org.sonatype.nexus.webresources.WebResourceBundle;
import org.sonatype.nexus.webresources.WebResourceService;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Default {@link WebResourceService} implementation.
 *
 * @since 2.8
 */
@Component
public class WebResourceServiceImpl
    implements WebResourceService
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final Map<String, WebResource> resourcePaths;

  @Autowired
  public WebResourceServiceImpl(
      final List<WebResourceBundle> bundles,
      final List<WebResource> resources)
  {
    this.resourcePaths = Maps.newHashMap();

    bundles.stream()
        .map(WebResourceBundle::getResources)
        .flatMap(Collection::stream)
        .filter(Objects::nonNull)
        .forEach(this::addResource);

    resources.forEach(this::addResource);
  }

  void addResource(final WebResource resource) {
    String path = resource.getPath();
    log.trace("Adding resource: {} -> {}", path, resource);
    final WebResource old = resourcePaths.put(path, resource);
    if (old != null) {
      // complain if any resources overlap
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

    WebResource resource = resourcePaths.get(path);
    if (resource != null) {
      log.trace("Found bound resource: {}", resource);
    }

    return resource;
  }
}
