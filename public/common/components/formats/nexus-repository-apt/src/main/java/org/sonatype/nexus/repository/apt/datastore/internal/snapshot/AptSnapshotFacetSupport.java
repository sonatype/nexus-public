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
package org.sonatype.nexus.repository.apt.datastore.internal.snapshot;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.apt.internal.AptFacetHelper;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFileParser;
import org.sonatype.nexus.repository.apt.internal.debian.Release;
import org.sonatype.nexus.repository.apt.internal.snapshot.AptFilterInputStream;
import org.sonatype.nexus.repository.apt.internal.snapshot.AptSnapshotFacet;
import org.sonatype.nexus.repository.apt.internal.snapshot.SnapshotComponentSelector;
import org.sonatype.nexus.repository.apt.internal.snapshot.SnapshotItem;
import org.sonatype.nexus.repository.apt.internal.snapshot.SnapshotItem.ContentSpecifier;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.bcpg.ArmoredInputStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Basic implementation of snapshots for apt datastore repositories.
 *
 * @since 3.31
 */
public abstract class AptSnapshotFacetSupport
    extends FacetSupport
    implements AptSnapshotFacet
{
  // Pre-compiled: matches <component>/binary-<arch>/Packages[.gz|.bz2|.xz]
  // [^/]+ ensures exactly one path segment before binary-, preventing greedy
  // matching across debian-installer sub-paths.
  private static final Pattern PACKAGE_INDEX_PATTERN =
      Pattern.compile("[^/]+/binary-[^/]+/Packages(\\.gz|\\.bz2|\\.xz)?$");

  @Override
  public boolean isSnapshotableFile(final String path) {
    return !path.endsWith(".deb") && !path.endsWith(".DEB");
  }

  @Override
  public void createSnapshot(final String id, final SnapshotComponentSelector selector) throws IOException {
    Iterable<SnapshotItem> snapshots = collectSnapshotItems(selector);
    createSnapshot(id, snapshots);
  }

  protected void createSnapshot(final String id, final Iterable<SnapshotItem> snapshots) throws IOException {
    checkNotNull(id);
    AptContentFacet contentFacet = facet(AptContentFacet.class);
    for (SnapshotItem item : snapshots) {
      String assetPath = createAssetPath(id, item.specifier.path);
      try (InputStream is = item.content.openInputStream();
          TempBlob tempBlob = contentFacet.getTempBlob(is, item.specifier.role.getMimeType())) {
        contentFacet.findOrCreateMetadataAsset(tempBlob, assetPath);
      }
    }
  }

  @Override
  @Nullable
  public Content getSnapshotFile(final String id, final String path) {
    checkNotNull(id);
    checkNotNull(path);

    String assetPath = createAssetPath(id, path);
    AptContentFacet contentFacet = facet(AptContentFacet.class);

    return contentFacet.get(assetPath).orElse(null);
  }

  @Override
  public void deleteSnapshot(final String id) {
    checkNotNull(id);

    AptContentFacet contentFacet = facet(AptContentFacet.class);
    String path = createAssetPath(id, StringUtils.EMPTY);
    contentFacet.deleteAssetsByPrefix(path);
  }

  protected Iterable<SnapshotItem> collectSnapshotItems(final SnapshotComponentSelector selector) throws IOException {
    AptContentFacet aptFacet = getRepository().facet(AptContentFacet.class);

    List<SnapshotItem> releaseIndexItems =
        fetchSnapshotItems(AptFacetHelper.getReleaseIndexSpecifiers(aptFacet.isFlat(), aptFacet.getDistribution()));
    Map<SnapshotItem.Role, SnapshotItem> itemsByRole = new EnumMap<>(
        releaseIndexItems.stream().collect(Collectors.toMap((SnapshotItem item) -> item.specifier.role, item -> item)));
    InputStream releaseStream = null;
    SnapshotItem snapshotItem = itemsByRole.get(SnapshotItem.Role.RELEASE_INDEX);
    if (snapshotItem != null) {
      releaseStream = snapshotItem.content.openInputStream();
    }
    else {
      SnapshotItem inlineItem = itemsByRole.get(SnapshotItem.Role.RELEASE_INLINE_INDEX);
      if (inlineItem != null) {
        // Do NOT use try-with-resources here: releaseStream wraps the opened stream
        // and must stay open until parseControlFile() finishes reading it.
        // The finally block below closes releaseStream when done.
        InputStream is = inlineItem.content.openInputStream();
        releaseStream = new AptFilterInputStream(new ArmoredInputStream(is));
      }
    }

    if (releaseStream == null) {
      throw new IOException("Invalid upstream repository: no release index present");
    }

    Release release;
    try {
      ControlFile index = new ControlFileParser().parseControlFile(releaseStream);
      release = new Release(index);
    }
    finally {
      releaseStream.close();
    }

    List<SnapshotItem> result = new ArrayList<>(releaseIndexItems);
    if (aptFacet.isFlat()) {
      result.addAll(fetchSnapshotItems(
          AptFacetHelper.getReleasePackageIndexes(aptFacet.isFlat(), aptFacet.getDistribution(), null, null)));
    }
    else {
      List<String> archs = selector.getArchitectures(release);
      List<String> comps = selector.getComponents(release);
      for (String arch : archs) {
        for (String comp : comps) {
          result.addAll(fetchSnapshotItems(
              AptFacetHelper.getReleasePackageIndexes(aptFacet.isFlat(), aptFacet.getDistribution(), comp, arch)));
        }
      }
    }

    if (!aptFacet.isFlat()) {
      // Add by-hash files using stored checksums from content attributes
      result.addAll(fetchByHashPackageItems(result, aptFacet));
    }

    // Collect remaining metadata files discovered from the Release manifest.
    // The Release file's SHA256/SHA1/MD5Sum sections enumerate every metadata file in the
    // distribution. This picks up i18n/Translation-*, dep11/, source/, Contents-*, and any
    // future Debian metadata types — without hardcoding file types or language codes.
    result.addAll(fetchManifestMetadataItems(release, aptFacet));

    return deduplicated(result);
  }

  private static List<SnapshotItem> deduplicated(final List<SnapshotItem> items) {
    Set<String> seen = new HashSet<>();
    List<SnapshotItem> unique = new ArrayList<>(items.size());
    for (SnapshotItem item : items) {
      if (seen.add(item.specifier.path)) {
        unique.add(item);
      }
    }
    return unique;
  }

  private List<SnapshotItem> fetchManifestMetadataItems(
      final Release release,
      final AptContentFacet aptFacet) throws IOException
  {
    String dist = aptFacet.getDistribution();
    List<ContentSpecifier> specs = new ArrayList<>();

    for (String relativePath : release.getManifestFiles()) {
      if (isAlreadyHandled(relativePath)) {
        continue;
      }
      String fullPath = aptFacet.isFlat()
          ? relativePath
          : String.format("dists/%s/%s", dist, relativePath);
      specs.add(new ContentSpecifier(fullPath, AptFacetHelper.resolveMetadataRole(relativePath)));
    }

    return specs.isEmpty() ? Collections.emptyList() : fetchSnapshotItems(specs);
  }

  // package-private for direct unit testing of the regex logic
  static boolean isAlreadyHandled(final String relativePath) {
    if (relativePath.contains("/by-hash/")) {
      return true;
    }
    return PACKAGE_INDEX_PATTERN.matcher(relativePath).matches();
  }

  /**
   * Fetch by-hash files for package indexes using stored checksums
   */
  private List<SnapshotItem> fetchByHashPackageItems(
      final List<SnapshotItem> packageItems,
      final AptContentFacet aptFacet) throws IOException
  {
    List<SnapshotItem> byHashItems = new ArrayList<>();
    for (SnapshotItem item : packageItems) {
      // Only process package index files, not release files
      if (isPackageIndexFile(item.specifier.role)) {
        byHashItems.addAll(createByHashItems(item, aptFacet));
      }
    }

    return byHashItems;
  }

  /**
   * Check if the role represents a package index file
   */
  private static boolean isPackageIndexFile(final SnapshotItem.Role role) {
    return role == SnapshotItem.Role.PACKAGE_INDEX_RAW ||
        role == SnapshotItem.Role.PACKAGE_INDEX_GZ ||
        role == SnapshotItem.Role.PACKAGE_INDEX_BZ2 ||
        role == SnapshotItem.Role.PACKAGE_INDEX_XZ;
  }

  /**
   * Create by-hash items for a package file using its stored checksums
   */
  private List<SnapshotItem> createByHashItems(
      final SnapshotItem packageItem,
      final AptContentFacet aptFacet) throws IOException
  {
    return Optional.ofNullable(packageItem.content)
        .map(content -> content.getAttributes().get(Asset.class))
        .filter(Asset::hasBlob)
        .flatMap(Asset::blob)
        .map(AssetBlob::checksums)
        .filter(checksums -> !checksums.isEmpty())
        .map(checksums -> {
          try {
            return fetchByHashItems(packageItem, aptFacet.getDistribution(), checksums);
          }
          catch (IOException e) {
            log.warn("Failed to fetch by-hash items for: {}", packageItem.specifier.path,
                log.isDebugEnabled() ? e : null);
            return Collections.<SnapshotItem>emptyList();
          }
        })
        .orElseGet(() -> {
          log.debug("No checksums available for: {}", packageItem.specifier.path);
          return Collections.emptyList();
        });
  }

  private List<SnapshotItem> fetchByHashItems(
      final SnapshotItem packageItem,
      final String dist,
      final Map<String, String> checksums) throws IOException
  {
    List<SnapshotItem> byHashItems = new ArrayList<>();
    String component = packageItem.specifier.component;
    String arch = packageItem.specifier.architecture;

    if (component == null || arch == null) {
      log.debug("Missing component or architecture metadata for: {}", packageItem.specifier.path);
      return byHashItems;
    }

    List<ContentSpecifier> byHashSpecs = new ArrayList<>();
    for (Map.Entry<String, String> entry : checksums.entrySet()) {
      String algorithm = entry.getKey();
      String hash = entry.getValue();

      if (algorithm == null || StringUtils.isBlank(hash)) {
        continue;
      }

      if (!hash.matches("[a-fA-F0-9]+")) {
        log.warn("Skipping by-hash specifier: unexpected checksum format for algorithm {}", algorithm);
        continue;
      }

      ContentSpecifier byHashSpec =
          AptFacetHelper.getByHashContentSpecifier(dist, component, arch, algorithm, hash, packageItem.specifier.role);
      byHashSpecs.add(byHashSpec);
    }

    if (!byHashSpecs.isEmpty()) {
      List<SnapshotItem> byHashFiles = fetchSnapshotItems(byHashSpecs);
      byHashItems.addAll(byHashFiles);

      if (!byHashFiles.isEmpty()) {
        log.debug("Added {} by-hash files for {}", byHashFiles.size(), packageItem.specifier.path);
      }
    }

    return byHashItems;
  }

  private String createAssetPath(final String id, final String path) {
    return "/snapshots/" + id + "/" + path;
  }

  protected abstract List<SnapshotItem> fetchSnapshotItems(
      final List<SnapshotItem.ContentSpecifier> specs) throws IOException;
}
