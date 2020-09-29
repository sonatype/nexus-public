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
package org.sonatype.nexus.repository.content.npm.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.ws.rs.core.MediaType;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AttributeChange;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponentBuilder;
import org.sonatype.nexus.repository.content.npm.NpmContentFacet;
import org.sonatype.nexus.repository.npm.internal.NpmAttributes;
import org.sonatype.nexus.repository.npm.internal.NpmFormat;
import org.sonatype.nexus.repository.npm.internal.NpmFormatAttributesExtractor;
import org.sonatype.nexus.repository.npm.internal.NpmJsonUtils;
import org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils;
import org.sonatype.nexus.repository.npm.internal.NpmPackageId;
import org.sonatype.nexus.repository.npm.internal.NpmPackageParser;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload.InputStreamSupplier;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.repository.http.HttpStatus.OK;
import static org.sonatype.nexus.repository.npm.internal.NpmJsonUtils.mapper;
import static org.sonatype.nexus.repository.npm.internal.NpmVersionComparator.extractNewestVersion;
import static org.sonatype.nexus.repository.view.ContentTypes.APPLICATION_JSON;
import static org.sonatype.nexus.repository.view.Status.success;

/**
 * @since 3.28
 */
public abstract class NpmFacetSupport
    extends FacetSupport
{
  protected static final List<HashAlgorithm> HASH_ALGORITHMS = Collections.singletonList(SHA1);

  protected static final String REPOSITORY_ROOT_ASSET = "/-/all";

  protected static final String REPOSITORY_SEARCH_ASSET = "/-/v1/search";

  protected final NpmPackageParser npmPackageParser;

  protected NpmFacetSupport(final NpmPackageParser npmPackageParser) {
    this.npmPackageParser = npmPackageParser;
  }

  /**
   * Parses JSON content into map.
   */
  @Nonnull
  protected NestedAttributesMap parse(final Supplier<InputStream> streamSupplier) throws IOException {
    return NpmJsonUtils.parse(streamSupplier);
  }

  /**
   * Formats an asset name for a tarball out of package name and tarball filename.
   */
  @Nonnull
  public static String tarballAssetName(final NpmPackageId packageId, final String tarballName) {
    return "/" + packageId.id() + "/-/" + tarballName;
  }

  /**
   * Find a tarball component by package name and version in repository.
   */
  protected Optional<FluentComponent> findPackageTarballComponent(final NpmPackageId packageId, final String version) {
    FluentComponentBuilder components = content().components().name(packageId.name()).version(version);

    if (packageId.scope() != null) {
      components.namespace(packageId.scope());
    }
    return components.find();
  }

  /**
   * Find a repository root asset by package name in repository.
   */
  protected Optional<FluentAsset> findRepositoryRootAsset() {
    return findAsset(REPOSITORY_ROOT_ASSET);
  }

  protected Optional<FluentAsset> findAsset(final String path) {
    return content().assets().path(path).find();
  }

  /**
   * Find a package root asset by package name in repository.
   */
  protected Optional<FluentAsset> findPackageRootAsset(final NpmPackageId packageId) {
    return findAsset(NpmContentFacet.metadataPath(packageId));
  }

  /**
   * Find a tarball asset by package name and tarball filename in repository.
   */
  protected Optional<FluentAsset> findTarballAsset(final NpmPackageId packageId, final String tarballName) {
    return findAsset(tarballAssetName(packageId, tarballName));
  }

  /**
   * Returns the package root JSON content by parsing it. It also decorates the JSON document with some fields.
   */
  protected Optional<NestedAttributesMap> loadPackageRoot(final NpmPackageId packageId) throws IOException {
    Optional<Content> content = content().get(packageId);
    if (!content.isPresent()) {
      return Optional.empty();
    }

    try (InputStream in = content.get().openInputStream()) {
      NestedAttributesMap metadata = NpmJsonUtils.parse(() -> in);
      metadata.set(NpmMetadataUtils.META_ID, NpmContentFacet.metadataPath(packageId));
      return Optional.of(metadata);
    }
  }

  /**
   * Returns a {@link Supplier} that will get the {@link InputStream} for the package root associated with the given
   * {@link Asset}.
   *
   * return {@link InputStreamSupplier}
   */
  protected InputStreamSupplier openPackageRoot(final FluentAsset packageRootAsset) {
    return () -> packageRootAssetToInputStream(packageRootAsset);
  }

  /**
   * Returns a new {@link InputStream} that returns an error object. Mostly useful for NPM Responses that have already
   * been written with a successful status (like a 200) but just before streaming out content found an issue preventing
   * the intended content to be streamed out.
   *
   * @return InputStream
   */
  protected InputStream errorInputStream(final String message) {
    NestedAttributesMap errorObject = new NestedAttributesMap("error", new HashMap<>());
    errorObject.set("success", false);
    errorObject.set("error", "Failed to stream response due to: " + message);
    return new ByteArrayInputStream(NpmJsonUtils.bytes(errorObject));
  }

  /**
   * Saves the package root JSON content by persisting content into root asset's blob. It also removes some transient
   * fields from JSON document.
   */
  protected void savePackageRoot(
      final NpmPackageId packageId,
      final NestedAttributesMap packageRoot) throws IOException
  {
    packageRoot.remove("_attachments");
    Date date = NpmMetadataUtils.maintainTime(packageRoot).toDate();

    byte[] bytes = NpmJsonUtils.bytes(packageRoot);
    FluentAsset asset = content().put(packageId, new StreamPayload(() -> new ByteArrayInputStream(bytes), bytes.length, MediaType.APPLICATION_JSON));
    asset.attributes(AttributeChange.SET, NpmAttributes.P_NPM_LAST_MODIFIED, date);
  }

  protected Iterable<String> findPackageTarballComponents(final NpmPackageId packageId) {
    return content().components().versions(packageId.scope(), packageId.name());
  }

  private static InputStream packageRootAssetToInputStream(final FluentAsset packageRootAsset) throws IOException {
    return packageRootAsset.download().openInputStream();
  }

  /**
   * Converts the tags to a {@link Content} containing the tags as a json object
   */
  protected Content distTagsToContent(final NestedAttributesMap distTags) throws IOException {
    final byte[] bytes = mapper.writeValueAsBytes(distTags.backing());
    return new Content(new BytesPayload(bytes, APPLICATION_JSON));
  }

  /**
   * Updates the packageRoot with this set of dist-tags
   */
  protected void updateDistTags(
      final NpmPackageId packageId,
      final String tag,
      final Object version) throws IOException
  {
    Optional<NestedAttributesMap> optPackageRoot = loadPackageRoot(packageId);
    if (optPackageRoot.isPresent()) {
      NestedAttributesMap packageRoot = optPackageRoot.get();

      NestedAttributesMap distTags = packageRoot.child(NpmMetadataUtils.DIST_TAGS);
      distTags.set(tag, version);

      savePackageRoot(packageId, packageRoot);
    }
  }

  /**
   * Deletes the {@param tag} from the packageRoot
   */
  protected void deleteDistTags(final NpmPackageId packageId, final String tag) throws IOException {
    Optional<NestedAttributesMap> optPackageRoot = loadPackageRoot(packageId);
    if (!optPackageRoot.isPresent()) {
      return;
    }
    NestedAttributesMap packageRoot = optPackageRoot.get();

    if (packageRoot.contains(NpmMetadataUtils.DIST_TAGS)) {
      NestedAttributesMap distTags = packageRoot.child(NpmMetadataUtils.DIST_TAGS);
      distTags.remove(tag);
      savePackageRoot(packageId, packageRoot);
    }
  }

  /**
   * Removes all tags that are associated with a given {@param version}. If that version is also set as the
   * latest, the new latest is also populated from the remaining package versions stored
   */
  protected void removeDistTagsFromTagsWithVersion(final NestedAttributesMap packageRoot, final String version) {
    if (packageRoot.contains(NpmMetadataUtils.DIST_TAGS)) {
      packageRoot.child(NpmMetadataUtils.DIST_TAGS).entries().removeIf(e -> version.equals(e.getValue()));
    }
  }

  /**
   * Merges the dist-tag responses from all members and merges the values
   */
  protected Response mergeDistTagResponse(final Map<Repository, Response> responses) {
    final List<NestedAttributesMap> collection = responses.values().stream().map(Response::getPayload)
        .filter(Objects::nonNull).map(NpmFacetSupport::readDistTagResponse).filter(Objects::nonNull).collect(toList());

    final NestedAttributesMap merged = collection.get(0);
    if (collection.size() > 1) {
      collection.subList(1, collection.size())
          .forEach(response -> response.backing().forEach(populateLatestVersion(merged)));
    }

    return new Response.Builder().status(success(OK))
        .payload(new BytesPayload(NpmJsonUtils.bytes(merged), APPLICATION_JSON)).build();
  }

  /**
   * Convert an {@link Asset} representing a package root to a {@link Content} via a {@link StreamPayload}.
   *
   * @param repository       {@link Repository} to look up package root from.
   * @param packageRootAsset {@link Asset} associated with blob holding package root.
   * @return Content of asset blob
   */
  protected static NpmContent toContent(final Content content)
  {
    NpmStreamPayload payload = new NpmStreamPayload(content::openInputStream);
    return new NpmContent(payload, content);
  }

  private static BiConsumer<String, Object> populateLatestVersion(final NestedAttributesMap merged) {
    return (k, v) -> {
      if (!merged.contains(k)) {
        merged.set(k, v);
      }
      else {
        final String newestVersion = extractNewestVersion.apply(merged.get(k, String.class), (String) v);
        merged.set(k, newestVersion);
      }
    };
  }

  private static NestedAttributesMap readDistTagResponse(final Payload payload) {
    try (InputStream is = payload.openInputStream()) {
      return NpmJsonUtils.parse(() -> is);
    }
    catch (IOException ignore) { // NOSONAR
    }
    return null;
  }

  protected NpmContentFacet content() {
    return getRepository().facet(NpmContentFacet.class);
  }

  protected Map<String, Object> maybeExtractFormatAttributes(final String packageId, final String version, final TempBlob blob) {
      Map<String, Object> formatAttributes = npmPackageParser.parsePackageJson(blob::get);
      if (formatAttributes.isEmpty()) {
        log.warn("No format attributes found in package.json for npm package ID {} version {}, will not be searchable",
            packageId, version);
        return Collections.emptyMap();
      }
      else {
        NestedAttributesMap attributes = new NestedAttributesMap(NpmFormat.NAME, new HashMap<>());
        NpmFormatAttributesExtractor formatAttributesExtractor = new NpmFormatAttributesExtractor(formatAttributes);
        formatAttributesExtractor.copyFormatAttributes(attributes);
        return attributes.backing();
      }
  }
}
