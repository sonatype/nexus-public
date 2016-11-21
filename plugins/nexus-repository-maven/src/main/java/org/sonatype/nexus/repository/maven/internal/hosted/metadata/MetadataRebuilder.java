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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
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
import org.sonatype.nexus.repository.maven.internal.MavenModels;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.maven.internal.hosted.metadata.MetadataUtils.metadataPath;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_GROUP;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_BUCKET;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

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
   */
  public void rebuild(final Repository repository,
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
      new Worker(repository, update, rebuildChecksums, groupId, artifactId, baseVersion).rebuildMetadata();
    }
    finally {
      UnitOfWork.end();
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
   */
  public void deleteAndRebuild(final Repository repository, final String groupId,
                               final String artifactId, final String baseVersion)
  {
    checkNotNull(repository);
    checkNotNull(groupId);
    checkNotNull(artifactId);
    checkNotNull(baseVersion);

    final StorageTx tx = repository.facet(StorageFacet.class).txSupplier().get();
    UnitOfWork.beginBatch(tx);
    boolean groupChange = false;
    try {
      // Delete the specific GAV
      MetadataUtils.delete(repository, metadataPath(groupId, artifactId, baseVersion));
      // Delete the GA; will be rebuilt as necessary but may hold the last GAV in which case rebuild would ignore it
      MetadataUtils.delete(repository, metadataPath(groupId, artifactId, null));

      // Check explicitly for whether or not we have Group level metadata that might need rebuilding, since this
      // is potentially the most expensive possible path to take.
      MavenPath groupPath = metadataPath(groupId, null, null);
      if (MetadataUtils.read(repository, groupPath) != null) {
        MetadataUtils.delete(repository, groupPath);
        // we have metadata for plugins at the Group level so we should build that as well
        groupChange = true;
      }
    }
    catch (IOException e) {
      Throwables.propagate(e);
    }
    finally {
      UnitOfWork.end();
    }

    if (groupChange) {
      rebuild(repository, true, false, groupId, null, null);
    }
    else {
      rebuild(repository, true, false, groupId, artifactId, null);
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

    public Worker(final Repository repository,
                  final boolean update,
                  final boolean rebuildChecksums,
                  @Nullable final String groupId,
                  @Nullable final String artifactId,
                  @Nullable final String baseVersion)
    {
      this.repository = repository;
      this.mavenFacet = repository.facet(MavenFacet.class);
      this.mavenPathParser = mavenFacet.getMavenPathParser();
      this.metadataBuilder = new MetadataBuilder();
      this.metadataUpdater = new MetadataUpdater(update, repository);
      this.sqlParams = Maps.newHashMap();
      this.sql = buildSql(groupId, artifactId, baseVersion);
      this.rebuildChecksums = rebuildChecksums;
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
          String.format(
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
        return tx.browse(sql, sqlParams);
      });
    }

    /**
     * Method rebuilding metadata that performs the group level processing. It uses memory conservative "async" SQL
     * approach, and calls {@link #rebuildMetadataInner(String, String, Set)} method as results are arriving.
     */
    public void rebuildMetadata()
    {
      String currentGroupId = null;
      for (ODocument doc : browseGAVs()) {
        final String groupId = doc.field("groupId", OType.STRING);
        final String artifactId = doc.field("artifactId", OType.STRING);
        final Set<String> baseVersions = doc.field("baseVersions", OType.EMBEDDEDSET);

        final boolean groupChange = !Objects.equals(currentGroupId, groupId);
        if (groupChange) {
          if (currentGroupId != null) {
            rebuildMetadataExitGroup(currentGroupId);
          }
          currentGroupId = groupId;
          metadataBuilder.onEnterGroupId(groupId);
        }
        rebuildMetadataInner(groupId, artifactId, baseVersions);
      }
      if (currentGroupId != null) {
        rebuildMetadataExitGroup(currentGroupId);
      }
    }

    /**
     * Process exits from group level, executed in isolation.
     */
    private void rebuildMetadataExitGroup(final String currentGroupId) {
      metadataUpdater.processMetadata(
          MetadataUtils.metadataPath(currentGroupId, null, null),
          metadataBuilder.onExitGroupId()
      );
    }

    /**
     * Method rebuilding metadata that performs artifact and baseVersion processing. While it is called from {@link
     * #rebuildMetadata()} method, it will use a separate TX/DB to perform writes, it does NOT
     * accept the TX from caller. Executed in isolation.
     */
    private void rebuildMetadataInner(final String groupId,
                                      final String artifactId,
                                      final Set<String> baseVersions)
    {
      final StorageTx tx = UnitOfWork.currentTx();

      metadataBuilder.onEnterArtifactId(artifactId);
      for (final String baseVersion : baseVersions) {
        metadataBuilder.onEnterBaseVersion(baseVersion);

        TransactionalStoreBlob.operation.call(() -> {
          final Iterable<Component> components = tx.findComponents(
              "group = :groupId and name = :artifactId and attributes.maven2." + Attributes.P_BASE_VERSION +
                  " = :baseVersion",
              ImmutableMap.<String, Object>of(
                  "groupId", groupId,
                  "artifactId", artifactId,
                  "baseVersion", baseVersion
              ),
              ImmutableList.of(repository),
              null // order by
          );

          for (Component component : components) {
            for (Asset asset : tx.browseAssets(component)) {
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

          metadataUpdater.processMetadata(
              MetadataUtils.metadataPath(groupId, artifactId, baseVersion),
              metadataBuilder.onExitBaseVersion()
          );

          return null;
        });
      }

      metadataUpdater.processMetadata(
          MetadataUtils.metadataPath(groupId, artifactId, null),
          metadataBuilder.onExitArtifactId()
      );
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
        throw Throwables.propagate(e);
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
        throw Throwables.propagate(e);
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
