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
package org.sonatype.nexus.plugins.capabilities.support;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import javax.inject.Inject;

import org.sonatype.nexus.plugins.capabilities.CapabilityReference;
import org.sonatype.nexus.plugins.capabilities.CapabilityRegistry;
import org.sonatype.nexus.plugins.capabilities.CapabilityRegistryEvent;
import org.sonatype.nexus.plugins.capabilities.CapabilityType;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.base.Throwables;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.plugins.capabilities.support.CapabilityReferenceFilterBuilder.capabilities;

/**
 * Support for components which need to handle capability registration upon booting.
 *
 * @since capabilities 2.2
 */
public abstract class CapabilityBooterSupport
{

  private static final Logger log = LoggerFactory.getLogger(CapabilityBooterSupport.class);

  @Inject
  public void installEventBus(final EventBus eventBus) {
    checkNotNull(eventBus).register(new Object()
    {
      @Subscribe
      public void handle(final CapabilityRegistryEvent.AfterLoad event) {
        eventBus.unregister(this);
        final CapabilityRegistry registry = event.getEventSender();
        try {
          boot(registry);
        }
        catch (Exception e) {
          throw Throwables.propagate(e);
        }
      }
    });
  }

  protected abstract void boot(final CapabilityRegistry registry)
      throws Exception;

  protected void maybeAddCapability(final CapabilityRegistry capabilityRegistry,
                                    final CapabilityType type,
                                    final boolean enabled,
                                    final String notes,
                                    final Map<String, String> properties)
      throws Exception
  {
    CapabilityReference reference = findCapability(capabilityRegistry, type);
    if (reference == null) {
      log.debug("Automatically adding capability type: {}; enabled: {}", type, enabled);
      addCapability(capabilityRegistry, type, enabled, notes, properties);
    }
  }

  protected CapabilityReference findCapability(final CapabilityRegistry capabilityRegistry,
                                               final CapabilityType type)
  {
    return findCapability(capabilityRegistry, type, true);
  }

  protected CapabilityReference findCapability(final CapabilityRegistry capabilityRegistry,
                                               final CapabilityType type,
                                               final boolean includeNotExposed)
  {
    CapabilityReferenceFilterBuilder.CapabilityReferenceFilter filter = capabilities().withType(type);

    if (includeNotExposed) {
      filter.includeNotExposed();
    }

    Collection<? extends CapabilityReference> capabilities = capabilityRegistry.get(filter);
    if (capabilities != null && !capabilities.isEmpty()) {
      return capabilities.iterator().next();
    }
    return null;
  }

  protected void addCapability(final CapabilityRegistry capabilityRegistry,
                               final CapabilityType type,
                               final boolean enabled,
                               final String notes,
                               final Map<String, String> properties)
      throws Exception
  {
    capabilityRegistry.add(
        type, enabled, notes == null ? "Automatically added on " + new Date() : notes, properties
    );
  }
}