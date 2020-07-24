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
package org.sonatype.nexus.repository.maven.internal.orient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.MultipleFailures;
import org.sonatype.nexus.orient.entity.AttachedEntityHelper;
import org.sonatype.nexus.orient.maven.MavenFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.internal.Attributes;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.AbstractMetadataRebuilder;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.AbstractMetadataUpdater;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;
import static org.sonatype.nexus.repository.maven.internal.hosted.metadata.MetadataUtils.metadataPath;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_GROUP;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_BUCKET;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;
import static org.sonatype.nexus.repository.storage.Query.builder;
import static org.sonatype.nexus.scheduling.CancelableHelper.checkCancellation;

/**
 * Maven 2 repository metadata re-builder.
 *
 * @since 3.0
 */
@Singleton
@Named("orient")
@Priority(Integer.MAX_VALUE)
public class OrientMetadataRebuilder
    extends AbstractMetadataRebuilder
{
  @Inject
  public OrientMetadataRebuilder(
      @Named("${nexus.maven.metadata.rebuild.bufferSize:-1000}") final int bufferSize,
      @Named("${nexus.maven.metadata.rebuild.timeoutSeconds:-60}") final int timeoutSeconds)
  {
    super(bufferSize, timeoutSeconds);
  }

  @Override
  public boolean rebuild(
      final Repository repository,
      final boolean update,
      final boolean rebuildChecksums,
      @Nullable final String groupId,
      @Nullable final String artifactId,
      @Nullable final String baseVersion)
  {
    checkNotNull(repository);
    final StorageTx tx = repository.facet(StorageFacet.class).txSupplier().get();
    UnitOfWork.beginBatch(tx);
    try {
      return rebuildInTransaction(repository, update, rebuildChecksums, groupId, artifactId, baseVersion);
    }
    finally {
      UnitOfWork.end();
    }
  }

  @Override
  public boolean rebuildInTransaction(
      final Repository repository,
      final boolean update,
      final boolean rebuildChecksums,
      @Nullable final String groupId,
      @Nullable final String artifactId,
      @Nullable final String baseVersion)
  {
    checkNotNull(repository);
    return new Worker(repository, update, rebuildChecksums, groupId, artifactId, baseVersion, bufferSize,
        timeoutSeconds, new OrientMetadataUpdater(update, repository)).rebuildMetadata();
  }

  @Override
  protected Set<String> deleteAllMetadataFiles(
      final Repository repository,
      final String groupId,
      final String artifactId,
      final String baseVersion)
  {
    final StorageTx tx = repository.facet(StorageFacet.class).txSupplier().get();
    UnitOfWork.beginBatch(tx);
    try {
      return super.deleteAllMetadataFiles(repository, groupId, artifactId, baseVersion);
    }
    finally {
      UnitOfWork.end();
    }
  }

  @Override
  protected Set<String> deleteGavMetadata(
      final Repository repository,
      final String groupId,
      final String artifactId,
      final String baseVersion)
  {
    MavenPath gavMetadataPath = metadataPath(groupId, artifactId, baseVersion);
    OrientMetadataUtils.delete(repository, gavMetadataPath);
    return MavenFacetUtils.getPathWithHashes(gavMetadataPath);
  }

  @Override
  public boolean exists(final Repository repository, final MavenPath mavenPath) {
    final StorageTx tx = repository.facet(StorageFacet.class).txSupplier().get();
    UnitOfWork.beginBatch(tx);
    try {
      return repository.facet(MavenFacet.class).exists(mavenPath);
    }
    finally {
      UnitOfWork.end();
    }
  }

  /**
   * Inner class that encapsulates the work, as metadata builder is stateful.
   */
  protected static class Worker
      extends AbstractMetadataRebuilder.Worker
  {
    private final Map<String, Object> sqlParams;

    private final String sql;

    private final MavenFacet mavenFacet;

    public Worker(
        final Repository repository, // NOSONAR
        final boolean update,
        final boolean rebuildChecksums,
        @Nullable final String groupId,
        @Nullable final String artifactId,
        @Nullable final String baseVersion,
        final int bufferSize,
        final int timeoutSeconds,
        final AbstractMetadataUpdater metadataUpdater
    )
    {
      super(repository, update, rebuildChecksums, groupId, artifactId, baseVersion, bufferSize, timeoutSeconds,
          metadataUpdater, repository.facet(MavenFacet.class).getMavenPathParser());
      this.sqlParams = Maps.newHashMap();
      this.sql = buildSql(groupId, artifactId, baseVersion);
      this.mavenFacet = repository.facet(MavenFacet.class);
    }

    /**
     * Builds up SQL and populates parameters map for it based on passed in parameters. As side effect, it populates
     * the {@link #sqlParams} map too with required parameters.
     */
    private String buildSql(
        @Nullable final String groupId,
        @Nullable final String artifactId,
        @Nullable final String baseVersion)
    {
      sqlParams.put("bucket", findBucketORID(repository));
      final StringBuilder builder = new StringBuilder();
      builder.append(
          format(
              "SELECT " +
                  "%s as groupId, " +
                  "%s as artifactId, " +
                  "set(%s.%s.%s) as baseVersions " +
                  "FROM %s WHERE %s=:bucket",
              P_GROUP,
              P_NAME,
              P_ATTRIBUTES,
              Maven2Format.NAME,
              Attributes.P_BASE_VERSION,
              "component", // Component DB class name
              P_BUCKET
          )
      );
      if (!Strings.isNullOrEmpty(groupId)) {
        builder.append(" AND " + P_GROUP + "=:groupId");
        sqlParams.put("groupId", groupId);
        if (!Strings.isNullOrEmpty(artifactId)) {
          builder.append(" AND " + P_NAME + "=:artifactId");
          sqlParams.put("artifactId", artifactId);
          if (!Strings.isNullOrEmpty(baseVersion)) {
            builder.append(
                " AND " + P_ATTRIBUTES
                    + "." + Maven2Format.NAME
                    + "." + Attributes.P_BASE_VERSION + "=:baseVersion");
            sqlParams.put("baseVersion", baseVersion);
          }
        }
      }
      builder.append(" GROUP BY " + P_GROUP + ", " + P_NAME + "");
      return builder.toString();
    }

    /**
     * Finds the {@link Bucket}\s {@link ORID} for passed in {@link Repository}.
     */
    private ORID findBucketORID(final Repository repository) {
      return Transactional.operation.call(() -> {
        final StorageTx tx = UnitOfWork.currentTx();
        return AttachedEntityHelper.id(tx.findBucket(repository));
      });
    }

    @Override
    protected List<Map<String, Object>> browseGAVs() {
      Iterable<ODocument> iterableGavs = Transactional.operation.call(() -> {
        final StorageTx tx = UnitOfWork.currentTx();
        return tx.browse(sql, sqlParams, bufferSize, timeoutSeconds);
      });
      return StreamSupport.stream(iterableGavs.spliterator(), false).map(odocument -> ImmutableMap.of(
          "groupId", odocument.field("groupId", OType.STRING),
          "artifactId", odocument.field("artifactId", OType.STRING),
          "baseVersions", odocument.field("baseVersions", OType.EMBEDDEDSET)
      )).collect(Collectors.toList());
    }

    @Override
    protected Content get(final MavenPath mavenPath) throws IOException {
      return mavenFacet.get(mavenPath);
    }

    @Override
    protected void put(final MavenPath mavenPath, final Payload payload) throws IOException {
      mavenFacet.put(mavenPath, payload);
    }

    @Override
    protected Optional<HashCode> getChecksum(final MavenPath mavenPath, final HashType hashType) {
      StorageTx tx = UnitOfWork.currentTx();
      return Optional.ofNullable(MavenFacetUtils.findAsset(tx, tx.findBucket(repository), mavenPath))
          .map(asset -> asset.getChecksum(hashType.getHashAlgorithm()));
    }

    @Override
    protected void rebuildMetadataInner(
        final String groupId,
        final String artifactId,
        final Set<String> baseVersions,
        final MultipleFailures failures)
    {
      final StorageTx tx = UnitOfWork.currentTx();

      metadataBuilder.onEnterArtifactId(artifactId);
      for (final String baseVersion : baseVersions) {
        checkCancellation();
        metadataBuilder.onEnterBaseVersion(baseVersion);

        TransactionalStoreBlob.operation.run(() -> {
          Bucket bucket = tx.findBucket(repository);

          Query query = builder()
              .where(P_GROUP).eq(groupId)
              .and(P_NAME).eq(artifactId)
              .build();

          /*
            Originally this query was done in one piece and included a 'WHERE attributes.maven2.baseVersion =' but that
            causes some severe performance problems because Orient decides not to use the index on group, name and 
            bucket and instead falls back to only using an index on group. This would cause the metadata rebuild to get 
            exponentially slower as the data size grew but also meant near-full table scans were being done no matter
            which repository the task was run against.
            
            More information and metrics can be found here: https://issues.sonatype.org/browse/NEXUS-17696 
           */
          Iterable<Component> filteredComponents = Iterables.filter(tx.browseComponents(query, bucket), (component) -> {
            String thisVersion = (String) component.formatAttributes().get(P_BASE_VERSION);
            return baseVersion.equals(thisVersion);
          });

          for (Component component : filteredComponents) {
            checkCancellation();

            for (Asset asset : tx.browseAssets(component)) {
              checkCancellation();
              final MavenPath mavenPath = mavenPathParser.parsePath(asset.name());
              if (mavenPath.isSubordinate()) {
                continue;
              }
              metadataBuilder.addArtifactVersion(mavenPath);
              if (rebuildChecksums) {
                mayUpdateChecksum(mavenPath, HashType.SHA1);
                mayUpdateChecksum(mavenPath, HashType.MD5);
              }
              final String packaging = component.formatAttributes().get(Attributes.P_PACKAGING, String.class);
              log.debug("POM packaging: {}", packaging);
              if ("maven-plugin".equals(packaging)) {
                metadataBuilder.addPlugin(getPluginPrefix(mavenPath.locateMainArtifact("jar")), artifactId,
                    component.formatAttributes().get(Attributes.P_POM_NAME, String.class));
              }
            }
          }

          processMetadata(metadataPath(groupId, artifactId, baseVersion), metadataBuilder.onExitBaseVersion(),
              failures);
        });
      }

      processMetadata(metadataPath(groupId, artifactId, null), metadataBuilder.onExitArtifactId(), failures);
    }
  }

  @Override
  public void deleteMetadata(final Repository repository, final List<String []> gavs) {
    checkNotNull(repository);
    checkNotNull(gavs);

    List<MavenPath> pathBatch = new ArrayList<>();
    for (String[] gav : gavs) {
      pathBatch.addAll(getPathsByGav(repository, gav[0], gav[1], gav[2]));
    }

    try {
      MavenFacetUtils.deleteWithHashes(repository.facet(MavenFacet.class), pathBatch);
    }
    catch (IOException e) {
      log.warn("Error encountered when deleting metadata: repository={}", repository);
      throw new RuntimeException(e);
    }
  }
}
