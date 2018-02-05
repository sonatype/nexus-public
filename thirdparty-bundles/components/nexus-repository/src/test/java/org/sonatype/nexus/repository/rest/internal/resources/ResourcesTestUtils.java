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
package org.sonatype.nexus.repository.rest.internal.resources;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;

import static com.google.common.collect.ImmutableMap.of;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.ASSETS;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.ATTRIBUTES;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.FORMAT;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.GROUP;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.ID;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.NAME;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.REPOSITORY_NAME;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.VERSION;

/**
 * Test utility class for API resources
 *
 * @since 3.6.1
 */
class ResourcesTestUtils
{
  static Map<String, Object> createComponent(final String name,
                                             final String repository,
                                             final String format,
                                             final String group,
                                             final String version,
                                             final List<?> assets)
  {
    return ImmutableMap.<String, Object>builder()
        .put(NAME, name)
        .put(FORMAT, format)
        .put(REPOSITORY_NAME, repository)
        .put(GROUP, group)
        .put(VERSION, version)
        .put(ASSETS, assets)
        .build();
  }

  static Map<String, Object> createAsset(final String name,
                                         final String format,
                                         final String sha1,
                                         final Map<String, Object> formatAttributes)
  {
    Map<String, Object> attributes = of("cache", of("last_verified", 1234), "checksum", of("sha1", sha1), format,
        formatAttributes);
    return of(NAME, name, ID, UUID.randomUUID().toString(), ATTRIBUTES, attributes);
  }
}
