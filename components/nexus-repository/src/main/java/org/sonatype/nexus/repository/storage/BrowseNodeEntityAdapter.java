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
package org.sonatype.nexus.repository.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.AttachedEntityId;
import org.sonatype.nexus.orient.entity.IterableEntityAdapter;
import org.sonatype.nexus.repository.browse.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.repository.storage.internal.BrowseNodeSqlBuilder;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.security.SecurityHelper;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.index.OCompositeKey;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Streams.stream;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.repository.storage.internal.BrowseNodeSqlBuilder.DB_CLASS;
import static org.sonatype.nexus.repository.storage.internal.BrowseNodeSqlBuilder.P_ASSET_ID;
import static org.sonatype.nexus.repository.storage.internal.BrowseNodeSqlBuilder.P_ASSET_NAME_LOWERCASE;
import static org.sonatype.nexus.repository.storage.internal.BrowseNodeSqlBuilder.P_CHILDREN_IDS;
import static org.sonatype.nexus.repository.storage.internal.BrowseNodeSqlBuilder.P_COMPONENT_ID;
import static org.sonatype.nexus.repository.storage.internal.BrowseNodeSqlBuilder.P_ID;
import static org.sonatype.nexus.repository.storage.internal.BrowseNodeSqlBuilder.P_PARENT_ID;
import static org.sonatype.nexus.repository.storage.internal.BrowseNodeSqlBuilder.P_PATH;
import static org.sonatype.nexus.repository.storage.internal.BrowseNodeSqlBuilder.P_REPOSITORY_NAME;

/**
 * {@link BrowseNode} entity-adapter.
 *
 * @since 3.6
 */
@Named
@Singleton
public class BrowseNodeEntityAdapter
    extends IterableEntityAdapter<BrowseNode>
{
  public static final String I_PARENT_ID_PATH = new OIndexNameBuilder().type(DB_CLASS).property(P_REPOSITORY_NAME)
      .property(P_PARENT_ID).property(P_PATH).build();

  private static final String I_ASSET_ID = new OIndexNameBuilder().type(DB_CLASS).property(P_ASSET_ID).build();

  private static final String NODE_WITH_ASSET_QUERY = String.format(
      "select from %s where %s = :asset_id", DB_CLASS, P_ASSET_ID);

  private static final String UPDATE_NODE_ASSET_COMPONENT = String.format(
      "UPDATE :%s SET %s = :%s, %s = :%s, %s = :%s RETURN AFTER @this", P_ID, P_ASSET_ID, P_ASSET_ID, P_COMPONENT_ID,
      P_COMPONENT_ID, P_ASSET_NAME_LOWERCASE, P_ASSET_NAME_LOWERCASE);

  private static final String UPDATE_NODE_ASSET = String
      .format("UPDATE :%s SET %s = :%s, %s = :%s RETURN AFTER @this", P_ID, P_ASSET_ID, P_ASSET_ID,
          P_ASSET_NAME_LOWERCASE, P_ASSET_NAME_LOWERCASE);

  private static final String UPDATE_NODE_COMPONENT = String.format("UPDATE :%s SET %s = :%s RETURN AFTER @this", P_ID,
      P_COMPONENT_ID, P_COMPONENT_ID);

  private static final String INSERT_QUERY = String.format(
      "INSERT INTO %s SET %s = :%s, %s = :%s, %s = :%s, %s = :%s, %s = :%s, %s = :%s RETURN @this", DB_CLASS,
      P_PARENT_ID, P_PARENT_ID, P_PATH, P_PATH, P_REPOSITORY_NAME, P_REPOSITORY_NAME,
      P_ASSET_ID, P_ASSET_ID, P_COMPONENT_ID, P_COMPONENT_ID, P_ASSET_NAME_LOWERCASE, P_ASSET_NAME_LOWERCASE);

  private static final String ADD_CHILD_QUERY = String
      .format("UPDATE :%s ADD %s = :%s", P_PARENT_ID, P_CHILDREN_IDS, P_CHILDREN_IDS);

  private static final String UPDATE_CHILDREN_QUERY = String.format(
      "update :%s add %s = (select from %s where %s = :%s)", P_ID, P_CHILDREN_IDS, DB_CLASS, P_PARENT_ID, P_ID);

  private static final String REMOVE_CHILD_QUERY = String.format(
      "UPDATE :%s REMOVE %s = :%s", P_PARENT_ID, P_CHILDREN_IDS, P_CHILDREN_IDS);

  private static final String TRUNCATE_REPOSITORY_QUERY = String
      .format("DELETE FROM %s WHERE %s = :%s", DB_CLASS, P_REPOSITORY_NAME, P_REPOSITORY_NAME);

  private final AssetEntityAdapter assetEntityAdapter;

  private final ComponentEntityAdapter componentEntityAdapter;

  private final SecurityHelper securityHelper;

  private final BrowseNodeSqlBuilder browseNodeSqlBuilder;

  private final int truncateCount;

  private final boolean enabled;

  @Inject
  public BrowseNodeEntityAdapter(final ComponentEntityAdapter componentEntityAdapter,
                                 final AssetEntityAdapter assetEntityAdapter,
                                 final SecurityHelper securityHelper,
                                 final BrowseNodeSqlBuilder browseNodeSqlBuilder,
                                 final BrowseNodeConfiguration configuration)
  {
    super(DB_CLASS);
    this.assetEntityAdapter = checkNotNull(assetEntityAdapter);
    this.componentEntityAdapter = checkNotNull(componentEntityAdapter);
    this.securityHelper = checkNotNull(securityHelper);
    this.browseNodeSqlBuilder = checkNotNull(browseNodeSqlBuilder);
    this.truncateCount = checkNotNull(configuration).getMaxTruncateCount();
    this.enabled = checkNotNull(configuration).isEnabled();
  }

  /**
   * @return the children or null if a node does not exist
   */
  @Nullable
  public Iterable<BrowseNode> getChildrenByPath(final ODatabaseDocumentTx db,
                                                final Iterable<String> pathSegments,
                                                final String assetRepositoryName,
                                                final String authzRepositoryName,
                                                final String format,
                                                final int maxNodes,
                                                @Nullable final String filter)
  {
    checkNotNull(db);
    checkNotNull(pathSegments);
    checkNotNull(assetRepositoryName);
    checkNotNull(authzRepositoryName);

    ORID parentId = null;
    for (String pathSegment : pathSegments) {
      BrowseNode node = getNode(db, assetRepositoryName, parentId, pathSegment);
      if (node == null) {
        return null;
      }
      parentId = recordIdentity(node);
    }

    Map<String, Object> parameters = new HashMap<>();

    // check repository permission first before falling back to content auth
    if (hasRepositoryPermission(db, authzRepositoryName)) {
      String getBrowseNodesQuery = browseNodeSqlBuilder
          .getBrowseNodesQuery(parentId, assetRepositoryName, filter, parameters, maxNodes);
      Iterable<BrowseNode> nodes = transform(db.command(new OCommandSQL(getBrowseNodesQuery)).execute(parameters));

      if (filter != null) {
        nodes = applyFilterToChildren(db, nodes, filter);
      }
      return nodes;
    }
    else {
      String getBrowseNodesWithAuthQuery = browseNodeSqlBuilder
          .getBrowseNodesQueryWithContentSelectorAuthz(parentId, assetRepositoryName, authzRepositoryName, format,
              filter, parameters, maxNodes);
      Iterable<BrowseNode> nodes = transform(
          db.command(new OCommandSQL(getBrowseNodesWithAuthQuery)).execute(parameters));
      return hasAuthorizedChildren(db, nodes, assetRepositoryName, authzRepositoryName, format, filter);
    }
  }

  private Iterable<BrowseNode> applyFilterToChildren(final ODatabaseDocumentTx db,
                                                     final Iterable<BrowseNode> children,
                                                     final String filter)
  {
    checkNotNull(filter);

    Map<String, Object> parameters = new HashMap<>();

    return doAccessibleChildrenQuery(db, children, browseNodeSqlBuilder.getChildMatchingFilterQuery(filter, parameters),
        parameters);
  }

  private Iterable<BrowseNode> hasAuthorizedChildren(final ODatabaseDocumentTx db,
                                                     final Iterable<BrowseNode> children,
                                                     final String assetRepositoryName,
                                                     final String authzRepositoryName,
                                                     final String format,
                                                     final String filter)
  {
    Map<String, Object> parameters = new HashMap<>();

    final String query = browseNodeSqlBuilder
        .getAuthorizedChildMaybeMatchingFilter(assetRepositoryName, authzRepositoryName, format, filter, parameters);

    return doAccessibleChildrenQuery(db, children, query, parameters);
  }

  private Iterable<BrowseNode> doAccessibleChildrenQuery(final ODatabaseDocumentTx db,
                                                         final Iterable<BrowseNode> children,
                                                         final String query,
                                                         final Map<String, Object> parameters)
  {
    List<BrowseNode> nodesWithChildren = stream(children).filter(child -> {
      Map<String, Object> params = new HashMap<>(parameters);
      params.put(P_ID, recordIdentity(child).getIdentity());
      Iterable<ODocument> accessibleChildren = db.command(new OCommandSQL(query)).execute(params);
      return accessibleChildren.iterator().hasNext();
    }).collect(toList());

    return nodesWithChildren.isEmpty() ? null : nodesWithChildren;
  }

  public BrowseNode save(final ODatabaseDocumentTx db, final BrowseNode node, final boolean updateChildLinks) {
    checkNotNull(db);
    checkNotNull(node);
    checkNotNull(node.getRepositoryName());
    checkNotNull(node.getPath());

    if (EntityHelper.hasMetadata(node)) {
      attachMetadata(node, editEntity(db, node));
      return node;
    }

    return upsert(db, node.getRepositoryName(), node.getParentId(), node.getPath(), node.getAssetId(),
        node.getComponentId(), node.getAssetNameLowercase(), updateChildLinks);
  }

  public BrowseNode upsert(final ODatabaseDocumentTx db,
                           final String repositoryName,
                           final EntityId parentId,
                           final String pathName,
                           final boolean updateChildLinks)
  {
    return upsert(db, repositoryName, parentId, pathName, null, null, null, updateChildLinks);
  }

  public BrowseNode upsert(final ODatabaseDocumentTx db,
                           final String repositoryName,
                           final EntityId parentId,
                           final String pathName,
                           final EntityId assetId,
                           final EntityId componentId,
                           final String assetNameLowercase,
                           final boolean createChildLinks)
  {
    checkNotNull(db);
    checkNotNull(repositoryName);
    checkNotNull(pathName);

    ORID parentRid = parentId != null ? recordIdentity(parentId) : null;

    Map<String, Object> parameters = new HashMap<>();
    parameters.put(P_ASSET_ID, assetId != null ? assetEntityAdapter.recordIdentity(assetId) : null);
    parameters.put(P_COMPONENT_ID, componentId != null ? componentEntityAdapter.recordIdentity(componentId) : null);
    parameters.put(P_ASSET_NAME_LOWERCASE, assetNameLowercase);

    BrowseNode node = getNode(db, repositoryName, parentRid, pathName);
    if (node != null) {
      parameters.put(P_ID, recordIdentity(EntityHelper.id(node)));

      // On upsert, the only change we make is adding a componentId or assetId
      return updateNode(db, node, assetId, componentId, parameters);
    }
    else {
      try {
        parameters.put(P_PATH, pathName);
        parameters.put(P_PARENT_ID, parentRid);
        parameters.put(P_REPOSITORY_NAME, repositoryName);

        node = transformEntity(db.command(new OCommandSQL(INSERT_QUERY)).execute(parameters));

        if (createChildLinks) {
          updateAsChild(db, node);
        }

        return node;
      }
      // Rare, but another thread might run an insert between hasNode and insert on this thread, update if that happens
      catch (OCommandExecutionException e) {
        log.error("Concurrent insert for path={}, attempting update instead", pathName, e);

        return upsert(db, repositoryName, parentId, pathName, assetId, componentId, assetNameLowercase,
            createChildLinks);
      }
    }
  }

  private BrowseNode updateNode(final ODatabaseDocumentTx db,
                                final BrowseNode node,
                                final EntityId assetId,
                                final EntityId componentId,
                                final Map<String, Object> parameters)
  {
    boolean updateAsset = assetId != null && node.getAssetId() == null;
    boolean updateComponent = componentId != null && node.getComponentId() == null;

    if (updateAsset && updateComponent) {
      List<ODocument> results = db.command(new OCommandSQL(UPDATE_NODE_ASSET_COMPONENT)).execute(parameters);
      return transformEntity(getFirst(results, null));
    }
    else if (updateAsset) {
      List<ODocument> results = db.command(new OCommandSQL(UPDATE_NODE_ASSET)).execute(parameters);
      return transformEntity(getFirst(results, null));
    }
    else if (updateComponent) {
      List<ODocument> results = db.command(new OCommandSQL(UPDATE_NODE_COMPONENT)).execute(parameters);
      return transformEntity(getFirst(results, null));
    }

    return node;
  }

  private void updateAsChild(final ODatabaseDocumentTx db, final BrowseNode node) {
    checkNotNull(db);
    checkNotNull(node);

    if (node.getParentId() != null) {
      Map<String, Object> parameters = new HashMap<>();
      parameters.put(P_CHILDREN_IDS, recordIdentity(EntityHelper.id(node)));
      parameters.put(P_PARENT_ID, recordIdentity(node.getParentId()));

      db.command(new OCommandSQL(ADD_CHILD_QUERY)).execute(parameters);
    }
  }

  public int truncateRepository(final ODatabaseDocumentTx db, final String repositoryName) {
    checkNotNull(db);
    checkNotNull(repositoryName);

    return db.command(new OCommandSQL(TRUNCATE_REPOSITORY_QUERY + " limit " + truncateCount))
        .execute(Collections.singletonMap(P_REPOSITORY_NAME, repositoryName));
  }

  @Override
  public void deleteEntity(final ODatabaseDocumentTx db, final BrowseNode node) {
    checkNotNull(db);
    checkNotNull(node);

    ORID nodeId = recordIdentity(EntityHelper.id(node));
    db.delete(nodeId);

    if (node.getParentId() != null) {
      Map<String, Object> parameters = new HashMap<>();
      parameters.put(P_CHILDREN_IDS, nodeId);
      parameters.put(P_PARENT_ID, recordIdentity(node.getParentId()));

      db.command(new OCommandSQL(REMOVE_CHILD_QUERY)).execute(parameters);

      ODocument document = document(db, node.getParentId());
      Set<OIdentifiable> children = document.field(P_CHILDREN_IDS);
      if (children.isEmpty()) {
        deleteEntity(db, transformEntity(document));
      }
    }
  }

  @Override
  public ODocument addEntity(final ODatabaseDocumentTx db, final BrowseNode node) {
    ODocument doc = super.addEntity(db, node);

    updateAsChild(db, node);

    return doc;
  }

  public BrowseNode getById(final ODatabaseDocumentTx db, final EntityId id) {
    checkNotNull(db);
    checkNotNull(id);

    return transformEntity(db.getRecord(recordIdentity(id)));
  }

  public Iterable<BrowseNode> getByAssetId(final ODatabaseDocumentTx db, final EntityId assetId) {
    checkNotNull(db);
    checkNotNull(assetId);

    Map<String, Object> parameters = new HashMap<>();
    parameters.put(P_ASSET_ID, assetEntityAdapter.recordIdentity(assetId));

    Iterable<ODocument> docs = db.command(new OCommandSQL(NODE_WITH_ASSET_QUERY)).execute(parameters);

    return transform(docs);
  }

  public Iterable<BrowseNode> getByPath(final ODatabaseDocumentTx db,
                                        final Iterable<String> pathSegments,
                                        final String repositoryName)
  {
    checkNotNull(db);
    checkNotNull(pathSegments);
    checkNotNull(repositoryName);

    List<BrowseNode> nodes = new ArrayList<>();
    for (String pathSegment : pathSegments) {
      ORID parentId = nodes.isEmpty() ? null : recordIdentity(nodes.get(nodes.size() - 1));
      BrowseNode node = getNode(db, repositoryName, parentId, pathSegment);
      //if we come across a missing node, we don't want to sent back a list of partial components, so just send back
      //an empty list
      if (node == null) {
        return Collections.emptyList();
      }
      nodes.add(node);
    }

    return nodes;
  }

  public void updateChildren(final ODatabaseDocumentTx db, final ORID browseNodeId) {
    db.command(new OCommandSQL(UPDATE_CHILDREN_QUERY)).execute(Collections.singletonMap(P_ID, browseNodeId));
  }

  private BrowseNode getNode(final ODatabaseDocumentTx db,
                             final String repositoryName,
                             final ORID parentId,
                             final String pathName)
  {
    checkNotNull(pathName);

    OIndex<?> index = db.getMetadata().getIndexManager().getIndex(I_PARENT_ID_PATH);
    ORecordId id = (ORecordId) index.get(new OCompositeKey(parentId, pathName, repositoryName));

    if (id == null) {
      return null;
    }

    return transformEntity(document(db, new AttachedEntityId(this, id)));
  }

  @Override
  protected BrowseNode newEntity() {
    return new BrowseNode();
  }

  @Override
  public void register(final ODatabaseDocumentTx db, @Nullable final Runnable initializer) {
    if (enabled) {
      super.register(db, initializer);
    }
  }

  @Override
  protected void defineType(final ODatabaseDocumentTx db, final OClass type) {
    if (!enabled) {
      return;
    }

    defineType(type);

    ODocument metadata = db.newInstance().field("ignoreNullValues", false);

    type.createIndex(I_PARENT_ID_PATH, INDEX_TYPE.UNIQUE.name(), null, metadata,
        new String[]{P_PARENT_ID, P_PATH, P_REPOSITORY_NAME});
    type.createIndex(I_ASSET_ID, INDEX_TYPE.NOTUNIQUE.name(), null, metadata, new String[]{P_ASSET_ID});
  }

  @Override
  protected void defineType(final OClass type) {
    if (!enabled) {
      return;
    }

    type.createProperty(P_REPOSITORY_NAME, OType.STRING).setMandatory(true).setNotNull(true);
    type.createProperty(P_PATH, OType.STRING).setMandatory(true).setNotNull(true);

    type.createProperty(P_CHILDREN_IDS, OType.LINKSET, type).setDefaultValue("[]").setNotNull(true);
    type.createProperty(P_PARENT_ID, OType.LINK, type);
    type.createProperty(P_COMPONENT_ID, OType.LINK, componentEntityAdapter.getSchemaType());
    type.createProperty(P_ASSET_ID, OType.LINK, assetEntityAdapter.getSchemaType());
    type.createProperty(P_ASSET_NAME_LOWERCASE, OType.STRING);
  }

  @Override
  protected void readFields(final ODocument document, final BrowseNode entity) throws Exception {
    ORID assetId = document.field(P_ASSET_ID, ORID.class);
    ORID componentId = document.field(P_COMPONENT_ID, ORID.class);
    String repositoryName = document.field(P_REPOSITORY_NAME, OType.STRING);
    String path = document.field(P_PATH, OType.STRING);
    ORID parentId = document.field(P_PARENT_ID, ORID.class);
    String assetNameLowercase = document.field(P_ASSET_NAME_LOWERCASE, OType.STRING);

    entity.withRepositoryName(repositoryName).withPath(path).withAssetNameLowercase(assetNameLowercase);

    entity.setAssetId(assetId != null ? new AttachedEntityId(assetEntityAdapter, assetId) : null);
    entity.setComponentId(componentId != null ? new AttachedEntityId(componentEntityAdapter, componentId) : null);
    entity.setParentId(parentId != null ? new AttachedEntityId(this, parentId) : null);
  }

  @Override
  protected void writeFields(final ODocument document, final BrowseNode entity) throws Exception {
    document.field(P_PATH, entity.getPath());
    document.field(P_ASSET_NAME_LOWERCASE, entity.getAssetNameLowercase());
    document.field(P_REPOSITORY_NAME, entity.getRepositoryName());
    document.field(P_PARENT_ID, entity.getParentId() != null ? recordIdentity(entity.getParentId()) : null);
    document.field(P_COMPONENT_ID,
        entity.getComponentId() != null ? componentEntityAdapter.recordIdentity(entity.getComponentId()) : null);
    document.field(P_ASSET_ID, entity.getAssetId() != null ? assetEntityAdapter.recordIdentity(entity.getAssetId())
        : null);
  }

  private boolean hasRepositoryPermission(final ODatabaseDocumentTx db, final String repositoryName) {
    boolean result = securityHelper
        .anyPermitted(new RepositoryViewPermission("*", repositoryName, BreadActions.BROWSE));

    //seems to be required since the security helper call above is nuking the current db
    db.activateOnCurrentThread();

    return result;
  }
}
