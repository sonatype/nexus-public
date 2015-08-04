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
package org.sonatype.nexus.proxy.walker;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.scheduling.TaskInterruptedException;
import org.sonatype.scheduling.TaskUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;

public class DefaultWalkerContext
    implements WalkerContext
{
  private final Repository resourceStore;

  private final WalkerFilter walkerFilter;

  private final ResourceStoreRequest request;

  private final WalkerThrottleController throttleController;

  private final TraversalType traversalType;

  private final boolean processCollections;

  private final Map<String, Object> context;

  private final List<WalkerProcessor> processors;

  private Throwable stopCause;

  private Comparator<StorageItem> itemComparator;

  private volatile boolean running;

  public DefaultWalkerContext(final Repository store, final ResourceStoreRequest request) {
    this(store, request, null);
  }

  public DefaultWalkerContext(final Repository store, final ResourceStoreRequest request, final WalkerFilter filter) {
    this(store, request, filter, TraversalType.DEPTH_FIRST, false);
  }

  /**
   * @deprecated Use another ctor.
   */
  @Deprecated
  public DefaultWalkerContext(final Repository store, final ResourceStoreRequest request, final WalkerFilter filter,
                              boolean localOnly)
  {
    this(store, request, filter);
  }

  public DefaultWalkerContext(final Repository store,
                              final ResourceStoreRequest request,
                              @Nullable final WalkerFilter filter,
                              final TraversalType traversalType,
                              final boolean processCollections)
    {
    this.resourceStore = checkNotNull(store);
    this.request =checkNotNull(request);
    this.walkerFilter = filter;
    this.running = true;
    if (request.getRequestContext().containsKey(WalkerThrottleController.CONTEXT_KEY, false)) {
      this.throttleController =
          (WalkerThrottleController) request.getRequestContext().get(WalkerThrottleController.CONTEXT_KEY, false);
    }
    else {
      this.throttleController = WalkerThrottleController.NO_THROTTLING;
    }
    this.traversalType = checkNotNull(traversalType);
    this.processCollections = processCollections;
    this.context = Maps.newHashMap();
    this.processors = Lists.newArrayList();
  }

  @Override
  public boolean isLocalOnly() {
    return request.isRequestLocalOnly();
  }

  @Override
  public Map<String, Object> getContext() {
    return context;
  }

  @Override
  public List<WalkerProcessor> getProcessors() {
    return processors;
  }

  @Override
  public WalkerFilter getFilter() {
    return walkerFilter;
  }

  @Override
  public Repository getRepository() {
    return resourceStore;
  }

  @Override
  public TraversalType getTraversalType() {
    return traversalType;
  }

  @Override
  public boolean isProcessCollections() {
    return processCollections;
  }

  @Override
  public ResourceStoreRequest getResourceStoreRequest() {
    return request;
  }

  @Override
  public boolean isStopped() {
    try {
      TaskUtil.checkInterruption();
    }
    catch (TaskInterruptedException e) {
      if (stopCause == null) {
        stopCause = e;
      }
      running = false;
    }
    return !running;
  }

  @Override
  public Throwable getStopCause() {
    return stopCause;
  }

  @Override
  public void stop(Throwable cause) {
    running = false;
    stopCause = cause;
  }

  @Override
  public WalkerThrottleController getThrottleController() {
    return this.throttleController;
  }

  @Override
  public Comparator<StorageItem> getItemComparator() {
    return itemComparator;
  }

  public void setItemComparator(final Comparator<StorageItem> itemComparator) {
    this.itemComparator = itemComparator;
  }

}
