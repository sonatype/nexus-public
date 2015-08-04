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
package org.sonatype.nexus.plugins.p2.repository.internal.capabilities;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.capability.support.CapabilitySupport;
import org.sonatype.nexus.plugins.capabilities.Condition;
import org.sonatype.nexus.plugins.capabilities.Tag;
import org.sonatype.nexus.plugins.capabilities.Taggable;
import org.sonatype.nexus.plugins.capabilities.support.condition.RepositoryConditions;
import org.sonatype.nexus.plugins.p2.repository.P2RepositoryAggregator;
import org.sonatype.nexus.plugins.p2.repository.P2RepositoryAggregatorConfiguration;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.plugins.capabilities.Tag.repositoryTag;
import static org.sonatype.nexus.plugins.capabilities.Tag.tags;
import static org.sonatype.nexus.plugins.p2.repository.internal.capabilities.P2RepositoryAggregatorCapabilityDescriptor.TYPE_ID;

@Named(TYPE_ID)
public class P2RepositoryAggregatorCapability
    extends CapabilitySupport<P2RepositoryAggregatorConfiguration>
    implements Taggable
{

  private final P2RepositoryAggregator service;

  private final RepositoryRegistry repositoryRegistry;

  @Inject
  public P2RepositoryAggregatorCapability(final P2RepositoryAggregator service,
                                          final RepositoryRegistry repositoryRegistry)
  {
    this.service = checkNotNull(service);
    this.repositoryRegistry = checkNotNull(repositoryRegistry);
  }

  @Override
  protected P2RepositoryAggregatorConfiguration createConfig(final Map<String, String> properties) throws Exception {
    return new P2RepositoryAggregatorConfiguration(properties);
  }

  @Override
  protected String renderDescription() {
    if (isConfigured()) {
      try {
        return repositoryRegistry.getRepository(getConfig().repositoryId()).getName();
      }
      catch (NoSuchRepositoryException e) {
        return getConfig().repositoryId();
      }
    }
    return null;
  }

  @Override
  protected void configure(final P2RepositoryAggregatorConfiguration config) throws Exception {
    service.addConfiguration(config);
  }

  @Override
  public void onUpdate() throws Exception {
    service.removeConfiguration(getConfig());
    super.onUpdate();
  }

  @Override
  protected void onRemove(final P2RepositoryAggregatorConfiguration config) throws Exception {
    service.removeConfiguration(config);
  }

  @Override
  protected void onActivate(final P2RepositoryAggregatorConfiguration config) throws Exception {
    service.enableAggregationFor(getConfig());
  }

  @Override
  protected void onPassivate(final P2RepositoryAggregatorConfiguration config) throws Exception {
    service.disableAggregationFor(getConfig());
  }

  @Override
  public Condition activationCondition() {
    return conditions().logical().and(
        conditions().repository().repositoryIsInService(new RepositoryConditions.RepositoryId()
        {
          @Override
          public String get() {
            return isConfigured() ? getConfig().repositoryId() : null;
          }
        }),
        conditions().capabilities().passivateCapabilityDuringUpdate()
    );
  }

  @Override
  public Condition validityCondition() {
    return conditions().repository().repositoryExists(new RepositoryConditions.RepositoryId()
    {
      @Override
      public String get() {
        return isConfigured() ? getConfig().repositoryId() : null;
      }
    });
  }

  @Override
  public Set<Tag> getTags() {
    return tags(repositoryTag(renderDescription()));
  }

}
