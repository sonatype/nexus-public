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
package org.sonatype.nexus.cleanup.internal.storage;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage;
import org.sonatype.nexus.cleanup.storage.event.CleanupPolicyDeletedEvent;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.transaction.Transactional;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * MyBatis {@link CleanupPolicyStorage} implementation.
 *
 * @since 3.21
 */
@Named("mybatis")
@Singleton
public class CleanupPolicyStorageImpl
    extends ConfigStoreSupport<CleanupPolicyDAO>
    implements CleanupPolicyStorage
{
  private final EventManager eventManager;

  @Inject
  public CleanupPolicyStorageImpl(final DataSessionSupplier sessionSupplier, final EventManager eventManager) {
    super(sessionSupplier);
    this.eventManager = checkNotNull(eventManager);
  }

  @Override
  public CleanupPolicy newCleanupPolicy() {
    return new CleanupPolicyData();
  }

  @Transactional
  @Override
  public List<CleanupPolicy> getAll() {
    return ImmutableList.copyOf(dao().browse());
  }

  @Transactional
  @Override
  public List<CleanupPolicy> getAllByFormat(final String format) {
    return ImmutableList.copyOf(dao().browseByFormat(format));
  }

  @Transactional
  @Override
  public long count() {
    return dao().count();
  }

  @Transactional
  @Override
  public CleanupPolicy add(final CleanupPolicy policy) {
    dao().create((CleanupPolicyData) policy);
    return policy;
  }

  @Transactional
  @Override
  public CleanupPolicy get(final String policyName) {
    return dao().read(policyName).orElse(null);
  }

  @Transactional
  @Override
  public boolean exists(final String policyName) {
    return dao().read(policyName).isPresent();
  }

  @Transactional
  @Override
  public CleanupPolicy update(final CleanupPolicy policy) {
    dao().update((CleanupPolicyData) policy);
    return policy;
  }

  @Override
  public void remove(final CleanupPolicy policy) {
    doRemove(policy);
    postDeletedEvent(policy);
  }

  @Transactional
  protected void doRemove(final CleanupPolicy policy) {
    dao().delete(policy.getName());
  }

  private void postDeletedEvent(final CleanupPolicy policy) {
    // notify cleanup policy has been removed
    eventManager.post(new CleanupPolicyDeletedEvent()
    {
      @Override
      public boolean isLocal() {
        return true;
      }

      @Override
      public CleanupPolicy getCleanupPolicy() {
        return policy;
      }
    });
  }
}
