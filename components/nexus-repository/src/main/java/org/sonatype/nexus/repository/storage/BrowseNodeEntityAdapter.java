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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.AttachedEntityId;
import org.sonatype.nexus.orient.entity.IterableEntityAdapter;
import org.sonatype.nexus.repository.browse.BrowseNodeConfiguration;

import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getFirst;
import static org.sonatype.nexus.common.text.Strings2.lower;

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
  private static final String DB_CLASS = new OClassNameBuilder().type("browse_node").build();

  public static final String P_REPOSITORY_NAME = "repository_name";

  public static final String P_PARENT_PATH = "parent_path";

  public static final String P_NAME = "name";

  public static final String P_COMPONENT_ID = "component_id";

  public static final String P_ASSET_ID = "asset_id";

  public static final String P_ASSET_NAME_LOWERCASE = "asset_name_lowercase";

  public static final String AUTHZ_REPOSITORY_NAME = "authz_repository_name";

  private static final String BASE_PATH = "base_path";

  private static final String BASE_BOUNDARY = "base_boundary";

  private static final String SUBTREE_BOUNDARY = "subtree_boundary";

  private static final String I_REPOSITORY_NAME_PARENT_PATH_NAME = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_REPOSITORY_NAME)
      .property(P_PARENT_PATH)
      .property(P_NAME)
      .build();

  private static final String I_COMPONENT_ID = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_COMPONENT_ID)
      .build();

  private static final String I_ASSET_ID = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_ASSET_ID)
      .build();

  private static final String FIND_BY_PATH = String.format(
      "select expand(rid) from index:%s where key=[:%s,:%s,:%s] limit 1",
      I_REPOSITORY_NAME_PARENT_PATH_NAME, P_REPOSITORY_NAME, P_PARENT_PATH, P_NAME);

  private static final String FIND_CHILDREN = String.format(
      "select from %s where (%s=:%s and %s=:%s)",
      DB_CLASS, P_REPOSITORY_NAME, P_REPOSITORY_NAME, P_PARENT_PATH, BASE_PATH);

  private static final String FIND_FIRST_SUBTREE = String.format(
      "select from %s where (%s=:%s and %s>:%s and %s<:%s)",
      DB_CLASS, P_REPOSITORY_NAME, P_REPOSITORY_NAME, P_PARENT_PATH, BASE_PATH, P_PARENT_PATH, BASE_BOUNDARY);

  private static final String FIND_NEXT_SUBTREE = String.format(
      "select from %s where (%s=:%s and %s>=:%s and %s<:%s)",
      DB_CLASS, P_REPOSITORY_NAME, P_REPOSITORY_NAME, P_PARENT_PATH, SUBTREE_BOUNDARY, P_PARENT_PATH, BASE_BOUNDARY);

  private static final String FIND_BY_COMPONENT = String.format(
      "select from %s where %s=:%s",
      DB_CLASS, P_COMPONENT_ID, P_COMPONENT_ID);

  private static final String FIND_BY_ASSET = String.format(
      "select from %s where %s=:%s limit 1",
      DB_CLASS, P_ASSET_ID, P_ASSET_ID);

  private static final String DELETE_BY_REPOSITORY = String.format(
      "delete from %s where %s=:%s limit :limit",
      DB_CLASS, P_REPOSITORY_NAME, P_REPOSITORY_NAME);

  private final ComponentEntityAdapter componentEntityAdapter;

  private final AssetEntityAdapter assetEntityAdapter;

  private final int timeoutMillis;

  @Inject
  public BrowseNodeEntityAdapter(final ComponentEntityAdapter componentEntityAdapter,
                                 final AssetEntityAdapter assetEntityAdapter,
                                 final BrowseNodeConfiguration configuration)
  {
    super(DB_CLASS);
    this.assetEntityAdapter = checkNotNull(assetEntityAdapter);
    this.componentEntityAdapter = checkNotNull(componentEntityAdapter);
    this.timeoutMillis = configuration.getQueryTimeout().toMillisI();
  }

  @Override
  protected void defineType(final OClass type) {
    type.createProperty(P_REPOSITORY_NAME, OType.STRING).setMandatory(true).setNotNull(true);
    type.createProperty(P_PARENT_PATH, OType.STRING).setMandatory(true).setNotNull(true);
    type.createProperty(P_NAME, OType.STRING).setMandatory(true).setNotNull(true);
    type.createProperty(P_COMPONENT_ID, OType.LINK, componentEntityAdapter.getSchemaType());
    type.createProperty(P_ASSET_ID, OType.LINK, assetEntityAdapter.getSchemaType());
    type.createProperty(P_ASSET_NAME_LOWERCASE, OType.STRING);
  }

  @Override
  protected void defineType(final ODatabaseDocumentTx db, final OClass type) {
    defineType(type);

    // primary index that guarantees path uniqueness for nodes in a given repository
    type.createIndex(I_REPOSITORY_NAME_PARENT_PATH_NAME, INDEX_TYPE.UNIQUE, P_REPOSITORY_NAME, P_PARENT_PATH, P_NAME);

    // save space and ignore nulls because we'll never query on a null component/asset id
    ODocument ignoreNullValues = db.newInstance().field("ignoreNullValues", true);
    type.createIndex(I_COMPONENT_ID, INDEX_TYPE.NOTUNIQUE.name(), null, ignoreNullValues, new String[] { P_COMPONENT_ID });
    type.createIndex(I_ASSET_ID, INDEX_TYPE.UNIQUE.name(), null, ignoreNullValues, new String[] { P_ASSET_ID });
  }

  @Override
  protected BrowseNode newEntity() {
    return new BrowseNode();
  }

  @Override
  protected void readFields(final ODocument document, final BrowseNode entity) throws Exception {
    String repositoryName = document.field(P_REPOSITORY_NAME, OType.STRING);
    String parentPath = document.field(P_PARENT_PATH, OType.STRING);
    String name = document.field(P_NAME, OType.STRING);

    entity.setRepositoryName(repositoryName);
    entity.setParentPath(parentPath);
    entity.setName(name);

    ORID componentId = document.field(P_COMPONENT_ID, ORID.class);
    if (componentId != null) {
      entity.setComponentId(new AttachedEntityId(componentEntityAdapter, componentId));
    }

    ORID assetId = document.field(P_ASSET_ID, ORID.class);
    if (assetId != null) {
      entity.setAssetId(new AttachedEntityId(assetEntityAdapter, assetId));
      String assetNameLowercase = document.field(P_ASSET_NAME_LOWERCASE, OType.STRING);
      entity.setAssetNameLowercase(assetNameLowercase);
    }
  }

  @Override
  protected void writeFields(final ODocument document, final BrowseNode entity) throws Exception {
    document.field(P_REPOSITORY_NAME, entity.getRepositoryName());
    document.field(P_PARENT_PATH, entity.getParentPath());
    document.field(P_NAME, entity.getName());

    if (entity.getComponentId() != null) {
      document.field(P_COMPONENT_ID, componentEntityAdapter.recordIdentity(entity.getComponentId()));
    }

    if (entity.getAssetId() != null) {
      document.field(P_ASSET_ID, assetEntityAdapter.recordIdentity(entity.getAssetId()));
      document.field(P_ASSET_NAME_LOWERCASE, entity.getAssetNameLowercase());
    }
  }

  /**
   * Associates a {@link BrowseNode} with the given {@link Component}.
   */
  public void createComponentNode(final ODatabaseDocumentTx db,
                                  final String repositoryName,
                                  final List<String> path,
                                  final Component component)
  {
    BrowseNode node = newNode(repositoryName, path);
    ODocument document = findNodeRecord(db, node);
    if (document == null) {
      // complete the new entity before persisting
      node.setComponentId(EntityHelper.id(component));
      addEntity(db, node);
    }
    else {
      ORID oldComponentId = document.field(P_COMPONENT_ID, ORID.class);
      ORID newComponentId = componentEntityAdapter.recordIdentity(component);
      if (oldComponentId == null) {
        // shortcut: merge new information directly into existing record
        document.field(P_COMPONENT_ID, newComponentId);
        document.save();
      }
      else if (!oldComponentId.equals(newComponentId)) {
        // retry in case this is due to an out-of-order delete event
        throw new BrowseNodeCollisionException("Node already has a component");
      }
    }
  }

  /**
   * Associates a {@link BrowseNode} with the given {@link Asset}.
   */
  public void createAssetNode(final ODatabaseDocumentTx db,
                              final String repositoryName,
                              final List<String> path,
                              final Asset asset)
  {
    BrowseNode node = newNode(repositoryName, path);
    ODocument document = findNodeRecord(db, node);
    if (document == null) {
      // complete the new entity before persisting
      node.setAssetId(EntityHelper.id(asset));
      node.setAssetNameLowercase(lower(asset.name()));
      addEntity(db, node);
    }
    else {
      ORID oldAssetId = document.field(P_ASSET_ID, ORID.class);
      ORID newAssetId = assetEntityAdapter.recordIdentity(asset);
      if (oldAssetId == null) {
        // shortcut: merge new information directly into existing record
        document.field(P_ASSET_ID, newAssetId);
        document.field(P_ASSET_NAME_LOWERCASE, lower(asset.name()));
        document.save();
      }
      else if (!oldAssetId.equals(newAssetId)) {
        // retry in case this is due to an out-of-order delete event
        throw new BrowseNodeCollisionException("Node already has an asset");
      }
    }
  }

  /**
   * Creates a basic {@link BrowseNode} for the given repository and path.
   */
  private static BrowseNode newNode(final String repositoryName, final List<String> path) {
    BrowseNode node = new BrowseNode();
    node.setRepositoryName(repositoryName);
    node.setParentPath(joinPath(path.subList(0, path.size() - 1)));
    node.setName(path.get(path.size() - 1));
    return node;
  }

  /**
   * Returns the {@link BrowseNode} with the same coordinates as the sample node; {@code null} if no such node exists.
   */
  @Nullable
  private static ODocument findNodeRecord(final ODatabaseDocumentTx db, BrowseNode node) {
    return getFirst(
        db.command(new OCommandSQL(FIND_BY_PATH)).execute(
            ImmutableMap.of(
                P_REPOSITORY_NAME, node.getRepositoryName(),
                P_PARENT_PATH, node.getParentPath(),
                P_NAME, node.getName())),
        null);
  }

  /**
   * Removes any {@link BrowseNode}s associated with the given component id.
   */
  public void deleteComponentNode(final ODatabaseDocumentTx db, final EntityId componentId) {
    // some formats have the same component appearing on different branches of the tree
    Iterable<ODocument> documents =
        db.command(new OCommandSQL(FIND_BY_COMPONENT)).execute(
            ImmutableMap.of(P_COMPONENT_ID, recordIdentity(componentId)));

    documents.forEach(document -> {
      if (document.containsField(P_ASSET_ID)) {
        // asset still exists, just remove component details
        document.removeField(P_COMPONENT_ID);
        document.save();
      }
      else {
        document.delete();
      }
    });
  }

  /**
   * Removes the {@link BrowseNode} associated with the given asset id.
   */
  public void deleteAssetNode(final ODatabaseDocumentTx db, final EntityId assetId) {
    // a given asset will only appear once in the tree
    ODocument document = getFirst(
        db.command(new OCommandSQL(FIND_BY_ASSET)).execute(
            ImmutableMap.of(P_ASSET_ID, recordIdentity(assetId))), null);

    if (document != null) {
      if (document.containsField(P_COMPONENT_ID)) {
        // component still exists, just remove asset details
        document.removeField(P_ASSET_ID);
        document.removeField(P_ASSET_NAME_LOWERCASE);
        document.save();
      }
      else {
        document.delete();
      }
    }
  }

  /**
   * Removes a number of {@link BrowseNode}s belonging to the given repository (so we can batch the deletes).
   */
  public int deleteByRepository(final ODatabaseDocumentTx db, final String repositoryName, final int limit) {
    return db.command(new OCommandSQL(DELETE_BY_REPOSITORY)).execute(
        ImmutableMap.of(P_REPOSITORY_NAME, repositoryName, "limit", limit));
  }

  /**
   * Returns the {@link BrowseNode}s directly visible under the given path, according to the given asset filter.
   */
  public List<BrowseNode> getByPath(final ODatabaseDocumentTx db,
                                    final String repositoryName,
                                    final List<String> path,
                                    final int maxNodes,
                                    final String assetFilter,
                                    final Map<String, Object> filterParameters)
  {
    // timeout function which helps avoid runaway subtree queries when filtering assets
    UnaryOperator<OCommandSQL> timeoutFunction = configureTimeoutFunction(assetFilter);

    List<BrowseNode> listing = new ArrayList<>();

    Map<String, Object> parameters = new HashMap<>(filterParameters);
    parameters.put(P_REPOSITORY_NAME, repositoryName);

    // STEP 1: make a note of any direct child nodes with visible assets (or no asset)

    OCommandSQL sql = buildQuery(FIND_CHILDREN, true, assetFilter, maxNodes);

    String basePath = joinPath(path);
    parameters.put(BASE_PATH, basePath);

    Map<String, BrowseNode> children = new HashMap<>();
    transform(db.command(sql).execute(parameters)).forEach(child -> {
      children.put(child.getName(), child);
    });

    // STEP 2: search for the first subtree with at least one (indirect) visible asset

    sql = buildQuery(FIND_FIRST_SUBTREE, false, assetFilter, 1);

    // subtree nodes have paths greater than '/org/foo/base/' and less than '/org/foo/base0'
    String baseBoundary = basePath.substring(0, basePath.length() - 1) + '0';
    parameters.put(BASE_BOUNDARY, baseBoundary);

    List<ODocument> subtree = db.command(timeoutFunction.apply(sql)).execute(parameters);

    sql = buildQuery(FIND_NEXT_SUBTREE, false, assetFilter, 1);

    while (!subtree.isEmpty() && listing.size() < maxNodes) {

      // STEP 3: build node from subtree path, using direct child nodes to fill in details

      // extract the name of the child folder directly under the base path
      String childName = childName(basePath, subtree.get(0).field(P_PARENT_PATH));

      // use direct child node if available, as it has the component/asset detail
      BrowseNode child = children.remove(childName);
      if (child == null) {
        // otherwise create a placeholder/virtual node that leads to the subtree
        child = new BrowseNode();
        child.setRepositoryName(repositoryName);
        child.setParentPath(basePath);
        child.setName(childName);
      }
      listing.add(child);

      // STEP 4: move on to the next subtree with at least one (indirect) visible asset

      // jump past the current subtree, for example if the last subtree was '/org/foo/base/wibble/'
      // then we kick-off the next search with any paths greater or equal to '/org/foo/base/wibble0'

      String subtreeBoundary = basePath + childName + '0';
      parameters.put(SUBTREE_BOUNDARY, subtreeBoundary);

      subtree = db.command(timeoutFunction.apply(sql)).execute(parameters);
    }

    // STEP 5: add any leftover direct child nodes with visible assets, and mark them as leaves

    for (BrowseNode child : children.values()) {
      if (child.getAssetId() != null) {
        child.setLeaf(true); // we know this is a leaf because we didn't find a matching subtree
        listing.add(child);
      }
    }

    return listing;
  }

  /**
   * Function that applies a gradually reducing timeout across successive queries until a deadline.
   */
  private UnaryOperator<OCommandSQL> configureTimeoutFunction(final String assetFilter) {
    // only apply if we're filtering assets as that's when subtree queries could take a while
    if (timeoutMillis > 0 && !assetFilter.isEmpty()) {
      long deadlineMillis = System.currentTimeMillis() + timeoutMillis;
      return sql -> {
        long remainingMillis = Math.max(1, deadlineMillis - System.currentTimeMillis());
        return new OCommandSQL(sql.getText() + " timeout " + remainingMillis + " return");
      };
    }
    return UnaryOperator.identity(); // otherwise apply no timeout
  }

  /**
   * Joins segments into a path which always starts and ends with a single slash.
   */
  private static String joinPath(final List<String> path) {
    StringBuilder buf = new StringBuilder("/");
    path.forEach(s -> buf.append(s).append('/'));
    return buf.toString();
  }

  /**
   * Extracts the name of the folder directly after the base path.
   *
   * Assumes basePath is a prefix of path, and basePath ends in a slash.
   */
  private static String childName(final String basePath, final String path) {
    return path.substring(basePath.length(), path.indexOf('/', basePath.length()));
  }

  /**
   * Builds a visible node query from the primary select clause, optional asset filter, and limit.
   *
   * Optionally include nodes which don't have assets (regardless of the filter) to allow their
   * component details to be used in the final listing when they overlap with visible subtrees.
   */
  private static OCommandSQL buildQuery(final String select,
                                        final boolean includeNonAssetNodes,
                                        final String assetFilter,
                                        final int limit)
  {
    StringBuilder buf = new StringBuilder(select);

    if (!assetFilter.isEmpty()) {
      buf.append(" and (").append(P_ASSET_ID);
      if (includeNonAssetNodes) {
        buf.append(" is null or ");
      }
      else {
        buf.append(" is not null and ");
      }
      buf.append(assetFilter).append(')');
    }

    buf.append(" limit ").append(limit);

    return new OCommandSQL(buf.toString());
  }

  /**
   * Enables deconfliction of browse nodes.
   */
  @Override
  public boolean resolveConflicts() {
    return true;
  }
}
