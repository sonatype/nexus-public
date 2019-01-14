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
package org.sonatype.nexus.repository.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Streams;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link ComponentMetadataProducer} implementation that uses all properties of a component & its assets as
 * metadata.
 *
 * @since 3.0
 */
@Named
@Singleton
public class DefaultComponentMetadataProducer
    implements ComponentMetadataProducer
{

  public static final String ATTRIBUTES = "attributes";

  public static final String CONTENT_TYPE = "content_type";

  public static final String FORMAT = "format";

  public static final String NAME = "name";

  public static final String GROUP = "group";

  public static final String REPOSITORY_NAME = "repository_name";

  public static final String VERSION = "version";

  public static final String NORMALIZED_VERSION = "normalized_version";

  public static final String ASSETS = "assets";

  public static final String ID = "id";

  public static final String IS_PRERELEASE_KEY = "isPrerelease";

  public static final String LAST_BLOB_UPDATED_KEY = "lastBlobUpdated";

  public static final String LAST_DOWNLOADED_KEY = "lastDownloaded";

  private static final Logger log = LoggerFactory.getLogger(DefaultComponentMetadataProducer.class);

  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("YYYY-MM-dd'T'HH:mm:ss.SSSZ");

  private final Set<ComponentMetadataProducerExtension> componentMetadataProducerExtensions;

  @Inject
  public DefaultComponentMetadataProducer(final Set<ComponentMetadataProducerExtension> componentMetadataProducerExtensions) {
    this.componentMetadataProducerExtensions = checkNotNull(componentMetadataProducerExtensions);
  }

  @Override
  public String getMetadata(final Component component,
                            final Iterable<Asset> assets,
                            final Map<String, Object> additional)
  {
    checkNotNull(component);
    checkNotNull(assets);
    checkNotNull(additional);

    Map<String, Object> metadata = new HashMap<>();
    put(metadata, FORMAT, component.format());
    put(metadata, GROUP, component.group());
    put(metadata, NAME, component.name());
    put(metadata, VERSION, component.version());
    put(metadata, ATTRIBUTES, component.attributes().backing());

    // Prepend numbers in the version with 0s to make each number 5 digits
    String normalizedVersion = getNormalizedVersion(component);
    put(metadata, NORMALIZED_VERSION, normalizedVersion);

    put(metadata, IS_PRERELEASE_KEY, isPrerelease(component, assets));
    lastBlobUpdated(assets)
        .ifPresent(dateTime -> put(metadata, LAST_BLOB_UPDATED_KEY, dateTime.toString(DATE_TIME_FORMATTER)));
    lastDownloaded(assets)
        .ifPresent(dateTime -> put(metadata, LAST_DOWNLOADED_KEY, dateTime.toString(DATE_TIME_FORMATTER)));

    List<Map<String, Object>> allAssetMetadata = new ArrayList<>();
    for (Asset asset : assets) {
      Map<String, Object> assetMetadata = new HashMap<>();
      put(assetMetadata, ID, documentId(asset));
      put(assetMetadata, NAME, asset.name());
      put(assetMetadata, CONTENT_TYPE, asset.contentType());
      put(assetMetadata, ATTRIBUTES, asset.attributes().backing());

      allAssetMetadata.add(assetMetadata);
    }
    if (!allAssetMetadata.isEmpty()) {
      metadata.put(ASSETS, allAssetMetadata);
    }

    for (ComponentMetadataProducerExtension componentMetadataProducerExtension : componentMetadataProducerExtensions) {
      metadata.putAll(componentMetadataProducerExtension.getComponentMetadata(component));
    }

    metadata.putAll(additional);

    try {
      return JsonUtils.from(metadata);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @VisibleForTesting
  public Optional<DateTime> lastDownloaded(final Iterable<Asset> assets)
  {
    return getDateTime(assets, Asset::lastDownloaded);
  }

  @VisibleForTesting
  public Optional<DateTime> lastBlobUpdated(final Iterable<Asset> assets)
  {
    return getDateTime(assets, Asset::blobUpdated);
  }

  private Optional<DateTime> getDateTime(final Iterable<Asset> assets,
                                         final Function<Asset, DateTime> dateTimeFunction)
  {
    return Streams.stream(assets)
        .map(dateTimeFunction)
        .filter(Objects::nonNull)
        .max(DateTime::compareTo);
  }

  protected boolean isPrerelease(final Component component,
                                 final Iterable<Asset> assets)
  {
    return false;
  }

  private String documentId(final Asset asset) {
    return EntityHelper.id(asset).getValue();
  }

  private static void put(final Map<String, Object> metadata, final String key, final Object value) {
    if (value != null) {
      metadata.put(key, value);
    }
  }

  public String getNormalizedVersion(Component component) {
    String version = component.version();
    if (StringUtils.isBlank(version)) {
      return "";
    }

    Matcher matcher = Pattern.compile("\\d+").matcher(version);
    StringBuilder replacementBuilder = new StringBuilder();
    int position = 0;
    while (matcher.find()) {
      replacementBuilder.append(version, position, matcher.start());
      position = matcher.end();
      try {
        replacementBuilder.append(String.format("%09d", Long.parseLong(matcher.group())));
      }
      catch (NumberFormatException e) {
        log.debug("Unable to parse number as long '{}'", matcher.group());
        replacementBuilder.append(matcher.group());
      }
    }
    return replacementBuilder.toString();
  }

}
