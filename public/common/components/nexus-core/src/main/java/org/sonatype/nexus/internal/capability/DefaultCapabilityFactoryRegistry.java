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

import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.sonatype.nexus.capability.Capability;
import org.sonatype.nexus.capability.CapabilityDescriptor;
import org.sonatype.nexus.capability.CapabilityDescriptorRegistry;
import org.sonatype.nexus.capability.CapabilityFactory;
import org.sonatype.nexus.capability.CapabilityFactoryRegistry;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.common.QualifierUtil;

import com.google.common.collect.Maps;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link CapabilityFactoryRegistry} implementation.
 *
 * @since capabilities 2.0
 */
@Primary
@Component
@Singleton
class DefaultCapabilityFactoryRegistry
    implements CapabilityFactoryRegistry, ApplicationContextAware
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final Map<String, CapabilityFactory> factories;

  private final Map<String, CapabilityFactory> dynamicFactories;

  private final CapabilityDescriptorRegistry capabilityDescriptorRegistry;

  private ApplicationContext context;

  private Map<String, String> capabilityByQualifier;

  @Inject
  DefaultCapabilityFactoryRegistry(
      final List<CapabilityFactory> factoriesList,
      final CapabilityDescriptorRegistry capabilityDescriptorRegistry)
  {
    this.capabilityDescriptorRegistry = checkNotNull(capabilityDescriptorRegistry);
    this.factories = QualifierUtil.buildQualifierBeanMap(checkNotNull(factoriesList));
    this.dynamicFactories = Maps.newConcurrentMap();
  }

  @Override
  public CapabilityFactoryRegistry register(final CapabilityType type, final CapabilityFactory factory) {
    checkNotNull(factory);
    checkArgument(!factories.containsKey(type.toString()), "Factory already registered for %s", type);
    checkArgument(!dynamicFactories.containsKey(type.toString()), "Factory already registered for %s", type);

    dynamicFactories.put(type.toString(), factory);
    log.debug("Added {} -> {}", type, factory);

    return this;
  }

  @Override
  public CapabilityFactoryRegistry unregister(final CapabilityType type) {
    if (type != null) {
      final CapabilityFactory factory = dynamicFactories.remove(type.toString());
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
        factory = () -> context.getBean(type.toString(), Capability.class);
      }
    }
    return factory;
  }

  @Override
  public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
    this.context = applicationContext;
  }
}
