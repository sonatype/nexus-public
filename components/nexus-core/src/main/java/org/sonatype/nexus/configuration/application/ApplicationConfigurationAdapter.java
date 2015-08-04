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
package org.sonatype.nexus.configuration.application;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.configuration.model.Configuration;
import org.sonatype.nexus.proxy.storage.local.LocalStorageContext;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Adapter for NexusConfiguration.
 *
 * @author cstamas
 */
@Singleton
@Named
public class ApplicationConfigurationAdapter
    implements ApplicationConfiguration
{
  private final NexusConfiguration nexusConfiguration;

  @Inject
  public ApplicationConfigurationAdapter(final NexusConfiguration nexusConfiguration) {
    this.nexusConfiguration = checkNotNull(nexusConfiguration);
  }

  @Deprecated
  @Override 
  public Configuration getConfigurationModel() {
    return nexusConfiguration.getConfigurationModel();
  }

  @Nullable
  @Override
  public File getInstallDirectory() {
    return nexusConfiguration.getInstallDirectory();
  }

  @Override
  public File getWorkingDirectory() {
    return nexusConfiguration.getWorkingDirectory();
  }

  @Override 
  public File getWorkingDirectory(final String key) {
    return nexusConfiguration.getWorkingDirectory(key);
  }

  @Override 
  public File getWorkingDirectory(final String key, final boolean createIfNeeded) {
    return nexusConfiguration.getWorkingDirectory(key, createIfNeeded);
  }

  @Override 
  public File getTemporaryDirectory() {
    return nexusConfiguration.getTemporaryDirectory();
  }

  @Override 
  public File getConfigurationDirectory() {
    return nexusConfiguration.getConfigurationDirectory();
  }

  @Override 
  public void saveConfiguration() throws IOException {
    nexusConfiguration.saveConfiguration();
  }

  @Override
  public LocalStorageContext getGlobalLocalStorageContext() {
    return nexusConfiguration.getGlobalLocalStorageContext();
  }

  @Override 
  public RemoteStorageContext getGlobalRemoteStorageContext() {
    return nexusConfiguration.getGlobalRemoteStorageContext();
  }

}
