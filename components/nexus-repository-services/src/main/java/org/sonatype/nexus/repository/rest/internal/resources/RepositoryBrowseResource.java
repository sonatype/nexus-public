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
package org.sonatype.nexus.repository.rest.internal.resources;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.common.template.TemplateParameters;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.node.BrowseListItem;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.browse.node.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.browse.node.BrowseNodeQueryService;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.security.SecurityHelper;

import com.google.common.collect.Iterables;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.sonatype.nexus.common.encoding.EncodingUtil.urlEncode;
import static org.sonatype.nexus.security.BreadActions.BROWSE;

/**
 * @since 3.6
 */
@Named
@Singleton
@Path(RepositoryBrowseResource.RESOURCE_URI)
@Produces(TEXT_HTML)
public class RepositoryBrowseResource
    extends ComponentSupport
    implements Resource
{
  private static final String REPOSITORY_PATH_SEGMENT = "{noop: (/)?}{repositoryPath: ((?<=/).*)?}";

  public static final String RESOURCE_URI = "/repository/browse/{repositoryName}" + REPOSITORY_PATH_SEGMENT;

  private static final String TEMPLATE_RESOURCE = "browseContentHtml.vm";

  private final RepositoryManager repositoryManager;

  private final BrowseNodeQueryService browseNodeQueryService;

  private final BrowseNodeConfiguration configuration;

  private final TemplateHelper templateHelper;

  private final SecurityHelper securityHelper;

  private final URL template;

  @Inject
  public RepositoryBrowseResource(
      final RepositoryManager repositoryManager,
      final BrowseNodeQueryService browseNodeQueryService,
      final BrowseNodeConfiguration configuration,
      final TemplateHelper templateHelper,
      final SecurityHelper securityHelper)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.browseNodeQueryService = checkNotNull(browseNodeQueryService);
    this.configuration = checkNotNull(configuration);
    this.templateHelper = checkNotNull(templateHelper);
    this.securityHelper = checkNotNull(securityHelper);
    this.template = getClass().getResource(TEMPLATE_RESOURCE);
    checkNotNull(template);
  }

  @GET
  public Response getHtml(@PathParam("repositoryName") final String repositoryName,
                          @PathParam("repositoryPath") final String repositoryPath,
                          @Context final UriInfo uriInfo)
  {
    log.debug("Get HTML directory listing for repository {} on path {}", repositoryName, repositoryPath);

    if (!uriInfo.getAbsolutePath().toString().endsWith("/")) {
      log.debug("Request does include a trailing slash, redirecting to include it");
      return Response.seeOther(UriBuilder.fromUri(uriInfo.getAbsolutePath()).path("/").build()).build();
    }

    Repository repository = repositoryManager.get(repositoryName);

    if (repository == null) {
      throw createNotFoundException(repositoryName, null);
    }

    List<String> pathSegments = new ArrayList<>();

    if (!isRoot(repositoryPath)) {
      pathSegments = asList(repositoryPath.split("/"));
    }

    Iterable<BrowseNode> browseNodes =
        browseNodeQueryService.getByPath(repository, pathSegments, configuration.getMaxHtmlNodes());

    final boolean permitted = securityHelper.allPermitted(new RepositoryViewPermission(repository, BROWSE));
    final boolean hasChildren = browseNodes != null && !Iterables.isEmpty(browseNodes);
    final List<BrowseListItem> listItems = hasChildren ?
        browseNodeQueryService.toListItems(repository, browseNodes) :
        Collections.emptyList();

    //if there are visible children return them, or if we are at the root node and permitted to browse the repo
    if (hasChildren || (isRoot(repositoryPath) && permitted)) {
      return Response
          .ok(templateHelper.render(template, initializeTemplateParameters(repositoryName, repositoryPath, listItems)))
          .build();
    }

    throw createNotFoundException(repositoryName, permitted ? repositoryPath : null);
  }

  private WebApplicationException createNotFoundException(final String repositoryName, final String repositoryPath) {
    if (repositoryPath == null) {
      log.debug("Requested repository could not be located or user does not have permission: {} ", repositoryName);
      return new WebApplicationException("Repository not found", NOT_FOUND);
    }
    else {
      log.debug("Requested path {} could not be located in repository {}", repositoryPath, repositoryName);
      return new WebApplicationException("Path not found", NOT_FOUND);
    }
  }

  private TemplateParameters initializeTemplateParameters(final String repositoryName, final String path, final List<BrowseListItem> listItems) {
    TemplateParameters templateParameters = templateHelper.parameters();

    if (isRoot(path)) {
      templateParameters.set("root", true);
    }

    templateParameters.set("requestPath", "/" + path);
    templateParameters.set("listItems", listItems);

    templateParameters.set("showMoreContentWarning", configuration.getMaxHtmlNodes() == listItems.size());
    if (Strings2.isBlank(path)) {
      templateParameters.set("encodedPath", String.format("/#browse/browse:%s", repositoryName));
    }
    else {
      String encodedPath = urlEncode("/" + path + "/");
      templateParameters.set("encodedPath", String.format("/#browse/browse:%s:%s", repositoryName, encodedPath));
    }
    templateParameters.set("searchUrl", "/#browse/search");

    return templateParameters;
  }

  private boolean isRoot(final String path) {
    return Strings2.isBlank(path);
  }
}
