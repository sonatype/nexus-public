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
package org.sonatype.nexus.repository.upgrade;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.DependsOn;
import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.orient.DatabaseUpgradeSupport;
import org.sonatype.nexus.orient.OIndexNameBuilder;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.orientechnologies.orient.core.metadata.schema.OType.EMBEDDEDMAP;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_COMPONENT;
import static org.sonatype.nexus.repository.storage.BucketEntityAdapter.P_REPOSITORY_NAME;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_VERSION;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * Upgrade step that updates name and version for yum components. Where there is a clash, share the component
 * among assets.
 *
 * @since 3.8
 */
@Named
@Singleton
@Upgrades(model = DatabaseInstanceNames.COMPONENT, from = "1.9", to = "1.10")
@DependsOn(model = DatabaseInstanceNames.CONFIG, version = "1.5")
public class ComponentDatabaseUpgrade_1_10 // NOSONAR
    extends DatabaseUpgradeSupport
{
  private static final int BATCH_SIZE = 500;

  private static final String SELECT_YUM_PROXY_REPOSITORIES =
      "select from repository where recipe_name = 'yum-proxy'";

  private static final String SELECT_COMPONENT_BATCH_SQL = String
      .format("select from component where bucket = ? and @rid > ? order by @rid asc limit %d", BATCH_SIZE);

  private static final String SELECT_COMPONENT_ASSETS_SQL = "select from asset where component = %s";

  private static final String FIND_COMPONENT =
      "select from component where name = ? and version = ? and bucket = ? limit 1";

  private static final String I_REPOSITORY_NAME = new OIndexNameBuilder()
      .type("bucket")
      .property(P_REPOSITORY_NAME)
      .build();

  private final Provider<DatabaseInstance> configDatabaseInstance;

  private final Provider<DatabaseInstance> componentDatabaseInstance;

  @Inject
  public ComponentDatabaseUpgrade_1_10(
      @Named(DatabaseInstanceNames.CONFIG) final Provider<DatabaseInstance> configDatabaseInstance,
      @Named(DatabaseInstanceNames.COMPONENT) final Provider<DatabaseInstance> componentDatabaseInstance)
  {
    this.configDatabaseInstance = checkNotNull(configDatabaseInstance);
    this.componentDatabaseInstance = checkNotNull(componentDatabaseInstance);
  }

  @Override
  public void apply() {
    if (hasSchemaClass(configDatabaseInstance, "repository")
        && hasSchemaClass(componentDatabaseInstance, "bucket")
        && hasSchemaClass(componentDatabaseInstance, "component")
        && hasSchemaClass(componentDatabaseInstance, "asset")) {

      List<String> repositories;
      try (ODatabaseDocumentTx configDb = configDatabaseInstance.get().connect()) {
        repositories = configDb.<List<ODocument>>query(
            new OSQLSynchQuery<ODocument>(SELECT_YUM_PROXY_REPOSITORIES))
            .stream()
            .map(d -> (String) d.field(P_REPOSITORY_NAME))
            .collect(toList());
      }

      log.info("Updating name and version for yum-proxy components, this could be a long-running operation");

      for (String repository : repositories) {
        try (ODatabaseDocumentTx componentDb = componentDatabaseInstance.get().connect()) {
          log.debug("Repairing component coordinates yum repository {}", repository);
          OIndex<?> bucketIdx = componentDb.getMetadata().getIndexManager().getIndex(I_REPOSITORY_NAME);
          OIdentifiable bucket = (OIdentifiable) bucketIdx.get(repository);
          if (bucket == null) {
            log.warn("Unable to find bucket for {}", repository);
          }
          else {
            fixComponentBatch(componentDb, bucket);
          }
        }
      }
    }
  }

  private void fixComponentBatch(final ODatabaseDocumentTx db, final OIdentifiable bucket) {
    log.debug("Processing batch of {} yum component records...", BATCH_SIZE);

    OSQLSynchQuery<Object> query = new OSQLSynchQuery<>(SELECT_COMPONENT_BATCH_SQL);

    List<ODocument> components = db.query(query, bucket, new ORecordId());

    while (!components.isEmpty()) {
      ORID last = components.get(components.size() - 1).getIdentity();

      for (ODocument component : components) {
        fixComponent(db, component, bucket);
      }

      components = db.query(query, bucket, last);
    }
  }

  private void fixComponent(final ODatabaseDocumentTx db,
                            final ODocument component,
                            final OIdentifiable bucket)
  {
    String selectComponentAssetsSql = String.format(SELECT_COMPONENT_ASSETS_SQL, component.getIdentity());
    List<ODocument> componentsAssets = db.query(new OSQLSynchQuery<>(selectComponentAssetsSql));
    if (!componentsAssets.isEmpty()) {
      Map<String, String> formatAttributes = extractFormatAttributes(componentsAssets);

      if (formatAttributes != null && !formatAttributes.isEmpty()) {
        String name = formatAttributes.get("name");
        String version = formatAttributes.get("version");
        String release = formatAttributes.get("release");

        String fullVersion = isNotBlank(release) ? version + "-" + release : version;

        //Skip if already correct
        if (component.field(P_NAME).equals(name) && component.field(P_VERSION).equals(fullVersion)) {
          return;
        }

        ODocument existingComponent = findComponent(db, name, fullVersion, bucket);

        if (existingComponent != null) {
          moveAssetsToComponent(componentsAssets, existingComponent);
          component.delete();
        }
        else {
          component.field(P_NAME, name);
          component.field(P_VERSION, fullVersion);
          component.save();
        }
      }
      else {
        log.warn("Unable to process Yum component because formatAttributes was null or empty. {}", component);
      }
    }
  }

  private Map<String, String> extractFormatAttributes(final List<ODocument> componentsAssets) {
    ODocument firstAsset = componentsAssets.get(0);
    Map<String, Object> attributes = firstAsset.field(P_ATTRIBUTES, EMBEDDEDMAP);
    return (Map<String, String>) attributes.get("yum");
  }

  private void moveAssetsToComponent(final List<ODocument> componentsAssets, final ODocument existingComponent) {
    for (ODocument componentsAsset : componentsAssets) {
      componentsAsset.field(P_COMPONENT, existingComponent.getIdentity());
      componentsAsset.save();
    }
  }

  private ODocument findComponent(final ODatabaseDocumentTx db,
                                  final String name,
                                  final String version,
                                  final OIdentifiable bucket)
  {
    List<ODocument> components = db.query(new OSQLSynchQuery<>(FIND_COMPONENT), name, version, bucket);
    if (!components.isEmpty()) {
      return components.get(0);
    }
    return null;
  }
}
