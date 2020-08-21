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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Long.parseLong;
import static java.time.format.DateTimeFormatter.ofPattern;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalAssetId;
import static org.sonatype.nexus.repository.search.index.SearchConstants.ASSETS;
import static org.sonatype.nexus.repository.search.index.SearchConstants.ATTRIBUTES;
import static org.sonatype.nexus.repository.search.index.SearchConstants.CONTENT_TYPE;
import static org.sonatype.nexus.repository.search.index.SearchConstants.GROUP;
import static org.sonatype.nexus.repository.search.index.SearchConstants.ID;
import static org.sonatype.nexus.repository.search.index.SearchConstants.IS_PRERELEASE_KEY;
import static org.sonatype.nexus.repository.search.index.SearchConstants.LAST_BLOB_UPDATED_KEY;
import static org.sonatype.nexus.repository.search.index.SearchConstants.LAST_DOWNLOADED_KEY;
import static org.sonatype.nexus.repository.search.index.SearchConstants.NAME;
import static org.sonatype.nexus.repository.search.index.SearchConstants.NORMALIZED_VERSION;
import static org.sonatype.nexus.repository.search.index.SearchConstants.VERSION;

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
  private static final Pattern DIGITS_PATTERN = Pattern.compile("\\d+");

  private static final DateTimeFormatter DATE_TIME_FORMATTER = ofPattern("YYYY-MM-dd'T'HH:mm:ss.SSSZ");

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
      assetDoc.put(ID, internalAssetId(asset));
      assetDoc.put(NAME, asset.path());
      Map<String, Object> attributes = new HashMap<>(asset.attributes().backing());
      assetDoc.put(CONTENT_TYPE, "");
      asset.blob().ifPresent(blob -> {
        assetDoc.put(CONTENT_TYPE, blob.contentType());
        attributes.put("checksum", blob.checksums());
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
    String version = component.version();
    if (isBlank(version)) {
      return "";
    }

    // Prepend any numbers in the version with 0s to make each number 9 digits

    Matcher digitsMatcher = DIGITS_PATTERN.matcher(version);
    StringBuilder paddedVersion = new StringBuilder();

    int position = 0;
    while (digitsMatcher.find()) {
      paddedVersion.append(version, position, digitsMatcher.start());
      position = digitsMatcher.end();
      try {
        paddedVersion.append(String.format("%09d", parseLong(digitsMatcher.group())));
      }
      catch (NumberFormatException e) {
        log.debug("Unable to parse number as long '{}'", digitsMatcher.group());
        paddedVersion.append(digitsMatcher.group());
      }
    }
    return paddedVersion.toString();
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
