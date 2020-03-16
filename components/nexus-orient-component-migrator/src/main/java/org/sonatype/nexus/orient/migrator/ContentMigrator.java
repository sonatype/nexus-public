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
package org.sonatype.nexus.orient.migrator;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.orientechnologies.orient.core.storage.impl.local.OAbstractPaginatedStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Strings.nullToEmpty;
import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.sql.Types.NULL;
import static java.util.Collections.nCopies;
import static java.util.UUID.nameUUIDFromBytes;

/**
 * Migrates component/asset content metadata from OrientDB to new-DB.
 *
 * @since 3.20
 */
public class ContentMigrator
{
  private static final Logger log = LoggerFactory.getLogger(ContentMigrator.class);

  private static final int BATCH_SIZE = 100_000;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final ODatabaseDocumentTx componentDb;

  private final Connection targetConnection;

  private final boolean extractBrowseNodes;

  private final Set<String> formats = new HashSet<>();

  private final Map<String, String> repositoryFormats = new HashMap<>();

  private final Map<ORID, Integer> bucketRepositoryIds = new HashMap<>();

  private final Map<String, ORID> repositoryNameToId = new HashMap<>();

  private final String databaseId;

  public ContentMigrator(final ODatabaseDocumentTx componentDb,
                         final Connection targetConnection, final boolean extractBrowseNodes) throws SQLException
  {
    this.componentDb = componentDb;
    this.targetConnection = targetConnection;
    this.extractBrowseNodes = extractBrowseNodes;

    databaseId = targetConnection.getMetaData().getDatabaseProductName();
  }

  public void extractAll() throws SQLException, IOException { // NOSONAR
    extractRepositories();
    extractComponents();
    extractAssets();
    if (extractBrowseNodes) {
      extractBrowseNodes();
    }
    cleanup();
  }

  /**
   * Migrates data into the relevant {@code content_repository} tables.
   */
  public void extractRepositories() throws SQLException, IOException { // NOSONAR

    for (ODocument bucket : componentDb.browseClass("bucket")) {
      String repositoryName = bucket.field("repository_name", OType.STRING);

      repositoryNameToId.put(repositoryName, bucket.getIdentity());

      ODocument result = new OSQLSynchQuery<ODocument>(
          "SELECT format FROM asset WHERE bucket = :bucket LIMIT 1").runFirst(bucket);

      if (result != null) {
        String format = result.field("format", OType.STRING);
        repositoryFormats.put(repositoryName, normalizeFormat(format));
      }
    }

    formats.addAll(repositoryFormats.values());

    createFormatSchema();

    try {
      addRidColumns("content_repository");

      Map<String, PreparedStatement> repositoryStatements = prepareBatch(
          "content_repository", "config_repository_id", "attributes", "rid");

      try {
        int repositoryCount = 0;
        for (ODocument bucket : componentDb.browseClass("bucket")) {
          String repositoryName = bucket.field("repository_name", OType.STRING);
          Map<?, ?> attributes = bucket.field("attributes", OType.EMBEDDEDMAP);

          UUID configRepositoryId = nameUUIDFromBytes(repositoryName.getBytes(UTF_8));
          String format = repositoryFormat(repositoryName);
          if (format != null) {
            insert(repositoryStatements.get(format), configRepositoryId, json(attributes), bucket);
            ++repositoryCount;
          }
          else {
            log.info("Skipping '{}' because it has no content", repositoryName);
          }
        }

        executeBatch(repositoryStatements);

        log.info("Migrating {} repositories spanning {} formats", repositoryCount, formats);

        extractBucketRepositoryIds();
      }
      finally {
        closeBatch(repositoryStatements);
      }
    }
    finally {
      dropRidColumns("content_repository");
    }
  }

  /**
   * Migrates data into the relevant {@code component} tables.
   */
  public void extractComponents() throws SQLException, IOException { // NOSONAR

    addRidColumns("component");

    Map<String, PreparedStatement> componentStatements = prepareBatch(
        "component", "repository_id", "namespace", "name", "version", "attributes", "rid");

    try {
      int componentCount = 0;
      long expectedComponentCount = componentDb.countClass("component");
      for (ODocument component : componentDb.browseClass("component")) {

        OIdentifiable bucket = component.field("bucket", OType.LINK);
        String format = normalizeFormat(component.field("format", OType.STRING));
        Map<?, ?> attributes = component.field("attributes", OType.EMBEDDEDMAP);
        String group = component.field("group", OType.STRING);
        String name = component.field("name", OType.STRING);
        String version = component.field("version", OType.STRING);

        insert(componentStatements.get(format), bucketRepositoryIds.get(bucket.getIdentity()),
            nullToEmpty(group), name, nullToEmpty(version), json(attributes), component);

        if (++componentCount % BATCH_SIZE == 0) {
          executeBatch(componentStatements);

          log.info("...migrating {}/{} components", componentCount, expectedComponentCount);

          // force clear as we won't revisit these records and it keeps memory use down
          ((OAbstractPaginatedStorage) componentDb.getStorage()).getReadCache().clear();
        }
      }

      executeBatch(componentStatements);

      log.info("Migrated {} components", componentCount);
    }
    finally {
      closeBatch(componentStatements);
    }
  }

  /**
   * Migrates data into the relevant {@code asset} tables.
   */
  public void extractAssets() throws SQLException, IOException { // NOSONAR

    addRidColumns("asset");
    addColumn("asset", "component_rid");

    Map<String, PreparedStatement> assetStatements = prepareBatch(
        "asset", "repository_id", "component_id", "asset_blob_id", "path", "last_downloaded", "attributes", "rid", "component_rid");

    Map<String, PreparedStatement> assetBlobStatements = prepareBatch(
        "asset_blob", "blob_ref", "blob_size", "content_type", "blob_created", "created_by", "created_by_ip");

    try {
      int assetCount = 0;
      Map<String, Integer> blobCountsPerFormat = new HashMap<>();
      long expectedAssetCount = componentDb.countClass("asset");
      for (ODocument asset : componentDb.browseClass("asset")) {

        OIdentifiable bucket = asset.field("bucket", OType.LINK);
        String format = normalizeFormat(asset.field("format", OType.STRING));
        Map<?, ?> attributes = asset.field("attributes", OType.EMBEDDEDMAP);
        OIdentifiable component = asset.field("component", OType.LINK);
        String name = asset.field("name", OType.STRING);
        long size = asset.field("size", OType.LONG);
        String contentType = asset.field("content_type", OType.STRING);
        String blobRef = asset.field("blob_ref", OType.STRING);
        Date lastDownloaded = asset.field("last_downloaded", OType.DATETIME);
        Date blobCreated = asset.field("blob_created", OType.DATETIME);
        Date blobUpdated = asset.field("blob_updated", OType.DATETIME);
        String createdBy =  asset.field("created_by", OType.STRING);
        String createdByIp = asset.field("created_by_ip", OType.STRING);

        Integer assetBlobId = null;
        if (blobRef != null) {
          insert(assetBlobStatements.get(format), blobRef, size, contentType,
              blobUpdated != null ? blobUpdated : blobCreated, createdBy, createdByIp);

          assetBlobId = blobCountsPerFormat.merge(format, 1, Integer::sum);
        }

        insert(assetStatements.get(format), bucketRepositoryIds.get(bucket.getIdentity()),
            null, assetBlobId, normalizePath(name), lastDownloaded, json(attributes), asset, component);

        if (++assetCount % BATCH_SIZE == 0) {
          executeBatch(assetBlobStatements);
          executeBatch(assetStatements);

          log.info("...migrating {}/{} assets", assetCount, expectedAssetCount);

          // force clear as we won't revisit these records and it keeps memory use down
          ((OAbstractPaginatedStorage) componentDb.getStorage()).getReadCache().clear();
        }
      }

      executeBatch(assetBlobStatements);
      executeBatch(assetStatements);

      log.info("Migrated {} assets", assetCount);
    }
    finally {
      closeBatch(assetBlobStatements);
      closeBatch(assetStatements);
    }

    log.info("Linking assets to components (this may take a while)");

    linkAssetsToComponents();
  }

  public void extractBrowseNodes() throws SQLException {
    addColumn("browse_node", "asset_rid", true);
    addColumn("browse_node", "component_rid", true);
    // for matching parent_id
    addColumn("browse_node", "parent_path", false);
    addColumn("browse_node", "parent_parent_path", false);
    addColumn("browse_node", "parent_name", false);

    createBrowseNodeParentIndex();

    Map<String, PreparedStatement> browseNodeStatements = prepareBatch(
        "browse_node", "repository_id", "format", "path", "name", "asset_rid", "component_rid", "parent_path", "parent_parent_path", "parent_name");

    try {
      int browseNodeCount = 0;

      long expectedBrowseNodeCount = componentDb.countClass("browse_node");
      for (ODocument browseNode : componentDb.browseClass("browse_node")) {
        String format = normalizeFormat(browseNode.field("format", OType.STRING));
        String repositoryName = browseNode.field("repository_name", OType.STRING);

        if (repositoryName != null) {
          ORID rid = repositoryNameToId.get(repositoryName);
          int repositoryId = bucketRepositoryIds.get(rid);

          String path = browseNode.field("path", OType.STRING);
          String name = browseNode.field("name", OType.STRING);

          String parentPath = browseNode.field("parent_path", OType.STRING);

          String parentParentPath = null;
          String parentName = null;

          if (!parentPath.equals("/")) {
            // remove trailing slash
            String parentPathMatch = parentPath.substring(0, parentPath.length() - 1);

            int lastSlash = parentPathMatch.lastIndexOf('/');
            parentParentPath = parentPathMatch.substring(0, lastSlash + 1);
            parentName = parentPathMatch.substring(lastSlash + 1);
          }

          OIdentifiable asset = browseNode.field("asset_id", OType.LINK);
          OIdentifiable component = browseNode.field("component_id", OType.LINK);

          insert(browseNodeStatements.get(format), repositoryId, format, path, name, asset, component,
              parentPath, parentParentPath, parentName);

          if (++browseNodeCount % BATCH_SIZE == 0) {
            executeBatch(browseNodeStatements);

            log.info("...migrating {}/{} browse nodes", browseNodeCount, expectedBrowseNodeCount);

            // force clear as we won't revisit these records and it keeps memory use down
            ((OAbstractPaginatedStorage) componentDb.getStorage()).getReadCache().clear();
          }
        }
      }

      executeBatch(browseNodeStatements);

      log.info("Migrated {} browse nodes", browseNodeCount);
    }
    finally {
      closeBatch(browseNodeStatements);
    }

    log.info("Linking browse nodes to assets (this may take a while)");

    linkBrowseNodesToAssets();

    log.info("Linking browse nodes to components (this may take a while)");

    linkBrowseNodesToComponents();

    log.info("Linking browse nodes to their parents (this may take a while)");

    linkBrowseNodesToParent();

    dropBrowseNodeParentIndex();
  }

  private void createBrowseNodeParentIndex() throws SQLException {
    try (Statement stmt = targetConnection.createStatement()) {
      for (String format : formats) {
        String formatTable = format + '_' + "browse_node";
        stmt.addBatch("CREATE INDEX IF NOT EXISTS idx_" + formatTable + "_repository_id_parent_path_name ON " + formatTable + "(repository_id, parent_path, name);");
      }
      stmt.executeBatch();
    }
  }

  private void dropBrowseNodeParentIndex() throws SQLException {
    try (Statement stmt = targetConnection.createStatement()) {
      for (String format : formats) {
        String formatTable = format + '_' + "browse_node";
        stmt.addBatch("DROP INDEX IF EXISTS idx_" + formatTable + "_repository_id_parent_path_name;");
      }
      stmt.executeBatch();
    }
  }

  public void cleanup() throws SQLException {
    log.info("Removing temporary rid columns (this may take a while)");

    dropRidColumns("component");
    dropRidColumns("asset");

    if (extractBrowseNodes) {
      dropColumn("browse_node", "asset_rid");
      dropColumn("browse_node", "component_rid");
      dropColumn("browse_node", "parent_path");
      dropColumn("browse_node", "parent_parent_path");
      dropColumn("browse_node", "parent_name");
    }

    log.info("Done");
  }

  /**
   * Provides a canonical name for the given format.
   */
  private static String normalizeFormat(final String format) {
    return format.replace("maven2", "maven");
  }

  /**
   * Attempts to deduce the format for the given repository without needing access to the config database.
   */
  private String repositoryFormat(final String repositoryName) {
    String format = repositoryFormats.get(repositoryName);
    if (format == null) {
      // repository has no content indicating the format, so try to guess from repository name
      // (if we had access to the config database then we could deduce it from the repo recipe)
      format = formats.stream().filter(repositoryName::contains).findFirst().orElse(null);
    }
    return format;
  }

  /**
   * Naive mapping that just prepends a slash to the existing asset name to get its path.
   */
  private static String normalizePath(final String path) {
    if (path.isEmpty()) {
      return path;
    }
    return path.charAt(0) == '/' ? path : '/' + path;
  }

  /**
   * Encodes the given attribute map so it can be stored in the database.
   */
  private Object json(final Map<?, ?> attributes) throws IOException {
    byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(attributes);
    return "H2".equals(databaseId) ? bytes : new String(bytes, UTF_8);
  }

  /**
   * Creates the necessary tables, keys, and indexes for the given format.
   */
  private void createFormatSchema() throws SQLException, IOException { // NOSONAR
    String formatTemplate = Resources.toString(getClass().getResource("/format_template.sql"), UTF_8);
    if ("PostgreSQL".equals(databaseId)) {
      formatTemplate = formatTemplate.replace("JSON NOT NULL", "JSONB NOT NULL");
    }
    try (Statement stmt = targetConnection.createStatement()) {
      for (String format : formats) {
        stmt.executeUpdate(formatTemplate.replace("${format}", format));
      }
    }
  }

  /**
   * Adds a new column to record Orient record-ids in the migrated data.
   */
  private void addRidColumns(final String table) throws SQLException {
    addColumn(table, "rid");
  }

  /**
   * Adds a new varchar column with index.
   */
  private void addColumn(final String table, final String var) throws SQLException {
    addColumn(table, var, true);
  }

  /**
   * Adds a new varchar column with optional index.
   */
  private void addColumn(final String table, final String var, boolean createIndex) throws SQLException {
    try (Statement stmt = targetConnection.createStatement()) {
      for (String format : formats) {
        String formatTable = format + '_' + table;
        stmt.addBatch("ALTER TABLE " + formatTable + " ADD COLUMN IF NOT EXISTS " + var + " VARCHAR;");
        if (createIndex) {
          if ("PostgreSQL".equals(databaseId)) {
            stmt.addBatch(
                "CREATE INDEX IF NOT EXISTS idx_" + formatTable + "_" + var + " ON " + formatTable + " USING HASH (" +
                    var + ");");
          }
          else {
            stmt.addBatch(
                "CREATE INDEX IF NOT EXISTS idx_" + formatTable + "_" + var + " ON " + formatTable + "(" + var + ");");
          }
        }
      }
      stmt.executeBatch();
    }
  }

  /**
   * Removes Orient record-ids from the newly migrated data.
   */
  private void dropRidColumns(final String table) throws SQLException {
    dropColumn(table, "rid");
  }

  /**
   * Removes Orient record-ids from the newly migrated data.
   */
  private void dropColumn(final String table, final String var) throws SQLException {
    try (Statement stmt = targetConnection.createStatement()) {
      for (String format : formats) {
        String formatTable = format + '_' + table;
        stmt.addBatch("DROP INDEX IF EXISTS idx_" + formatTable + "_" + var + ";");
        stmt.addBatch("ALTER TABLE " + formatTable + " DROP COLUMN IF EXISTS " + var + ";");
      }
      stmt.executeBatch();
    }
  }

  /**
   * Extracts the mapping from the bucket record-id in Orient to the new {@code repository_id} sequence.
   */
  private void extractBucketRepositoryIds() throws SQLException {
    try (Statement stmt = targetConnection.createStatement()) {
      for (String format : formats) {
        try (ResultSet resultSet = stmt.executeQuery("SELECT rid, repository_id FROM " + format + "_content_repository;")) {
          while (resultSet.next()) {
            bucketRepositoryIds.put(new ORecordId(resultSet.getString(1)), resultSet.getInt(2));
          }
        }
      }
    }
  }

  /**
   * Populates {@code asset.component_id} by joining the asset and component tables on the original Orient record-id.
   */
  private void linkAssetsToComponents() throws SQLException {
    try (Statement stmt = targetConnection.createStatement()) {
      for (String format : formats) {
        if ("H2".equals(databaseId)) {
          stmt.addBatch("MERGE INTO " + format + "_asset AS A USING " + format + "_component AS C"
              + " ON A.component_rid = C.rid WHEN MATCHED THEN UPDATE SET A.component_id = C.component_id;");
        }
        else {
          stmt.addBatch("UPDATE " + format + "_asset AS A SET component_id = C.component_id FROM "
              + format + "_component AS C WHERE A.component_rid = C.rid;");
        }
      }
      stmt.executeBatch();
    }
  }

  /**
   * Populates {@code browse_node.asset_id} by joining the asset and browse_node tables on the original Orient record-id.
   */
  private void linkBrowseNodesToAssets() throws SQLException {
    try (Statement stmt = targetConnection.createStatement()) {
      for (String format : formats) {
        if ("H2".equals(databaseId)) {
          stmt.addBatch("MERGE INTO " + format + "_browse_node AS N USING " + format + "_asset AS A"
              + " ON N.asset_rid = A.rid WHEN MATCHED THEN UPDATE SET N.asset_id = A.asset_id;");
        }
        else {
          stmt.addBatch("UPDATE " + format + "_browse_node AS N SET asset_id = A.asset_id FROM "
              + format + "_asset AS A WHERE N.asset_rid = A.rid;");
        }
      }
      stmt.executeBatch();
    }
  }

  /**
   * Populates {@code browse_node.component_id} by joining the component and browse_node tables on the original Orient record-id.
   */
  private void linkBrowseNodesToComponents() throws SQLException {
    try (Statement stmt = targetConnection.createStatement()) {
      for (String format : formats) {
        if ("H2".equals(databaseId)) {
          stmt.addBatch("MERGE INTO " + format + "_browse_node AS N USING " + format + "_component AS C"
              + " ON N.asset_rid = C.rid WHEN MATCHED THEN UPDATE SET N.component_id = C.component_id;");
        }
        else {
          stmt.addBatch("UPDATE " + format + "_browse_node AS N SET component_id = C.component_id FROM "
              + format + "_component AS C WHERE N.component_rid = C.rid;");
        }
      }
      stmt.executeBatch();
    }
  }

  /**
   * Populates {@code browse_node.parent_id} by joining the browse_node table to it's parent using path and parent_path
   */
  private void linkBrowseNodesToParent() throws SQLException {
    try (Statement stmt = targetConnection.createStatement()) {
      for (String format : formats) {
        if ("H2".equals(databaseId)) {
          stmt.addBatch("UPDATE " + format + "_browse_node AS N SET parent_id = (SELECT P.browse_node_id FROM " + format
              + "_browse_node AS P WHERE N.repository_id = P.repository_id AND N.parent_parent_path = P.parent_path"
              + " AND N.parent_name = P.name);");
        }
        else {
          stmt.addBatch("UPDATE " + format + "_browse_node AS N set parent_id ="
              + " (SELECT browse_node_id FROM " + format + "_browse_node AS P WHERE"
              + " N.repository_id = P.repository_id AND N.parent_parent_path = P.parent_path AND N.parent_name = P.name)");
        }
      }
      stmt.executeBatch();
    }
  }

  /**
   * Prepares a new batch statement for inserting data into the given table.
   */
  private Map<String, PreparedStatement> prepareBatch(final String table, final String... parameterNames)
      throws SQLException
  {
    String tableLayout = new StringBuilder(table)
        .append(" (")
        .append(join(",", parameterNames))
        .append(") VALUES (")
        .append(join(",", nCopies(parameterNames.length, "?")))
        .append(");")
        .toString();

    Map<String, PreparedStatement> stmts = new HashMap<>();
    for (String format : formats) {
      stmts.put(format, targetConnection.prepareStatement("INSERT INTO " + format + '_' + tableLayout)); // NOSONAR
    }
    return stmts;
  }

  /**
   * Executes the given collection of batch statements.
   */
  private static void executeBatch(final Map<String, PreparedStatement> stmts) throws SQLException {
    for (PreparedStatement stmt : stmts.values()) {
      stmt.executeBatch();
    }
  }

  /**
   * Closes the given collection of batch statements.
   */
  private static void closeBatch(final Map<String, PreparedStatement> stmts) throws SQLException {
    for (PreparedStatement stmt : stmts.values()) {
      stmt.close();
    }
  }

  /**
   * Adds an SQL command to the batch statement to insert the given data.
   */
  private static void insert(final PreparedStatement stmt, final Object... parameterValues)
      throws SQLException
  {
    for (int i = 0; i < parameterValues.length; i++) {
      setParameter(stmt, i + 1, parameterValues[i]);
    }
    stmt.addBatch();
  }

  /**
   * Simple mapping from Java data to SQL parameters.
   */
  private static void setParameter(final PreparedStatement stmt, final int index, final Object value)
      throws SQLException
  {
    if (value == null) {
      stmt.setNull(index, NULL);
    }
    else if (value instanceof String) {
      stmt.setString(index, (String) value);
    }
    else if (value instanceof Integer) {
      stmt.setInt(index, (int) value);
    }
    else if (value instanceof Long) {
      stmt.setLong(index, (long) value);
    }
    else if (value instanceof OIdentifiable) {
      stmt.setString(index, ((OIdentifiable) value).getIdentity().toString());
    }
    else if (value instanceof Date) {
      stmt.setTimestamp(index, new Timestamp(((Date) value).getTime()));
    }
    else if (value instanceof byte[]) {
      stmt.setBytes(index, (byte[]) value);
    }
    else {
      stmt.setObject(index, value);
    }
  }
}
