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
package org.sonatype.nexus.testsuite.testsupport.maven;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.orient.maven.OrientMavenFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenHostedFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.internal.MavenMimeRulesSource;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentMaintenance;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.io.CharStreams;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import org.joda.time.DateTime;

import static java.util.Collections.singletonList;
import static java.util.stream.StreamSupport.stream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.ORIENT_ENABLED;
import static org.sonatype.nexus.repository.maven.MavenMetadataRebuildFacet.METADATA_FORCE_REBUILD;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.GROUP;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

@FeatureFlag(name = ORIENT_ENABLED)
@Named("orient")
@Singleton
@Priority(Integer.MAX_VALUE)
public class OrientMavenTestHelper
    extends MavenTestHelper
{
  @Inject
  @Named(DatabaseInstanceNames.COMPONENT)
  public Provider<DatabaseInstance> databaseInstanceProvider;

  @Override
  public void verifyHashesExistAndCorrect(final Repository repository, final String path) throws Exception {
    OrientMavenFacet mavenFacet = repository.facet(OrientMavenFacet.class);
    MavenPath mavenPath = mavenFacet.getMavenPathParser().parsePath(path);
    UnitOfWork.begin(repository.facet(StorageFacet.class).txSupplier());
    try {
      Content content = mavenFacet.get(mavenPath);
      assertThat(content, notNullValue());
      Map<HashAlgorithm, HashCode> hashCodes =
          content.getAttributes().require(Content.CONTENT_HASH_CODES_MAP, Content.T_CONTENT_HASH_CODES_MAP);
      for (HashType hashType : HashType.values()) {
        Content contentHash = mavenFacet.get(mavenPath.hash(hashType));
        String storageHash = hashCodes.get(hashType.getHashAlgorithm()).toString();
        assertThat(storageHash, notNullValue());
        // Maven deployer does not create these hashes by default yet but we are storing the calculated values in the asset attributes
        if(contentHash == null && (hashType  == HashType.SHA256 ||  hashType  == HashType.SHA512)) {
          continue;
        }
        try (InputStream is = contentHash.openInputStream()) {
          String mavenHash = CharStreams.toString(new InputStreamReader(is, StandardCharsets.UTF_8));
          assertThat(storageHash, equalTo(mavenHash));
        }
      }
    }
    finally {
      UnitOfWork.end();
    }
  }

  @Override
  public void rebuildMetadata(
      final Repository repository,
      final String groupId,
      final String artifactId,
      final String baseVersion,
      final boolean rebuildChecksums,
      final boolean update)
  {
    repository.facet(MavenHostedFacet.class).rebuildMetadata(groupId, artifactId, baseVersion, rebuildChecksums, update);
  }

  @Override
  public void deleteComponents(final Repository repository, final String version, final int expectedNumber) {
    final List<Component> components = findComponents(repository, version);
    assertThat(components, hasSize(expectedNumber));
    ComponentMaintenance componentMaintenance = repository.facet(ComponentMaintenance.class);
    for (Component component : components) {
      componentMaintenance.deleteComponent(component.getEntityMetadata().getId());
    }
  }

  @Override
  public void deleteAssets(final Repository repository, final String version, final int expectedNumber) {
    List<Asset> assets = findAssets(repository, version);
    assertThat(assets, hasSize(expectedNumber));
    ComponentMaintenance componentMaintenance = repository.facet(ComponentMaintenance.class);
    for (Asset asset : assets) {
      componentMaintenance.deleteAsset(asset.getEntityMetadata().getId());
    }
  }

  private List<Component> findComponents(final Repository repository, final String version) {
    StorageTx tx = repository.facet(StorageFacet.class).txSupplier().get();
    tx.begin();
    final List<Component> components = Lists.newArrayList(tx.findComponents(
        Query.builder()
            .where(GROUP).eq("org.sonatype.nexus.testsuite")
            .and("attributes." + Maven2Format.NAME + "." + P_BASE_VERSION).eq(version)
            .build(),
        singletonList(repository)
    ));
    tx.close();
    return components;
  }

  private List<Asset> findAssets(final Repository repository, final String version) {
    final List<Component> components = findComponents(repository, version);
    StorageTx tx = repository.facet(StorageFacet.class).txSupplier().get();
    tx.begin();
    List<Asset> assets = components.stream()
        .map(tx::browseAssets)
        .flatMap(assetIterable -> stream(assetIterable.spliterator(), false))
        .collect(Collectors.toList());
    tx.close();
    return assets;
  }

  @Override
  public void writeWithoutValidation(
      final Repository repository,
      final String path,
      final Payload payload) throws IOException
  {
    OrientMavenFacet mavenFacet = repository.facet(OrientMavenFacet.class);
    StorageFacet storageFacet = repository.facet(StorageFacet.class);

    MavenPath mavenPath = mavenFacet.getMavenPathParser().parsePath(path);
    UnitOfWork.begin(repository.facet(StorageFacet.class).txSupplier());
    try {
      try (TempBlob tempBlob = storageFacet.createTempBlob(payload, HashType.ALGORITHMS)) {

        mavenFacet.put(mavenPath, tempBlob, MavenMimeRulesSource.METADATA_TYPE, new AttributesMap());
      }
    }
    finally {
      UnitOfWork.end();
    }
  }

  @Override
  public void write(final Repository repository, final String path, final Payload payload) throws IOException {
    OrientMavenFacet mavenFacet = repository.facet(OrientMavenFacet.class);
    MavenPath mavenPath = mavenFacet.getMavenPathParser().parsePath(path);
    UnitOfWork.begin(repository.facet(StorageFacet.class).txSupplier());
    try {
      mavenFacet.put(mavenPath, payload);
    }
    finally {
      UnitOfWork.end();
    }
  }

  @Override
  public Payload read(final Repository repository, final String path) throws IOException {
    OrientMavenFacet mavenFacet = repository.facet(OrientMavenFacet.class);
    MavenPath mavenPath = mavenFacet.getMavenPathParser().parsePath(path);
    UnitOfWork.begin(repository.facet(StorageFacet.class).txSupplier());
    try {
      return mavenFacet.get(mavenPath);
    }
    finally {
      UnitOfWork.end();
    }
  }

  @Override
  public boolean delete(final Repository repository, final String path) throws Exception {
    OrientMavenFacet mavenFacet = repository.facet(OrientMavenFacet.class);
    MavenPath mavenPath = mavenFacet.getMavenPathParser().parsePath(path);
    UnitOfWork.begin(repository.facet(StorageFacet.class).txSupplier());
    try {
      return !mavenFacet.delete(mavenPath).isEmpty();
    }
    finally {
      UnitOfWork.end();
    }
  }

  @Override
  public DateTime getLastDownloadedTime(final Repository repository, final String assetPath) {
    MavenFacet mavenFacet = repository.facet(MavenFacet.class);
    MavenPath mavenPath = mavenFacet.getMavenPathParser().parsePath(assetPath);
    try (StorageTx tx = repository.facet(StorageFacet.class).txSupplier().get()) {
      tx.begin();

      Asset asset = tx.findAssetWithProperty(P_NAME, mavenPath.getPath(), tx.findBucket(repository));

      return asset.lastDownloaded();
    }
  }

  @Override
  public EntityId createComponent(
      final Repository repository,
      final String groupId,
      final String artifactId,
      final String version)
  {
    try (StorageTx storageTx = repository.facet(StorageFacet.class).txSupplier().get()) {
      storageTx.begin();
      Bucket bucket = storageTx.findBucket(repository);
      Component component = storageTx.createComponent(bucket, repository.getFormat())
          .name(artifactId)
          .group(groupId)
          .version(version);
      Map<String, Object> mavenAttr =
          ImmutableMap.of("groupId", groupId, "artifactId", artifactId, "baseVersion", version);
      component.attributes(new NestedAttributesMap("", Collections.singletonMap("maven2", mavenAttr)));
      storageTx.saveComponent(component);
      String path = String.format("%s/%s/%s/%s-%s.jar", groupId, artifactId, version, artifactId, version);
      Asset asset = storageTx.createAsset(bucket, component).name(path);
      storageTx.saveAsset(asset);
      storageTx.commit();
      return EntityHelper.id(component);
    }
  }

  @Override
  public List<MavenTestComponent> loadComponents(final Repository repository) {
    StorageTx tx = repository.facet(StorageFacet.class).txSupplier().get();
    tx.begin();
    ArrayList<Component> components = Lists.newArrayList(tx.browseComponents(tx.findBucket(repository)));
    tx.close();

    List<MavenTestComponent> result = new ArrayList<>();
    for (Component component : components) {
      result.add(new MavenTestComponent(component.name(),
          component.attributes().child("maven2").get("baseVersion").toString(), component.version(),
          component.lastUpdated()));
    }
    return result;
  }

  @Override
  public void updateBlobCreated(final Repository repository, final Date date) {
    String sql = "UPDATE asset SET blob_created = :blobCreated WHERE bucket.repository_name = :repositoryName";
    HashMap<String, Object> sqlParams = new HashMap<>();
    sqlParams.put("repositoryName", repository.getName());
    sqlParams.put("blobCreated", date);
    ODatabaseDocumentTx tx = databaseInstanceProvider.get().acquire();
    tx.begin();
    tx.command(new OCommandSQL(sql)).execute(sqlParams);
    tx.commit();
    tx.close();
  }

  @Override
  public List<String> findComponents(final Repository repository) {
    String sql = "SELECT name, attributes['maven2']['baseVersion'] AS baseVersion FROM component " +
        "WHERE bucket.repository_name = :repositoryName";
    HashMap<String, Object> sqlParams = new HashMap<>();
    sqlParams.put("repositoryName", repository.getName());

    ODatabaseDocumentTx tx = databaseInstanceProvider.get().acquire();
    tx.begin();
    List<ODocument> results = tx.command(new OCommandSQL(sql)).execute(sqlParams);
    tx.commit();
    tx.close();

    return results.stream()
        .map(document -> document.field("name") + ":" + document.field("baseVersion"))
        .collect(Collectors.toList());
  }

  @Override
  public List<String> findAssets(final Repository repository) {
    String sql = "SELECT name FROM asset WHERE bucket.repository_name = :repositoryName";
    HashMap<String, Object> sqlParams = new HashMap<>();
    sqlParams.put("repositoryName", repository.getName());

    ODatabaseDocumentTx tx = databaseInstanceProvider.get().acquire();
    tx.begin();
    List<ODocument> results = tx.command(new OCommandSQL(sql)).execute(sqlParams);
    tx.commit();
    tx.close();

    return results.stream()
        .map(document -> "/" + document.field("name"))
        .collect(Collectors.toList());
  }

  @Override
  public List<String> findAssetsExcludingFlaggedForRebuild(final Repository repository) {
    String sql = "SELECT name, attributes FROM asset WHERE bucket.repository_name = :repositoryName";
    HashMap<String, Object> sqlParams = new HashMap<>();
    sqlParams.put("repositoryName", repository.getName());

    ODatabaseDocumentTx tx = databaseInstanceProvider.get().acquire();
    tx.begin();
    List<ODocument> results = tx.command(new OCommandSQL(sql)).execute(sqlParams);
    tx.commit();
    tx.close();

    return results.stream()
        .filter(this::isNotFlaggedForRebuild)
        .map(document -> "/" + document.field("name"))
        .collect(Collectors.toList());
  }

  private boolean isNotFlaggedForRebuild(final ODocument document) {
    Map<String, Object> attributes = document.field("attributes", OType.EMBEDDEDMAP);
    if (attributes != null) {
      Map<String, Object> maven2Attributes = (Map<String, Object>) attributes.get("maven2");
      if (maven2Attributes != null) {
        return !Boolean.TRUE.equals(maven2Attributes.get("forceRebuild"));
      }
    }
    return true;
  }

  @Override
  public void markMetadataForRebuild(final Repository repository, final String path) {
    StorageTx tx = repository.facet(StorageFacet.class).txSupplier().get();
    tx.begin();
    Bucket bucket = tx.findBucket(repository);
    Asset metadataAsset = tx.findAssetWithProperty(P_NAME, path, bucket);
    assertNotNull("Could not set forceRebuild flag, because requested path does not exist", metadataAsset);
    metadataAsset.formatAttributes().set(METADATA_FORCE_REBUILD, true);
    tx.saveAsset(metadataAsset);
    tx.commit();
    tx.close();
  }
}
