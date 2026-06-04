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
package org.sonatype.nexus.repository.apt.datastore.internal.proxy.metadata;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.common.cooperation2.Cooperation2Factory;
import org.sonatype.nexus.common.time.Clock;
import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.proxy.AptProxyFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.metadata.AptMetadataFacetSupport;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile.Paragraph;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFileParser;
import org.sonatype.nexus.repository.apt.internal.gpg.AptSigningFacet;
import org.sonatype.nexus.repository.apt.internal.hosted.CompressingTempFileStore;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.repository.apt.internal.AptProperties.GZ;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.P_ARCHITECTURE;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.P_INDEX_SECTION;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.P_PACKAGE_NAME;
import static org.sonatype.nexus.repository.apt.internal.AptProperties.P_PACKAGE_VERSION;

/**
 * APT proxy metadata facet for merging upstream metadata with cached packages and re-signing.
 * <p>
 * When signing is configured, this facet:
 * - Merges upstream package metadata with locally cached packages
 * - Re-signs the metadata with the proxy's GPG key
 * - Ensures cached packages remain available even when removed from upstream
 * <p>
 * When signing is not configured, the proxy operates in passthrough mode.
 */
@Component
@Qualifier(AptFormat.NAME + "-proxy")
@Exposed
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AptProxyMetadataFacet
    extends AptMetadataFacetSupport
{
  /**
   * ThreadLocal to safely pass reset flag from rebuildMetadata(boolean) to doRebuildMetadata().
   * Ensures thread-safety when multiple threads call rebuildMetadata on the same facet instance.
   */
  private final ThreadLocal<Boolean> resetRequested = ThreadLocal.withInitial(() -> false);

  private static final Pattern ARCHITECTURES_PATTERN = Pattern.compile("^Architectures:\\s*(.+)$", Pattern.MULTILINE);

  private static final Pattern COMPONENTS_PATTERN = Pattern.compile("^Components:\\s*(.+)$", Pattern.MULTILINE);

  /**
   * Field name used in Debian control files for package filename.
   */
  private static final String FILENAME_FIELD = "Filename: ";

  /**
   * Length of hash prefix to display in debug logs (first 8 hex characters).
   */
  private static final int HASH_PREFIX_LENGTH = 8;

  /**
   * Separator for composite keys (component|architecture) in CompressingTempFileStore.
   */
  private static final String SLICE_KEY_SEPARATOR = "|";

  private static final String SLICE_KEY_SEPARATOR_PATTERN = "\\|";

  private static final Comparator<PackageEntry> STABLE_COMPARATOR = Comparator
      .comparing(PackageEntry::packageName)
      .thenComparing(PackageEntry::version)
      .thenComparing(PackageEntry::architecture)
      .thenComparing(PackageEntry::filename);

  @Autowired
  public AptProxyMetadataFacet(
      final ObjectMapper mapper,
      final Clock clock,
      final Cooperation2Factory cooperationFactory,
      @Value("${nexus.apt.metadata.cooperation.enabled:true}") final boolean cooperationEnabled,
      @Value("${nexus.apt.metadata.cooperation.majorTimeout:0s}") final Duration majorTimeout,
      @Value("${nexus.apt.metadata.cooperation.minorTimeout:30s}") final Duration minorTimeout,
      @Value("${nexus.apt.metadata.cooperation.threadsPerKey:100}") final int threadsPerKey)
  {
    super(mapper, clock, cooperationFactory, cooperationEnabled, majorTimeout, minorTimeout, threadsPerKey);
  }

  /**
   * Gets metadata for a path, handling staleness check and rebuild if needed.
   * This is the main entry point for serving proxy metadata.
   *
   * @param context the HTTP request context
   * @param path the metadata file path
   * @param distribution the distribution name (may be null)
   * @return the metadata content, or empty if unavailable (rebuilding)
   */
  public Optional<Content> getMetadata(final Context context, final String path, final String distribution) {
    // Get existing metadata
    Optional<Content> existing = content().get(path);

    if (existing.isEmpty()) {
      // No metadata yet - trigger rebuild and return empty (client will retry)
      if (distribution != null) {
        log.info("No metadata found for: {} - triggering rebuild", path);
        tryRebuild(context, distribution);
      }
      return Optional.empty();
    }

    Content content = existing.get();

    // Check if stale
    if (!isStale(context, content)) {
      // Fresh metadata - serve it
      log.trace("Serving fresh metadata for: {}", path);
      return existing;
    }

    // Stale metadata - check if upstream changed
    log.info("Metadata is stale for: {} - checking upstream", path);

    if (distribution == null) {
      // Can't check upstream without distribution - serve stale
      return existing;
    }

    UpstreamCheckResult upstreamCheck = checkUpstreamChanges(distribution);

    if (!upstreamCheck.changed()) {
      // Upstream unchanged - refresh timestamp and serve fresh content
      log.debug("Upstream unchanged for distribution: {} - refreshing timestamp", distribution);
      Optional<Content> refreshed = refreshTimestamp(path);
      return refreshed.isPresent() ? refreshed : existing;
    }

    // Upstream changed - rebuild and WAIT for completion (up to 30s timeout)
    log.info("Upstream changed for distribution: {} - rebuilding and waiting", distribution);
    return tryRebuildAndWait(context, distribution, path);
  }

  /**
   * Attempts to rebuild metadata and wait for completion using Cooperation2.
   * Only one thread per distribution will actually rebuild; others wait.
   *
   * @param context the HTTP request context
   * @param distribution the distribution name
   * @param path the metadata file path being requested
   * @return the rebuilt metadata, or empty if rebuild failed or timed out
   */
  private Optional<Content> tryRebuildAndWait(
      final Context context,
      final String distribution,
      final String path)
  {
    try {
      // Use Cooperation2 to rebuild and wait for completion
      // Only one thread per distribution rebuilds, others wait
      Content rebuiltContent = cooperation.on(() -> {
        // Leader: execute rebuild in this thread
        log.info("Rebuilding metadata for distribution: {}", distribution);
        try {
          rebuildDistributionMetadata(context, distribution);

          // Fetch fresh metadata
          return content().get(path).orElse(null);
        }
        catch (IOException e) {
          log.error("Failed to rebuild metadata for distribution: {}", distribution, e);
          return null;
        }
      })
          .cooperate(distribution);

      Optional<Content> rebuilt = Optional.ofNullable(rebuiltContent);

      if (rebuilt.isPresent()) {
        log.info("Rebuild completed, serving fresh metadata for: {}", path);
        return rebuilt;
      }
      else {
        log.warn("Rebuild completed but metadata not found or failed for: {}", path);
        return Optional.empty(); // Handler will return 503
      }
    }
    catch (Exception e) {
      // Timeout (30s) or other error - return empty so Handler returns 503
      log.warn("Rebuild timeout or error for distribution: {} - client should retry", distribution, e);
      return Optional.empty(); // Handler will return 503 Service Unavailable
    }
  }

  /**
   * Attempts to rebuild metadata using Cooperation2 for coordination.
   * Only one thread per distribution will actually rebuild.
   */
  private void tryRebuild(final Context context, final String distribution) {
    try {
      cooperation.on(() -> {
        try {
          rebuildDistributionMetadata(context, distribution);
          return null;
        }
        catch (IOException e) {
          log.error("Failed to rebuild metadata for distribution: {}", distribution, e);
          return null;
        }
      })
          .cooperate(distribution);
    }
    catch (Exception e) {
      log.warn("Error coordinating rebuild for distribution: {}", distribution, e);
    }
  }

  /**
   * Checks if metadata content is stale based on MetadataMaxAge.
   */
  private boolean isStale(final Context context, final Content content) {
    CacheInfo cacheInfo = content.getAttributes().get(CacheInfo.class);

    if (cacheInfo != null) {
      // If CacheInfo exists, use standard cache staleness check
      CacheController cacheController = getMetadataCacheController();
      boolean stale = cacheController.isStale(cacheInfo);
      log.debug("Staleness check (CacheInfo): lastVerified={}, stale={}", cacheInfo.getLastVerified(), stale);
      return stale;
    }

    // For generated metadata without CacheInfo, check lastModified against MaxAge
    Long lastModifiedMillis = extractLastModifiedMillis(content);

    if (lastModifiedMillis == null) {
      log.warn("No CacheInfo or lastModified found for metadata, assuming fresh");
      return false;
    }

    // Get MetadataMaxAge from cache controller config
    // For generated metadata, always use the metadata cache controller
    CacheController cacheController = getMetadataCacheController();
    int maxAgeSeconds = cacheController.getContentMaxAgeSeconds();

    long ageSeconds = (System.currentTimeMillis() - lastModifiedMillis) / 1000;
    boolean stale = ageSeconds > maxAgeSeconds;

    log.debug("Staleness check: age={}s, maxAge={}s, stale={}", ageSeconds, maxAgeSeconds, stale);
    return stale;
  }

  /**
   * Extracts lastModified timestamp in milliseconds from Content attributes.
   * Handles both DateTime (legacy upstream passthrough) and Long (generated metadata) formats.
   *
   * @param content the content to extract timestamp from
   * @return timestamp in milliseconds, or null if not found
   */
  private Long extractLastModifiedMillis(final Content content) {
    // Try DateTime first (legacy format from upstream passthrough)
    org.joda.time.DateTime lastModifiedDateTime =
        content.getAttributes().get(Content.CONTENT_LAST_MODIFIED, org.joda.time.DateTime.class);
    if (lastModifiedDateTime != null) {
      return lastModifiedDateTime.getMillis();
    }

    // Try Long format (our generated metadata)
    return content.getAttributes().get(Content.CONTENT_LAST_MODIFIED, Long.class);
  }

  /**
   * Gets the metadata cache controller for staleness checks and cache management.
   * <p>
   * For generated proxy metadata, we always use the metadata cache controller (not the content
   * cache controller) because metadata files have different staleness requirements than
   * content files (packages).
   * <p>
   * The metadata cache controller respects the MetadataMaxAge configuration setting.
   *
   * @return the metadata cache controller
   */
  private CacheController getMetadataCacheController() {
    AptProxyFacet aptProxyFacet = getRepository().facet(AptProxyFacet.class);
    return aptProxyFacet.getMetadataCacheController();
  }

  /**
   * Checks if upstream Release file has changed.
   */
  private UpstreamCheckResult checkUpstreamChanges(final String distribution) {
    try {
      boolean isFlat = content().isFlat();

      // Fetch upstream Release
      Optional<byte[]> upstreamRelease = fetchUpstreamRelease(distribution, isFlat);

      if (upstreamRelease.isEmpty()) {
        // Upstream unavailable - assume changed
        log.debug("Upstream Release unavailable for distribution: {}", distribution);
        return new UpstreamCheckResult(true, null);
      }

      // Extract the actual Release content from InRelease (strip PGP header and signature)
      // This is necessary because each repo signs with its own key, so signatures differ
      byte[] releaseContent = extractReleaseContent(upstreamRelease.get());
      String upstreamHash = calculateSHA256(releaseContent);

      log.debug("Fetched {} bytes from upstream for distribution: {}", releaseContent.length, distribution);
      log.debug("Upstream hash: {}", upstreamHash);

      // Compare with stored hash
      String storedHash = keyValue().getUpstreamReleaseHash(distribution);
      log.debug("Stored hash: {}", storedHash);

      if (storedHash == null) {
        log.debug("No stored hash for distribution: {} - assuming changed", distribution);
        // Save the hash for next time
        keyValue().setUpstreamReleaseHash(distribution, upstreamHash);
        return new UpstreamCheckResult(true, upstreamHash);
      }

      boolean changed = !upstreamHash.equals(storedHash);
      log.debug("Upstream hash comparison for {}: stored={}, upstream={}, changed={}",
          distribution, storedHash.substring(0, HASH_PREFIX_LENGTH), upstreamHash.substring(0, HASH_PREFIX_LENGTH),
          changed);

      if (changed) {
        // Update stored hash
        keyValue().setUpstreamReleaseHash(distribution, upstreamHash);
      }

      return new UpstreamCheckResult(changed, upstreamHash);
    }
    catch (RuntimeException e) {
      // Parsing errors, hash calculation issues, or unexpected runtime errors
      log.warn("Error checking upstream changes for distribution: {} - {}", distribution, e.getMessage());
      log.debug("Full stack trace:", e);
      return new UpstreamCheckResult(true, null);
    }
    catch (Exception e) {
      // Checked exceptions or other unexpected errors
      log.warn("Unexpected error checking upstream changes for distribution: {}", distribution, e);
      return new UpstreamCheckResult(true, null);
    }
  }

  /**
   * Fetches upstream Release file (tries InRelease first, then Release).
   */
  private Optional<byte[]> fetchUpstreamRelease(
      final String distribution,
      final boolean isFlat)
  {
    // Try InRelease first
    String inReleasePath = isFlat ? "InRelease" : "dists/" + distribution + "/InRelease";
    Optional<byte[]> inRelease = fetchUpstreamContentRaw(inReleasePath);
    if (inRelease.isPresent()) {
      return inRelease;
    }

    // Fall back to Release
    String releasePath = isFlat ? "Release" : "dists/" + distribution + "/Release";
    return fetchUpstreamContentRaw(releasePath);
  }

  /**
   * Extracts Release content from raw bytes (removes PGP header and signature if present).
   * <p>
   * InRelease files are clearsigned and have this format:
   * 
   * <pre>
   * -----BEGIN PGP SIGNED MESSAGE-----
   * Hash: SHA256
   *
   * Origin: Ubuntu        &lt;-- actual Release content starts here
   * Label: Ubuntu
   * ...
   *                       &lt;-- content ends before signature
   * -----BEGIN PGP SIGNATURE-----
   * ...
   * -----END PGP SIGNATURE-----
   * </pre>
   * <p>
   * We extract only the Release content (between the blank line after Hash and the signature)
   * because each repository signs with its own key, so signatures will always differ.
   *
   * @param rawContent the raw bytes from upstream (may be clearsigned InRelease or plain Release)
   * @return the Release content without PGP header/signature
   */
  private static byte[] extractReleaseContent(final byte[] rawContent) {
    String content = new String(rawContent, StandardCharsets.UTF_8);

    // Check if this is a clearsigned file (InRelease)
    if (content.startsWith("-----BEGIN PGP SIGNED MESSAGE-----")) {
      // Find the blank line after the hash header (marks start of actual content)
      int headerEnd = content.indexOf("\n\n");
      if (headerEnd < 0) {
        headerEnd = content.indexOf("\r\n\r\n");
      }

      // Find the signature start (marks end of actual content)
      int signatureStart = content.indexOf("\n-----BEGIN PGP SIGNATURE-----");
      if (signatureStart < 0) {
        signatureStart = content.indexOf("\r\n-----BEGIN PGP SIGNATURE-----");
      }

      if (headerEnd > 0 && signatureStart > headerEnd) {
        // Extract content between header and signature
        String releaseContent = content.substring(headerEnd, signatureStart).trim();
        return releaseContent.getBytes(StandardCharsets.UTF_8);
      }
    }

    // Plain Release file (not clearsigned) - return as-is
    return rawContent;
  }

  /**
   * Calculates SHA256 hash of content.
   */
  private String calculateSHA256(final byte[] content) {
    return DigestUtils.sha256Hex(content);
  }

  /**
   * Refreshes the cache info on existing metadata (marks as recently verified).
   * Uses markAsCached() which is the standard Nexus mechanism for cache management.
   */
  private Optional<Content> refreshTimestamp(final String path) {
    try {
      Optional<FluentAsset> asset = content().getAsset(path);
      if (asset.isPresent()) {
        FluentAsset fluentAsset = asset.get();

        // Get current CacheInfo for logging
        CacheInfo oldCacheInfo = fluentAsset.attributes().get(CacheInfo.class);
        log.debug("Before refresh - CacheInfo: {}", oldCacheInfo);

        // Get the current cache info from the controller (has current timestamp)
        CacheController cacheController = getMetadataCacheController();
        CacheInfo newCacheInfo = cacheController.current();

        // Mark as cached with current cache info - this updates CACHE attributes
        fluentAsset.markAsCached(newCacheInfo);

        log.debug("Refreshed cache info for: {} - new CacheInfo: {}", path, newCacheInfo);

        // Return fresh Content with updated cache info
        return Optional.of(fluentAsset.download());
      }
      else {
        log.warn("Asset not found for timestamp refresh: {}", path);
      }
    }
    catch (Exception e) {
      log.warn("Failed to refresh timestamp for: {}", path, e);
    }
    return Optional.empty();
  }

  /**
   * Result of checking upstream changes.
   */
  private record UpstreamCheckResult(boolean changed, String hash)
  {
  }

  public Optional<Content> rebuildMetadata(final boolean shouldReset) throws IOException {
    resetRequested.set(shouldReset);
    try {
      return super.rebuildMetadata();
    }
    finally {
      resetRequested.remove();
    }
  }

  @Override
  protected Content doRebuildMetadata() throws IOException {
    boolean shouldReset = resetRequested.get();

    // Clear all existing metadata if this is a reset
    if (shouldReset) {
      content().deleteAssetsByPrefix("/dists/");
    }

    AptSigningFacet signingFacet = signing();

    // If signing is not configured, operate in passthrough mode
    if (!signingFacet.isConfigured()) {
      log.debug("Signing not configured for proxy repository: {} - passthrough mode",
          getRepository().getName());
      return null;
    }

    log.info("Rebuilding proxy metadata for repository: {}", getRepository().getName());

    Set<String> distributions = getDistributionsToProcess();
    if (distributions.isEmpty()) {
      log.info("No distributions tracked for repository: {}. " +
          "If you recently added signing to an existing proxy repository, " +
          "run 'apt update' from your client to track distributions and trigger metadata generation.",
          getRepository().getName());
      return null;
    }

    FluentAsset lastReleaseAsset = null;
    for (String distribution : distributions) {
      lastReleaseAsset = rebuildDistributionMetadata(null, distribution);
    }

    return lastReleaseAsset != null ? lastReleaseAsset.download() : null;
  }

  /**
   * Determines which distributions to process based on configuration.
   * <p>
   * - If a distribution is configured, only process that one
   * - If no distribution is configured (blank), process all tracked distributions
   */
  private Set<String> getDistributionsToProcess() {
    String configuredDist = content().getDistribution();
    boolean enforce = content().isEnforceDistribution();

    if (enforce && !Strings.isNullOrEmpty(configuredDist)) {
      // Enforce mode: only process the configured distribution
      return Set.of(configuredDist);
    }

    // Multi-distribution mode: process all tracked distributions
    return keyValue().getTrackedDistributions();
  }

  /**
   * Rebuilds metadata for a single distribution by merging upstream and cached packages.
   * <p>
   * Algorithm:
   * 1. Discover slices (component/architecture) from upstream Release
   * 2. For each slice:
   * - Fetch upstream Packages (if reachable)
   * - Load previous generated Packages (preserves cached-only packages)
   * - Compute required filenames = union(upstream, previous)
   * - Load cached entries matching those filenames
   * - Merge: upstream first, then cached overrides (cached has correct local checksums)
   * 3. Write Packages files with deterministic ordering (one per component/architecture)
   * 4. Generate Release + Sign
   *
   * @param context the HTTP request context (may be null for scheduled rebuilds)
   * @param distribution the distribution name to rebuild metadata for
   * @return the Release file asset
   * @throws IOException if the rebuild fails
   */
  public FluentAsset rebuildDistributionMetadata(
      @Nullable final Context context,
      final String distribution) throws IOException
  {
    log.debug("Rebuilding metadata for distribution: {} in repository: {}",
        distribution, getRepository().getName());

    AptContentFacet aptFacet = content();
    AptSigningFacet signingFacet = signing();

    // Discover slices from upstream Release file
    Set<Slice> slices = discoverSlicesFromUpstream(context, distribution);
    if (slices.isEmpty()) {
      log.debug("No slices discovered for distribution: {}, falling back to cached packages", distribution);
      return rebuildFromCachedOnly(distribution, aptFacet, signingFacet);
    }

    StringBuilder sha256Builder = new StringBuilder();
    StringBuilder md5Builder = new StringBuilder();
    // Use TreeSet for deterministic ordering in Release file
    Set<String> architectures = new TreeSet<>();
    Set<String> components = new TreeSet<>();

    // Read cached packages from KV store (O(1) instead of scanning pool)
    Map<String, PackageEntry> allCachedPackages = loadCachedPackagesFromKV();
    log.debug("Found {} total cached packages in KV store", allCachedPackages.size());

    // Build filename -> entry map for O(1) lookups during merge
    Map<String, PackageEntry> cachedByFilename = new HashMap<>();
    allCachedPackages.values().forEach(e -> cachedByFilename.put(e.filename(), e));

    // Add slices for cached architectures not advertised by upstream
    // This ensures cached packages persist even if upstream stops advertising their architecture
    Set<String> missingArchitectures = findMissingArchitectures(allCachedPackages, slices);
    addSlicesForMissingArchitectures(slices, missingArchitectures);

    try (CompressingTempFileStore store = new CompressingTempFileStore()) {
      // Key by Slice (component + architecture) to avoid mixing components
      Map<Slice, Writer> writersBySlice = new HashMap<>();

      try {
        for (Slice slice : slices) {
          // 1. Fetch upstream Packages (if reachable)
          Map<String, PackageEntry> upstreamEntries = fetchUpstreamPackages(context, distribution, slice);
          log.debug("Fetched {} upstream entries for {}/{}", upstreamEntries.size(), slice.component(),
              slice.architecture());

          // 2. Load previous generated Packages (preserves cached-only packages)
          Map<String, PackageEntry> previousEntries = loadPreviousGeneratedPackages(distribution, slice);
          log.debug("Loaded {} previous entries for {}/{}", previousEntries.size(), slice.component(),
              slice.architecture());

          // 3. Compute required filenames = union(upstream, previous)
          Set<String> requiredFilenames = new HashSet<>();
          upstreamEntries.values().forEach(e -> requiredFilenames.add(e.filename()));
          previousEntries.values().forEach(e -> requiredFilenames.add(e.filename()));

          // 4. Find cached entries matching required filenames (O(1) per lookup via cachedByFilename map)
          Map<String, PackageEntry> cachedEntries = new HashMap<>();
          for (String filename : requiredFilenames) {
            PackageEntry cached = cachedByFilename.get(filename);
            if (cached != null) {
              cachedEntries.put(cached.key(), cached);
            }
          }

          // Additionally, include ALL cached packages matching this slice's architecture
          // This ensures cached packages persist even if deleted from upstream and not in previousEntries
          // (NEXUS-49457: Keep cached packages available after upstream deletion)
          addCachedPackagesForArchitecture(cachedEntries, allCachedPackages, slice);

          // 5. Merge: upstream first, then cached overrides (cached has correct local checksums)
          Map<String, PackageEntry> merged = new LinkedHashMap<>();
          merged.putAll(upstreamEntries);
          merged.putAll(cachedEntries); // cached wins - has correct local checksums

          if (merged.isEmpty()) {
            log.debug("No packages for slice {}/{}", slice.component(), slice.architecture());
            continue;
          }

          // Sort for deterministic output
          List<PackageEntry> sortedEntries = merged.values()
              .stream()
              .sorted(STABLE_COMPARATOR)
              .collect(Collectors.toList());

          architectures.add(slice.architecture());
          components.add(slice.component());
          log.debug("Writing {} merged packages for {}/{}", sortedEntries.size(), slice.component(),
              slice.architecture());

          // Write with deterministic ordering - use composite key for store
          String storeKey = slice.component() + SLICE_KEY_SEPARATOR + slice.architecture();
          Writer writer = writersBySlice.computeIfAbsent(slice, k -> store.openOutput(storeKey));
          for (PackageEntry entry : sortedEntries) {
            writer.write(entry.indexSection());
            writer.write("\n\n");
          }
        }
      }
      finally {
        for (Writer writer : writersBySlice.values()) {
          IOUtils.closeQuietly(writer, null);
        }
      }

      // Store package index files - parse composite key to get component and arch
      for (Map.Entry<String, CompressingTempFileStore.FileMetadata> entry : store.getFiles().entrySet()) {
        String[] parts = entry.getKey().split(SLICE_KEY_SEPARATOR_PATTERN);
        String component = parts[0];
        String arch = parts[1];
        storePackageIndexFiles(aptFacet, distribution, component, arch, entry.getValue(),
            md5Builder, sha256Builder);
      }
    }

    if (architectures.isEmpty()) {
      log.debug("No architectures found for distribution: {}", distribution);
      return null;
    }

    // Build and store Release files with actual components
    String releaseFile = buildReleaseFile(distribution, components, architectures,
        md5Builder.toString(), sha256Builder.toString());

    log.info("Generated merged metadata for distribution {} with components: {}, architectures: {}",
        distribution, components, architectures);

    return generateReleaseFiles(distribution, releaseFile, aptFacet, signingFacet);
  }

  /**
   * Fallback: rebuild from cached packages only (when upstream is unreachable).
   * Since we don't know the original component, all packages are placed in DEFAULT_COMPONENT ("main").
   */
  public FluentAsset rebuildFromCachedOnly(
      final String distribution,
      final AptContentFacet aptFacet,
      final AptSigningFacet signingFacet) throws IOException
  {
    // Read cached packages from KV store (O(1) instead of scanning pool)
    Map<String, PackageEntry> allCachedPackages = loadCachedPackagesFromKV();

    if (allCachedPackages.isEmpty()) {
      log.debug("No cached packages found in KV store for distribution: {}", distribution);
      return null;
    }

    // Group by architecture - all go into DEFAULT_COMPONENT since we don't know original component
    Map<String, List<PackageEntry>> packagesByArch = allCachedPackages.values()
        .stream()
        .collect(Collectors.groupingBy(PackageEntry::architecture));

    StringBuilder sha256Builder = new StringBuilder();
    StringBuilder md5Builder = new StringBuilder();
    Set<String> architectures = new TreeSet<>();
    Set<String> components = new TreeSet<>();
    components.add(DEFAULT_COMPONENT);

    try (CompressingTempFileStore store = new CompressingTempFileStore()) {
      Map<String, Writer> writersByArch = new HashMap<>();

      try {
        for (Map.Entry<String, List<PackageEntry>> archEntry : packagesByArch.entrySet()) {
          String arch = archEntry.getKey();
          List<PackageEntry> packages = archEntry.getValue();

          List<PackageEntry> sortedEntries = packages.stream()
              .sorted(STABLE_COMPARATOR)
              .collect(Collectors.toList());

          if (sortedEntries.isEmpty()) {
            continue;
          }

          architectures.add(arch);
          // Use composite key with DEFAULT_COMPONENT
          String storeKey = DEFAULT_COMPONENT + SLICE_KEY_SEPARATOR + arch;
          Writer writer = writersByArch.computeIfAbsent(arch, k -> store.openOutput(storeKey));
          for (PackageEntry entry : sortedEntries) {
            writer.write(entry.indexSection());
            writer.write("\n\n");
          }
        }
      }
      finally {
        for (Writer writer : writersByArch.values()) {
          IOUtils.closeQuietly(writer, null);
        }
      }

      // Store package index files - parse composite key to get component and arch
      for (Map.Entry<String, CompressingTempFileStore.FileMetadata> entry : store.getFiles().entrySet()) {
        String[] parts = entry.getKey().split(SLICE_KEY_SEPARATOR_PATTERN);
        String component = parts[0];
        String arch = parts[1];
        storePackageIndexFiles(aptFacet, distribution, component, arch, entry.getValue(),
            md5Builder, sha256Builder);
      }
    }

    if (architectures.isEmpty()) {
      return null;
    }

    String releaseFile = buildReleaseFile(distribution, components, architectures,
        md5Builder.toString(), sha256Builder.toString());

    log.info("Generated metadata from cached packages for distribution {} with components: {}, architectures: {}",
        distribution, components, architectures);

    return generateReleaseFiles(distribution, releaseFile, aptFacet, signingFacet);
  }

  /**
   * Discovers available slices (component/architecture pairs) by fetching upstream Release file.
   */
  private Set<Slice> discoverSlicesFromUpstream(@Nullable final Context context, final String distribution) {
    Set<Slice> slices = new HashSet<>();

    try {
      boolean isFlat = content().isFlat();
      String releasePath = isFlat ? "Release" : "dists/" + distribution + "/Release";
      Optional<String> releaseContent = fetchUpstreamContent(releasePath);

      if (releaseContent.isEmpty()) {
        log.debug("Could not fetch upstream Release for distribution: {}", distribution);
        return slices;
      }

      String content = releaseContent.get();

      // Parse Architectures field
      Set<String> architectures = new HashSet<>();
      Matcher archMatcher = ARCHITECTURES_PATTERN.matcher(content);
      if (archMatcher.find()) {
        String archList = archMatcher.group(1).trim();
        architectures.addAll(Arrays.asList(archList.split("\\s+")));
      }

      // Parse Components field
      Set<String> components = new HashSet<>();
      Matcher compMatcher = COMPONENTS_PATTERN.matcher(content);
      if (compMatcher.find()) {
        String compList = compMatcher.group(1).trim();
        components.addAll(Arrays.asList(compList.split("\\s+")));
      }

      // Default fallbacks
      if (architectures.isEmpty()) {
        architectures.addAll(Arrays.asList("amd64", "i386", "arm64", "armhf", "all"));
      }
      if (components.isEmpty()) {
        components.add("main");
      }

      // Build slices
      for (String component : components) {
        for (String arch : architectures) {
          slices.add(new Slice(component, arch));
        }
      }

      log.debug("Discovered {} slices from upstream Release: components={}, architectures={}",
          slices.size(), components, architectures);
    }
    catch (Exception e) {
      // Non-fatal: Return empty slices list, allowing rebuild to use cache-only fallback.
      // Upstream may be temporarily unavailable or Release file may be malformed.
      log.warn("Failed to discover slices from upstream Release for distribution: {}", distribution, e);
    }

    return slices;
  }

  /**
   * Fetches package entries directly from upstream Packages file.
   */
  private Map<String, PackageEntry> fetchUpstreamPackages(
      @Nullable final Context context,
      final String distribution,
      final Slice slice)
  {
    try {
      // Try gzipped version first (smaller)
      String packagesPath = packageIndexPath(distribution, slice.component, slice.architecture, GZ);
      Optional<byte[]> gzContent = fetchUpstreamContentRaw(packagesPath);

      if (gzContent.isPresent()) {
        try (InputStream is = new GZIPInputStream(new ByteArrayInputStream(gzContent.get()))) {
          return parsePackagesStream(is);
        }
      }

      // Fall back to plain version
      packagesPath = packageIndexPath(distribution, slice.component, slice.architecture, "");
      Optional<String> plainContent = fetchUpstreamContent(packagesPath);

      if (plainContent.isPresent()) {
        try (InputStream is = new ByteArrayInputStream(plainContent.get().getBytes(StandardCharsets.UTF_8))) {
          return parsePackagesStream(is);
        }
      }

      log.trace("No upstream Packages file found for {}/{}/{}", distribution, slice.component, slice.architecture);
      return Map.of();
    }
    catch (Exception e) {
      log.warn("Failed to fetch upstream Packages for {}/{}/{}: {}",
          distribution, slice.component, slice.architecture, e.getMessage());
      return Map.of();
    }
  }

  /**
   * Fetches content from upstream as a String.
   */
  private Optional<String> fetchUpstreamContent(final String path) {
    return fetchUpstreamContentRaw(path)
        .map(bytes -> new String(bytes, StandardCharsets.UTF_8));
  }

  /**
   * Fetches raw content from upstream.
   * Requires HTTP Context - if null (background task), returns empty.
   */
  private Optional<byte[]> fetchUpstreamContentRaw(final String path) {
    HttpContext httpContext = new BasicHttpContext();
    HttpResponse response = null;
    try {
      ProxyFacet proxyFacet = facet(ProxyFacet.class);
      HttpClientFacet httpClientFacet = facet(HttpClientFacet.class);
      HttpClient httpClient = httpClientFacet.getHttpClient();

      URI fetchUri = proxyFacet.getRemoteUrl().resolve(path);
      HttpGet getRequest = new HttpGet(fetchUri);

      // Force fresh fetch from upstream (bypass cache)
      getRequest.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
      getRequest.setHeader("Pragma", "no-cache");

      // Execute HTTP request (requires handler thread with HTTP Context)
      response = httpClient.execute(getRequest, httpContext);
      int statusCode = response.getStatusLine().getStatusCode();

      if (statusCode == HttpStatus.SC_OK) {
        HttpEntity entity = response.getEntity();
        if (entity != null) {
          return Optional.of(IOUtils.toByteArray(entity.getContent()));
        }
      }
      else if (statusCode != HttpStatus.SC_NOT_FOUND) {
        log.debug("Upstream returned {} for path: {}", statusCode, path);
      }
    }
    catch (NullPointerException e) {
      // Async tasks don't have HTTP Context - use cache-only mode
      log.debug("No HTTP Context available (async task) - skipping upstream fetch for path: {}", path);
      return Optional.empty();
    }
    catch (Exception e) {
      // Non-fatal: Return empty Optional, allowing caller to handle missing content gracefully.
      // Common scenarios: network issues, upstream downtime, or malformed responses.
      log.debug("Failed to fetch upstream content for path {}: {}", path, e.getMessage());
    }
    finally {
      HttpClientUtils.closeQuietly(response);
    }
    return Optional.empty();
  }

  /**
   * Loads package entries from previously generated Packages file.
   */
  private Map<String, PackageEntry> loadPreviousGeneratedPackages(
      final String distribution,
      final Slice slice) throws IOException
  {
    // Previous generated files are stored at the same path
    // They would have been generated by a previous rebuild
    // Use leading / for local storage lookup (content().get expects paths with leading /)
    String packagesPath = "/" + packageIndexPath(distribution, slice.component, slice.architecture, "");

    // Check if we have a generated version (distinguished by checking if signing is present)
    // For simplicity, we check if the file exists and has been modified recently
    Optional<Content> content = content().get(packagesPath);

    if (content.isEmpty()) {
      return Map.of();
    }

    // Parse the existing file - it might be upstream or previously generated
    // The merge algorithm handles both cases correctly
    return parsePackagesFile(content.get());
  }

  /**
   * Loads cached package metadata from KV store and converts to PackageEntry map.
   * This is O(n) where n is the number of cached packages, but reads are fast since
   * the data is already indexed in the KV store.
   *
   * @return map of package key to PackageEntry
   */
  private Map<String, PackageEntry> loadCachedPackagesFromKV() {
    Map<String, PackageEntry> result = new HashMap<>();

    keyValue().browsePackagesMetadata()
        .map(this::deserialize)
        .forEach(metadata -> {
          try {
            PackageEntry entry = packageEntryFromMetadata(metadata);
            if (entry != null) {
              result.put(entry.key(), entry);
            }
          }
          catch (Exception e) {
            log.warn("Failed to convert KV metadata to PackageEntry: {}", e.getMessage());
          }
        });

    return result;
  }

  /**
   * Converts KV store metadata to a PackageEntry.
   * The metadata contains the same format attributes stored during caching.
   *
   * @param metadata format attributes map from KV store
   * @return PackageEntry or null if metadata is incomplete
   */
  private PackageEntry packageEntryFromMetadata(final Map<String, Object> metadata) {
    String indexSection = (String) metadata.get(P_INDEX_SECTION);
    String packageName = (String) metadata.get(P_PACKAGE_NAME);
    String version = (String) metadata.get(P_PACKAGE_VERSION);
    String architecture = (String) metadata.get(P_ARCHITECTURE);

    if (indexSection == null || packageName == null || architecture == null) {
      log.debug("Incomplete metadata in KV store: package={}, arch={}, hasIndexSection={}",
          packageName, architecture, indexSection != null);
      return null;
    }

    // Version might be null in some edge cases, default to empty string
    if (version == null) {
      version = "";
    }

    // Extract filename from index_section
    String filename = extractFilenameFromIndexSection(indexSection);
    if (filename == null || filename.isEmpty()) {
      log.debug("Could not extract filename from index_section for package: {}", packageName);
      return null;
    }

    return new PackageEntry(packageName, version, architecture, filename, indexSection);
  }

  /**
   * Extracts the Filename field from an index section string.
   */
  private String extractFilenameFromIndexSection(final String indexSection) {
    // Look for "Filename: " pattern in the control file text
    int filenameStart = indexSection.indexOf(FILENAME_FIELD);
    if (filenameStart < 0) {
      return null;
    }
    filenameStart += FILENAME_FIELD.length();
    int filenameEnd = indexSection.indexOf('\n', filenameStart);
    if (filenameEnd < 0) {
      filenameEnd = indexSection.length();
    }
    return indexSection.substring(filenameStart, filenameEnd).trim();
  }

  /**
   * Parses a Packages file into package entries.
   */
  private Map<String, PackageEntry> parsePackagesFile(final Content content) throws IOException {
    try (var inputStream = content.openInputStream()) {
      return parsePackagesStream(inputStream);
    }
  }

  /**
   * Parses a Packages stream into package entries.
   */
  private Map<String, PackageEntry> parsePackagesStream(final InputStream inputStream) throws IOException {
    Map<String, PackageEntry> result = new LinkedHashMap<>();

    ControlFile controlFile = new ControlFileParser().parseControlFile(inputStream);

    for (Paragraph paragraph : controlFile.getParagraphs()) {
      String packageName = paragraph.getField("Package").map(f -> f.value).orElse("");
      String version = paragraph.getField("Version").map(f -> f.value).orElse("");
      String architecture = paragraph.getField("Architecture").map(f -> f.value).orElse("");
      String filename = paragraph.getField("Filename").map(f -> f.value).orElse("");

      if (!packageName.isEmpty() && !filename.isEmpty()) {
        PackageEntry entry = new PackageEntry(
            packageName, version, architecture, filename, paragraph.toString());
        result.put(entry.key(), entry);
      }
    }

    return result;
  }

  /**
   * Adds all cached packages matching a slice's architecture to the cached entries map.
   * This ensures cached packages persist even when deleted from upstream (NEXUS-49457).
   *
   * @param cachedEntries mutable map to add matching packages to
   * @param allCachedPackages all cached packages from KV store
   * @param slice the slice containing the architecture to match
   */
  private void addCachedPackagesForArchitecture(
      final Map<String, PackageEntry> cachedEntries,
      final Map<String, PackageEntry> allCachedPackages,
      final Slice slice)
  {
    int beforeCount = cachedEntries.size();
    int matchingArchCount = 0;
    for (PackageEntry cached : allCachedPackages.values()) {
      if (cached.architecture().equals(slice.architecture())) {
        matchingArchCount++;
        cachedEntries.putIfAbsent(cached.key(), cached);
      }
    }
    int addedCount = cachedEntries.size() - beforeCount;

    log.debug("Found {} cached entries for {}/{}: {} from requiredFilenames, {} added from " +
        "architecture-based inclusion (total {} matching '{}' in KV store)",
        cachedEntries.size(), slice.component(), slice.architecture(),
        beforeCount, addedCount, matchingArchCount, slice.architecture());
  }

  /**
   * Finds architectures that are present in cached packages but not advertised by upstream.
   * This supports NEXUS-49457 by identifying cached packages that need slices preserved.
   *
   * @param allCachedPackages all cached packages from KV store
   * @param slices upstream slices discovered from Release file
   * @return set of architectures present in cache but missing from upstream
   */
  private Set<String> findMissingArchitectures(
      final Map<String, PackageEntry> allCachedPackages,
      final Set<Slice> slices)
  {
    Set<String> cachedArchitectures = allCachedPackages.values()
        .stream()
        .map(PackageEntry::architecture)
        .collect(Collectors.toSet());
    Set<String> upstreamArchitectures = slices.stream()
        .map(Slice::architecture)
        .collect(Collectors.toSet());
    Set<String> missingArchitectures = new HashSet<>(cachedArchitectures);
    missingArchitectures.removeAll(upstreamArchitectures);
    return missingArchitectures;
  }

  /**
   * Adds slices for architectures that are cached but not advertised by upstream.
   * This ensures cached packages remain available even when upstream stops advertising their architecture.
   * Supports NEXUS-49457.
   *
   * @param slices mutable set of slices to add to
   * @param missingArchitectures architectures needing new slices
   */
  private void addSlicesForMissingArchitectures(
      final Set<Slice> slices,
      final Set<String> missingArchitectures)
  {
    if (!missingArchitectures.isEmpty()) {
      log.debug("Found {} cached architectures not in upstream: {}. Adding slices to preserve cached packages.",
          missingArchitectures.size(), missingArchitectures);
      // Add slices for missing architectures using the first component from existing slices
      String defaultComponent = slices.isEmpty() ? DEFAULT_COMPONENT : slices.iterator().next().component();
      for (String arch : missingArchitectures) {
        slices.add(new Slice(defaultComponent, arch));
      }
    }
  }

  private String packageIndexPath(String distribution, String component, String architecture, String extension) {
    boolean isFlat = content().isFlat();
    if (isFlat) {
      return "Packages" + extension;
    }
    return "dists/" + distribution + "/" + component + "/binary-" + architecture + "/Packages" + extension;
  }

  private AptSigningFacet signing() {
    return facet(AptSigningFacet.class);
  }

  /**
   * Represents a component/architecture slice.
   */
  private record Slice(String component, String architecture)
  {
  }

  /**
   * Represents a package entry with all relevant fields for sorting and merging.
   */
  private record PackageEntry(
      String packageName,
      String version,
      String architecture,
      String filename,
      String indexSection)
  {
    String key() {
      return packageName + ":" + version + ":" + architecture;
    }
  }
}
