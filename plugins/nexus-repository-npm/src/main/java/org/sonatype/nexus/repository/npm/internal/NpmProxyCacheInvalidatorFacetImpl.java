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
package org.sonatype.nexus.repository.npm.internal;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Named;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryUpdatedEvent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * Handles invalidating NPM proxy cache when the URL for the repository changes.
 *
 * @since 3.21
 */
@Named
@Facet.Exposed
public class NpmProxyCacheInvalidatorFacetImpl
    extends FacetSupport
{
  @Subscribe
  @AllowConcurrentEvents
  protected void on(final RepositoryUpdatedEvent event) {
    final Repository repository = event.getRepository();

    repository.optionalFacet(NpmProxyFacet.class).ifPresent(npm -> {
      if (!Objects.equals(getRemoteUrl(repository.getConfiguration()), getRemoteUrl(event.getOldConfiguration()))) {
        npm.invalidateProxyCaches();
      }
    });
  }

  private static Object getRemoteUrl(final Configuration configuration) {
    return Optional.ofNullable(configuration.getAttributes().get("proxy")).map(proxy -> {
      if (proxy instanceof Map) {
        return proxy.get("remoteUrl");
      }
      return null;
    }).orElse(null);
  }
}
