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
package org.sonatype.nexus.internal.capability;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.capability.Capability;
import org.sonatype.nexus.capability.CapabilityDescriptor;
import org.sonatype.nexus.capability.CapabilityDescriptorRegistry;
import org.sonatype.nexus.capability.CapabilityFactory;
import org.sonatype.nexus.capability.CapabilityFactoryRegistry;
import org.sonatype.nexus.capability.CapabilityType;

import com.google.common.collect.Maps;
import com.google.inject.ConfigurationException;
import com.google.inject.Key;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.inject.BeanLocator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.inject.name.Names.named;

/**
 * Default {@link CapabilityFactoryRegistry} implementation.
 *
 * @since capabilities 2.0
 */
@Named
@Singleton
class DefaultCapabilityFactoryRegistry
    extends ComponentSupport
    implements CapabilityFactoryRegistry
{

  private final Map<String, CapabilityFactory> factories;

  private final Map<String, CapabilityFactory> dynamicFactories;

  private final CapabilityDescriptorRegistry capabilityDescriptorRegistry;

  private final BeanLocator beanLocator;

  @Inject
  DefaultCapabilityFactoryRegistry(final Map<String, CapabilityFactory> factories,
                                   final CapabilityDescriptorRegistry capabilityDescriptorRegistry,
                                   final BeanLocator beanLocator)
  {
    this.beanLocator = checkNotNull(beanLocator);
    this.capabilityDescriptorRegistry = checkNotNull(capabilityDescriptorRegistry);
    this.factories = checkNotNull(factories);
    this.dynamicFactories = Maps.newConcurrentMap();
  }

  @Override
  public CapabilityFactoryRegistry register(final CapabilityType type, final CapabilityFactory factory) {
    checkNotNull(factory);
    checkArgument(!factories.containsKey(type), "Factory already registered for %s", type);
    checkArgument(!dynamicFactories.containsKey(type), "Factory already registered for %s", type);

    dynamicFactories.put(type.toString(), factory);
    log.debug("Added {} -> {}", type, factory);

    return this;
  }

  @Override
  public CapabilityFactoryRegistry unregister(final CapabilityType type) {
    if (type != null) {
      final CapabilityFactory factory = dynamicFactories.remove(type);
      log.debug("Removed {} -> {}", type, factory);
    }

    return this;
  }

  @Override
  public CapabilityFactory get(final CapabilityType type) {
    CapabilityFactory factory = factories.get(checkNotNull(type).toString());
    if (factory == null) {
      factory = dynamicFactories.get(checkNotNull(type).toString());
    }
    if (factory == null) {
      final CapabilityDescriptor descriptor = capabilityDescriptorRegistry.get(type);
      if (descriptor instanceof CapabilityFactory) {
        factory = (CapabilityFactory) descriptor;
      }
      if (factory == null) {
        try {
          final Iterable<? extends BeanEntry<?, Capability>> entries = beanLocator.locate(
              Key.get(Capability.class, named(type.toString()))
          );
          if (entries != null && entries.iterator().hasNext()) {
            factory = new CapabilityFactory()
            {
              @Override
              public Capability create() {
                return entries.iterator().next().getValue();
              }
            };
          }
        }
        catch (ConfigurationException ignore) {
          // ignore
        }
      }
    }
    return factory;
  }

}
