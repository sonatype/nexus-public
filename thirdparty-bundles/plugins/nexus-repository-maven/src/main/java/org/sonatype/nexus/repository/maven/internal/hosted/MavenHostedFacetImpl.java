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
package org.sonatype.nexus.repository.maven.internal.hosted;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.entity.EntityBatchEvent;
import org.sonatype.nexus.common.entity.EntityEvent;
import org.sonatype.nexus.orient.entity.AttachedEntityHelper;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenHostedFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.internal.Attributes;
import org.sonatype.nexus.repository.maven.internal.Constants;
import org.sonatype.nexus.repository.maven.internal.MavenFacetUtils;
import org.sonatype.nexus.repository.maven.internal.MavenModels;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.MetadataRebuilder;
import org.sonatype.nexus.repository.storage.ComponentEvent;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.transform;

/**
 * A {@link MavenHostedFacet} implementation.
 *
 * @since 3.0
 */
@Named
public class MavenHostedFacetImpl
    extends FacetSupport
    implements MavenHostedFacet
{
  private static final String MAVEN_ARCHETYPE_PACKAGING = "maven-archetype";

  private static final String SELECT_HOSTED_ARCHETYPES = "SELECT " +
      "attributes.maven2.groupId AS groupId, " +
      "attributes.maven2.artifactId AS artifactId, " +
      "attributes.maven2.version AS version, " +
      "attributes.maven2.pom_description AS description " +
      "FROM component " +
      "WHERE bucket=:bucket " +
      "AND attributes.maven2.packaging=:packaging";

  private final MetadataRebuilder metadataRebuilder;
  
  private MavenFacet mavenFacet;

  private MavenPath archetypeCatalogMavenPath;

  @Inject
  public MavenHostedFacetImpl(final MetadataRebuilder metadataRebuilder)
  {
    this.metadataRebuilder = checkNotNull(metadataRebuilder);
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    this.mavenFacet = facet(MavenFacet.class);
    this.archetypeCatalogMavenPath = mavenFacet.getMavenPathParser()
        .parsePath("/" + Constants.ARCHETYPE_CATALOG_FILENAME);
  }

  @Override
  public void rebuildMetadata(@Nullable final String groupId, @Nullable final String artifactId,
                              @Nullable final String baseVersion, final boolean rebuildChecksums)
  {
    final boolean update = !Strings.isNullOrEmpty(groupId)
        || !Strings.isNullOrEmpty(artifactId)
        || !Strings.isNullOrEmpty(baseVersion);
    log.debug("Rebuilding Maven2 hosted repository metadata: repository={}, update={}, g={}, a={}, bV={}",
        getRepository().getName(), update, groupId, artifactId, baseVersion);
    metadataRebuilder.rebuild(getRepository(), update, rebuildChecksums, groupId, artifactId, baseVersion);
  }

  @Override
  public int rebuildArchetypeCatalog() throws IOException {
    log.debug("Rebuilding hosted archetype catalog for {}", getRepository().getName());
    return doRebuildArchetypeCatalog();
  }

  @Override
  public void deleteMetadata(final String groupId, final String artifactId, final String baseVersion) {
    log.debug("Deleting Maven2 hosted repository metadata: repository={}, g={}, a={}, bV={}", getRepository().getName(),
        groupId, artifactId, baseVersion);
    metadataRebuilder.deleteAndRebuild(getRepository(), groupId, artifactId, baseVersion);
  }

  @TransactionalStoreBlob
  protected int doRebuildArchetypeCatalog() throws IOException {
    final Path path = Files.createTempFile("hosted-archetype-catalog", "xml");
    int count = 0;
    try {
      StorageTx tx = UnitOfWork.currentTx();
      Iterable<Archetype> archetypes = getArchetypes(tx);
      ArchetypeCatalog hostedCatalog = new ArchetypeCatalog();
      Iterables.addAll(hostedCatalog.getArchetypes(), archetypes);
      count = hostedCatalog.getArchetypes().size();

      try (Content content = MavenFacetUtils.createTempContent(
          path,
          ContentTypes.APPLICATION_XML,
          (OutputStream outputStream) -> MavenModels.writeArchetypeCatalog(outputStream, hostedCatalog))) {
        MavenFacetUtils.putWithHashes(mavenFacet, archetypeCatalogMavenPath, content);
        log.trace("Rebuilt hosted archetype catalog for {} with {} archetype", getRepository().getName(), count);
      }
    }
    finally {
      Files.delete(path);
      
    }
    return count;
  }

  /**
   * Returns the archetypes to publish for a hosted repository, the SELECT result count will be in parity with
   * published records count!
   */
  protected Iterable<Archetype> getArchetypes(final StorageTx tx) throws IOException {
    Map<String, Object> sqlParams = new HashMap<>();
    sqlParams.put("bucket", AttachedEntityHelper.id(tx.findBucket(getRepository())));
    sqlParams.put("packaging", MAVEN_ARCHETYPE_PACKAGING);
    return transform(
        tx.browse(SELECT_HOSTED_ARCHETYPES, sqlParams),
        (ODocument document) -> {
          Archetype archetype = new Archetype();
          archetype.setGroupId(document.field("groupId", String.class));
          archetype.setArtifactId(document.field("artifactId", String.class));
          archetype.setVersion(document.field("version", String.class));
          archetype.setDescription(document.field("description", String.class));
          return archetype;
        }
    );
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final EntityBatchEvent batchEvent) {
    // are we affected by this event?
    boolean deleteCatalog = false;
    for (final EntityEvent event : batchEvent.getEvents()) {
      if (event instanceof ComponentEvent) {
        final ComponentEvent componentEvent = (ComponentEvent) event;
        if (getRepository().getName().equals(componentEvent.getRepositoryName()) &&
            MAVEN_ARCHETYPE_PACKAGING.equals(
                componentEvent.getComponent().formatAttributes().get(Attributes.P_PACKAGING, String.class))) {
          deleteCatalog = true;
          break;
        }
      }
    }

    if (deleteCatalog) {
      UnitOfWork.begin(getRepository().facet(StorageFacet.class).txSupplier());
      try {
        TransactionalDeleteBlob.operation.throwing(IOException.class).call(() ->
            MavenFacetUtils.deleteWithHashes(mavenFacet, archetypeCatalogMavenPath)
        );
      }
      catch (IOException e) {
        log.warn("Could not delete {}", archetypeCatalogMavenPath, e);
      }
      finally {
        UnitOfWork.end();
      }
    }
  }
}
