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
package org.sonatype.nexus.coreui;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validator for {@link DockerRepositoryNameConstraint} that ensures Docker repository names are lowercase.
 * Existing repositories with mixed-case names are exempted to allow configuration updates.
 */
@Named
public class DockerRepositoryNameValidator
    implements ConstraintValidator<DockerRepositoryNameConstraint, RepositoryXO>
{
  private static final Logger log = LoggerFactory.getLogger(DockerRepositoryNameValidator.class);

  private RepositoryManager repositoryManager;

  /**
   * No-arg constructor for Bean Validation API compatibility and other cases where validators are
   * instantiated directly (for example, {@code Validation.buildDefaultValidatorFactory()} in tests).
   * In such non-DI scenarios, {@link RepositoryManager}-based existing-repository exemption logic is
   * not available because the dependency is not injected.
   */
  public DockerRepositoryNameValidator() {
    this.repositoryManager = null;
  }

  /**
   * Constructor with dependency injection for full validation logic including existing repository exemption.
   */
  @Inject
  public DockerRepositoryNameValidator(final RepositoryManager repositoryManager) {
    this.repositoryManager = repositoryManager;
  }

  // Constants matching org.sonatype.nexus.repository.docker.internal.DockerFormat.NAME
  // the coreui plugin doesn't have a dependency on the docker plugin, so we can't import this constant directly
  private static final String DOCKER_FORMAT = "docker";

  // Recipe name prefixes matching:
  // - org.sonatype.nexus.repository.docker.internal.datastore.recipe.DockerHostedRecipe.NAME = "docker-hosted"
  // - org.sonatype.nexus.repository.docker.internal.datastore.recipe.DockerProxyRecipe.NAME = "docker-proxy"
  // - org.sonatype.nexus.repository.docker.internal.datastore.recipe.DockerGroupRecipe.NAME = "docker-group"
  // the coreui plugin doesn't have a dependency on the docker plugin, so we can't import these constants directly
  private static final String DOCKER_RECIPE_PREFIX = "docker-";

  @Override
  public boolean isValid(final RepositoryXO value, final ConstraintValidatorContext context) {
    if (value == null || value.getName() == null) {
      return true;
    }

    // Check both format and recipe fields to determine if this is a Docker repository
    boolean isDockerRepository = false;

    if (value.getFormat() != null && DOCKER_FORMAT.equalsIgnoreCase(value.getFormat())) {
      isDockerRepository = true;
    }

    if (value.getRecipe() != null && value.getRecipe().toLowerCase().startsWith(DOCKER_RECIPE_PREFIX)) {
      isDockerRepository = true;
    }

    // Only validate for Docker repositories
    if (!isDockerRepository) {
      return true;
    }

    String name = value.getName();

    // If the name is already lowercase, it's valid
    if (name.equals(Strings2.lower(name))) {
      return true;
    }

    // CRITICAL FIX: Exempt existing repositories to allow updates
    // Check if repository already exists - if so, allow the update
    // Only perform this check if RepositoryManager is available (injected via DI)
    if (repositoryManager != null) {
      try {
        if (repositoryManager.exists(name)) {
          log.debug("Allowing update to existing Docker repository with mixed-case name: {}", name);
          return true;
        }
      }
      catch (Exception e) {
        log.warn("Could not check repository existence for: {}", name, e);
        // On error, fail safe and allow the operation
        return true;
      }
    }

    // Only enforce lowercase for NEW repository creation
    log.debug("Blocking creation of new Docker repository with mixed-case name: {}", name);
    return false;
  }
}
