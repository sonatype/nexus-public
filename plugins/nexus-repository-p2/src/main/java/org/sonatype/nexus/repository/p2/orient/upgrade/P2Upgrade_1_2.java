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
package org.sonatype.nexus.repository.p2.orient.upgrade;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.common.upgrade.DependsOn;
import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.orient.DatabaseUpgradeSupport;
import org.sonatype.nexus.repository.p2.internal.AssetKind;
import org.sonatype.nexus.repository.p2.internal.P2Format;
import org.sonatype.nexus.repository.p2.internal.metadata.UriToSiteHashUtil;
import org.sonatype.nexus.repository.p2.orient.internal.proxy.OrientP2ProxyFacet;

import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;

/**
 * Upgrade step to update {@code name} for p2 assets migrating to support nested composite repositories and delete
 * browse_node entries for p2 repositories forcing them to be rebuilt by
 * {@link org.sonatype.nexus.repository.browse.internal.RebuildBrowseNodesManager}.
 *
 * @since 3.28
 */
@Named
@Singleton
@Upgrades(model = P2Model.NAME, from = "1.1", to = "1.2")
@DependsOn(model = DatabaseInstanceNames.COMPONENT, version = "1.14", checkpoint = true)
@DependsOn(model = DatabaseInstanceNames.CONFIG, version = "1.8", checkpoint = true)
public class P2Upgrade_1_2
    extends AbstractP2Upgrade
{
  private static final String P_NAME = "name";

  private static final String SELECT_BY_ASSET_KIND = "SELECT FROM asset WHERE bucket = ? AND attributes.p2.asset_kind IN ?";

  private static final String SELECT_NESTED_ASSETS =
      "SELECT FROM asset WHERE bucket = ? AND (name like 'http/%' OR name like 'https/%')";

  private static final String SELECT_REPOSITORY_URL = "SELECT attributes.proxy.remoteUrl FROM repository WHERE repository_name = ?";

  private static final String UPDATE_ASSET_KIND = "update asset set attributes.p2.asset_kind = ? " +
          "where attributes.p2.asset_kind in ?";

  public static final String MARKER_FILE = P2Upgrade_1_2.class.getSimpleName() + ".marker";

  private final Pattern metadataPattern = Pattern.compile("^(?<url>.*\\/)(?<path>(content|artifacts|compositeContent|compositeArtifacts)\\.(jar|xml|xml\\.xz))$");

  private final Pattern bundlePattern = Pattern.compile("^(?<url>.*\\/)(?<path>(features|binary|plugins)\\/(.*))$");

  private Path markerFile;

  @Inject
  public P2Upgrade_1_2(
      @Named(DatabaseInstanceNames.CONFIG) final Provider<DatabaseInstance> configDatabaseInstance,
      @Named(DatabaseInstanceNames.COMPONENT) final Provider<DatabaseInstance> componentDatabaseInstance,
      final ApplicationDirectories directories)
  {
    super(configDatabaseInstance, componentDatabaseInstance);
    this.markerFile = new File(directories.getWorkDirectory("db"), MARKER_FILE).toPath();
  }

  @Override
  public void apply() throws IOException {
    if (hasSchemaClass(configDatabaseInstance, "repository")
        && hasSchemaClass(componentDatabaseInstance, ASSET_CLASS_NAME)) {
      List<String> p2RepositoryNames = getP2RepositoryNames();

      if (!p2RepositoryNames.isEmpty()) {
        updateAssetKinds();
        p2RepositoryNames.forEach(this::updateNestedAssets);
        p2RepositoryNames.forEach(this::updateRootMetadata);
      }
      if (!Files.exists(markerFile)) {
        Files.createFile(markerFile);
      }
    }
  }

  private void updateNestedAssets(final String repositoryName) {
    log.debug("Update nested assets in {}", repositoryName);

    DatabaseUpgradeSupport.withDatabase(componentDatabaseInstance, db -> {
      bucketFor(db, repositoryName).ifPresent(bucket -> {
        List<ODocument> assets =
            db.<List<ODocument>> query(new OSQLSynchQuery<ODocument>(SELECT_NESTED_ASSETS), bucket.getIdentity());

        Map<String, String> pathToSite = new HashMap<>();

        int count = 0;
        for (ODocument doc : assets) {
          String name = doc.field(P_NAME);
          log.trace("Updating {}", P_NAME);

          Matcher metadataMatcher = metadataPattern.matcher(name);
          Matcher bundleMatcher = bundlePattern.matcher(name);
          if (!metadataMatcher.matches() && !bundleMatcher.matches()) {
            log.warn("Skipping unknown asset: {}", name);
            continue;
          }

          Matcher match = metadataMatcher.matches() ? metadataMatcher : bundleMatcher;
          String url = LegacyPathUtil.unescapePathToUri(match.group("url"));
          String path = match.group("path");
          String siteHash = pathToSite.computeIfAbsent(url, UriToSiteHashUtil::map);
          log.trace("url:'{}' path:'{}' siteHash:'{}'", url, path, siteHash);

          doc.field(P_NAME, siteHash + '/' + path);

          if (metadataMatcher.matches()) {
            Map<String, Object> attributes = doc.field(P_ATTRIBUTES, OType.EMBEDDEDMAP);
            Map<String, Object> p2Attributes =
                (Map<String, Object>) attributes.computeIfAbsent(P2Format.NAME, n -> new HashMap<String, Object>());
            p2Attributes.put(OrientP2ProxyFacet.REMOTE_HASH, siteHash);
            p2Attributes.put(OrientP2ProxyFacet.REMOTE_URL, url + path);

            doc.field(P_ATTRIBUTES, attributes, OType.EMBEDDEDMAP);
          }

          db.save(doc);

          count++;
        }

        if (count > 0) {
          log.info("Updated {} p2 asset(s) in repository {}: ", count, repositoryName);
        }
      });
    });
  }

  private void updateRootMetadata(final String repositoryName) {
    log.debug("Updating attributes for root composites in {}", repositoryName);

    String repositoryUrl = getRepositoryUrl(repositoryName);
    if (Strings2.isBlank(repositoryUrl)) {
      log.warn("Skipping {} unable to determine repository url", repositoryName);
      return;
    }

    DatabaseUpgradeSupport.withDatabase(componentDatabaseInstance, db -> {
      bucketFor(db, repositoryName).ifPresent(bucket -> {
        List<ODocument> assets = db.<List<ODocument>> query(
            new OSQLSynchQuery<ODocument>(SELECT_BY_ASSET_KIND),
            bucket.getIdentity(),
            Arrays.asList(AssetKind.COMPOSITE_ARTIFACTS.toString(), AssetKind.COMPOSITE_CONTENT.toString(),
                AssetKind.ARTIFACTS_METADATA.toString(), AssetKind.CONTENT_METADATA.toString()));

        int count = 0;
        for (ODocument doc : assets) {
          String path = doc.field(P_NAME);
          Map<String, Object> attributes = doc.field(P_ATTRIBUTES, OType.EMBEDDEDMAP);
          Map<String, Object> p2Attributes =
              (Map<String, Object>) attributes.computeIfAbsent(P2Format.NAME, n -> new HashMap<String, Object>());
          p2Attributes.put(OrientP2ProxyFacet.REMOTE_URL, repositoryUrl + path);

          doc.field(P_ATTRIBUTES, attributes, OType.EMBEDDEDMAP);
          db.save(doc);

          count++;
        }

        if (count > 0) {
          log.info("Updated {} p2 asset(s) names in repository {}: ", count, repositoryName);
        }
      });
    });
  }

  private String getRepositoryUrl(final String repositoryName) {
    String[] repositoryUrl = new String[1];

    DatabaseUpgradeSupport.withDatabase(configDatabaseInstance, db -> {
      List<String> results =
          db.<List<ODocument>> query(new OSQLSynchQuery<ODocument>(SELECT_REPOSITORY_URL), repositoryName).stream()
              .map(doc -> (String) doc.field("attributes")).collect(Collectors.toList());
      if (results.isEmpty()) {
        return;
      }
      repositoryUrl[0] = results.get(0);

      if (!repositoryUrl[0].endsWith("/")) {
        repositoryUrl[0] += '/';
      }
    });
    return repositoryUrl[0];
  }

  private void updateAssetKinds() {
    updateAssetKind(AssetKind.ARTIFACTS_METADATA, Arrays.asList("ARTIFACT_JAR", "ARTIFACT_XML", "ARTIFACT_XML_XZ"));
    updateAssetKind(AssetKind.CONTENT_METADATA, Arrays.asList("CONTENT_JAR", "CONTENT_XML", "CONTENT_XML_XZ"));
    updateAssetKind(AssetKind.COMPOSITE_ARTIFACTS, Arrays.asList("COMPOSITE_ARTIFACTS_JAR", "COMPOSITE_ARTIFACTS_XML"));
    updateAssetKind(AssetKind.COMPOSITE_CONTENT, Arrays.asList("COMPOSITE_CONTENT_JAR", "COMPOSITE_CONTENT_XML"));
    updateAssetKind(AssetKind.BINARY_BUNDLE, Arrays.asList("COMPONENT_BINARY"));
    updateAssetKind(AssetKind.BUNDLE, Arrays.asList("COMPONENT_PLUGINS", "COMPONENT_FEATURES"));
  }

  private void updateAssetKind(final AssetKind kind, final List<String> oldKinds) {
    OCommandSQL updateAssetCommand = new OCommandSQL(UPDATE_ASSET_KIND);

    DatabaseUpgradeSupport.withDatabase(componentDatabaseInstance, db -> {
      db.command(updateAssetCommand).execute(kind.toString(), oldKinds);
    });
  }
}
