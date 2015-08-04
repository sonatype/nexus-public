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

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.client.core.Condition;
import org.sonatype.nexus.client.core.spi.SubsystemFactory;
import org.sonatype.nexus.client.core.spi.SubsystemProvider;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @since 2.1
 */
@Named
@Singleton
public class JerseyNexusClientFactory
    extends NexusClientFactoryImpl
{

  public JerseyNexusClientFactory(final SubsystemFactory<?, JerseyNexusClient>... subsystemFactories) {
    super(
        Lists.<SubsystemProvider>newArrayList(
            new SubsystemFactoriesSubsystemProvider(Sets.newHashSet(subsystemFactories))
        )
    );
  }

  public JerseyNexusClientFactory(final Condition connectionCondition,
                                  final SubsystemFactory<?, JerseyNexusClient>... subsystemFactories)
  {
    super(
        connectionCondition,
        Lists.<SubsystemProvider>newArrayList(
            new SubsystemFactoriesSubsystemProvider(Sets.newHashSet(subsystemFactories))
        )
    );
  }

  public JerseyNexusClientFactory(final Set<SubsystemFactory<?, JerseyNexusClient>> subsystemFactories) {
    super(
        Lists.<SubsystemProvider>newArrayList(
            new SubsystemFactoriesSubsystemProvider(subsystemFactories)
        )
    );
  }

  /**
   * @since 2.7
   */
  @Inject
  public JerseyNexusClientFactory(final List<SubsystemProvider> subsystemProviders) {
    super(subsystemProviders);
  }

}
