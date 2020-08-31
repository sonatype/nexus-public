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
package org.sonatype.nexus.repository.content.npm.internal.search.legacy;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.npm.NpmContentFacet;
import org.sonatype.nexus.repository.npm.internal.NpmJsonUtils;
import org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils;
import org.sonatype.nexus.repository.npm.internal.NpmPackageId;
import org.sonatype.nexus.repository.npm.internal.NpmPackageParser;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload.InputStreamSupplier;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.base.Strings;
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
  public NpmSearchIndexFacetHosted(final EventManager eventManager, final NpmPackageParser npmPackageParser) {
    super(eventManager, npmPackageParser);
  }

  /**
   * Builds the index by querying (read only access) the underlying CMA structures.
   */
  @Nonnull
  @Override
  protected Content buildIndex(final Path path) throws IOException {
    NpmContentFacet content = getRepository().facet(NpmContentFacet.class);

    try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
         JsonGenerator generator = NpmJsonUtils.mapper.getFactory().createGenerator(writer)) {
      DateTime updated = new DateTime();

      generator.writeStartObject();
      generator.writeNumberField(NpmMetadataUtils.META_UPDATED, updated.getMillis());

      Continuation<FluentComponent> components = content.components().browse(1000, null);

      while (!components.isEmpty()) {
        components.forEach(component -> {
          NpmPackageId packageId =
              Strings.isNullOrEmpty(component.namespace()) ? new NpmPackageId(null, component.name())
                  : new NpmPackageId(component.namespace(), component.name());
          Optional<NestedAttributesMap> packageRoot = loadAndShrink(packageId);
          if (packageRoot.isPresent()) {
            try {
              generator.writeObjectField(packageId.id(), packageRoot.get().backing());
            }
            catch (IOException e) {
              throw new UncheckedIOException(e);
            }
          }
        });
        components = content.components().browse(1000, components.nextContinuationToken());
      }

      generator.writeEndObject();
      generator.flush();
    }
    catch (UncheckedIOException e) {
      throw e.getCause();
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

  private Optional<NestedAttributesMap> loadAndShrink(final NpmPackageId packageId) {
    try {
      return loadPackageRoot(packageId).map(NpmSearchIndexFacetHosted::shrink);
    }
    catch (IOException e) {
      return Optional.empty();
    }
  }

  private static NestedAttributesMap shrink(final NestedAttributesMap packageRoot) {
    if (!packageRoot.isEmpty()) {
      return NpmMetadataUtils.shrink(packageRoot);
    }
    return packageRoot;
  }
}
