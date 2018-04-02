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
package org.sonatype.nexus.repository.npm.internal.search.legacy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload.InputStreamSupplier;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.common.base.Predicate;
import com.google.common.io.Closer;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.npm.internal.NpmJsonUtils.mapper;
import static org.sonatype.nexus.repository.npm.internal.NpmJsonUtils.rawMapJsonTypeRef;

/**
 * npm search index filter.
 *
 * @since 3.0
 * @deprecated No longer actively used by npm upstream, replaced by v1 search api (NEXUS-13150).
 */
@Deprecated
public class NpmSearchIndexFilter
{
  /**
   * Predicate based on "last modified" timestamp.
   */
  public static class PackageModifiedSince
      implements Predicate<NestedAttributesMap>
  {
    private final DateTime since;

    public PackageModifiedSince(final DateTime since) {
      this.since = checkNotNull(since);
    }

    @Override
    public boolean apply(final NestedAttributesMap input) {
      final DateTime lastModified = NpmMetadataUtils.lastModified(input);
      // note: ancient packages will not have this field, let them through
      return lastModified == null || since.isBefore(lastModified);
    }
  }

  private NpmSearchIndexFilter() {
    // nop
  }

  /**
   * Helper method to filter npm index document by time.modified property. It accepts {@code null} as {@code since}
   * parameter, in which case it returns the passed in index as-is.
   */
  public static Content filterModifiedSince(final Content fullIndex,
                                            @Nullable DateTime lastModified) throws IOException
  {
    if (lastModified == null) {
      return fullIndex;
    }
    else {
      return filter(fullIndex, new PackageModifiedSince(lastModified));
    }
  }

  /**
   * Filters the npm index document with given predicate/
   */
  static Content filter(final Content fullIndex,
                        final Predicate<NestedAttributesMap> predicate) throws IOException
  {
    checkNotNull(fullIndex);
    checkNotNull(predicate);
    final Path path = Files.createTempFile("npm-searchIndex-filter", "json");
    final Closer closer = Closer.create();
    try {
      final JsonParser jsonParser = mapper.getFactory().createParser(closer.register(fullIndex.openInputStream()));
      if (jsonParser.nextToken() == JsonToken.START_OBJECT &&
          NpmMetadataUtils.META_UPDATED.equals(jsonParser.nextFieldName())) {
        jsonParser.nextValue(); // skip value
      }
      final JsonGenerator generator = closer.register(
          mapper.getFactory().createGenerator(new BufferedOutputStream(Files.newOutputStream(path)))
      );
      generator.writeStartObject();
      generator.writeNumberField(NpmMetadataUtils.META_UPDATED, System.currentTimeMillis());
      NestedAttributesMap packageRoot;
      while ((packageRoot = getNext(jsonParser)) != null) {
        if (predicate.apply(packageRoot)) {
          generator.writeObjectField(packageRoot.getKey(), packageRoot.backing());
        }
      }
      generator.writeEndObject();
      generator.flush();
    }
    catch (Throwable t) {
      throw closer.rethrow(t);
    }
    finally {
      closer.close();
    }

    return new Content(new StreamPayload(
        new InputStreamSupplier()
        {
          @Nonnull
          @Override
          public InputStream get() throws IOException {
            return new BufferedInputStream(Files.newInputStream(path));
          }
        },
        Files.size(path),
        ContentTypes.APPLICATION_JSON)
    );
  }

  /**
   * Returns the next package from the {@link JsonParser}.
   */
  @Nullable
  private static NestedAttributesMap getNext(final JsonParser origin) throws IOException {
    String packageName = origin.nextFieldName();
    if (packageName == null) {
      // depleted
      return null;
    }
    else {
      origin.nextToken();
      return new NestedAttributesMap(packageName, origin.readValueAs(rawMapJsonTypeRef));
    }
  }
}
