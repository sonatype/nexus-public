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
package org.sonatype.nexus.proxy.maven.routing.internal;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.routing.Manager;
import org.sonatype.nexus.proxy.maven.routing.PrefixSource;
import org.sonatype.nexus.proxy.maven.routing.ProxyRequestFilter;
import org.sonatype.nexus.proxy.maven.routing.events.PrefixFilePublishedRepositoryEvent;
import org.sonatype.nexus.proxy.maven.routing.events.PrefixFileUnpublishedRepositoryEvent;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of the {@link ProxyRequestFilter}.
 *
 * @author cstamas
 */
@Named
@Singleton
public class ProxyRequestFilterImpl
    extends ComponentSupport
    implements ProxyRequestFilter
{
  private final ApplicationStatusSource applicationStatusSource;

  private final Manager manager;

  /**
   * Constructor.
   */
  @Inject
  public ProxyRequestFilterImpl(final EventBus eventBus, final ApplicationStatusSource applicationStatusSource,
                                final Manager manager)
  {
    checkNotNull(eventBus);
    this.applicationStatusSource = checkNotNull(applicationStatusSource);
    this.manager = checkNotNull(manager);
    eventBus.register(this);
  }

  @Override
  public boolean allowed(final MavenProxyRepository mavenProxyRepository,
                         final ResourceStoreRequest resourceStoreRequest)
  {
    final PathMatcher pathMatcher = getPathMatcherFor(mavenProxyRepository);
    if (pathMatcher != null) {
      final boolean allowed = pathMatcher.matches(resourceStoreRequest.getRequestPath());
      if (!allowed) {
        // flag the request as rejected
        resourceStoreRequest.getRequestContext().put(Manager.ROUTING_REQUEST_REJECTED_FLAG_KEY, Boolean.TRUE);
      }
      return allowed;
    }
    else {
      // no pathMatcher for a proxy, it does not publishes it
      return true;
    }
  }

  // ==

  private final ConcurrentHashMap<String, PathMatcher> pathMatchers = new ConcurrentHashMap<String, PathMatcher>();

  protected PathMatcher getPathMatcherFor(final MavenProxyRepository mavenProxyRepository) {
    return pathMatchers.get(mavenProxyRepository.getId());
  }

  protected boolean dropPathMatcherFor(final MavenProxyRepository mavenProxyRepository) {
    return pathMatchers.remove(mavenProxyRepository.getId()) != null;
  }

  protected void buildPathMatcherFor(final MavenProxyRepository mavenProxyRepository) {
    try {
      final PrefixSource prefixSource = manager.getPrefixSourceFor(mavenProxyRepository);
      if (prefixSource.supported()) {
        final PathMatcher pathMatcher = new PathMatcher(prefixSource.readEntries(), Integer.MAX_VALUE);
        pathMatchers.put(mavenProxyRepository.getId(), pathMatcher);
      }
      else {
        dropPathMatcherFor(mavenProxyRepository);
      }
    }
    catch (IOException e) {
      log.warn("Could not build PathMatcher for {}!", mavenProxyRepository, e);
      dropPathMatcherFor(mavenProxyRepository);
    }
  }

  // == Events

  protected boolean isRepositoryHandled(final Repository repository) {
    return applicationStatusSource.getSystemStatus().isNexusStarted() && repository != null
        && repository.getRepositoryKind().isFacetAvailable(MavenProxyRepository.class);
  }

  /**
   * Handler for {@link PrefixFilePublishedRepositoryEvent} event.
   */
  @Subscribe
  public void onPrefixFilePublishedRepositoryEvent(final PrefixFilePublishedRepositoryEvent evt) {
    final MavenProxyRepository mavenProxyRepository = evt.getRepository().adaptToFacet(MavenProxyRepository.class);
    if (isRepositoryHandled(mavenProxyRepository)) {
      buildPathMatcherFor(mavenProxyRepository);
    }
  }

  /**
   * Handler for {@link PrefixFileUnpublishedRepositoryEvent} event.
   */
  @Subscribe
  public void onPrefixFileUnpublishedRepositoryEvent(final PrefixFileUnpublishedRepositoryEvent evt) {
    final MavenProxyRepository mavenProxyRepository = evt.getRepository().adaptToFacet(MavenProxyRepository.class);
    if (isRepositoryHandled(mavenProxyRepository)) {
      dropPathMatcherFor(mavenProxyRepository);
    }
  }
}
