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
package org.sonatype.nexus.maven.tasks;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.maven.tasks.descriptors.ReleaseRemovalTaskDescriptor;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.targets.Target;
import org.sonatype.nexus.proxy.targets.TargetRegistry;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.maven.tasks.descriptors.ReleaseRemovalTaskDescriptor.ID;

/**
 * @since 2.5
 */
@Named
@Singleton
public class DefaultReleaseRemover
    extends ComponentSupport
    implements ReleaseRemover
{

  private final RepositoryRegistry repositoryRegistry;

  private final TargetRegistry targetRegistry;

  private final ContentClass maven2ContentClass;

  private final Map<String, ReleaseRemoverBackend> backends;

  @Inject
  public DefaultReleaseRemover(final RepositoryRegistry repositoryRegistry,
                               final TargetRegistry targetRegistry,
                               final @Named("maven2") ContentClass maven2ContentClass,
                               final Map<String, ReleaseRemoverBackend> backends)
  {
    this.repositoryRegistry = checkNotNull(repositoryRegistry);
    this.targetRegistry = checkNotNull(targetRegistry);
    this.maven2ContentClass = checkNotNull(maven2ContentClass);
    this.backends = checkNotNull(backends);
  }

  @Override
  public ReleaseRemovalResult removeReleases(final ReleaseRemovalRequest request)
      throws NoSuchRepositoryException
  {
    logDetails(request);
    ReleaseRemovalResult result = new ReleaseRemovalResult(request.getRepositoryId());
    Repository repository = repositoryRegistry.getRepository(request.getRepositoryId());
    Target repositoryTarget = targetRegistry.getRepositoryTarget(request.getTargetId());

    if (!Strings.isNullOrEmpty(request.getTargetId()) && repositoryTarget == null) {
      throw new IllegalStateException(
          "The specified repository target does not exist. Perhaps it has been deleted since this repository target was configured? Target id = "
              + request.getTargetId());
    }

    if (!process(request, result, repository, repositoryTarget)) {
      throw new IllegalArgumentException("The repository with ID=" + repository.getId() + " is not valid for "
          + ID);
    }
    log.debug("Results of {} are: {}", ReleaseRemovalTaskDescriptor.ID, result);
    return result;
  }

  private boolean process(final ReleaseRemovalRequest request, final ReleaseRemovalResult result,
                          final Repository repository, final Target repositoryTarget)
  {
    if (!repository.getRepositoryContentClass().isCompatible(maven2ContentClass)) {
      log.debug("Skipping '{}' is not a maven 2 repository", repository.getId());
      return false;
    }

    if (!repository.getLocalStatus().shouldServiceRequest()) {
      log.debug("Skipping '{}' because the repository is out of service", repository.getId());
      return false;
    }

    if (repository.getRepositoryKind().isFacetAvailable(ProxyRepository.class)) {
      log.debug("Skipping '{}' because it is a proxy repository", repository.getId());
      return false;
    }

    if (repository.getRepositoryKind().isFacetAvailable(GroupRepository.class)) {
      log.debug("Skipping '{}' because it is a group repository", repository.getId());
      return false;
    }

    MavenRepository mavenRepository = repository.adaptToFacet(MavenRepository.class);

    if (mavenRepository == null) {
      log.debug("Skipping '{}' because it could not be adapted to MavenRepository", repository.getId());
      return false;
    }

    if (!RepositoryPolicy.RELEASE.equals(mavenRepository.getRepositoryPolicy())) {
      log.debug("Skipping '{}' because it is a snapshot or mixed repository", repository.getId());
      return false;
    }

    try {
      final ReleaseRemoverBackend backend;
      if (request.isUseIndex()) {
        backend = checkNotNull(backends.get(ReleaseRemoverBackend.INDEX), "Index backend not found");
      }
      else {
        backend = checkNotNull(backends.get(ReleaseRemoverBackend.WALKER), "Walker backend not found");
      }
      backend.removeReleases(request, result, mavenRepository, repositoryTarget);
      return true;
    }
    catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private void logDetails(final ReleaseRemovalRequest request) {
    log.info("Removing older releases from repository: {}", request.getRepositoryId());
    if (log.isDebugEnabled()) {
      log.debug("With parameters: ");
      log.debug("    NumberOfVersionsToKeep: {}", request.getNumberOfVersionsToKeep());
      log.debug("    RepositoryTarget applied: {}", request.getTargetId());
    }
  }
}
