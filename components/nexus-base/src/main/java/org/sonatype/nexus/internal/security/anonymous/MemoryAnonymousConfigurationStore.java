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
package org.sonatype.nexus.internal.security.anonymous;

import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;

import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * In-memory {@link AnonymousConfigurationStore}.
 */
@Named("memory")
@Singleton
@Priority(Integer.MIN_VALUE)
@VisibleForTesting
public class MemoryAnonymousConfigurationStore
    extends ComponentSupport
    implements AnonymousConfigurationStore
{
  private AnonymousConfiguration model;

  @Override
  @Nullable
  public synchronized AnonymousConfiguration load() {
    return model;
  }

  @Override
  public synchronized void save(final AnonymousConfiguration configuration) {
    this.model = checkNotNull(configuration);
  }

  @Override
  public AnonymousConfiguration newConfiguration() {
    return new MemoryAnonymousConfiguration();
  }

  private static class MemoryAnonymousConfiguration
      implements AnonymousConfiguration
  {
    private String realmName;

    private String userId;

    private boolean enabled;

    private MemoryAnonymousConfiguration() {
      // no arg
    }

    @Override
    public AnonymousConfiguration copy() {
      MemoryAnonymousConfiguration configuration = new MemoryAnonymousConfiguration();
      configuration.setEnabled(enabled);
      configuration.setRealmName(realmName);
      configuration.setUserId(userId);
      return configuration;
    }

    @Override
    public String getRealmName() {
      return realmName;
    }

    @Override
    public String getUserId() {
      return userId;
    }

    @Override
    public boolean isEnabled() {
      return enabled;
    }

    @Override
    public void setEnabled(final boolean enabled) {
      this.enabled = enabled;
    }

    @Override
    public void setRealmName(final String realmName) {
      this.realmName = realmName;
    }

    @Override
    public void setUserId(final String userId) {
      this.userId = userId;
    }
  }
}
