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
package org.sonatype.nexus.repository.httpbridge.internal;

import java.util.Iterator;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.capability.CapabilityEvent;
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.app.ManagedLifecycle.Phase;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.repository.httpbridge.legacy.LegacyUrlCapabilityDescriptor;
import org.sonatype.nexus.repository.httpbridge.legacy.LegacyUrlEnabledHelper;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.eclipse.sisu.inject.BeanLocator;
import org.eclipse.sisu.inject.InjectorBindings;
import org.eclipse.sisu.inject.MutableBeanLocator;
import org.eclipse.sisu.wire.ParameterKeys;
import org.eclipse.sisu.wire.WireModule;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.SESSION_ENABLED;

/**
 * Manages the injection of {@link LegacyHttpBridgeModule} based on the capability being enabled or the system property
 *
 * @since 3.7
 */
@Named
@Singleton
@FeatureFlag(name = SESSION_ENABLED)
@ManagedLifecycle(phase = Phase.TASKS)
public class LegacyHttpBridgeService
    extends LifecycleSupport
    implements EventAware
{
  private final MutableBeanLocator locator;

  private final LegacyUrlEnabledHelper legacyUrlEnabledHelper;

  private InjectorBindings legacyBridgeInjector;

  @Inject
  public LegacyHttpBridgeService(
      final MutableBeanLocator locator,
      final LegacyUrlEnabledHelper legacyUrlEnabledHelper)
  {
    this.locator = checkNotNull(locator);
    this.legacyUrlEnabledHelper = checkNotNull(legacyUrlEnabledHelper);
  }

  @Override
  protected void doStart() {
    toggleLegacyHttpBridgeModule();
  }

  @AllowConcurrentEvents
  @Subscribe
  public void handle(final CapabilityEvent event) {
    if (event.getReference().context().descriptor().type().equals(LegacyUrlCapabilityDescriptor.TYPE)) {
      toggleLegacyHttpBridgeModule();
    }
  }

  private synchronized void toggleLegacyHttpBridgeModule() {
    if (legacyUrlEnabledHelper.isEnabled()) {
      addLegacyHttpBridgeModule();
    }
    else {
      removeLegacyHttpBridgeModule();
    }
  }

  protected AbstractModule getLegacyHttpBridgeModule() {
    return new LegacyHttpBridgeModule();
  }

  private void addLegacyHttpBridgeModule() {
    if (legacyBridgeInjector == null) {
      this.legacyBridgeInjector = new InjectorBindings(
          Guice.createInjector(new WireModule(getLegacyHttpBridgeModule(), new AbstractModule()
          {
            @Override
            protected void configure() {
              // support injection of application components by wiring via shared locator
              bind(BeanLocator.class).toInstance(locator);

              // support injection of application properties
              Optional.ofNullable(locator.locate(ParameterKeys.PROPERTIES))
                  .map(Iterable::iterator)
                  .map(Iterator::next)
                  .map(b -> b.getValue())
                  .ifPresent(m -> bind(ParameterKeys.PROPERTIES).toInstance(m));
            }
          })));
      locator.add(legacyBridgeInjector);
    }
  }

  private void removeLegacyHttpBridgeModule() {
    if (legacyBridgeInjector != null) {
      locator.remove(legacyBridgeInjector);
      this.legacyBridgeInjector = null;
    }
  }
}
