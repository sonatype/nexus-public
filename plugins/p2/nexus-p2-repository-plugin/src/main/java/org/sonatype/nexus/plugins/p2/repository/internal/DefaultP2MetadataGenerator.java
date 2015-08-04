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
package org.sonatype.nexus.plugins.p2.repository.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.plugins.p2.repository.P2MetadataGenerator;
import org.sonatype.nexus.plugins.p2.repository.P2MetadataGeneratorConfiguration;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.p2.bridge.ArtifactRepository;
import org.sonatype.p2.bridge.MetadataRepository;
import org.sonatype.p2.bridge.Publisher;
import org.sonatype.p2.bridge.model.InstallableArtifact;
import org.sonatype.p2.bridge.model.InstallableUnit;
import org.sonatype.p2.bridge.model.InstallableUnitArtifact;
import org.sonatype.p2.bridge.model.TouchpointType;
import org.sonatype.sisu.resource.scanner.helper.ListenerSupport;
import org.sonatype.sisu.resource.scanner.scanners.SerialScanner;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;

import static org.sonatype.nexus.plugins.p2.repository.internal.NexusUtils.createTemporaryP2Repository;
import static org.sonatype.nexus.plugins.p2.repository.internal.NexusUtils.getRelativePath;
import static org.sonatype.nexus.plugins.p2.repository.internal.NexusUtils.isHidden;
import static org.sonatype.nexus.plugins.p2.repository.internal.NexusUtils.localStorageOfRepositoryAsFile;
import static org.sonatype.nexus.plugins.p2.repository.internal.NexusUtils.retrieveFile;
import static org.sonatype.nexus.plugins.p2.repository.internal.NexusUtils.retrieveItem;
import static org.sonatype.nexus.plugins.p2.repository.internal.NexusUtils.storeItemFromFile;
import static org.sonatype.nexus.plugins.p2.repository.internal.P2ArtifactAnalyzer.getP2Type;
import static org.sonatype.nexus.plugins.p2.repository.internal.P2ArtifactAnalyzer.parseP2Artifact;

@Named
@Singleton
public class DefaultP2MetadataGenerator
    implements P2MetadataGenerator
{

  @Inject
  private Logger logger;

  private final Map<String, P2MetadataGeneratorConfiguration> configurations;

  private final RepositoryRegistry repositories;

  private final ArtifactRepository artifactRepository;

  private final MetadataRepository metadataRepository;

  private final Publisher publisher;

  private final MimeSupport mimeSupport;

  @Inject
  public DefaultP2MetadataGenerator(final RepositoryRegistry repositories,
                                    final MimeSupport mimeSupport,
                                    final ArtifactRepository artifactRepository,
                                    final MetadataRepository metadataRepository,
                                    final Publisher publisher)
  {
    this.repositories = repositories;
    this.mimeSupport = mimeSupport;
    this.artifactRepository = artifactRepository;
    this.metadataRepository = metadataRepository;
    this.publisher = publisher;
    configurations = new HashMap<>();
  }

  @Override
  public P2MetadataGeneratorConfiguration getConfiguration(final String repositoryId) {
    return configurations.get(repositoryId);
  }

  @Override
  public void addConfiguration(final P2MetadataGeneratorConfiguration configuration) {
    configurations.put(configuration.repositoryId(), configuration);
  }

  @Override
  public void removeConfiguration(final P2MetadataGeneratorConfiguration configuration) {
    configurations.remove(configuration.repositoryId());
  }

  @Override
  public void generateP2Metadata(final StorageItem item) {
    final P2MetadataGeneratorConfiguration configuration = getConfiguration(item.getRepositoryId());
    if (configuration == null) {
      return;
    }
    logger.debug("Generate P2 metadata for [{}:{}]", item.getRepositoryId(), item.getPath());

    // TODO only regenerate if jar is newer

    try {
      final Repository repository = repositories.getRepository(item.getRepositoryId());
      final File file = retrieveFile(repository, item.getPath());
      final GenericP2Artifact desc = parseP2Artifact(file);
      if (desc == null) {
        logger.debug("[{}:{}] is neither an OSGi bundle nor an feature. Bailing out.", item.getRepositoryId(),
            item.getPath());
        return;
      }

      final InstallableArtifact artifact = new InstallableArtifact();
      artifact.setId(desc.getId());
      artifact.setClassifier(desc.getType().getClassifier());
      artifact.setVersion(desc.getVersion());
      artifact.setPath(file.getAbsolutePath());
      artifact.setRepositoryPath(item.getPath());

      final Collection<InstallableUnit> ius;
      final Collection<InstallableUnit> artifactsUis;
      switch (desc.getType()) {
        case BUNDLE:
          ius = publisher.generateIUs(true /* generateCapabilities */, true /* generateManifest */,
              true /* generateTouchpointData */, file);
          artifactsUis = ius;
          break;
        case FEATURE:
          ius = publisher.generateFeatureIUs(true /* generateCapabilities */,
              true /* generateRequirements */, file);
          artifactsUis = new ArrayList<InstallableUnit>();
          for (InstallableUnit iu : ius) {
            if (!iu.getId().endsWith(".feature.group")) {
              artifactsUis.add(iu);
            }
          }
          break;
        default:
          throw new IllegalStateException("Unsupported artifact type " + desc.getType().name());
      }

      attachArtifact(artifact, artifactsUis);
      storeP2Data(artifact, ius, repository);
    }
    catch (final Exception e) {
      logger.warn(
          String.format("Could not generate p2 metadata of [%s:%s] due to %s. Bailing out.",
              item.getRepositoryId(), item.getPath(), e.getMessage()), e);
      return;
    }
  }

  /**
   * Attaches the given artifact to the passed installable units.
   *
   * @param artifact The artifact to attach
   * @param ius      The {@link InstallableUnit}s were to attach the artifact
   */
  private void attachArtifact(InstallableArtifact artifact, Collection<InstallableUnit> ius) {
    for (final InstallableUnit iu : ius) {
      final InstallableUnitArtifact iuArtifact = new InstallableUnitArtifact();
      iuArtifact.setId(artifact.getId());
      iuArtifact.setClassifier(artifact.getClassifier());
      iuArtifact.setVersion(artifact.getVersion());

      iu.addArtifact(iuArtifact);

      final TouchpointType touchpointType = new TouchpointType();
      touchpointType.setId("org.eclipse.equinox.p2.osgi");
      touchpointType.setVersion("1.0.0");

      iu.setTouchpointType(touchpointType);
    }
  }

  /**
   * Stores the P2 data for the passed artifact.
   *
   * @param artifact   The artifact for which to create the entries
   * @param ius        The installable units for the passed artifact
   * @param repository The repository were to store the data
   * @throws Exception If an error occurred while
   */
  private void storeP2Data(InstallableArtifact artifact, Collection<InstallableUnit> ius, Repository repository)
      throws Exception
  {
    File tempP2Repository = null;
    try {
      final String extension = FilenameUtils.getExtension(artifact.getRepositoryPath());

      tempP2Repository = createTemporaryP2Repository();

      artifactRepository.write(tempP2Repository.toURI(), Collections.singleton(artifact), artifact.getId(),
          null /** repository properties */
          , new String[][]{
          {"(classifier=" + artifact.getClassifier() + ")", "${repoUrl}" + artifact.getRepositoryPath()}
      });

      final String p2ArtifactsPath =
          artifact.getRepositoryPath().substring(0,
              artifact.getRepositoryPath().length() - extension.length() - 1)
              + "-p2Artifacts.xml";

      storeItemFromFile(
          p2ArtifactsPath,
          new File(tempP2Repository, "artifacts.xml"),
          repository,
          mimeSupport.guessMimeTypeFromPath(p2ArtifactsPath)
      );

      metadataRepository.write(tempP2Repository.toURI(), ius, artifact.getId(), null /** repository properties */
      );

      final String p2ContentPath =
          artifact.getRepositoryPath().substring(0,
              artifact.getRepositoryPath().length() - extension.length() - 1)
              + "-p2Content.xml";

      storeItemFromFile(
          p2ContentPath,
          new File(tempP2Repository, "content.xml"),
          repository,
          mimeSupport.guessMimeTypeFromPath(p2ContentPath)
      );
    }
    finally {
      FileUtils.deleteDirectory(tempP2Repository);
    }
  }

  @Override
  public void removeP2Metadata(final StorageItem item) {
    final P2MetadataGeneratorConfiguration configuration = getConfiguration(item.getRepositoryId());
    if (configuration == null) {
      return;
    }
    logger.debug("Removing P2 metadata for [{}:{}]", item.getRepositoryId(), item.getPath());
    // TODO implement
  }

  @Override
  public void scanAndRebuild(final String repositoryId, final String resourceStorePath) {
    logger.debug("Rebuilding P2 metadata for repository [{}], path [{}]", repositoryId, resourceStorePath);

    final P2MetadataGeneratorConfiguration configuration = getConfiguration(repositoryId);
    if (configuration == null) {
      logger.warn(
          "Rebuilding P2 metadata for [{}] not executed as P2 Metadata Generator capability is not enabled for this repository",
          repositoryId);
      return;
    }

    try {
      final Repository repository = repositories.getRepository(repositoryId);
      final File localStorage = localStorageOfRepositoryAsFile(repository);
      File scanPath = localStorage;
      if (resourceStorePath != null) {
        scanPath = new File(scanPath, resourceStorePath);
      }

      new SerialScanner().scan(scanPath, new ListenerSupport()
      {

        @Override
        public void onFile(final File file) {
          final String path = getRelativePath(localStorage, file);
          if (!isHidden(path) && getP2Type(file) != null) {
            try {
              final StorageItem bundle = retrieveItem(repository, path);
              generateP2Metadata(bundle);
            }
            catch (final Exception e) {
              logger.warn(
                  String.format("P2 metadata for bundle [%s] not created due to [%s]", path,
                      e.getMessage()), e);
            }
          }
        }

      });
    }
    catch (final Exception e) {
      logger.warn(String.format(
          "Rebuilding P2 metadata not executed as repository [%s] could not be scanned due to [%s]",
          repositoryId, e.getMessage()), e);
    }
  }

  @Override
  public void scanAndRebuild(final String resourceStorePath) {
    for (final Repository repository : repositories.getRepositories()) {
      scanAndRebuild(repository.getId(), resourceStorePath);
    }
  }

}
