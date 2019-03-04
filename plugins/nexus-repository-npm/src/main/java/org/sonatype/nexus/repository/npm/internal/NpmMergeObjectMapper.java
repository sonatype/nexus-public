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
package org.sonatype.nexus.repository.npm.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.json.MergeObjectMapper;
import org.sonatype.nexus.repository.json.NestedAttributesMapJsonParser;
import org.sonatype.nexus.repository.json.NestedAttributesMapStdValueInstantiator;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.MapDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.DIST_TAGS;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.LATEST;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.META_ID;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.META_REV;
import static org.sonatype.nexus.repository.npm.internal.NpmVersionComparator.versionComparator;

/**
 * {@link ObjectMapper} implementation for NPM Merging (the NXRM way) from {@link InputStream}s
 *
 * @since 3.next
 */
public class NpmMergeObjectMapper
    extends MergeObjectMapper
{
  /**
   * Same as {@link #merge(List, Charset)} but no {@link Charset} needed to be provided.
   *
   * @see #merge(List, Charset)
   */
  @Override
  public NestedAttributesMap merge(final List<InputStream> inputStreams) throws IOException {
    return merge(inputStreams, null);
  }

  /**
   * Merge the given {@link InputStream}s into a {@link NestedAttributesMap}. The merging is done according the
   * following rules :
   * <br/>
   * <br/>
   * 1) The order of the {@link InputStream}s dictate which values will be considered the dominant and
   * preserved (the last is the most dominant) value.
   * <br/>
   * 2) The versions in the npm "versions" field will be consolidated together.
   * <br/>
   * 3) We set the "dist-tags/latest" by tracking all versions and comparing which one is the latest.
   * <br/>
   * 4) We remove the "_id" and "_rev" fields as they have no meaning after merge
   *
   * @param inputStreams {@link List} of {@link InputStream}s
   * @param charset      {@link Charset} used for changing from default UTF-8
   * @return NestedAttributesMap
   */
  @Override
  public NestedAttributesMap merge(final List<InputStream> inputStreams,
                                   @Nullable final Charset charset)
      throws IOException
  {
    String latestVersion = null;
    NestedAttributesMap result = new NestedAttributesMap("mergeMap", newHashMap());

    for (InputStream inputStream : inputStreams) {
      merge(result, inputStream, charset);

      latestVersion = updateLatestVersion(result, latestVersion);
    }

    // after all has been parsed mark what we considered the latest version, note that
    // latestVersion might be null leaving a empty dist_tags in the json output
    result.child(DIST_TAGS).set(LATEST, latestVersion);

    // clean up meta id and rev as they have no meaning after merge
    result.remove(META_ID);
    result.remove(META_REV);

    return result;
  }

  @Override
  protected void deserialize(final NestedAttributesMapJsonParser parser,
                             final DeserializationContext context,
                             final MapDeserializer rootDeserializer) throws IOException
  {
    StdValueInstantiator instantiator = (StdValueInstantiator) rootDeserializer.getValueInstantiator();

    new MapDeserializer(rootDeserializer.getValueType(),
        new NestedAttributesMapStdValueInstantiator(instantiator, parser.getRoot()), null,
        new NpmNestedAttributesMapUntypedObjectDeserializer(parser), null)
        .deserialize(parser, context);
  }

  private String updateLatestVersion(final NestedAttributesMap result, final String latestVersion) {
    String pkgLatestVersion = result.child(DIST_TAGS).get(LATEST, String.class);

    if (nonNull(pkgLatestVersion) && (isNull(latestVersion) ||
        versionComparator.compare(pkgLatestVersion, latestVersion) > 0)) {
      return pkgLatestVersion;
    }

    return latestVersion;
  }
}
