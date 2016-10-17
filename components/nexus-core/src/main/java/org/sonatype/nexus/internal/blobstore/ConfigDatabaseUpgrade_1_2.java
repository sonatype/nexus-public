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
package org.sonatype.nexus.internal.blobstore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.upgrade.Upgrade;
import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.orient.OClassNameBuilder;

import com.google.common.annotations.VisibleForTesting;
import com.orientechnologies.orient.core.collate.OCaseInsensitiveCollate;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;

/**
 * Upgrades the config database to ensure that {@link org.sonatype.nexus.blobstore.api.BlobStoreConfiguration} names are
 * unique regardless of case.
 *
 * @since 3.1
 */
@Named
@Singleton
@Upgrades(model = DatabaseInstanceNames.CONFIG, from = "1.1", to = "1.2")
public class ConfigDatabaseUpgrade_1_2 // NOSONAR
    extends ComponentSupport
    implements Upgrade
{

  static final String DB_CLASS = new OClassNameBuilder()
      .prefix("repository")
      .type("blobstore")
      .build();

  private static final String P_NAME = "name";

  private static final String BLOBSTORE_NAME_DETAIL_FIELD = "blobstoreName";

  private static final String JOB_DATA_MAP = "jobDataMap";

  private static final String ATTRIBUTES = "attributes";

  private static final String BLOB_STORE_NAME = "blobStoreName";

  private final Provider<DatabaseInstance> componentDatabaseInstance;

  private final Provider<DatabaseInstance> configDatabaseInstance;

  @Inject
  public ConfigDatabaseUpgrade_1_2(
      @Named(DatabaseInstanceNames.CONFIG) final Provider<DatabaseInstance> databaseInstance,
      @Named(DatabaseInstanceNames.COMPONENT) final Provider<DatabaseInstance> componentDatabaseInstance)
  {
    this.configDatabaseInstance = checkNotNull(databaseInstance);
    this.componentDatabaseInstance = checkNotNull(componentDatabaseInstance);
  }

  @Override
  public void apply() throws Exception {
    Map<String, String> renames = renameDuplicateBlobStores();
    updateRepositoriesForRenamedBlobStores(renames);
    updateBlobStoreJobs(renames);
    updateAssetBlobStoreRefs(renames);
    upgradeSchemaToCaseInsensitiveName();
  }

  @VisibleForTesting
  void updateAssetBlobStoreRefs(final Map<String, String> renamedBlobStores) {
    try (ODatabaseDocumentTx db = componentDatabaseInstance.get().connect()) {
      renamedBlobStores.forEach((originalName, newName) -> {
        log.debug("Searching for BlobStoreRefs, original name: {}, new name: {} ", originalName, newName);

        OSQLSynchQuery query = new OSQLSynchQuery<>("select from asset where blob_ref like ? and @rid > ? limit 100");
        String nameTestValue = originalName + "@%";

        List<ODocument> results = db.query(query, nameTestValue, new ORecordId());

        while (!results.isEmpty()) {
          log.debug("Updating set of {} Refs", results.size());
          ORID last = results.get(results.size() - 1).getIdentity();

          results.forEach(doc -> updateDocWithNewName(originalName, newName, doc));

          results = db.query(query, nameTestValue, last);
        }
      });
    }
  }

  private void updateDocWithNewName(final String originalName, final String newName, final ODocument doc) {
    BlobRef blobRef = BlobRef.parse(doc.field("blob_ref", String.class));
    if (blobRef.getStore().equals(originalName)) {
      doc.field("blob_ref", new BlobRef(blobRef.getNode(), newName, blobRef.getBlob())).save();
    }
  }

  @VisibleForTesting
  void updateBlobStoreJobs(Map<String, String> renamedBlobStores) {
    try (ODatabaseDocumentTx db = configDatabaseInstance.get().connect()) {

      stream(db.browseClass("quartz_job_detail").spliterator(), false)
          .forEach(doc -> {

            Map<String, Map<String, Object>> valueData = doc.field("value_data");

            Map<String, Object> jobDataMap = getJobData(doc, valueData);
            if (jobDataMap == null) {
              return;
            }

            String blobStoreName = (String) jobDataMap.get(BLOBSTORE_NAME_DETAIL_FIELD);
            if (renamedBlobStores.containsKey(blobStoreName)) {
              jobDataMap.put(BLOBSTORE_NAME_DETAIL_FIELD, renamedBlobStores.get(blobStoreName));
              doc.field("value_data", valueData).save();
            }
          });
    }
  }

  private Map<String, Object> getJobData(final ODocument doc,
                                         final Map<String, Map<String, Object>> valueData)
  {

    Map<String, Object> jobDataMap = valueData.get(JOB_DATA_MAP);

    if (jobDataMap == null) {
      log.debug("Job {} does not contain a jobDataMap, skipping.", doc);
      return null;
    }

    if (jobDataMap.get(BLOBSTORE_NAME_DETAIL_FIELD) == null) {
      log.debug("Job {} does not refer to a blob store, skipping.", doc);
      return null;
    }
    return jobDataMap;
  }

  @VisibleForTesting
  void upgradeSchemaToCaseInsensitiveName() {
    try (ODatabaseDocumentTx db = configDatabaseInstance.get().connect()) {

      OClass blobStoreClass = db.getMetadata().getSchema().getClass(DB_CLASS);
      blobStoreClass.getProperty(P_NAME).setCollate(new OCaseInsensitiveCollate());

    }
  }

  @VisibleForTesting
  Map<String, String> renameDuplicateBlobStores() {
    try (ODatabaseDocumentTx db = configDatabaseInstance.get().connect()) {

      List<ODocument> allBlobStores = stream(db.browseClass("repository_blobstore").spliterator(), false)
          .collect(toList());

      log.debug("Found {} BlobStores to check for name collisions after case-insensitivity is added.",
          allBlobStores.size());

      Map<String, List<ODocument>> toUpdate = allBlobStores.stream()
          .collect(toMap(doc -> doc.field(P_NAME, String.class).toString().toLowerCase(),
              doc -> new ArrayList<>(singletonList(doc)),
              (oDocuments, oDocuments2) -> {
                oDocuments.addAll(oDocuments2);
                return oDocuments;
              }));

      return toUpdate.entrySet().stream()
          .filter(entry -> entry.getValue().size() > 1)
          .map(entry -> {
            if (entry.getKey().contains("@")) {
              throw new IllegalStateException("Invalid blob store name: " + entry.getKey());
            }
            log.debug("For lower-cased blob store name {}, {} collisions found.", entry.getKey(),
                entry.getValue().size());
            AtomicInteger counter = new AtomicInteger(0);

            return entry.getValue().stream()
                .sorted((o1, o2) -> o2.field(P_NAME, String.class).toString().compareTo(o1.field(P_NAME, String.class)))
                .map(document -> updateDocumentNameWithModifier(document, counter.getAndIncrement()))
                .collect(toList());
          })
          .flatMap(List::stream)
          .filter(Objects::nonNull)
          .collect(toMap(rename -> rename.originalName, rename -> rename.newName));
    }
  }

  private Rename updateDocumentNameWithModifier(final ODocument document,
                                                final int modifier)
  {
    String name = document.field(P_NAME, String.class);

    if (modifier == 0) {
      log.debug("BlobStore {} name will not be updated");
      return null;
    }

    String newName = name + "-" + Integer.toString(modifier);
    log.info("BlobStore {} will be renamed to {}", name, newName);
    document.field(P_NAME, newName).save();

    return new Rename(newName, name);
  }

  @VisibleForTesting
  void updateRepositoriesForRenamedBlobStores(Map<String, String> renamedBlobStores) {
    try (ODatabaseDocumentTx db = configDatabaseInstance.get().connect()) {

      stream(db.browseClass("repository").spliterator(), false)
          .forEach(doc -> {
            Map<String, Map<String, Object>> attributes = doc.field(ATTRIBUTES);

            if (attributes != null) {
              Map<String, Object> storage = attributes.get("storage");

              if (storage != null) {
                String blobStoreName = (String) storage.get(BLOB_STORE_NAME);

                if (renamedBlobStores.containsKey(blobStoreName)) {
                  storage.put(BLOB_STORE_NAME, renamedBlobStores.get(blobStoreName));
                  doc.field(ATTRIBUTES, attributes).save();
                }
              }
            }
          });
    }
  }

  @VisibleForTesting
  public static class Rename
  {
    final String newName;

    final String originalName;

    @VisibleForTesting
    public Rename(final String newName, final String originalName) {
      this.newName = newName;
      this.originalName = originalName;
    }
  }
}
