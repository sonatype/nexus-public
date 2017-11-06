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
package org.sonatype.nexus.repository.storage.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.Lifecycle;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.storage.BrowseNode;
import org.sonatype.nexus.repository.storage.BrowseNodeEntityAdapter;
import org.sonatype.nexus.repository.storage.BrowseNodeStore;
import org.sonatype.nexus.repository.types.GroupType;

import com.google.common.collect.Iterables;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OCompositeKey;
import com.orientechnologies.orient.core.index.OIndexCursor;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SCHEMAS;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;
import static org.sonatype.nexus.repository.storage.BrowseNodeEntityAdapter.I_PARENT_ID_PATH;

/**
 * @since 3.6
 */
@Singleton
@ManagedLifecycle(phase = SCHEMAS)
@Named
public class BrowseNodeStoreImpl
    extends StateGuardLifecycleSupport
    implements BrowseNodeStore, Lifecycle
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final BrowseNodeEntityAdapter entityAdapter;

  private final int truncateCount;

  private final int updateChildCount;

  @Inject
  public BrowseNodeStoreImpl(@Named("component") final Provider<DatabaseInstance> databaseInstance,
                             final BrowseNodeEntityAdapter entityAdapter,
                             final BrowseNodeConfiguration configuration)
  {
    checkNotNull(configuration);
    this.databaseInstance = checkNotNull(databaseInstance);
    this.entityAdapter = checkNotNull(entityAdapter);
    this.truncateCount = configuration.getMaxTruncateCount();
    this.updateChildCount = configuration.getMaxUpdateChildCount();
  }

  @Override
  protected void doStart() throws Exception {
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      entityAdapter.register(db);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public BrowseNode createNodes(final String repositoryName, final Iterable<String> pathSegments) {
    return createNodes(repositoryName, pathSegments, true);
  }

  @Override
  public BrowseNode createNodes(final String repositoryName, final Iterable<String> pathSegments, final boolean createChildLinks) {
    BrowseNode[] parentPath = new BrowseNode[1];

    for (String pathName : pathSegments) {
      inTxRetry(databaseInstance).run(
          db -> {
            parentPath[0] = entityAdapter.upsert(db, repositoryName,
                parentPath[0] != null ? EntityHelper.id(parentPath[0]) : null, pathName, createChildLinks);
          });
    }

    return parentPath[0];
  }

  @Override
  @Guarded(by = STARTED)
  public BrowseNode getById(final EntityId pathId) {
    try (ODatabaseDocumentTx db = databaseInstance.get().acquire()) {
      return entityAdapter.getById(db, pathId);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public BrowseNode getByAssetId(final EntityId assetId) {
    try (ODatabaseDocumentTx db = databaseInstance.get().acquire()) {
      return Iterables.getFirst(entityAdapter.getByAssetId(db, assetId), null);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public Iterable<BrowseNode> getByPath(final String repositoryName, final Iterable<String> pathSegments) {
    try (ODatabaseDocumentTx db = databaseInstance.get().acquire()) {
      return entityAdapter.getByPath(db, pathSegments, repositoryName);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public Iterable<BrowseNode> getChildrenByPath(final Repository repository,
                                                final Iterable<String> pathSegments,
                                                final int maxNodes,
                                                final String filter)
  {
    String repositoryName = repository.getName();
    String format = repository.getFormat().getValue();
    if (repository.getType() instanceof GroupType) {
      return getChildrenByPath(getRepositoryNamesFromGroup(repository), format, pathSegments, repositoryName, maxNodes, filter);
    }
    else {
      return getChildrenByPath(repository.getName(), format, pathSegments, repositoryName, maxNodes, filter);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public BrowseNode save(final BrowseNode path) {
    return save(path, true);
  }

  @Override
  @Guarded(by = STARTED)
  public BrowseNode save(final BrowseNode path, final boolean updateChildLinks) {
    return inTxRetry(databaseInstance).call(db -> entityAdapter.save(db, path, updateChildLinks));
  }

  @Override
  @Guarded(by = STARTED)
  public void deleteNode(final BrowseNode node) {
    inTxRetry(databaseInstance).run(db -> entityAdapter.deleteEntity(db, node));
  }

  @Override
  @Guarded(by = STARTED)
  public void deleteNodeByAssetId(final EntityId assetId) {
    inTx(databaseInstance).call(db -> entityAdapter.getByAssetId(db, assetId)).forEach(assetNode -> {
      if (assetNode.isLeaf()) {
        deleteNode(assetNode);
      }
      else {
        assetNode.setAssetId(null);
        save(assetNode, false);
      }
    });
  }

  @Override
  @Guarded(by = STARTED)
  public void deleteNodeByComponentId(final EntityId componentId) {
    inTx(databaseInstance).call(db -> entityAdapter.getByComponentId(db, componentId)).forEach(componentNode -> {
      componentNode.setComponentId(null);
      save(componentNode, false);
    });
  }

  @Override
  @Guarded(by = STARTED)
  public void truncateRepository(final String repositoryName) {
    log.debug("Deleting all browse nodes for repositoryName={}", repositoryName);

    int removedCount;
    ProgressLogIntervalHelper progressLogger = new ProgressLogIntervalHelper(log, 60);
    do {
      removedCount = inTxRetry(databaseInstance).call(db -> entityAdapter.truncateRepository(db, repositoryName));
      progressLogger.info("Deleted {} browse nodes for repositoryName={} in {}", removedCount, repositoryName,
          progressLogger.getElapsed());
    }
    //keep repeating the delete until there are none left
    while (removedCount == truncateCount);

    progressLogger.flush();
    log.debug("All browse nodes deleted for repositoryName={} in ", repositoryName, progressLogger.getElapsed());
  }

  @Override
  @Guarded(by = STARTED)
  public void updateChildNodes(final String repositoryName) {
    try (ODatabaseDocumentTx db = databaseInstance.get().acquire()) {
      OIndexCursor cursor = db.getMetadata().getIndexManager().getIndex(I_PARENT_ID_PATH).cursor();
      cursor.setPrefetchSize(updateChildCount);
      Map<Object, OIdentifiable> entries = new HashMap<>();

      do {
        entries.clear();

        //pull out the max number of index entries we are allowed
        while (entries.size() < updateChildCount) {
          Map.Entry<Object, OIdentifiable> entry = cursor.nextEntry();
          //null entry marks the end of the cursor
          if (entry == null) {
            break;
          }
          entries.put(entry.getKey(), entry.getValue());
        }

        if (entries.size() > 0) {
          inTxRetry(databaseInstance).run(database -> entries.entrySet().forEach(entry -> {
            Object key = entry.getKey();
            if (key instanceof OCompositeKey && repositoryName.equals(((OCompositeKey) key).getKeys().get(2))) {
              entityAdapter.updateChildren(database, entry.getValue().getIdentity());
            }
          }));
        }
      }
      while (entries.size() >= updateChildCount);
    }
  }

  private Iterable<BrowseNode> getChildrenByPath(final String repositoryName,
                                                 final String format,
                                                 final Iterable<String> pathSegments,
                                                 final String repositoryNameForPermission,
                                                 final int maxNodes,
                                                 final String filter)
  {
    try (ODatabaseDocumentTx db = databaseInstance.get().acquire()) {
      return entityAdapter.getChildrenByPath(db, pathSegments, repositoryName, repositoryNameForPermission, format, maxNodes, filter);
    }
  }

  private Iterable<BrowseNode> getChildrenByPath(final Iterable<String> repositoryNames,
                                                 final String format,
                                                 final Iterable<String> pathSegments,
                                                 final String repositoryNameForPermission,
                                                 final int maxNodes,
                                                 final String filter)
  {
    Set<String> nodeNames = new HashSet<>();
    List<BrowseNode> children = null;
    for (String repositoryName : repositoryNames) {
      Iterable<BrowseNode> nodes = getChildrenByPath(repositoryName, format, pathSegments, repositoryNameForPermission, maxNodes, filter);
      if (nodes == null) {
        continue;
      }
      children = children != null ? children : new ArrayList<>();
      for (BrowseNode node : nodes) {
        if (!nodeNames.contains(node.getPath())) {
          children.add(node);
          nodeNames.add(node.getPath());
        }
      }
    }
    return children;
  }

  private Iterable<String> getRepositoryNamesFromGroup(final Repository repository) {
    Set<String> repositoryNames = new LinkedHashSet<>();
    repositoryNames.add(repository.getName());
    repository.facet(GroupFacet.class).leafMembers().stream().map(Repository::getName).forEach(repositoryNames::add);
    return repositoryNames;
  }
}
