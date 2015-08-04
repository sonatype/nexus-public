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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.routing.discovery.DiscoveryResult;
import org.sonatype.nexus.proxy.maven.routing.discovery.Prioritized.PriorityOrderingComparator;
import org.sonatype.nexus.proxy.maven.routing.discovery.RemoteContentDiscoverer;
import org.sonatype.nexus.proxy.maven.routing.discovery.RemoteStrategy;
import org.sonatype.nexus.proxy.maven.routing.discovery.StrategyFailedException;
import org.sonatype.nexus.proxy.maven.routing.discovery.StrategyResult;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link RemoteContentDiscoverer} implementation.
 *
 * @author cstamas
 * @since 2.4
 */
@Named
@Singleton
public class RemoteContentDiscovererImpl
    extends ComponentSupport
    implements RemoteContentDiscoverer
{
  private final List<RemoteStrategy> remoteStrategies;

  /**
   * Constructor.
   */
  @Inject
  public RemoteContentDiscovererImpl(final List<RemoteStrategy> remoteStrategies) {
    this.remoteStrategies = checkNotNull(remoteStrategies);
  }

  @Override
  public DiscoveryResult<MavenProxyRepository> discoverRemoteContent(final MavenProxyRepository mavenProxyRepository) {
    final ArrayList<RemoteStrategy> appliedStrategies = new ArrayList<RemoteStrategy>(remoteStrategies);
    Collections.sort(appliedStrategies, new PriorityOrderingComparator<RemoteStrategy>());
    return discoverRemoteContent(mavenProxyRepository, appliedStrategies);
  }

  @Override
  public DiscoveryResult<MavenProxyRepository> discoverRemoteContent(final MavenProxyRepository mavenProxyRepository,
                                                                     final List<RemoteStrategy> remoteStrategies)
  {
    final DiscoveryResult<MavenProxyRepository> discoveryResult =
        new DiscoveryResult<MavenProxyRepository>(mavenProxyRepository);
    for (RemoteStrategy strategy : remoteStrategies) {
      log.debug("Discovery of {} with strategy {} attempted", mavenProxyRepository, strategy.getId());
      try {
        final StrategyResult strategyResult = strategy.discover(mavenProxyRepository);
        if (strategyResult.isRoutingEnabled()) {
          discoveryResult.recordSuccess(strategy.getId(), strategyResult.getMessage(),
              strategyResult.getPrefixSource());
        }
        else {
          // the strategy explicitly requested to disable automatic routing for the repository
          discoveryResult.recordFailure(strategy.getId(), strategyResult.getMessage());
          break;
        }
      }
      catch (StrategyFailedException e) {
        discoveryResult.recordFailure(strategy.getId(), e.getMessage());
      }
      catch (InvalidInputException e) {
        final String message =
            "Remote strategy " + strategy.getId() + " on " + mavenProxyRepository + " detected invalid input, results discarded: "
                + e.getMessage();
        log.info(message);
        discoveryResult.recordFailure(strategy.getId(), message);
        break;
      }
      catch (Exception e) {
        if (log.isDebugEnabled()) {
          log.warn("Remote strategy {} error on {}", strategy.getId(), mavenProxyRepository, e);
        }
        else {
          log.warn("Remote strategy {} error on {}: {}", strategy.getId(), mavenProxyRepository, e.toString());
        }
        discoveryResult.recordError(strategy.getId(), e);
        break;
      }

      if (discoveryResult.isSuccessful()) {
        log.debug("Discovery of {} with strategy {} successful", mavenProxyRepository,
            strategy.getId());
        break;
      }
      else {
        log.debug("Discovery of {} with strategy {} unsuccessful", mavenProxyRepository,
            strategy.getId());
      }
    }
    return discoveryResult;
  }
}
