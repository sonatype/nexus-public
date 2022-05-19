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

import javax.annotation.Nullable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.validation.Valid
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull
import javax.validation.groups.Default

import org.sonatype.nexus.coreui.search.BrowseableFormatXO
import org.sonatype.nexus.coreui.service.RepositoryUiService
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.extdirect.model.StoreLoadParameters
import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Recipe
import org.sonatype.nexus.validation.Validate
import org.sonatype.nexus.validation.group.Create
import org.sonatype.nexus.validation.group.Update

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import com.softwarementors.extjs.djn.config.annotations.DirectPollMethod
import org.apache.shiro.authz.annotation.RequiresAuthentication

/**
 * Repository {@link DirectComponent}.
 *
 * @since 3.0
 */
@Named
@Singleton
@DirectAction(action = "coreui_Repository")
class RepositoryComponent
    extends DirectComponentSupport
{
  @Inject
  RepositoryUiService repositoryUiService

  @DirectMethod
  @Timed
  @ExceptionMetered
  List<RepositoryXO> read() {
    return repositoryUiService.read()
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  List<ReferenceXO> readRecipes() {
    return repositoryUiService.readRecipes()
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  List<Format> readFormats() {
    return repositoryUiService.readFormats()
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  List<BrowseableFormatXO> getBrowseableFormats() {
    return repositoryUiService.getBrowseableFormats()
  }

  /**
   * Retrieve a list of available repositories references.
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  List<RepositoryReferenceXO> readReferences(final @Nullable StoreLoadParameters parameters) {
    return repositoryUiService.readReferences(parameters)
  }

  /**
   * Retrieve a list of available repositories references + add an entry for all repositories '*".
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  List<RepositoryReferenceXO> readReferencesAddingEntryForAll(final @Nullable StoreLoadParameters parameters) {
    return repositoryUiService.readReferencesAddingEntryForAll(parameters)
  }

  /**
   * Retrieve a list of available repositories references + add an entry for all repositories '*' and an entry for
   * format 'All (format) repositories' '*(format)'".
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  List<RepositoryReferenceXO> readReferencesAddingEntriesForAllFormats(final @Nullable StoreLoadParameters parameters) {
    return repositoryUiService.readReferencesAddingEntriesForAllFormats(parameters)
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate(groups = [Create, Default])
  RepositoryXO create(final @NotNull @Valid RepositoryXO repositoryXO) throws Exception {
    return repositoryUiService.create(repositoryXO)
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate(groups = [Update, Default])
  RepositoryXO update(final @NotNull @Valid RepositoryXO repositoryXO) throws Exception {
    return repositoryUiService.update(repositoryXO)
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  void remove(final @NotEmpty String name) throws Exception {
    repositoryUiService.remove(name)
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  String rebuildIndex(final @NotEmpty String name) {
    return repositoryUiService.rebuildIndex(name)
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  void invalidateCache(final @NotEmpty String name) {
    repositoryUiService.invalidateCache(name)
  }

  @Timed
  @ExceptionMetered
  @DirectPollMethod(event = "coreui_Repository_readStatus")
  @RequiresAuthentication
  List<RepositoryStatusXO> readStatus(final Map<String, String> params) {
    return repositoryUiService.readStatus(params)
  }

  void addRecipe(String format, Recipe recipe) {
    repositoryUiService.addRecipe(format, recipe)
  }
}
