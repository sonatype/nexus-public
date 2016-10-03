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
package org.sonatype.nexus.internal.selector;

import java.lang.ref.SoftReference;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.selector.JexlSelector;
import org.sonatype.nexus.selector.Selector;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorEvaluationException;
import org.sonatype.nexus.selector.SelectorManager;
import org.sonatype.nexus.selector.VariableSource;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * Default {@link SelectorManager} implementation.
 * 
 * @since 3.1
 */
@Named
@Singleton
@ManagedLifecycle(phase = SERVICES)
public class SelectorManagerImpl
    extends StateGuardLifecycleSupport
    implements SelectorManager, EventAware
{
  private static final SoftReference<List<SelectorConfiguration>> EMPTY_CACHE = new SoftReference<>(null);

  private final SelectorConfigurationStore store;

  private volatile SoftReference<List<SelectorConfiguration>> cachedBrowseResult = EMPTY_CACHE;

  @Inject
  public SelectorManagerImpl(final SelectorConfigurationStore store) {
    this.store = checkNotNull(store);
  }

  @Override
  @Guarded(by = STARTED)
  public List<SelectorConfiguration> browse() {
    List<SelectorConfiguration> result;

    // double-checked lock to minimize caching attempts
    if ((result = cachedBrowseResult.get()) == null) {
      synchronized (this) {
        if ((result = cachedBrowseResult.get()) == null) {
          result = ImmutableList.copyOf(store.browse());
          // maintain this result in memory-sensitive cache
          cachedBrowseResult = new SoftReference<>(result);
        }
      }
    }

    return result;
  }

  @Override
  @Guarded(by = STARTED)
  public SelectorConfiguration read(final EntityId entityId) {
    return store.read(entityId);
  }

  @Override
  @Guarded(by = STARTED)
  public void create(final SelectorConfiguration configuration) {
    store.create(configuration);
  }

  @Override
  @Guarded(by = STARTED)
  public void update(final SelectorConfiguration configuration) {
    store.update(configuration);
  }

  @Override
  @Guarded(by = STARTED)
  public void delete(final SelectorConfiguration configuration) {
    store.delete(configuration);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final SelectorConfigurationEvent event) {
    cachedBrowseResult = EMPTY_CACHE;
  }

  @Override
  @Guarded(by = STARTED)
  public boolean evaluate(final SelectorConfiguration selectorConfiguration, final VariableSource variableSource)
      throws SelectorEvaluationException
  {
    Selector selector = createSelector(selectorConfiguration);

    try {
      return selector.evaluate(variableSource);
    }
    catch (Exception e) {
      throw new SelectorEvaluationException("Selector '" + selectorConfiguration.getName() + "' evaluation in error",
          e);
    }
  }

  private Selector createSelector(final SelectorConfiguration config) throws SelectorEvaluationException {
    if ("jexl".equals(config.getType())) {
      return new JexlSelector((String) config.getAttributes().get("expression"));
    }

    throw new SelectorEvaluationException("Invalid selector type encountered: " + config.getType());
  }
}
