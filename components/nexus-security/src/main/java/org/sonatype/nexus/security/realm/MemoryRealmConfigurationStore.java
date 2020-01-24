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
package org.sonatype.nexus.security.realm;

import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * In-memory {@link RealmConfigurationStore}.
 *
 * @since 3.0
 */
@Named("memory")
@Singleton
@Priority(Integer.MIN_VALUE)
@VisibleForTesting
public class MemoryRealmConfigurationStore
  extends ComponentSupport
  implements RealmConfigurationStore
{
  private RealmConfiguration model;

  @Override
  public RealmConfiguration newEntity() {
    return new MemoryRealmConfiguration();
  }

  @Override
  @Nullable
  public synchronized RealmConfiguration load() {
    return model;
  }

  @Override
  public synchronized void save(final RealmConfiguration configuration) {
    this.model = checkNotNull(configuration);
  }

  /**
   * @since 3.20
   */
  private static class MemoryRealmConfiguration
      implements RealmConfiguration, Cloneable {

    private List<String> realmNames;

    MemoryRealmConfiguration() {
      // package private
    }

    @Override
    public List<String> getRealmNames() {
      return realmNames;
    }

    @Override
    public void setRealmNames(@Nullable final List<String> realmNames) {
      this.realmNames = realmNames;
    }

    @Override
    public MemoryRealmConfiguration copy() {
      try {
        MemoryRealmConfiguration copy = (MemoryRealmConfiguration) clone();
        if (realmNames != null) {
          copy.realmNames = Lists.newArrayList(realmNames);
        }
        return copy;
      }
      catch (CloneNotSupportedException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
