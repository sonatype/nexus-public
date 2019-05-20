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
package org.sonatype.nexus.repository.maven.internal.hosted.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.goodies.common.MultipleFailures;
import org.sonatype.nexus.orient.entity.AttachedEntityHelper;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.maven.internal.Attributes;
import org.sonatype.nexus.repository.maven.internal.Constants;
import org.sonatype.nexus.repository.maven.internal.DigestExtractor;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.internal.MavenFacetUtils;
import org.sonatype.nexus.repository.maven.internal.MavenModels;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import static com.google.common.base.Preconditions.checkArgument;
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
@Named
public class MetadataRebuilder
    extends ComponentSupport
{
  private final int bufferSize;

  private final int timeoutSeconds;

  @Inject
  public MetadataRebuilder(@Named("${nexus.maven.metadata.rebuild.bufferSize:-1000}") final int bufferSize,
                           @Named("${nexus.maven.metadata.rebuild.timeoutSeconds:-60}") final int timeoutSeconds)
  {
    this.bufferSize = bufferSize;
    this.timeoutSeconds = timeoutSeconds;
  }
  /**
   * Rebuilds/updates Maven metadata.
   *
   * @param repository  The repository whose metadata needs rebuild (Maven2 format, Hosted type only).
   * @param update      if {@code true}, updates existing metadata, otherwise overwrites them with newly generated
   *                    ones.
   * @param rebuildChecksums whether or not checksums should be checked and corrected if found                     
   *                           missing or incorrect                    
   * @param groupId     scope the work to given groupId.
   * @param artifactId  scope the work to given artifactId (groupId must be given).
   * @param baseVersion scope the work to given baseVersion (groupId and artifactId must ge given).
   *
   * @return whether the rebuild actually triggered
   */
  public boolean rebuild(final Repository repository,
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
      return new Worker(repository, update, rebuildChecksums, groupId, artifactId, baseVersion, bufferSize, timeoutSeconds)
          .rebuildMetadata();
    }
    finally {
      UnitOfWork.end();
    }
  }

  /**
   * Delete the metadata for the input list of GAbVs.
   *
   * @param repository The repository whose metadata needs rebuilding (Maven2 format, Hosted type only).
   * @param gavs       A list of gavs for which metadata will be deleted
   *
   * @since 3.14
   */
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

  /**
   * Collect all {@link MavenPath} for the provided GAbV.
   *
   * @param repository  The repository associated with the provided GAbV (Maven2 format, Hosted type only).
   * @param groupId     scope the work to given groupId.
   * @param artifactId  scope the work to given artifactId (groupId must be given).
   * @param baseVersion scope the work to given baseVersion (groupId and artifactId must ge given).
   * @return list of all paths for the input coordinates
   *
   * @since 3.14
   */
  private List<MavenPath> getPathsByGav(final Repository repository,
                                        final String groupId,
                                        final String artifactId,
                                        final String baseVersion)
  {
    checkNotNull(groupId);
    checkNotNull(artifactId);
    checkNotNull(baseVersion);

    log.debug("Collecting MavenPaths for Maven2 hosted repository metadata: repository={}, g={}, a={}, bV={}",
        repository.getName(), groupId, artifactId, baseVersion);

    List<MavenPath> paths = new ArrayList<>();
    try {
      // Build path for specific GAV
      paths.add(metadataPath(groupId, artifactId, baseVersion));

      // Build path for the GA; will be rebuilt as necessary but may hold the last GAV in which case rebuild would ignore it
      paths.add(metadataPath(groupId, artifactId, null));

      // Check explicitly for whether or not we have Group level metadata that might need rebuilding, since this
      // is potentially the most expensive possible path to take.
      MavenPath groupPath = metadataPath(groupId, null, null);
      if (MetadataUtils.exists(repository, groupPath)) {
        paths.add(groupPath);
      }
      return paths;
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Delete metadata for the given GAbV and rebuild metadata for the GA. If Group level metadata is present, rebuild
   * at that level to account for plugin deletion.
   * 
   * @param repository  The repository whose metadata needs rebuild (Maven2 format, Hosted type only).
   * @param groupId     scope the work to given groupId.
   * @param artifactId  scope the work to given artifactId (groupId must be given).
   * @param baseVersion scope the work to given baseVersion (groupId and artifactId must ge given).
   * @return paths of deleted metadata files
   */
  public Set<String> deleteAndRebuild(final Repository repository, final String groupId,
                               final String artifactId, final String baseVersion)
  {
    checkNotNull(repository);
    checkNotNull(groupId);
    checkNotNull(artifactId);
    checkNotNull(baseVersion);

    Set<String> deletedPaths = new HashSet<>();
    final StorageTx tx = repository.facet(StorageFacet.class).txSupplier().get();
    UnitOfWork.beginBatch(tx);
    boolean groupChange = false;
    try {
      // Delete the specific GAV
      MavenPath gavMetadataPath = metadataPath(groupId, artifactId, baseVersion);
      MetadataUtils.delete(repository, gavMetadataPath);
      deletedPaths.addAll(MavenFacetUtils.getPathWithHashes(gavMetadataPath));
      // Delete the GA; will be rebuilt as necessary but may hold the last GAV in which case rebuild would ignore it
      MavenPath gaMetadataPath = metadataPath(groupId, artifactId, null);
      MetadataUtils.delete(repository, gaMetadataPath);
      deletedPaths.addAll(MavenFacetUtils.getPathWithHashes(gaMetadataPath));

      // Check explicitly for whether or not we have Group level metadata that might need rebuilding, since this
      // is potentially the most expensive possible path to take.
      MavenPath groupPath = metadataPath(groupId, null, null);
      if (MetadataUtils.exists(repository, groupPath)) {
        MetadataUtils.delete(repository, groupPath);
        deletedPaths.addAll(MavenFacetUtils.getPathWithHashes(groupPath));
        // we have metadata for plugins at the Group level so we should build that as well
        groupChange = true;
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    finally {
      UnitOfWork.end();
    }

    boolean rebuild;
    if (groupChange) {
      rebuild = rebuild(repository, true, false, groupId, null, null);
    }
    else {
      rebuild = rebuild(repository, true, false, groupId, artifactId, null);
    }

    if (rebuild) {
      return Collections.emptySet();
    } else {
      return deletedPaths;
    }
  }

  /**
   * Inner class that encapsulates the work, as metadata builder is stateful.
   */
  private static class Worker
      extends ComponentSupport
  {
    private final Repository repository;

    private final MavenFacet mavenFacet;

    private final MavenPathParser mavenPathParser;

    private final MetadataBuilder metadataBuilder;

    private final MetadataUpdater metadataUpdater;

    private final Map<String, Object> sqlParams;

    private final String sql;
    
    private final boolean rebuildChecksums;

    private final int bufferSize;

    private final long timeoutSeconds;

    public Worker(final Repository repository, // NOSONAR
                  final boolean update,
                  final boolean rebuildChecksums,
                  @Nullable final String groupId,
                  @Nullable final String artifactId,
                  @Nullable final String baseVersion,
                  final int bufferSize,
                  final int timeoutSeconds
    )
    {
      this.repository = repository;
      this.mavenFacet = repository.facet(MavenFacet.class);
      this.mavenPathParser = mavenFacet.getMavenPathParser();
      this.metadataBuilder = new MetadataBuilder();
      this.metadataUpdater = new MetadataUpdater(update, repository);
      this.sqlParams = Maps.newHashMap();
      this.sql = buildSql(groupId, artifactId, baseVersion);
      this.rebuildChecksums = rebuildChecksums;
      this.bufferSize = bufferSize;
      this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Builds up SQL and populates parameters map for it based on passed in parameters. As side effect, it populates
     * the {@link #sqlParams} map too with required parameters.
     */
    private String buildSql(@Nullable final String groupId,
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

    /**
     * Returns {@link Iterable} with Orient documents for GAVs.
     */
    private Iterable<ODocument> browseGAVs() {
      return Transactional.operation.call(() -> {
        final StorageTx tx = UnitOfWork.currentTx();
        return tx.browse(sql, sqlParams, bufferSize, timeoutSeconds);
      });
    }

    /**
     * Method rebuilding metadata that performs the group level processing. It uses memory conservative "async" SQL
     * approach, and calls {@link #rebuildMetadataInner(String, String, Set, MultipleFailures)} method as results are
     * arriving.
     */
    public boolean rebuildMetadata()
    {
      final MultipleFailures failures = new MultipleFailures();
      boolean metadataRebuilt = false;

      checkCancellation();
      String currentGroupId = null;

      try {
        for (ODocument doc : browseGAVs()) {
          checkCancellation();
          final String groupId = doc.field("groupId", OType.STRING);
          final String artifactId = doc.field("artifactId", OType.STRING);
          final Set<String> baseVersions = doc.field("baseVersions", OType.EMBEDDEDSET);

          final boolean groupChange = !Objects.equals(currentGroupId, groupId);
          if (groupChange) {
            if (currentGroupId != null) {
              rebuildMetadataExitGroup(currentGroupId, failures);
            }
            currentGroupId = groupId;
            metadataBuilder.onEnterGroupId(groupId);
          }
          rebuildMetadataInner(groupId, artifactId, baseVersions, failures);
          metadataRebuilt = true;
        }

        if (currentGroupId != null) {
          rebuildMetadataExitGroup(currentGroupId, failures);
          metadataRebuilt = true;
        }
      }
      finally {
        maybeLogFailures(failures);
      }

      return metadataRebuilt;
    }

    /**
     * Logs any failures recorded during metadata
     */
    private void maybeLogFailures(final MultipleFailures failures) {
      if (failures.isEmpty()) {
        return;
      }

      log.warn("Errors encountered during metadata rebuild:");
      failures.getFailures().forEach(failure -> log.warn(failure.getMessage(), failure));
    }

    /**
     * Process exits from group level, executed in isolation.
     */
    private void rebuildMetadataExitGroup(final String currentGroupId, final MultipleFailures failures) {
      processMetadata(metadataPath(currentGroupId, null, null), metadataBuilder.onExitGroupId(), failures);
    }

    /**
     * Helper method that will capture exceptions that occur from the {@link MetadataUpdater} in a
     * {@link MultipleFailures} store
     */
    private void processMetadata(final MavenPath metadataPath,
                                 final Maven2Metadata metadata,
                                 final MultipleFailures failures)
    {
      try {
        metadataUpdater.processMetadata(metadataPath, metadata);
      }
      catch (Exception e) {
        failures.add(new MetadataException("Error processing metadata for path: " + metadataPath.getPath(), e));
      }
    }

    /**
     * Method rebuilding metadata that performs artifact and baseVersion processing. While it is called from {@link
     * #rebuildMetadata()} method, it will use a separate TX/DB to perform writes, it does NOT
     * accept the TX from caller. Executed in isolation.
     */
    private void rebuildMetadataInner(final String groupId,
                                      final String artifactId,
                                      final Set<String> baseVersions,
                                      final MultipleFailures failures)
    {
      final StorageTx tx = UnitOfWork.currentTx();

      metadataBuilder.onEnterArtifactId(artifactId);
      for (final String baseVersion : baseVersions) {
        checkCancellation();
        metadataBuilder.onEnterBaseVersion(baseVersion);

        TransactionalStoreBlob.operation.call(() -> {
          
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
                mayUpdateChecksum(asset, mavenPath, HashType.SHA1);
                mayUpdateChecksum(asset, mavenPath, HashType.MD5);
              }
              final String packaging = component.formatAttributes().get(Attributes.P_PACKAGING, String.class);
              log.debug("POM packaging: {}", packaging);
              if ("maven-plugin".equals(packaging)) {
                metadataBuilder.addPlugin(getPluginPrefix(mavenPath.locateMainArtifact("jar")), artifactId,
                    component.formatAttributes().get(Attributes.P_POM_NAME, String.class));
              }
            }
          }

          processMetadata(metadataPath(groupId, artifactId, baseVersion), metadataBuilder.onExitBaseVersion(), failures);

          return null;
        });
      }

      processMetadata(metadataPath(groupId, artifactId, null), metadataBuilder.onExitArtifactId(), failures);
    }

    /**
     * Verifies and may fix/create the broken/non-existent Maven hashes (.sha1/.md5 files).
     */
    private void mayUpdateChecksum(final Asset asset, final MavenPath mavenPath,
                                   final HashType hashType)
    {
      HashCode checksum = asset.getChecksum(hashType.getHashAlgorithm());
      if (checksum == null) {
        // this means that an asset stored in maven repository lacks checksum required by maven repository (see maven facet)
        log.warn("Asset with path {} lacks checksum {}", mavenPath, hashType);
        return;
      }
      String assetChecksum = checksum.toString();
      final MavenPath checksumPath = mavenPath.hash(hashType);
      try {
        final Content content = mavenFacet.get(checksumPath);
        if (content != null) {
          try (InputStream is = content.openInputStream()) {
            final String mavenChecksum = DigestExtractor.extract(is);
            if (Objects.equals(assetChecksum, mavenChecksum)) {
              return; // all is OK: exists and matches
            }
          }
        }
      }
      catch (IOException e) {
        log.warn("Error reading {}", checksumPath, e);
      }
      // we need to generate/write it
      try {
        log.debug("Generating checksum file: {}", checksumPath);
        final StringPayload mavenChecksum = new StringPayload(assetChecksum, Constants.CHECKSUM_CONTENT_TYPE);
        mavenFacet.put(checksumPath, mavenChecksum);
      }
      catch (IOException e) {
        log.warn("Error writing {}", checksumPath, e);
        throw new RuntimeException(e);
      }
    }

    /**
     * Parses the DOM of a XML.
     */
    private Xpp3Dom parse(final MavenPath mavenPath, final InputStream is) {
      try {
        Xpp3Dom dom = MavenModels.parseDom(is);
        if (dom == null) {
          log.debug("Could not parse POM: {} @ {}", repository.getName(), mavenPath.getPath());
        }
        return dom;
      }
      catch (IOException e) {
        log.warn("Could not parse POM: {} @ {}", repository.getName(), mavenPath.getPath(), e);
        throw new RuntimeException(e);
      }
    }

    /**
     * Returns the plugin prefix of a Maven plugin, by opening up the plugin JAR, and reading the Maven Plugin
     * Descriptor. If fails, falls back to mangle artifactId (ie. extract XXX from XXX-maven-plugin or
     * maven-XXX-plugin).
     */
    private String getPluginPrefix(final MavenPath mavenPath) {
      // sanity checks: is artifact and extension is "jar", only possibility for maven plugins currently
      checkArgument(mavenPath.getCoordinates() != null);
      checkArgument(Objects.equals(mavenPath.getCoordinates().getExtension(), "jar"));
      String prefix = null;
      try {
        final Content jarFile = mavenFacet.get(mavenPath);
        if (jarFile != null) {
          try (ZipInputStream zip = new ZipInputStream(jarFile.openInputStream())) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
              if (!entry.isDirectory() && "META-INF/maven/plugin.xml".equals(entry.getName())) {
                final Xpp3Dom dom = parse(mavenPath, zip);
                prefix = getChildValue(dom, "goalPrefix", null);
                break;
              }
              zip.closeEntry();
            }
          }
        }
      }
      catch (IOException e) {
        log.warn("Unable to read plugin.xml of {}", mavenPath, e);
      }
      if (prefix != null) {
        return prefix;
      }
      if ("maven-plugin-plugin".equals(mavenPath.getCoordinates().getArtifactId())) {
        return "plugin";
      }
      else {
        return mavenPath.getCoordinates().getArtifactId().replaceAll("-?maven-?", "").replaceAll("-?plugin-?", "");
      }
    }

    /**
     * Helper method to get node's immediate child or default.
     */
    private String getChildValue(final Xpp3Dom doc, final String childName, final String defaultValue) {
      Xpp3Dom child = doc.getChild(childName);
      if (child == null) {
        return defaultValue;
      }
      return child.getValue();
    }
  }
}
