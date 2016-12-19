/**
 * Copyright (c) 2016-current Walmart, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.walmart.warm.hazelcast;

import java.io.File;
import java.util.List;

import javax.annotation.Nullable;

import com.hazelcast.core.HazelcastInstance;

/**
 * Manager of {@link HazelcastInstance}s.
 *
 * @since 1.2.14
 */
public interface HazelcastManager
{
  /**
   * Returns a singleton/shared {@link HazelcastInstance}.
   *
   * Instance is created on-demand, first caller will actually initiate the instance creation.
   */
  HazelcastInstance sharedInstance();

  /**
   * Shut down the shared instance if it exists.
   */
  void shutdown();

  /**
   * Returns a new {@link HazelcastInstance}, created with passed configuration, classloader and
   * configuration participants (may be empty list). Caller of this method <b>must ensure</b> that instance will be
   * properly shut down once not needed or system is shut down (caller is responsible for instance lifecycle too).
   * Configuration file may be {@code null}, but other parameters may not.
   * <p>
   * Note: if you have {@code null} configFile, then consider using shared instance returned
   * by {@link #sharedInstance()} method, instead of creating new Hazelcast instance, as in that case, discovery will
   * be used from Nexus default Hazelcast configuration, and your instance will join the cluster too (if there is one).
   * Still, having multiple Hazelcast instances per JVM is completely valid.
   *
   * @param configFile                The {@link File} pointing at Hazelcast XML configuration file, may be {@code
   *                                  null}.
   * @param classLoader               The classloader to apply on Hazelcast.
   * @param configurationParticipants The non-{@code null} list of {@link HazelcastConfigParticipant}, or empty list.
   */
  HazelcastInstance newInstance(@Nullable final File configFile,
                                final ClassLoader classLoader,
                                final List<HazelcastConfigParticipant> configurationParticipants);
}
