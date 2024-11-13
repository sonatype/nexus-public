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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
      try (InputStream is = itemsByRole.get(SnapshotItem.Role.RELEASE_INLINE_INDEX).content.openInputStream()) {
        if (is != null) {
          ArmoredInputStream aIs = new ArmoredInputStream(is);
          releaseStream = new AptFilterInputStream(aIs);
        }
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

    return result;
  }

  private String createAssetPath(final String id, final String path) {
    return "/snapshots/" + id + "/" + path;
  }

  protected abstract List<SnapshotItem> fetchSnapshotItems(final List<SnapshotItem.ContentSpecifier> specs)
      throws IOException;
}
