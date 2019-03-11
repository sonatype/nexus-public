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
package org.sonatype.nexus.repository;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.MultipleFailures;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.scheduling.TaskInterruptedException;
import org.sonatype.nexus.scheduling.TaskSupport;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Support for tasks that applies to repositories.
 *
 * If task is configured to run against a repository group (or all repositories) the repository list will be exploded
 * such that the task is run against all repositories referenced in all groups including the group repository itself.
 * Thus, task business logic should not need to process group members themselves
 *
 * @since 3.0
 */
@Named
public abstract class RepositoryTaskSupport
    extends TaskSupport
{
  public static final String REPOSITORY_NAME_FIELD_ID = "repositoryName";

  public static final String ALL_REPOSITORIES = "*";

  private RepositoryManager repositoryManager;

  private Type groupType;

  private Set<Repository> processedRepositories;

  @Inject
  public void install(final RepositoryManager repositoryManager, @Named(GroupType.NAME) final Type groupType) {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.groupType = checkNotNull(groupType, "repository group type required");
  }

  @Override
  protected Object execute() throws Exception {
    processedRepositories = Sets.newHashSet();
    MultipleFailures failures = new MultipleFailures();
    for (Repository repository : findRepositories()) {
      if (isCanceled()) {
        break;
      }

      try {
        execute(repository);
      }
      catch (TaskInterruptedException e) { // NOSONAR
        throw e;
      }
      catch (Exception e) {
        log.error("Failed to run task '{}' on repository '{}'", getMessage(), repository.getName(), e);
        failures.add(e);
      }
    }

    failures.maybePropagate(String.format("Failed to run task '%s'", getMessage()));
    return null;
  }

  @Nonnull
  private Iterable<Repository> findRepositories() {
    final String repositoryName = getRepositoryField();
    checkArgument(!Strings.isNullOrEmpty(repositoryName));
    if (ALL_REPOSITORIES.equals(repositoryName)) {
      return Iterables.filter(repositoryManager.browse(), this::appliesTo);
    }
    else {
      Repository repository = repositoryManager.get(repositoryName);
      if (repository == null) {
        log.warn("Repository '{}' was not found while running task '{}'", repositoryName, getMessage());
        throw new TaskInterruptedException(String.format("Repository '%s' was not found.", repositoryName), true);
      }
      checkState(appliesTo(repository));
      return ImmutableList.of(repository);
    }
  }

  /**
   * Extract repository field out of configuration.
   */
  protected String getRepositoryField() {
    return getConfiguration().getString(REPOSITORY_NAME_FIELD_ID);
  }

  /**
   * Returns true if the repository is of type {@link GroupType}
   *
   * @since 3.6.1
   */
  protected boolean isGroupRepository(final Repository repository) {
    return groupType.equals(repository.getType());
  }

  /**
   * Tracks processed repositories
   *
   * @since 3.6.1
   */
  protected void markProcessed(final Repository repository) {
    processedRepositories.add(repository);
  }

  /**
   * Returns true if the specified repository has already been processed during task execution
   *
   * @since 3.6.1
   */
  protected boolean hasBeenProcessed(final Repository repository) {
    return processedRepositories.contains(repository);
  }

  /**
   * Execute against specified repository.
   */
  protected abstract void execute(final Repository repository);

  /**
   * Return true if the task should be run against specified repository.
   */
  protected abstract boolean appliesTo(final Repository repository);
}
