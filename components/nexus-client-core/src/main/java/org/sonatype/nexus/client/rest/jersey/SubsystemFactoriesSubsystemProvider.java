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
package org.sonatype.nexus.client.rest.jersey;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.client.core.Condition;
import org.sonatype.nexus.client.core.NexusClient;
import org.sonatype.nexus.client.core.spi.SubsystemFactory;
import org.sonatype.nexus.client.core.spi.SubsystemProvider;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link SubsystemProvider} that creates subsystems by using a given set of {@link SubsystemFactory}s.
 *
 * @since 2.7
 */
@Named
@Singleton
public class SubsystemFactoriesSubsystemProvider
    implements SubsystemProvider
{

  private Set<SubsystemFactory<?, JerseyNexusClient>> subsystemFactories;

  @Inject
  public SubsystemFactoriesSubsystemProvider(final Set<SubsystemFactory<?, JerseyNexusClient>> subsystemFactories) {
    this.subsystemFactories = checkNotNull(subsystemFactories, "subsystemFactories cannot be null");
  }

  @Override
  public Object get(final Class type, final Map<Object, Object> context) {
    checkNotNull(type, "type cannot be null");
    checkNotNull(context, "context cannot be null");

    final JerseyNexusClient jerseyNexusClient = (JerseyNexusClient) context.get(NexusClient.class);
    if (jerseyNexusClient != null) {
      for (final SubsystemFactory<?, JerseyNexusClient> subsystemFactory : subsystemFactories) {
        if (canCreate(subsystemFactory, type, jerseyNexusClient)) {
          final Object subsystem = subsystemFactory.create(jerseyNexusClient);
          if (subsystem != null) {
            return subsystem;
          }
        }
      }
    }

    return null;
  }

  /**
   * Adapts a {@link SubsystemFactory} to a {@link SubsystemProvider}.
   *
   * @param subsystemFactory to be adapted (cannot be null
   * @return adapted (never null)
   */
  public static SubsystemProvider adapt(final SubsystemFactory<?, JerseyNexusClient> subsystemFactory) {
    checkNotNull(subsystemFactory, "subsystemFactory cannot be null");

    return new SubsystemProvider()
    {
      @Override
      public Object get(final Class type, final Map<Object, Object> context) {
        checkNotNull(type, "type cannot be null");
        checkNotNull(context, "context cannot be null");

        final JerseyNexusClient jerseyNexusClient = (JerseyNexusClient) context.get(NexusClient.class);
        if (jerseyNexusClient != null && canCreate(subsystemFactory, type, jerseyNexusClient)) {
          return subsystemFactory.create(jerseyNexusClient);
        }
        return null;
      }
    };
  }

  private static boolean canCreate(final SubsystemFactory<?, JerseyNexusClient> subsystemFactory,
                                   final Class type,
                                   final JerseyNexusClient jerseyNexusClient)
  {
    if (!type.isAssignableFrom(subsystemFactory.getType())) {
      return false;
    }
    final Condition condition = subsystemFactory.availableWhen();
    return condition == null || condition.isSatisfiedBy(jerseyNexusClient.getStatus());
  }

}
