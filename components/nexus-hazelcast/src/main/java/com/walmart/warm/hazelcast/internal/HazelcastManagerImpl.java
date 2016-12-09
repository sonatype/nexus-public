/**
 * Copyright (c) 2016-current Walmart, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.walmart.warm.hazelcast.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.base.Throwables;
import com.hazelcast.config.Config;
import com.hazelcast.config.ConfigXmlGenerator;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.walmart.warm.hazelcast.HazelcastConfigParticipant;
import com.walmart.warm.hazelcast.HazelcastManager;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manager of {@link HazelcastInstance}s.
 *
 * @since 1.2.14
 */
@Named
@Singleton
public class HazelcastManagerImpl
    extends ComponentSupport
    implements HazelcastManager
{
  @Nullable
  private final File configFile;

  private final List<HazelcastConfigParticipant> configurationParticipants;

  private final ClassLoader classLoader;

  private HazelcastInstance sharedInstance;

  @Inject
  public HazelcastManagerImpl(@Nullable @Named("hazelcast.config") final File configFile,
                              final List<HazelcastConfigParticipant> configurationParticipants,
                              @Nullable @Named("nexus-uber") final ClassLoader classLoader)
  {
    this.configFile = configFile;
    this.configurationParticipants = checkNotNull(configurationParticipants);
    // TODO: hack to make it out of nexus-core scope (mainly in tests)
    this.classLoader = classLoader == null ? HazelcastManager.class.getClassLoader() : classLoader;

    log.info("Configuration file: {}", configFile);
  }

  /**
   * Returns a singleton/shared {@link HazelcastInstance}.
   *
   * Instance is created on-demand, first caller will actually initiate the instance creation.
   */
  @Override
  public synchronized HazelcastInstance sharedInstance() {
    if (sharedInstance == null) {
      sharedInstance = newInstance(configFile, classLoader, configurationParticipants);
      log.info("Created shared instance");
    }
    return sharedInstance;
  }

  /**
   * Shut down the shared instance if it exists.
   */
  @Override
  public synchronized void shutdown() {
    if (sharedInstance != null) {
      log.info("Shutting down shared instance");
      sharedInstance.shutdown();
      sharedInstance = null;
      log.info("Shut down");
    }
  }

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
   * @param configFile                The {@link File} pointing at Hazelcast XML configuration file, may be {@code null}.
   * @param classLoader               The classloader to apply on Hazelcast.
   * @param configurationParticipants The non-{@code null} list of {@link HazelcastConfigParticipant}, or empty list.
   */
  @Override
  public HazelcastInstance newInstance(@Nullable final File configFile,
                                       final ClassLoader classLoader,
                                       final List<HazelcastConfigParticipant> configurationParticipants)
  {
    Config config = getHazelcastConfig(configFile, classLoader, configurationParticipants);

    // maybe dump configuration to log
    if (log.isTraceEnabled()) {
      ConfigXmlGenerator generator = new ConfigXmlGenerator(true);
      log.trace("Config: {}", generator.generate(config));
    }

    return Hazelcast.newHazelcastInstance(config);
  }

  private Config getHazelcastConfig(@Nullable final File configFile,
                                    final ClassLoader classLoader,
                                    final List<HazelcastConfigParticipant> configurationParticipants)
  {
    final Config config;
    if (null != configFile && configFile.isFile()) {
      try {
        log.debug("Loading configuration from file: {}", configFile);
        config = new FileSystemXmlConfig(configFile);
      }
      catch (FileNotFoundException e) {
        throw Throwables.propagate(e);
      }
    }
    else {
      // complain; as we probably want a configuration file to set the proper settings
      log.warn("Loading fresh configuration");
      config = new XmlConfigBuilder().build();
    }
    config.setClassLoader(classLoader);

    for (HazelcastConfigParticipant participant : configurationParticipants) {
      log.debug("Applying configuration participant: {}", participant);
      participant.apply(config);
    }

    return config;
  }
}
