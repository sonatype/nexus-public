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
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.npm.internal.NpmFacetUtils;
import org.sonatype.nexus.repository.npm.internal.NpmJsonUtils;
import org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils;
import org.sonatype.nexus.repository.npm.internal.NpmPackageId;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetManager;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload.InputStreamSupplier;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.Sets;
import org.joda.time.DateTime;

/**
 * npm search index facet for hosted repositories: it has all the needed
 * bits stored as CMA structures, so it build the index by executing a query.
 *
 * @since 3.0
 * @deprecated No longer actively used by npm upstream, replaced by v1 search api (NEXUS-13150).
 */
@Deprecated
@Named
public class NpmSearchIndexFacetHosted
    extends NpmSearchIndexFacetCaching
{
  @Inject
  public NpmSearchIndexFacetHosted(final EventManager eventManager, final AssetManager assetManager) {
    super(eventManager, assetManager);
  }

  /**
   * Builds the index by querying (read only access) the underlying CMA structures.
   */
  @Nonnull
  @Override
  protected Content buildIndex(final StorageTx tx, final Path path) throws IOException {
    Bucket bucket = tx.findBucket(getRepository());
    try (final Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      Set<NpmPackageId> packageIds = Sets.newHashSet(NpmFacetUtils.findAllPackageNames(tx, bucket));
      DateTime updated = new DateTime();
      JsonGenerator generator = NpmJsonUtils.mapper.getFactory().createGenerator(writer);
      generator.writeStartObject();
      generator.writeNumberField(NpmMetadataUtils.META_UPDATED, updated.getMillis());
      for (NpmPackageId packageId : packageIds) {
        final Asset packageRootAsset = NpmFacetUtils.findPackageRootAsset(tx, bucket, packageId);
        if (packageRootAsset != null) { // removed during iteration, skip
          NestedAttributesMap packageRoot = NpmFacetUtils.loadPackageRoot(tx, packageRootAsset);
          if (!packageRoot.isEmpty()) {
            NpmMetadataUtils.shrink(packageRoot);
            generator.writeObjectField(packageId.id(), packageRoot.backing());
          }
        }
      }
      generator.writeEndObject();
      generator.flush();
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
}
