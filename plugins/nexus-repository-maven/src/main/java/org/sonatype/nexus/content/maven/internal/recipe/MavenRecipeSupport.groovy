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
package org.sonatype.nexus.content.maven.internal.recipe

import javax.inject.Inject
import javax.inject.Provider

import org.sonatype.nexus.content.maven.MavenArchetypeCatalogFacet
import org.sonatype.nexus.content.maven.MavenContentFacet
import org.sonatype.nexus.content.maven.MavenMetadataRebuildFacet
import org.sonatype.nexus.content.maven.internal.VersionPolicyHandler
import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.RecipeSupport
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.content.browse.BrowseFacet
import org.sonatype.nexus.repository.content.search.SearchFacet
import org.sonatype.nexus.repository.http.PartialFetchHandler
import org.sonatype.nexus.repository.maven.MavenPathParser
import org.sonatype.nexus.repository.maven.internal.MavenSecurityFacet
import org.sonatype.nexus.repository.routing.RoutingRuleHandler
import org.sonatype.nexus.repository.security.SecurityHandler
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler
import org.sonatype.nexus.repository.view.handlers.HandlerContributor
import org.sonatype.nexus.repository.view.handlers.LastDownloadedHandler
import org.sonatype.nexus.repository.view.handlers.TimingHandler

import groovy.transform.CompileStatic

/**
 * @since 3.25
 */
@CompileStatic
abstract class MavenRecipeSupport
    extends RecipeSupport
{

  @Inject
  Provider<ConfigurableViewFacet> viewFacet

  @Inject
  ExceptionHandler exceptionHandler

  @Inject
  TimingHandler timingHandler

  @Inject
  RoutingRuleHandler routingHandler

  @Inject
  SecurityHandler securityHandler

  @Inject
  PartialFetchHandler partialFetchHandler

  @Inject
  ConditionalRequestHandler conditionalRequestHandler

  @Inject
  ContentHeadersHandler contentHeadersHandler

  @Inject
  HandlerContributor handlerContributor

  @Inject
  LastDownloadedHandler lastDownloadedHandler

  @Inject
  VersionPolicyHandler versionPolicyHandler

  @Inject
  Provider<MavenSecurityFacet> securityFacet

  @Inject
  MavenPathParser mavenPathParser

  @Inject
  MavenContentHandler mavenContentHandler

  @Inject
  MavenArchetypeCatalogHandler archetypeCatalogHandler

  @Inject
  MavenMetadataRebuildHandler mavenMetadataRebuildHandler

  @Inject
  Provider<MavenContentFacet> mavenContentFacet

  @Inject
  Provider<SearchFacet> searchFacet

  @Inject
  Provider<BrowseFacet> browseFacet

  @Inject
  Provider<MavenArchetypeCatalogFacet> mavenArchetypeCatalogFacet

  @Inject
  Provider<MavenMetadataRebuildFacet> mavenMetadataRebuildFacet

  protected MavenRecipeSupport(final Type type, final Format format) {
    super(type, format)
  }
}
