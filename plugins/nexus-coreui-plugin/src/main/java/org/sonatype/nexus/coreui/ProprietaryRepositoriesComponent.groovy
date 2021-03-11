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
package org.sonatype.nexus.coreui

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.validation.Valid
import javax.validation.constraints.NotNull

import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.rest.api.AuthorizingRepositoryManager
import org.sonatype.nexus.repository.types.HostedType
import org.sonatype.nexus.validation.Validate

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import org.apache.shiro.authz.annotation.RequiresAuthentication
import org.apache.shiro.authz.annotation.RequiresPermissions

import static org.sonatype.nexus.repository.config.ConfigurationConstants.COMPONENT
import static org.sonatype.nexus.repository.config.ConfigurationConstants.PROPRIETARY_COMPONENTS

/**
 * Proprietary Repositories Settings {@link DirectComponent}.
 *
 * @since 3.30
 */
@Named
@Singleton
@DirectAction(action = 'coreui_ProprietaryRepositories')
class ProprietaryRepositoriesComponent
    extends DirectComponentSupport
{
  @Inject
  AuthorizingRepositoryManager repositoryManager

  static Boolean isHosted(final Repository repository) {
    return HostedType.NAME.equals(repository.getType().getValue())
  }

  static Boolean isProprietary(final Repository repository) {
    return repository.getConfiguration().attributes(COMPONENT).get(PROPRIETARY_COMPONENTS, Boolean.class)
  }

  void setProprietaryStatus(final Repository repository, final boolean status) {
    def newConfig = repository.getConfiguration().copy()
    newConfig.attributes(COMPONENT).set(PROPRIETARY_COMPONENTS, status)
    repositoryManager.update(newConfig)
  }

  /**
   * Retrieves the proprietary repo info, containing a list of all repos with proprietary components
   * @return the list of proprietary repos
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:settings:read')
  ProprietaryRepositoriesXO read() {
    return new ProprietaryRepositoriesXO(
        enabledRepositories: repositoryManager.getRepositoriesWithAdmin()
        .findAll { Repository repo -> isProprietary(repo) }
        .collect { Repository repo -> repo.getName() }
        .sort()
        .toList()
    )
  }

  /**
   * Retrieves hosted repos.
   * @return a list of hosted repo
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:settings:read')
  List<ReferenceXO> readPossibleRepos() {
    repositoryManager.getRepositoriesWithAdmin()
        .findAll { Repository repository -> isHosted(repository) }
        .collect {repository -> new ReferenceXO( id: repository.getName(), name: repository.getName())}
        .sort { a, b -> a.name.compareToIgnoreCase(b.name) }
  }

  /**
   * Updates proprietary repo settings.
   * @return the list of proprietary repos
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions('nexus:settings:update')
  @Validate
  ProprietaryRepositoriesXO update(final @NotNull @Valid ProprietaryRepositoriesXO proprietaryRepositoriesXO) {
    def shouldBeEnabled = proprietaryRepositoriesXO.enabledRepositories.toSet()
    repositoryManager.getRepositoriesWithAdmin()
        .findAll {Repository repository -> isHosted(repository) }
        .findAll { Repository repo ->
          (!isProprietary(repo) && shouldBeEnabled.contains(repo.getName())) ||
              (isProprietary(repo) && !shouldBeEnabled.contains(repo.getName())) }
        .forEach({ Repository repo -> setProprietaryStatus(repo, shouldBeEnabled.contains(repo.getName())) })

    read()
  }
}
