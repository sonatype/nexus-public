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
package org.sonatype.nexus.repository.browse.internal.orient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.packageurl.PackageUrl;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.AttachedEntityId;
import org.sonatype.nexus.orient.entity.IterableEntityAdapter;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.ossindex.PackageUrlService;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;

import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.OCompositeKey;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.StringUtils.appendIfMissing;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;

/**
 * {@link BrowseNode} entity-adapter.
 *
 * @since 3.6
 */
@Named
@Singleton
public class BrowseNodeEntityAdapter
    extends IterableEntityAdapter<OrientBrowseNode>
{
  private static final String DB_CLASS = new OClassNameBuilder().type("browse_node").build();

  public static final String P_REPOSITORY_NAME = "repository_name";

  public static final String P_FORMAT = "format";

  public static final String P_PATH = "path";

  public static final String P_PARENT_PATH = "parent_path";

  public static final String P_NAME = "name";

  public static final String P_PACKAGE_URL = "package_url";

  public static final String P_COMPONENT_ID = "component_id";

  public static final String P_ASSET_ID = "asset_id";

  public static final String AUTHZ_REPOSITORY_NAME = "authz_repository_name";

  private static final String BASE_PATH = "base_path";

  private static final String LIMIT = "limit";

  private static final String KEY = "key";

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

  private static final String FIND_BY_PARENT_PATH = String.format(
      "select expand(rid) from index:%s where key=:%s limit 1",
      I_REPOSITORY_NAME_PARENT_PATH_NAME, KEY);

  private static final String FIND_CHILDREN = String.format(
      "select from %s where (%s=:%s and %s=:%s)",
      DB_CLASS, P_REPOSITORY_NAME, P_REPOSITORY_NAME, P_PARENT_PATH, BASE_PATH);

  private static final String CHILD_COUNT = String.format(
      "select rid from `index:%s` where key=:%s limit :%s",
      I_REPOSITORY_NAME_PARENT_PATH_NAME, KEY, LIMIT);

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

  private final PackageUrlService packageUrlService;

  @Inject
  public BrowseNodeEntityAdapter(final ComponentEntityAdapter componentEntityAdapter,
                                 final AssetEntityAdapter assetEntityAdapter,
                                 final PackageUrlService packageUrlService)
  {
    super(DB_CLASS);
    this.assetEntityAdapter = checkNotNull(assetEntityAdapter);
    this.componentEntityAdapter = checkNotNull(componentEntityAdapter);
    this.packageUrlService = checkNotNull(packageUrlService);
  }

  @Override
  protected void defineType(final OClass type) {
    type.createProperty(P_REPOSITORY_NAME, OType.STRING).setMandatory(true).setNotNull(true);
    type.createProperty(P_FORMAT, OType.STRING).setMandatory(true).setNotNull(true);
    type.createProperty(P_PATH, OType.STRING).setMandatory(true).setNotNull(true);
    type.createProperty(P_PARENT_PATH, OType.STRING).setMandatory(true).setNotNull(true);
    type.createProperty(P_NAME, OType.STRING).setMandatory(true).setNotNull(true);
    type.createProperty(P_COMPONENT_ID, OType.LINK, componentEntityAdapter.getSchemaType());
    type.createProperty(P_ASSET_ID, OType.LINK, assetEntityAdapter.getSchemaType());
    type.createProperty(P_PACKAGE_URL, OType.STRING);
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
  protected OrientBrowseNode newEntity() {
    return new OrientBrowseNode();
  }

  @Override
  protected void readFields(final ODocument document, final OrientBrowseNode entity) {
    String repositoryName = document.field(P_REPOSITORY_NAME, OType.STRING);
    String format = document.field(P_FORMAT, OType.STRING);
    String path = document.field(P_PATH, OType.STRING);
    String parentPath = document.field(P_PARENT_PATH, OType.STRING);
    String name = document.field(P_NAME, OType.STRING);
    String packageUrl = document.field(P_PACKAGE_URL, OType.STRING);

    entity.setRepositoryName(repositoryName);
    entity.setFormat(format);
    entity.setPath(path);
    entity.setParentPath(parentPath);
    entity.setName(name);
    entity.setPackageUrl(packageUrl);

    ORID componentId = document.field(P_COMPONENT_ID, ORID.class);
    if (componentId != null) {
      entity.setComponentId(new AttachedEntityId(componentEntityAdapter, componentId));
    }

    ORID assetId = document.field(P_ASSET_ID, ORID.class);
    if (assetId != null) {
      entity.setAssetId(new AttachedEntityId(assetEntityAdapter, assetId));
    }
  }

  @Override
  protected void writeFields(final ODocument document, final OrientBrowseNode entity) throws Exception {
    document.field(P_REPOSITORY_NAME, entity.getRepositoryName());
    document.field(P_FORMAT, entity.getFormat());
    document.field(P_PATH, entity.getPath());
    document.field(P_PARENT_PATH, entity.getParentPath());
    document.field(P_NAME, entity.getName());

    if (entity.getComponentId() != null) {
      document.field(P_PACKAGE_URL, entity.getPackageUrl());
      document.field(P_COMPONENT_ID, componentEntityAdapter.recordIdentity(entity.getComponentId()));
    }

    if (entity.getAssetId() != null) {
      document.field(P_ASSET_ID, assetEntityAdapter.recordIdentity(entity.getAssetId()));
    }
  }

  /**
   * Associates a {@link OrientBrowseNode} with the given {@link Component}.
   */
  public void createComponentNode(final ODatabaseDocumentTx db,
                                  final String repositoryName,
                                  final String format,
                                  final List<? extends BrowsePath> paths,
                                  final Component component)
  {
    //create any parent folder nodes for this component if not already existing
    maybeCreateParentNodes(db, repositoryName, format, paths.subList(0, paths.size() - 1));

    //now create the component node
    OrientBrowseNode node = newNode(repositoryName, format, paths);
    ODocument document = findNodeRecord(db, node);
    if (document == null) {
      // complete the new entity before persisting
      EntityId componentId = EntityHelper.id(component);
      String packageUrl = packageUrlService
          .getPackageUrl(component.format(), component.group(), component.name(), component.version())
          .map(PackageUrl::toString)
          .orElse(null);
      node.setPackageUrl(packageUrl);
      node.setComponentId(componentId);
      addEntity(db, node);
    }
    else {
      ORID oldComponentId = document.field(P_COMPONENT_ID, ORID.class);
      ORID newComponentId = componentEntityAdapter.recordIdentity(component);
      String packageUrl = packageUrlService
          .getPackageUrl(component.format(), component.group(), component.name(), component.version())
          .map(PackageUrl::toString)
          .orElse(null);
      if (packageUrl != null || oldComponentId == null) {
        // shortcut: merge new information directly into existing record
        document.field(P_COMPONENT_ID, newComponentId);
        document.field(P_PACKAGE_URL, packageUrl);
        document.save();
      }
      else if (!oldComponentId.equals(newComponentId)) {
        // retry in case this is due to an out-of-order delete event
        throw new BrowseNodeCollisionException("Node already has a component");
      }
    }
  }

  /**
   * Associates a {@link OrientBrowseNode} with the given {@link Asset}.
   */
  public void createAssetNode(final ODatabaseDocumentTx db,
                              final String repositoryName,
                              final String format,
                              final List<? extends BrowsePath> paths,
                              final Asset asset)
  {
    //create any parent folder nodes for this asset if not already existing
    maybeCreateParentNodes(db, repositoryName, format, paths.subList(0, paths.size() - 1));

    //now create the asset node
    OrientBrowseNode node = newNode(repositoryName, format, paths);
    ODocument document = findNodeRecord(db, node);
    if (document == null) {
      // complete the new entity before persisting
      node.setAssetId(EntityHelper.id(asset));
      addEntity(db, node);
    }
    else {
      ORID oldAssetId = document.field(P_ASSET_ID, ORID.class);
      ORID newAssetId = assetEntityAdapter.recordIdentity(asset);
      if (oldAssetId == null) {
        // shortcut: merge new information directly into existing record
        document.field(P_ASSET_ID, newAssetId);
        String path = document.field(P_PATH, OType.STRING);

        //if this node is now an asset, we don't want a trailing slash
        if (!asset.name().endsWith("/") && path.endsWith("/")) {
          path = path.substring(0, path.length() - 1);
          document.field(P_PATH, path);
        }
        document.save();
      }
      else if (!oldAssetId.equals(newAssetId)) {
        // retry in case this is due to an out-of-order delete event
        throw new BrowseNodeCollisionException("Node already has an asset");
      }
    }
  }

  /**
   * Iterate over the list of path strings, and create a browse node for each one if not already there.
   */
  private void maybeCreateParentNodes(final ODatabaseDocumentTx db,
                                      final String repositoryName,
                                      final String format,
                                      final List<? extends BrowsePath> paths)
  {
    for (int i = paths.size() ; i > 0 ; i--) {
      OrientBrowseNode parentNode = newNode(repositoryName, format, paths.subList(0, i));
      if (!parentNode.getPath().endsWith("/")) {
        parentNode.setPath(parentNode.getPath() + "/");
      }
      ODocument document = findNodeRecord(db, parentNode);
      if (document == null) {
        addEntity(db, parentNode);
      }
      else {
        //if the parent exists, but doesn't have proper folder path (ending with "/") change it
        //this would typically only happen with nested assets
        if (!document.field(P_PATH).toString().endsWith("/")) {
          document.field(P_PATH, document.field(P_PATH) + "/");
          document.save();
        }
        break;
      }
    }
  }

  /**
   * Creates a basic {@link BrowseNode} for the given repository and path.
   */
  private static OrientBrowseNode newNode(
      final String repositoryName,
      final String format,
      final List<? extends BrowsePath> paths)
  {
    OrientBrowseNode node = new OrientBrowseNode();
    node.setRepositoryName(repositoryName);
    node.setFormat(format);
    node.setPaths(paths);
    return node;
  }

  /**
   * Returns the {@link BrowseNode} with the same coordinates as the sample node; {@code null} if no such node exists.
   */
  @Nullable
  private static ODocument findNodeRecord(final ODatabaseDocumentTx db, final OrientBrowseNode node) {
    return getFirst(
        db.command(new OCommandSQL(FIND_BY_PARENT_PATH)).execute(
            ImmutableMap.of(KEY, indexKey(node.getRepositoryName(), node.getParentPath(), node.getName()))),
        null);
  }

  private static OCompositeKey indexKey(final String... keys) {
    return new OCompositeKey((Object[]) keys);
  }

  /**
   * Returns true if an asset node for an asset with the given assetId exists.
   */
  public boolean assetNodeExists(final ODatabaseDocumentTx db, final EntityId assetId) {
    Iterable<ODocument> documents =
        db.command(new OCommandSQL(FIND_BY_ASSET)).execute(
            ImmutableMap.of(P_ASSET_ID, recordIdentity(assetId)));
    return !isEmpty(documents);
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
        maybeDeleteParents(db, document.field(P_REPOSITORY_NAME), document.field(P_PARENT_PATH));
        document.delete();
      }
    });
  }

  /**
   * Removes the {@link BrowseNode} associated with the given asset id
   */
  public void deleteAssetNode(final ODatabaseDocumentTx db, final EntityId assetId) {
    // a given asset will only appear once in the tree
    ODocument document = getFirst(
        db.command(new OCommandSQL(FIND_BY_ASSET)).execute(
            ImmutableMap.of(P_ASSET_ID, recordIdentity(assetId))), null);


    if (document != null) {
      String repository = document.field(P_REPOSITORY_NAME);
      String nodePath = appendIfMissing(prependIfMissing(document.field(P_PATH), "/"), "/");
      if (document.containsField(P_COMPONENT_ID) || !hasNoChildren(db, repository, nodePath)) {
        // component still exists, just remove asset details
        // OR there are child nodes, need to keep node to allow their access.
        document.removeField(P_ASSET_ID);
        document.save();
      }
      else {
        maybeDeleteParents(db, repository, document.field(P_PARENT_PATH));
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
  public List<OrientBrowseNode> getByPath(
      final ODatabaseDocumentTx db,
      final String repositoryName,
      final List<String> path,
      final int maxNodes,
      final String assetFilter,
      final Map<String, Object> filterParameters)
  {
    Map<String, Object> parameters = new HashMap<>(filterParameters);
    parameters.put(P_REPOSITORY_NAME, repositoryName);

    // STEP 1: make a note of any direct child nodes that are visible
    OCommandSQL sql = buildQuery(FIND_CHILDREN, assetFilter, maxNodes);

    String basePath = joinPath(path);
    parameters.put(BASE_PATH, basePath);

    List<OrientBrowseNode> children = newArrayList(transform(db.command(sql).execute(parameters)));

    children.forEach(child -> {
      // STEP 2: check if the child has any children of its own, if not, it's a leaf
      if (childCountEqualTo(db, repositoryName, child.getParentPath() + child.getName() + "/", 0)) {
        child.setLeaf(true);
      }
    });

    return children;
  }

  /**
   * remove any parent nodes that only contain 1 child, and if not an asset/component node of course
   */
  private void maybeDeleteParents(final ODatabaseDocumentTx db, final String repositoryName, final String parentPath) {
    //count of 1 meaning the node we are currently deleting
    if (!"/".equals(parentPath) && childCountEqualTo(db, repositoryName, parentPath, 1)) {
      ODocument parent = getFirst(db.command(new OCommandSQL(FIND_BY_PARENT_PATH)).execute(ImmutableMap.of(
              KEY, indexKey(repositoryName, previousParentPath(parentPath), previousParentName(parentPath)))),
          null);

      if (parent != null && parent.field(P_COMPONENT_ID) == null && parent.field(P_ASSET_ID) == null) {
        maybeDeleteParents(db, repositoryName, parent.field(P_PARENT_PATH));
        parent.delete();
      }
    }
  }

  /**
   * take a string path and return the string at the previous level, i.e. "/foo/bar/com/" -> "/foo/bar/"
   */
  private String previousParentPath(final String parentPath) {
    //parentPath always ends with slash, pull it out for this check
    String withoutSlash = parentPath.substring(0, parentPath.length() - 1);
    //make sure to include the slash
    return withoutSlash.substring(0, withoutSlash.lastIndexOf('/') + 1);
  }

  /**
   * take a string path and return the string of the last segment, i.e. "/foo/bar/com/" -> "com"
   */
  private String previousParentName(final String parentPath) {
    //parentPath always ends with slash, pull it out for this check
    String withoutSlash = parentPath.substring(0, parentPath.length() - 1);

    return withoutSlash.substring(withoutSlash.lastIndexOf('/') + 1);
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
   * Builds a visible node query from the primary select clause, optional asset filter, and limit.
   *
   * Optionally include nodes which don't have assets (regardless of the filter) to allow their
   * component details to be used in the final listing when they overlap with visible subtrees.
   */
  private OCommandSQL buildQuery(final String select,
                                        final String assetFilter,
                                        final int limit)
  {
    StringBuilder buf = new StringBuilder(select);

    if (!assetFilter.isEmpty()) {
      buf.append(" and (").append(assetFilter).append(')');
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


  private boolean hasNoChildren(final ODatabaseDocumentTx db, final String repo, final String path){
    return childCountEqualTo(db, repo, path, 0);
  }

  private boolean childCountEqualTo(final ODatabaseDocumentTx db,
                                    final String repositoryName, final String basePath, final long expected)
  {
    List<ODocument> docs = db.command(new OCommandSQL(CHILD_COUNT)).execute(
        ImmutableMap.of(KEY, indexKey(repositoryName, basePath), LIMIT, expected + 1));
    return docs.size() == expected;
  }
}
