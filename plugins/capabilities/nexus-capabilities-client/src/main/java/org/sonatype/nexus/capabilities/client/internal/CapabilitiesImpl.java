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
package org.sonatype.nexus.capabilities.client.internal;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.sonatype.nexus.capabilities.client.Capabilities;
import org.sonatype.nexus.capabilities.client.Capability;
import org.sonatype.nexus.capabilities.client.Filter;
import org.sonatype.nexus.capabilities.client.exceptions.CapabilityFactoryNotAvailableException;
import org.sonatype.nexus.capabilities.client.exceptions.MultipleCapabilitiesFoundException;
import org.sonatype.nexus.capabilities.client.spi.CapabilityClient;
import org.sonatype.nexus.capabilities.client.spi.CapabilityFactory;
import org.sonatype.nexus.capabilities.model.CapabilityStatusXO;
import org.sonatype.nexus.client.core.exception.NexusClientNotFoundException;
import org.sonatype.nexus.client.rest.jersey.ContextAwareUniformInterfaceException;
import org.sonatype.sisu.siesta.client.ClientBuilder.Target.Factory;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Jersey based Capabilities Nexus Client Subsystem implementation.
 *
 * @since capabilities 2.1
 */
public class CapabilitiesImpl
    implements Capabilities
{

  private static final Logger LOG = LoggerFactory.getLogger(CapabilitiesImpl.class);

  private static final Filter ALL = null;

  public static final String NO_RESPONSE_BODY = null;

  private final CapabilityClient client;

  private final Set<CapabilityFactory> capabilityFactories;

  @Inject
  public CapabilitiesImpl(final Factory factory,
                          final CapabilityFactoriesSet capabilityFactoriesSet)
  {
    client = checkNotNull(factory, "factory").build(CapabilityClient.class);
    this.capabilityFactories = checkNotNull(capabilityFactoriesSet).get();
  }

  @Override
  public Capability create(final String type) {
    return findFactoryOf(type).create(client);
  }

  @Override
  public Capability get(final String id) {
    return convert(client.getStatus(checkNotNull(id)));
  }

  @Override
  public Collection<Capability> get() {
    LOG.debug("Retrieving all capabilities");
    return queryFor(ALL);
  }

  @Override
  public Collection<Capability> get(final Filter filter) {
    LOG.debug("Retrieving all capabilities using filter '{}'", checkNotNull(filter).toQueryMap());
    return queryFor(filter);
  }

  @Override
  public Capability getUnique(final Filter filter)
      throws MultipleCapabilitiesFoundException, NexusClientNotFoundException
  {
    final Collection<Capability> capabilities = get(filter);
    if (capabilities.size() == 0) {
      throw new NexusClientNotFoundException(
          String.format("No capability found matching filter '%s'", filter),
          NO_RESPONSE_BODY
      );
    }
    if (capabilities.size() > 1) {
      throw new MultipleCapabilitiesFoundException(filter, capabilities);
    }
    return capabilities.iterator().next();
  }

  @Override
  public <C extends Capability> C create(final Class<C> type)
      throws CapabilityFactoryNotAvailableException
  {
    return findFactoryOf(type).create(client);
  }

  @Override
  public <C extends Capability> C get(final Class<C> type, final String id) {
    checkNotNull(type);
    final Capability capability = get(id);
    if (!type.isAssignableFrom(capability.getClass())) {
      throw new ClassCastException(
          String.format(
              "Expected an '%s' but found that capability is an '%s'",
              type.getName(), capability.getClass().getName()
          )
      );
    }
    return type.cast(capability);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <C extends Capability> Collection<C> get(final Class<C> type, final Filter filter) {
    LOG.debug("Retrieving all capabilities of type {}", type.getName());
    final Collection<Capability> capabilities = queryFor(filter);
    for (final Capability capability : capabilities) {
      if (!type.isAssignableFrom(capability.getClass())) {
        throw new ClassCastException(
            String.format(
                "Expected an '%s' but found that capability is an '%s'",
                type.getName(), capability.getClass().getName()
            )
        );
      }
    }
    return (Collection<C>) capabilities;
  }

  @Override
  public <C extends Capability> C getUnique(final Class<C> type, final Filter filter)
      throws MultipleCapabilitiesFoundException, NexusClientNotFoundException, ClassCastException
  {
    checkNotNull(type);
    final Capability capability = getUnique(filter);
    if (!type.isAssignableFrom(capability.getClass())) {
      throw new ClassCastException(
          String.format(
              "Expected an '%s' but found that capability is an '%s'",
              type.getName(), capability.getClass().getName()
          )
      );
    }
    return type.cast(capability);
  }

  @SuppressWarnings("unchecked")
  private <C extends Capability> CapabilityFactory<C> findFactoryOf(final Class<C> type) {
    for (final CapabilityFactory factory : capabilityFactories) {
      if (factory.canCreate(type)) {
        LOG.debug(
            "Using factory {} for capability type {}",
            factory.getClass().getName(), type.getName()
        );
        return (CapabilityFactory<C>) factory;
      }
    }
    throw new CapabilityFactoryNotAvailableException((Class<Capability>) type);
  }

  private CapabilityFactory findFactoryOf(final String type) {
    checkNotNull(type);
    for (final CapabilityFactory factory : capabilityFactories) {
      if (factory.canCreate(type)) {
        LOG.debug(
            "Using factory {} for type '{}'",
            factory.getClass().getName(), type
        );
        return factory;
      }
    }
    LOG.debug(
        "Using factory {} for type '{}'",
        GenericCapabilityFactory.class.getName(), type
    );
    return new GenericCapabilityFactory(type);
  }

  private Collection<Capability> queryFor(final Filter filter) {
    final List<CapabilityStatusXO> resource;
    if (filter != null) {
      resource = client.search(filter.toQueryMap());
    }
    else {
      resource = client.get();
    }

    return Collections2.transform(resource, new Function<CapabilityStatusXO, Capability>()
    {
      @Override
      public Capability apply(@Nullable final CapabilityStatusXO input) {
        return convert(input);
      }
    });
  }

  private Capability convert(final CapabilityStatusXO resource) {
    if (resource == null) {
      return null;
    }
    return findFactoryOf(resource.getCapability().getTypeId()).create(client, resource);
  }

}
