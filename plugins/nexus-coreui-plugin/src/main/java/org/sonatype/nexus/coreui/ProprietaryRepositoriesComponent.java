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

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.sonatype.nexus.extdirect.DirectComponent;
import org.sonatype.nexus.extdirect.DirectComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.rest.api.AuthorizingRepositoryManager;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.validation.Validate;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.COMPONENT;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.PROPRIETARY_COMPONENTS;

/**
 * Proprietary Repositories Settings {@link DirectComponent}.
 */
@Named
@Singleton
@DirectAction(action = "coreui_ProprietaryRepositories")
public class ProprietaryRepositoriesComponent
    extends DirectComponentSupport
{
  private final AuthorizingRepositoryManager repositoryManager;

  @Inject
  public ProprietaryRepositoriesComponent(final AuthorizingRepositoryManager authorizingRepositoryManager) {
    this.repositoryManager = checkNotNull(authorizingRepositoryManager);
  }

  /**
   * Retrieves the proprietary repo info, containing a list of all repos with proprietary components
   *
   * @return the list of proprietary repos
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:settings:read")
  public ProprietaryRepositoriesXO read() {
    List<String> enabledRepositories = repositoryManager.getRepositoriesWithAdmin()
        .stream()
        .filter(ProprietaryRepositoriesComponent::isProprietary)
        .map(Repository::getName)
        .sorted()
        .collect(Collectors.toList()); // NOSONAR
    ProprietaryRepositoriesXO proprietaryRepositoriesXO = new ProprietaryRepositoriesXO();
    proprietaryRepositoriesXO.setEnabledRepositories(enabledRepositories);
    return proprietaryRepositoriesXO;
  }

  /**
   * Retrieves hosted repos.
   *
   * @return a list of hosted repo
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:settings:read")
  public List<ReferenceXO> readPossibleRepos() {
    return repositoryManager.getRepositoriesWithAdmin()
        .stream()
        .filter(ProprietaryRepositoriesComponent::isHosted)
        .map(repo -> new ReferenceXO(repo.getName(), repo.getName()))
        .sorted(Comparator.comparing(ReferenceXO::getName))
        .collect(Collectors.toList()); // NOSONAR
  }

  /**
   * Updates proprietary repo settings.
   *
   * @return the list of proprietary repos
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:settings:update")
  @Validate
  ProprietaryRepositoriesXO update(@NotNull @Valid final ProprietaryRepositoriesXO proprietaryRepositoriesXO) {
    Set<String> shouldBeEnabled = new HashSet<>(proprietaryRepositoriesXO.getEnabledRepositories());
    repositoryManager.getRepositoriesWithAdmin()
        .stream()
        .filter(ProprietaryRepositoriesComponent::isHosted)
        .filter(repo -> (!isProprietary(repo) && shouldBeEnabled.contains(repo.getName()))
            || (isProprietary(repo) && !shouldBeEnabled.contains(repo.getName())))
        .forEach(repo -> setProprietaryStatus(repo, shouldBeEnabled.contains(repo.getName())));
    return read();
  }

  private static boolean isHosted(final Repository repository) {
    return HostedType.NAME.equals(repository.getType().getValue());
  }

  private static boolean isProprietary(final Repository repository) {
    return Boolean.TRUE.equals(
        repository.getConfiguration().attributes(COMPONENT).get(PROPRIETARY_COMPONENTS, Boolean.class));
  }

  private void setProprietaryStatus(final Repository repository, final boolean status) {
    Configuration newConfig = repository.getConfiguration().copy();
    newConfig.attributes(COMPONENT).set(PROPRIETARY_COMPONENTS, status);
    try {
      repositoryManager.update(newConfig);
    }
    catch (Exception e) {
      log.error("Error updating proprietary status in repo: {}", repository.getName(), e);
      throw new RuntimeException(e);
    }
  }
}
