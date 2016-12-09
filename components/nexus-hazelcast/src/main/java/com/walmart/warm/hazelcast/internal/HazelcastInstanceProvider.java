/**
 * Copyright (c) 2016-current Walmart, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.walmart.warm.hazelcast.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.hazelcast.core.HazelcastInstance;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provider of shared {@link HazelcastInstance}.
 *
 * @since 1.2.14
 */
@Named
@Singleton
public class HazelcastInstanceProvider
    implements Provider<HazelcastInstance>
{
  private final HazelcastManagerImpl hazelcastManager;

  @Inject
  public HazelcastInstanceProvider(final HazelcastManagerImpl hazelcastManager) {
    this.hazelcastManager = checkNotNull(hazelcastManager);
  }

  @Override
  public HazelcastInstance get() {
    return hazelcastManager.sharedInstance();
  }
}
