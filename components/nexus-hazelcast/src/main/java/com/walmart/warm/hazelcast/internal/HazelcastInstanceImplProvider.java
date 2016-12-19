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
import com.hazelcast.instance.HazelcastInstanceImpl;
import com.hazelcast.instance.HazelcastInstanceProxy;
import org.eclipse.sisu.Typed;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides access to {@link HazelcastInstanceImpl}.
 *
 * This is for special use-only.
 *
 * @since 1.8
 */
@Named
@Singleton
@Typed(HazelcastInstanceImpl.class)
@SuppressWarnings("CdiTypedAnnotationInspection")
public class HazelcastInstanceImplProvider
    implements Provider<HazelcastInstanceImpl>
{
  private final Provider<HazelcastInstance> hazelcastInstance;

  @Inject
  public HazelcastInstanceImplProvider(final Provider<HazelcastInstance> hazelcastInstance) {
    this.hazelcastInstance = checkNotNull(hazelcastInstance);
  }

  /**
   * Try to convert {@link HazelcastInstance} to {@link HazelcastInstanceImpl}.
   *
   * @throws RuntimeException Unable to convert
   */
  @Override
  public HazelcastInstanceImpl get() {
    HazelcastInstance instance = hazelcastInstance.get();
    if (instance instanceof HazelcastInstanceProxy) {
      return ((HazelcastInstanceProxy) instance).getOriginal();
    }
    else if (instance instanceof HazelcastInstanceImpl) {
      return (HazelcastInstanceImpl)instance;
    }

    throw new RuntimeException("Unable to convert HazelcastInstance to HazelcastInstanceImpl; found type: " + instance.getClass().getName());
  }
}
