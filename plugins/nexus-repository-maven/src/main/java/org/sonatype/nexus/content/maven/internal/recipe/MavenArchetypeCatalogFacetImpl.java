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
package org.sonatype.nexus.content.maven.internal.recipe;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.content.maven.MavenArchetypeCatalogFacet;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.content.maven.internal.event.RebuildMavenArchetypeCatalogEvent;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentQuery;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.internal.Constants;
import org.sonatype.nexus.repository.maven.internal.utils.HashedPayload;
import org.sonatype.nexus.repository.view.Payload;

import com.google.common.collect.Iterables;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.common.hash.HashCode;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;

import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.repository.maven.internal.MavenModels.writeArchetypeCatalog;
import static org.sonatype.nexus.repository.maven.internal.utils.MavenIOUtils.createStreamPayload;
import static org.sonatype.nexus.repository.maven.internal.utils.MavenIOUtils.hashesToPayloads;
import static org.sonatype.nexus.repository.view.ContentTypes.APPLICATION_XML;

/**
 * Rebuilds the maven archetype catalog for a given repository.
 *
 * @since 3.25
 */
@Named
public class MavenArchetypeCatalogFacetImpl
    extends FacetSupport
    implements MavenArchetypeCatalogFacet
{
  private static final String HOSTED_ARCHETYPE_CATALOG = "hosted-archetype-catalog";

  private static final String XML = "xml";

  public static final String MAVEN_ARCHETYPE_KIND = "maven-archetype";

  private static final String ARCHETYPE_CATALOG_PATH = "/" + Constants.ARCHETYPE_CATALOG_FILENAME;

  private MavenPath archetypeCatalogMavenPath;

  private MavenContentFacet mavenContentFacet;

  private final int componentPageSize;

  @Inject
  public MavenArchetypeCatalogFacetImpl(@Named("${maven.archetypes.page.size:-10}") final int componentPageSize) {
    this.componentPageSize = componentPageSize;
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    mavenContentFacet = facet(MavenContentFacet.class);
    this.archetypeCatalogMavenPath = mavenContentFacet.getMavenPathParser()
        .parsePath(ARCHETYPE_CATALOG_PATH);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final RebuildMavenArchetypeCatalogEvent event) throws IOException {
    if (StringUtils.equals(getRepository().getName(), event.getRepositoryName())) {
      deleteExistingCatalog();
    }
  }

  @Override
  public void rebuildArchetypeCatalog() throws IOException {
    deleteExistingCatalog();

    log.debug("Rebuilding hosted archetype catalog for {}", getRepository().getName());

    Path path = Files.createTempFile(HOSTED_ARCHETYPE_CATALOG, XML);
    ArchetypeCatalog hostedCatalog = createArchetypeCatalog();

    try {
      HashedPayload hashedPayload = createArchetypeCatalogFile(hostedCatalog, path);
      try (Payload payload = hashedPayload.getPayload()) {
        mavenContentFacet.put(archetypeCatalogMavenPath, payload);
        putHashedContent(archetypeCatalogMavenPath, hashedPayload);
        log.trace("Rebuilt hosted archetype catalog for {} with {} archetype",
            getRepository().getName(), hostedCatalog.getArchetypes().size());
      }
    }
    finally {
      Files.delete(path);
    }
  }

  private void deleteExistingCatalog() throws IOException {
    mavenContentFacet.delete(archetypeCatalogMavenPath);
    for (HashType hashType : HashType.values()) {
      mavenContentFacet.delete(archetypeCatalogMavenPath.main().hash(hashType));
    }
  }

  private void putHashedContent(final MavenPath mavenPath, final HashedPayload hashedPayload) throws IOException {
    Map<HashAlgorithm, HashCode> hashCodes = hashedPayload.getHashCodes();
    for (Entry<HashType, Payload> entry : hashesToPayloads(hashCodes).entrySet()) {
      mavenContentFacet.put(mavenPath.hash(entry.getKey()), entry.getValue());
    }
  }

  private ArchetypeCatalog createArchetypeCatalog() {
    ArchetypeCatalog hostedCatalog = new ArchetypeCatalog();
    Iterables.addAll(hostedCatalog.getArchetypes(), getArchetypes());
    return hostedCatalog;
  }

  private HashedPayload createArchetypeCatalogFile(final ArchetypeCatalog hostedCatalog, final Path path)
      throws IOException
  {
    return createStreamPayload(
        path,
        APPLICATION_XML,
        (OutputStream outputStream) -> writeArchetypeCatalog(outputStream, hostedCatalog)
    );
  }

  private Iterable<Archetype> getArchetypes() {
    List<Archetype> archetypes = new ArrayList<>();

    FluentQuery<FluentComponent> archetypeQuery = mavenContentFacet.components().byKind(MAVEN_ARCHETYPE_KIND);
    Continuation<FluentComponent> components = archetypeQuery.browse(componentPageSize, null);
    while (!components.isEmpty()) {
      archetypes.addAll(components.stream().map(this::toArchetype).collect(toList()));
      components = archetypeQuery.browse(componentPageSize, components.nextContinuationToken());
    }
    return archetypes;
  }

  private Archetype toArchetype(final FluentComponent component) {
    Archetype archetype = new Archetype();
    archetype.setGroupId(component.namespace());
    archetype.setArtifactId(component.name());
    archetype.setVersion(component.version());
    return archetype;
  }
}
