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
package org.sonatype.nexus.repository.content.search;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.search.normalize.VersionNumberExpander;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.time.format.DateTimeFormatter.ofPattern;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalAssetId;
import static org.sonatype.nexus.repository.content.store.InternalIds.toExternalId;
import static org.sonatype.nexus.repository.search.index.SearchConstants.*;

/**
 * Default {@link SearchDocumentProducer} that combines properties of components and their assets.
 *
 * @since 3.25
 */
@Named
@Singleton
public class DefaultSearchDocumentProducer
    extends ComponentSupport
    implements SearchDocumentProducer
{
  private static final DateTimeFormatter DATE_TIME_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

  private static final ObjectWriter WRITER = new ObjectMapper().writerWithDefaultPrettyPrinter();

  private final Set<SearchDocumentExtension> documentExtensions;

  @Inject
  public DefaultSearchDocumentProducer(final Set<SearchDocumentExtension> documentExtensions) {
    this.documentExtensions = checkNotNull(documentExtensions);
  }

  @Override
  public String getDocument(final FluentComponent component, final Map<String, Object> commonFields) {
    checkNotNull(component);
    checkNotNull(commonFields);

    Map<String, Object> componentDoc = new HashMap<>();
    componentDoc.put(GROUP, component.namespace());
    componentDoc.put(NAME, component.name());
    componentDoc.put(VERSION, component.version());
    componentDoc.put(ATTRIBUTES, component.attributes().backing());

    componentDoc.put(NORMALIZED_VERSION, getNormalizedVersion(component));
    componentDoc.put(IS_PRERELEASE_KEY, isPrerelease(component));

    Collection<FluentAsset> assets = component.assets();

    lastBlobUpdated(assets).ifPresent(dateTime -> componentDoc.put(LAST_BLOB_UPDATED_KEY, format(dateTime)));
    lastDownloaded(assets).ifPresent(dateTime -> componentDoc.put(LAST_DOWNLOADED_KEY, format(dateTime)));

    List<Map<String, Object>> assetDocs = new ArrayList<>();
    for (Asset asset : assets) {
      Map<String, Object> assetDoc = new HashMap<>();
      assetDoc.put(ID, toExternalId(internalAssetId(asset)).getValue());
      assetDoc.put(NAME, asset.path());
      Map<String, Object> attributes = new HashMap<>(asset.attributes().backing());
      assetDoc.put(CONTENT_TYPE, "");
      asset.blob().ifPresent(blob -> {
        assetDoc.put(CONTENT_TYPE, blob.contentType());
        assetDoc.put(UPLOADER, blob.createdBy().orElse(null));
        assetDoc.put(UPLOADER_IP, blob.createdByIp().orElse(null));
        assetDoc.put(FILE_SIZE, blob.blobSize());
        asset.lastDownloaded().ifPresent(dateTime -> assetDoc.put(LAST_DOWNLOADED_KEY, format(dateTime)));
        attributes.put("checksum", blob.checksums());

        // Not ideal, but demonstrates why strongly typed objects would be better than Maps of attributes.
        Map<String, Object> content = new HashMap<>();
        content.put("last_modified", blob.blobCreated().toInstant().toEpochMilli());
        attributes.put("content", content);
      });
      assetDoc.put(ATTRIBUTES, attributes);
      assetDocs.add(assetDoc);
    }
    if (!assetDocs.isEmpty()) {
      componentDoc.put(ASSETS, assetDocs);
    }

    for (SearchDocumentExtension extension : documentExtensions) {
      componentDoc.putAll(extension.getFields(component));
    }

    componentDoc.putAll(commonFields);

    try {
      return WRITER.writeValueAsString(componentDoc);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Override this method to mark certain components as "pre-release" in search.
   */
  protected boolean isPrerelease(final FluentComponent component) {
    return false;
  }

  /**
   * Override this method to customize how versions are normalized in search.
   */
  protected String getNormalizedVersion(final FluentComponent component) {
    return VersionNumberExpander.expand(component.version());
  }

  /**
   * Finds the last time one of the component's assets was downloaded.
   */
  @VisibleForTesting
  Optional<OffsetDateTime> lastDownloaded(final Collection<? extends Asset> assets) {
    return assets.stream()
        .map(Asset::lastDownloaded)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .max(OffsetDateTime::compareTo);
  }

  /**
   * Finds the last time one of the component's assets was updated.
   */
  @VisibleForTesting
  Optional<OffsetDateTime> lastBlobUpdated(final Collection<? extends Asset> assets) {
    return assets.stream()
        .map(Asset::blob)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(AssetBlob::blobCreated)
        .max(OffsetDateTime::compareTo);
  }

  /**
   * Formats the given {@link OffsetDateTime} as an ISO timestamp.
   */
  private static String format(final OffsetDateTime value) {
    return value.format(DATE_TIME_FORMATTER);
  }
}
