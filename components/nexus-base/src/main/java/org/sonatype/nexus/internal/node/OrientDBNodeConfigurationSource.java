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
package org.sonatype.nexus.internal.node;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.Lifecycle;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.node.NodeConfiguration;
import org.sonatype.nexus.common.node.NodeConfigurationSource;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;

import com.google.common.collect.ImmutableList;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SCHEMAS;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;

/**
 * Orient {@link NodeConfigurationSource}
 *
 * @since 3.6
 */
@Named
@Singleton
@ManagedLifecycle(phase = SCHEMAS)
public class OrientDBNodeConfigurationSource
    extends StateGuardLifecycleSupport
    implements NodeConfigurationSource, Lifecycle
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final NodeConfigurationEntityAdapter entityAdapter;

  @Inject
  public OrientDBNodeConfigurationSource(
      @Named(DatabaseInstanceNames.CONFIG) final Provider<DatabaseInstance> databaseInstance,
      final NodeConfigurationEntityAdapter entityAdapter)
  {
    this.databaseInstance = checkNotNull(databaseInstance);
    this.entityAdapter = checkNotNull(entityAdapter);
  }

  @Override
  protected void doStart() {
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      // register schema
      entityAdapter.register(db);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public List<NodeConfiguration> loadAll() {
    return inTx(databaseInstance).call(db -> ImmutableList.copyOf(entityAdapter.browse(db)));
  }

  @Override
  @Guarded(by = STARTED)
  public String create(final NodeConfiguration configuration) {
    checkNotNull(configuration);
    checkNotNull(configuration.getId());
    try {
      return inTxRetry(databaseInstance).call(db -> {
        entityAdapter.addEntity(db, configuration);
        return configuration.getId();
      });
    }
    catch (ORecordDuplicatedException e) {
      throw new IllegalArgumentException(
          "Duplicated node record: id=" + configuration.getId() + ", name=" + configuration.getFriendlyNodeName(), e);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public boolean update(final NodeConfiguration configuration) {
    checkNotNull(configuration);
    try {
      return inTxRetry(databaseInstance).call(db -> {
        final ODocument doc = entityAdapter.selectById(db, configuration.getId());
        if (doc != null) {
          entityAdapter.writeEntity(doc, configuration);
          return true;
        }
        return false;
      });
    }
    catch (OConcurrentModificationException e) {
      throw new IllegalArgumentException(
          "Stale node update attempt id=" + configuration.getId() + ", name=" + configuration.getFriendlyNodeName(), e);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public boolean delete(final String nodeId) {
    checkNotNull(nodeId);
    return inTxRetry(databaseInstance).call(db -> {
      final ODocument doc = entityAdapter.selectById(db, nodeId);
      if (doc != null) {
        db.delete(doc);
        return true;
      }
      return false;
    });
  }

  @Override
  @Guarded(by = STARTED)
  public Optional<NodeConfiguration> getById(final String nodeId) {
    checkNotNull(nodeId);
    return inTx(databaseInstance).call(db -> {
      ODocument doc = entityAdapter.selectById(db, nodeId);
      if (doc == null) {
        return Optional.empty();
      }
      return Optional.ofNullable(entityAdapter.readEntity(doc));
    });
  }

  @Override
  @Guarded(by = STARTED)
  public void setFriendlyName(final String nodeId, final String friendlyName) {
    checkNotNull(nodeId);
    Optional<NodeConfiguration> it = getById(nodeId);
    NodeConfiguration configuration;
    if (it.isPresent()) {
      configuration = it.get();
      configuration.setFriendlyNodeName(friendlyName);
      update(configuration);
    }
    else {
      create(new NodeConfiguration(nodeId, friendlyName));
    }
  }
}
