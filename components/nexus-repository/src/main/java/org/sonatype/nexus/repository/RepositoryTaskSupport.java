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

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.MultipleFailures;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.scheduling.TaskSupport;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Support for tasks that applies to repositories.
 *
 * @since 3.0
 */
@Named
public abstract class RepositoryTaskSupport
    extends TaskSupport
{
  public static final String REPOSITORY_NAME_FIELD_ID = "repositoryName";

  private RepositoryManager repositoryManager;

  @Inject
  public void install(final RepositoryManager repositoryManager) {
    this.repositoryManager = checkNotNull(repositoryManager);
  }

  @Override
  protected Object execute() throws Exception {
    MultipleFailures failures = new MultipleFailures();
    for (Repository repository : findRepositories()) {
      if (!isCanceled()) {
        try {
          execute(repository);
        }
        catch (Exception e) {
          log.error("Failed to run task '{}' on repository '{}'", getMessage(), repository.getName(), e);
          failures.add(e);
        }
      }
    }
    failures.maybePropagate(String.format("Failed to run task '%s'", getMessage()));
    return null;
  }

  @Nonnull
  private Iterable<Repository> findRepositories() {
    final String repositoryName = getRepositoryField();
    checkArgument(!Strings.isNullOrEmpty(repositoryName));
    if ("*".equals(repositoryName)) {
      return Iterables.filter(repositoryManager.browse(), new Predicate<Repository>()
      {
        @Override
        public boolean apply(final Repository input) {
          return appliesTo(input);
        }
      });
    }
    else {
      Repository repository = checkNotNull(repositoryManager.get(repositoryName));
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
   * Execute against specified repository.
   */
  protected abstract void execute(final Repository repository);

  /**
   * Return true if the task should be run against specified repository.
   */
  protected abstract boolean appliesTo(final Repository repository);
}
