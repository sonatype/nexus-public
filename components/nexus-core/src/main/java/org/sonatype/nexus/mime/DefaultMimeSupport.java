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
package org.sonatype.nexus.mime;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MediaTypeRegistry;
import org.apache.tika.mime.MimeTypes;

/**
 * Default implementation of {@link MimeSupport} component using MimeUtil2 library and the
 * {@link NexusMimeTypes}.
 *
 * @since 2.0
 */
@Named
@Singleton
public class DefaultMimeSupport
    extends ComponentSupport
    implements MimeSupport
{

  private final TikaConfig tikaConfig;

  /**
   * "Low" level Tika detector, as {@link org.apache.tika.Tika} hides too much.
   */
  private final Detector detector;

  /**
   * A loading cache of extension to MIME type.
   */
  private final LoadingCache<String, List<String>> extensionToMimeTypeCache;

  @Inject
  public DefaultMimeSupport() {
    this(new NexusMimeTypes());
  }

  @VisibleForTesting
  public DefaultMimeSupport(final NexusMimeTypes nexusMimeTypes) {
    this.tikaConfig = TikaConfig.getDefaultConfig();
    this.detector = tikaConfig.getDetector();

    // create the cache
    extensionToMimeTypeCache =
        CacheBuilder.newBuilder().maximumSize(500).build(new CacheLoader<String, List<String>>()
        {
          @Override
          public List<String> load(final String key)
              throws Exception
          {
            final List<String> detected = Lists.newArrayList();
            final NexusMimeTypes.NexusMimeType mimeType = nexusMimeTypes.getMimeTypes(key);
            if (mimeType != null) {
              // add Nexus matches first
              detected.addAll(mimeType.getMimetypes());
              if (mimeType.isOverride()) {
                return detected;
              }
            }
            // ask Tika too
            final Metadata metadata = new Metadata();
            metadata.set(Metadata.RESOURCE_NAME_KEY, "dummy." + key);
            MediaType mediaType = detector.detect(null, metadata);
            // unravel to least specific
            while (mediaType != null) {
              detected.add(mediaType.getBaseType().toString());
              mediaType = tikaConfig.getMediaTypeRegistry().getSupertype(mediaType);
            }
            return detected;
          }
        });
  }

  @Override
  public String guessMimeTypeFromPath(final MimeRulesSource mimeRulesSource, final String path) {
    if (mimeRulesSource != null) {
      final String hardRule = mimeRulesSource.getRuleForPath(path);
      if (!Strings.isNullOrEmpty(hardRule)) {
        return hardRule;
      }
    }
    return guessMimeTypeFromPath(path);
  }

  @Override
  public String guessMimeTypeFromPath(final String path) {
    final List<String> mimeTypes = guessMimeTypesListFromPath(path);
    if (mimeTypes.isEmpty()) {
      // what here?
      return MimeTypes.OCTET_STREAM;
    }
    else {
      return mimeTypes.get(0);
    }
  }

  @Override
  public List<String> guessMimeTypesListFromPath(final String path) {
    final String pathExtension = FilenameUtils.getExtension(path);
    try {
      return extensionToMimeTypeCache.get(pathExtension);
    }
    catch (ExecutionException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public String detectMimeTypesFromContent(final StorageFileItem fileItem)
      throws IOException
  {
    final List<String> mimeTypes = detectMimeTypesListFromContent(fileItem);
    if (mimeTypes.isEmpty()) {
      // what here?
      return MimeTypes.OCTET_STREAM;
    }
    else {
      return mimeTypes.get(0);
    }
  }

  @Override
  public List<String> detectMimeTypesListFromContent(final ContentLocator content)
      throws IOException
  {
    final List<String> detected = Lists.newArrayList();
    MediaType mediaType;
    try (final TikaInputStream tis = TikaInputStream.get(content.getContent())) {
      mediaType = detector.detect(tis, new Metadata());
    }
    // unravel to least specific
    while (mediaType != null) {
      detected.add(mediaType.getBaseType().toString());
      mediaType = tikaConfig.getMediaTypeRegistry().getSupertype(mediaType);
    }
    return detected;
  }

  @Override
  public List<String> detectMimeTypesListFromContent(final StorageFileItem fileItem)
      throws IOException
  {
    final List<String> detected = Lists.newArrayList();
    final Metadata metadata = new Metadata();
    metadata.set(Metadata.RESOURCE_NAME_KEY, fileItem.getName());
    MediaType mediaType;
    try (final TikaInputStream tis = TikaInputStream.get(fileItem.getInputStream())) {
      mediaType = detector.detect(tis, metadata);
    }
    // unravel to least specific
    while (mediaType != null) {
      detected.add(mediaType.getBaseType().toString());
      mediaType = tikaConfig.getMediaTypeRegistry().getSupertype(mediaType);
    }
    return detected;
  }
}
