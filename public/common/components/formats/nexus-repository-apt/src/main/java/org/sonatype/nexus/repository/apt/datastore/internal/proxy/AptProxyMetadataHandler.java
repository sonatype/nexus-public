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
package org.sonatype.nexus.repository.apt.datastore.internal.proxy;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Singleton;

import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.data.AptKeyValueFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.proxy.metadata.AptProxyMetadataFacet;
import org.sonatype.nexus.repository.apt.internal.gpg.AptSigningFacet;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for serving APT proxy metadata.
 * <p>
 * When signing is configured, this handler:
 * - Tracks distributions as they're accessed
 * - Serves generated (proxy-signed) metadata when available
 * - Returns 503 when generated metadata is needed but not yet available
 * - Falls through to upstream only when safe (no cached-only packages)
 * <p>
 * When signing is not configured, operates in passthrough mode.
 * <p>
 * Supports both flat and non-flat repository layouts:
 * - Non-flat: metadata under dists/distribution/ (e.g., /dists/jammy/Release)
 * - Flat: metadata at root level (e.g., /Release)
 */
@Component
@Singleton
public class AptProxyMetadataHandler
    implements Handler
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private static final Pattern DISTS_PATH_PATTERN = Pattern.compile("^/?dists/([^/]+)/(.*)$");

  // Metadata files that are generated (Release, InRelease, Release.gpg, Packages*)
  // Note: We only generate .gz and .bz2, but patterns include .xz because upstream repos may use it
  private static final Pattern METADATA_PATTERN = Pattern.compile(
      "^/?dists/[^/]+/(Release|InRelease|Release\\.gpg|.*/Packages(\\.gz|\\.bz2|\\.xz)?)$");

  // Metadata files for flat repositories (at root level, without dists/ prefix)
  private static final Pattern FLAT_METADATA_PATTERN = Pattern.compile(
      "^/?(Release|InRelease|Release\\.gpg|Packages(\\.gz|\\.bz2|\\.xz)?)$");

  @Override
  public Response handle(final Context context) throws Exception {
    String path = context.getRequest().getPath();
    AptContentFacet contentFacet = context.getRepository().facet(AptContentFacet.class);
    boolean isFlat = contentFacet.isFlat();

    // Determine if this is a metadata path and extract distribution
    MetadataInfo metadataInfo = extractMetadataInfo(path, contentFacet, isFlat);

    // Track distribution if valid (for all dists/ paths, not just metadata)
    String distribution = metadataInfo.distribution();
    if (distribution != null) {
      AptKeyValueFacet keyValueFacet = context.getRepository().facet(AptKeyValueFacet.class);
      keyValueFacet.trackDistribution(distribution);
    }

    // If not a metadata path, pass through
    if (!metadataInfo.isMetadataPath()) {
      return context.proceed();
    }

    AptSigningFacet signingFacet = context.getRepository().facet(AptSigningFacet.class);

    // If signing is not configured, pure passthrough mode
    if (!signingFacet.isConfigured()) {
      return context.proceed();
    }

    // Delegate to metadata facet for all metadata logic
    AptProxyMetadataFacet metadataFacet = context.getRepository().facet(AptProxyMetadataFacet.class);
    Optional<Content> metadata = metadataFacet.getMetadata(context, path, distribution);

    if (metadata.isPresent()) {
      return HttpResponses.ok(metadata.get());
    }

    // No metadata available - either rebuilding or passthrough
    if (distribution != null && hasGeneratedMetadata(contentFacet, distribution, isFlat)) {
      // We've generated metadata before but not available now - rebuilding
      return HttpResponses.serviceUnavailable("Metadata rebuild in progress. Please retry.");
    }

    // No generated metadata exists yet - safe to passthrough to upstream
    return context.proceed();
  }

  /**
   * Extracts metadata information from the request path.
   *
   * @param path the request path
   * @param contentFacet the content facet
   * @param isFlat whether this is a flat repository
   * @return metadata information
   */
  private MetadataInfo extractMetadataInfo(
      final String path,
      final AptContentFacet contentFacet,
      final boolean isFlat)
  {
    if (isFlat) {
      return extractFlatMetadataInfo(path, contentFacet);
    }
    else {
      return extractNonFlatMetadataInfo(path);
    }
  }

  /**
   * Extracts metadata info for flat repositories.
   */
  private MetadataInfo extractFlatMetadataInfo(final String path, final AptContentFacet contentFacet) {
    if (FLAT_METADATA_PATTERN.matcher(path).matches()) {
      String distribution = contentFacet.getDistribution();
      if (distribution == null || distribution.trim().isEmpty()) {
        log.warn("Flat repository metadata requested but no distribution configured");
        return new MetadataInfo(null, false);
      }
      return new MetadataInfo(distribution, true);
    }

    // Not metadata (pool files, etc.)
    return new MetadataInfo(null, false);
  }

  /**
   * Extracts metadata info for non-flat repositories.
   */
  private MetadataInfo extractNonFlatMetadataInfo(final String path) {
    Matcher distsMatcher = DISTS_PATH_PATTERN.matcher(path);
    if (!distsMatcher.matches()) {
      // Not a dists/ path (pool files, etc.)
      return new MetadataInfo(null, false);
    }

    String distribution = distsMatcher.group(1);
    boolean isMetadata = METADATA_PATTERN.matcher(path).matches();
    return new MetadataInfo(distribution, isMetadata);
  }

  /**
   * Checks if we have any generated metadata for the distribution.
   * If we do, we should not mix with upstream.
   *
   * @param contentFacet the content facet
   * @param distribution the distribution name
   * @param isFlat whether this is a flat repository
   * @return true if generated metadata exists
   */
  private boolean hasGeneratedMetadata(
      final AptContentFacet contentFacet,
      final String distribution,
      final boolean isFlat)
  {
    // Check for Release file - if it exists, we've generated metadata
    String releasePath = isFlat ? "/Release" : "/dists/" + distribution + "/Release";
    return contentFacet.get(releasePath).isPresent();
  }

  /**
   * Metadata information extracted from a request path.
   *
   * @param distribution the distribution name (null if not a dists/ path or flat repo without distribution)
   * @param isMetadataPath true if this is a metadata file path
   */
  private record MetadataInfo(String distribution, boolean isMetadataPath)
  {
  }
}
