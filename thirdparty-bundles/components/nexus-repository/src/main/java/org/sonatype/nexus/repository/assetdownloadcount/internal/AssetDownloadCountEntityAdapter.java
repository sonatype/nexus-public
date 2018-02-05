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
package org.sonatype.nexus.repository.assetdownloadcount.internal;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.IterableEntityAdapter;
import org.sonatype.nexus.repository.assetdownloadcount.DateType;

import com.google.common.collect.Iterables;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import org.joda.time.DateTime;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link AssetDownloadCount} entity-adapter.
 *
 * @since 3.3
 */
@Named
@Singleton
public class AssetDownloadCountEntityAdapter
    extends IterableEntityAdapter<AssetDownloadCount>
{
  private static final String DB_CLASS = new OClassNameBuilder()
      .type("assetdownloadcount")
      .build();

  public static final String P_REPOSITORY_NAME = "repository_name";

  public static final String P_ASSET_NAME = "asset_name";

  public static final String P_NODE_ID = "node_id";

  public static final String P_COUNT = "count";

  public static final String P_DATE_TYPE = "date_type";

  public static final String P_DATE = "date";

  private static final String DATED_COUNT_QUERY = String
      .format("select sum(%s) from %s where %s = :repositoryName and %s = :assetName and %s = :dateType and %s = :date",
          P_COUNT, DB_CLASS, P_REPOSITORY_NAME, P_ASSET_NAME, P_DATE_TYPE, P_DATE);

  private static final String DATED_COUNT_ALL_OF_REPO_QUERY = String
      .format("select sum(%s) from %s where %s = :repositoryName and %s = :assetName and %s = :dateType and %s = :date",
          P_COUNT, DB_CLASS, P_REPOSITORY_NAME, P_ASSET_NAME, P_DATE_TYPE, P_DATE);

  private static final String TOTAL_COUNT_QUERY = String
      .format("select sum(%s) from %s where %s = :repositoryName and %s = :assetName and %s = :dateType", P_COUNT,
          DB_CLASS, P_REPOSITORY_NAME, P_ASSET_NAME, P_DATE_TYPE);

  private static final String UPDATE_COUNT_QUERY = String.format(
      "update %s set count = :count, node_id = :nodeId, repository_name = :repositoryName, asset_name = :assetName, "
          + "date_type = :dateType, date = :date upsert where %s = :nodeId and %s = :repositoryName and "
          + "%s = :assetName and %s = :dateType and %s = :date",
      DB_CLASS, P_NODE_ID, P_REPOSITORY_NAME, P_ASSET_NAME, P_DATE_TYPE, P_DATE);

  private static final String REMOVE_OLD_QUERY = String
      .format("delete from %s where %s = :nodeId and %s = :dateType and %s < :date limit :limit", DB_CLASS, P_NODE_ID,
          P_DATE_TYPE, P_DATE);

  private static final String ASSET_TOTAL_FOR_DATE_TYPE_QUERY = String
      .format("select from %s where %s = :repositoryName and %s = :assetName and %s = :dateType", DB_CLASS,
          P_REPOSITORY_NAME, P_ASSET_NAME, P_DATE_TYPE);

  private static final String REPO_TOTAL_FOR_DATE_TYPE_QUERY = String
      .format("select from %s where %s = :repositoryName and %s = :dateType", DB_CLASS,
          P_REPOSITORY_NAME, P_DATE_TYPE);

  private static final String I_REPO_NAME_ASSET_NAME_DATE_TYPE_DATE = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_REPOSITORY_NAME)
      .property(P_ASSET_NAME)
      .property(P_DATE_TYPE)
      .property(P_DATE)
      .build();

  private static final String I_NODE_ID_REPO_NAME_ASSET_NAME_DATE_TYPE_DATE = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_NODE_ID)
      .property(P_REPOSITORY_NAME)
      .property(P_ASSET_NAME)
      .property(P_DATE_TYPE)
      .property(P_DATE)
      .build();

  private static final String I_NODE_ID_DATE_TYPE_DATE = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_NODE_ID)
      .property(P_DATE_TYPE)
      .property(P_DATE)
      .build();

  private static final String I_REPO_NAME_DATE_TYPE_DATE = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_REPOSITORY_NAME)
      .property(P_DATE_TYPE)
      .build();

  private final String nodeId;

  private final int maxDeleteSize;

  @Inject
  public AssetDownloadCountEntityAdapter(final NodeAccess nodeAccess,
                                         @Named("${nexus.assetdownloads.max.delete.size:-1000}") final int maxDeleteSize)
  {
    super(DB_CLASS);
    this.nodeId = nodeAccess.getId();
    this.maxDeleteSize = maxDeleteSize;
  }

  @Override
  protected void defineType(final OClass type) {
    type.createProperty(P_REPOSITORY_NAME, OType.STRING).setMandatory(true).setNotNull(true);
    type.createProperty(P_ASSET_NAME, OType.STRING).setMandatory(true).setNotNull(true);
    type.createProperty(P_NODE_ID, OType.STRING).setMandatory(true).setNotNull(true);
    type.createProperty(P_COUNT, OType.LONG).setMandatory(true).setNotNull(true);
    type.createProperty(P_DATE_TYPE, OType.STRING).setMandatory(true).setNotNull(true);
    type.createProperty(P_DATE, OType.DATETIME).setMandatory(true).setNotNull(true);

    type.createIndex(I_REPO_NAME_ASSET_NAME_DATE_TYPE_DATE, INDEX_TYPE.NOTUNIQUE, P_REPOSITORY_NAME, P_ASSET_NAME,
        P_DATE_TYPE, P_DATE);
    type.createIndex(I_NODE_ID_REPO_NAME_ASSET_NAME_DATE_TYPE_DATE, INDEX_TYPE.NOTUNIQUE, P_NODE_ID, P_REPOSITORY_NAME,
        P_ASSET_NAME, P_DATE_TYPE, P_DATE);
    type.createIndex(I_NODE_ID_DATE_TYPE_DATE, INDEX_TYPE.NOTUNIQUE, P_NODE_ID, P_DATE_TYPE, P_DATE);
    type.createIndex(I_REPO_NAME_DATE_TYPE_DATE, INDEX_TYPE.NOTUNIQUE, P_REPOSITORY_NAME, P_DATE_TYPE);
  }

  @Override
  protected AssetDownloadCount newEntity() {
    return new AssetDownloadCount();
  }

  @Override
  protected void readFields(final ODocument document, final AssetDownloadCount entity) {
    String repositoryName = document.field(P_REPOSITORY_NAME, OType.STRING);
    String assetName = document.field(P_ASSET_NAME, OType.STRING);
    String nodeId = document.field(P_NODE_ID, OType.STRING);
    Long count = document.field(P_COUNT, OType.LONG);
    DateType dateType = DateType.valueOf(document.field(P_DATE_TYPE, OType.STRING));
    Date date = document.field(P_DATE, OType.DATETIME);

    entity.withRepositoryName(repositoryName).withNodeId(nodeId).withDateType(dateType)
        .withDate(new DateTime(date)).withCount(count);

    if (assetName != null) {
      entity.withAssetName(assetName);
    }
  }

  @Override
  protected void writeFields(final ODocument document, final AssetDownloadCount entity) {
    document.field(P_REPOSITORY_NAME, entity.getRepositoryName());
    document.field(P_ASSET_NAME, entity.getAssetName());
    document.field(P_NODE_ID, entity.getNodeId());
    document.field(P_COUNT, entity.getCount());
    document.field(P_DATE_TYPE, entity.getDateType());
    document.field(P_DATE, entity.getDate().toDate());
  }

  public long getCount(final ODatabaseDocumentTx db, final String repositoryName, final String assetName) {
    checkNotNull(db);
    checkNotNull(repositoryName);
    checkNotNull(assetName);

    Map<String, Object> parameters = buildQueryParameters(repositoryName, assetName, DateType.DAY, null, null);
    Iterable<ODocument> docs = db.command(new OCommandSQL(TOTAL_COUNT_QUERY)).execute(parameters);

    ODocument result = Iterables.getFirst(docs, null);

    if (result != null) {
      return result.field("sum", OType.LONG);
    }

    return 0;
  }

  public long getCount(final ODatabaseDocumentTx db,
                       final String repositoryName,
                       final String assetName,
                       final DateType dateType)
  {
    checkNotNull(db);
    checkNotNull(repositoryName);
    checkNotNull(assetName);
    checkNotNull(dateType);

    Map<String, Object> parameters = buildQueryParameters(repositoryName, assetName, dateType, null, null);
    String query = String.format(
        "select sum(%s) from %s where %s = :repositoryName and %s = :assetName and %s = :dateType",
        P_COUNT, DB_CLASS, P_REPOSITORY_NAME, P_ASSET_NAME, P_DATE_TYPE);
    Iterable<ODocument> docs = db.command(new OCommandSQL(query)).execute(parameters);

    ODocument result = Iterables.getFirst(docs, null);

    if (result != null) {
      return result.field("sum", OType.LONG);
    }

    return 0;
  }

  public long getCount(final ODatabaseDocumentTx db,
                       final String repositoryName,
                       final DateType dateType,
                       final DateTime date)
  {
    checkNotNull(db);
    checkNotNull(repositoryName);
    checkNotNull(dateType);
    checkNotNull(date);

    Map<String, Object> parameters = buildQueryParameters(repositoryName, "", dateType, date, null);
    Iterable<ODocument> docs = db.command(new OCommandSQL(DATED_COUNT_ALL_OF_REPO_QUERY)).execute(parameters);

    ODocument result = Iterables.getFirst(docs, null);

    if (result != null) {
      return result.field("sum", OType.LONG);
    }

    return 0;
  }

  public long getCount(final ODatabaseDocumentTx db,
                       final String repositoryName,
                       final String assetName,
                       final DateType dateType,
                       final DateTime date)
  {
    checkNotNull(db);
    checkNotNull(repositoryName);
    checkNotNull(assetName);
    checkNotNull(dateType);
    checkNotNull(date);

    Map<String, Object> parameters = buildQueryParameters(repositoryName, assetName, dateType, date, null);
    Iterable<ODocument> docs = db.command(new OCommandSQL(DATED_COUNT_QUERY)).execute(parameters);

    ODocument result = Iterables.getFirst(docs, null);

    if (result != null) {
      return result.field("sum", OType.LONG);
    }

    return 0;
  }

  public void incrementCount(final ODatabaseDocumentTx db,
                             final String repositoryName,
                             final String assetName,
                             final long count)
  {
    checkNotNull(db);
    checkNotNull(repositoryName);
    checkNotNull(assetName);

    DateTime now = new DateTime();

    incrementCount(db, repositoryName, assetName, nodeId, DateType.DAY, now, count);
    incrementCount(db, repositoryName, assetName, nodeId, DateType.MONTH, now, count);
  }

  public long[] getCounts(final ODatabaseDocumentTx db,
                          final String repositoryName,
                          final String assetName,
                          final DateType dateType)
  {
    checkNotNull(db);
    checkNotNull(repositoryName);
    checkNotNull(assetName);
    checkNotNull(dateType);

    Map<String, Object> parameters = buildQueryParameters(repositoryName, assetName, dateType, null, null);
    Iterable<ODocument> docs = db.command(new OCommandSQL(ASSET_TOTAL_FOR_DATE_TYPE_QUERY)).execute(parameters);

    return getCounts(docs, dateType);
  }

  public void setCount(final ODatabaseDocumentTx db,
                       final String repositoryName,
                       final DateType dateType,
                       final DateTime date,
                       final long count)
  {
    checkNotNull(db);
    checkNotNull(repositoryName);
    checkNotNull(date);

    Map<String, Object> parameters = buildQueryParameters(repositoryName, "", dateType, date, nodeId);
    parameters.put("count", count);

    db.command(new OCommandSQL(UPDATE_COUNT_QUERY)).execute(parameters);
  }

  public long[] getCounts(final ODatabaseDocumentTx db,
                          final String repositoryName,
                          final DateType dateType)
  {
    checkNotNull(db);
    checkNotNull(repositoryName);
    checkNotNull(dateType);

    Map<String, Object> parameters = buildQueryParameters(repositoryName, "", dateType, null, null);
    Iterable<ODocument> docs = db.command(new OCommandSQL(REPO_TOTAL_FOR_DATE_TYPE_QUERY)).execute(parameters);

    return getCounts(docs, dateType);
  }

  public int removeOldRecords(final ODatabaseDocumentTx db, final DateType dateType) {
    checkNotNull(db);
    checkNotNull(dateType);

    DateTime date = dateType.toOldestDate(new DateTime());

    Map<String, Object> parameters = buildQueryParameters(null, "", dateType, date, nodeId);
    parameters.put("limit", maxDeleteSize);

    return db.command(new OCommandSQL(REMOVE_OLD_QUERY)).execute(parameters);
  }

  public int getMaxDeleteSize() {
    return maxDeleteSize;
  }

  private Map<String, Object> buildQueryParameters(final String repositoryName,
                                                   final String assetName,
                                                   final DateType dateType,
                                                   final DateTime date,
                                                   final String nodeId)
  {
    Map<String, Object> params = new HashMap<>();

    if (repositoryName != null) {
      params.put("repositoryName", repositoryName);
    }

    if (assetName != null) {
      params.put("assetName", assetName);
    }

    if (dateType != null) {
      params.put("dateType", dateType);
    }

    if (date != null) {
      params.put("date", dateType.standardizeDate(date).toDate());
    }

    if (nodeId != null) {
      params.put("nodeId", nodeId);
    }

    return params;
  }

  private void incrementCount(final ODatabaseDocumentTx db,
                              final String repositoryName,
                              final String assetName,
                              final String nodeId,
                              final DateType dateType,
                              final DateTime date,
                              final long count)
  {
    Map<String, Object> parameters = buildQueryParameters(repositoryName, assetName, dateType, date, nodeId);

    String query = String.format("update %s increment count = %s where %s = :nodeId and %s = :repositoryName "
        + "and %s = :assetName and %s = :dateType and %s = :date", DB_CLASS, count, P_NODE_ID, P_REPOSITORY_NAME,
        P_ASSET_NAME, P_DATE_TYPE, P_DATE);

    Integer updateCount = db.command(new OCommandSQL(query)).execute(parameters);
    if (updateCount < 1) {
      addEntity(db, newEntity().withRepositoryName(repositoryName).withAssetName(assetName).withNodeId(nodeId)
          .withDateType(dateType).withDate(new DateTime(dateType.standardizeDate(date))).withCount(count));
    }
  }

  private long[] getCounts(final Iterable<ODocument> docs, final DateType dateType) {
    int numberToKeep = dateType.getNumberToKeep();
    long[] counts = new long[numberToKeep];

    DateTime current = dateType.standardizeDate(DateTime.now());
    DateTime oldest = dateType.toOldestDate(current);
    Map<DateTime, Long> dateCountMap = new HashMap<>();

    //build map of date -> count, will take multiple records (in case of HA) into account
    for (ODocument doc : docs) {
      AssetDownloadCount assetDownloadCount = readEntity(doc);

      dateCountMap.put(assetDownloadCount.getDate(),
          dateCountMap.getOrDefault(assetDownloadCount.getDate(), 0L) + assetDownloadCount.getCount());
    }

    // iterate over the last X dates and pull any counts out of the map,
    // using 0 otherwise
    for (int i = 0; i < numberToKeep && oldest.isBefore(current); i++) {
      counts[i] = dateCountMap.getOrDefault(current, 0L);
      current = dateType.decrement(current);
    }

    return counts;
  }
}
