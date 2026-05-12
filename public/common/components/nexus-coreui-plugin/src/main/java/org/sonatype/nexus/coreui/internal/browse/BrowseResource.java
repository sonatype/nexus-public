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
package org.sonatype.nexus.coreui.internal.browse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.sonatype.nexus.common.encoding.EncodingUtil;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.coreui.BrowseNodeXO;
import org.sonatype.nexus.coreui.ComponentHelper;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.browse.node.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.browse.node.BrowseNodeQueryService;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.security.SecurityHelper;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.sonatype.nexus.common.app.FeatureFlags.PREVIEW_UI_SETTINGS_ENABLED;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;
import static org.sonatype.nexus.security.BreadActions.BROWSE;

/**
 * REST API resource for browsing repository contents as JSON.
 *
 * Provides endpoints for:
 * - GET /v1/repositories/{repositoryName}/browse - List browse nodes at a path
 * - DELETE /v1/repositories/{repositoryName}/browse - Delete a folder
 */
@Api(value = "Repository Browse")
@Component
@Singleton
@ConditionalOnProperty(name = PREVIEW_UI_SETTINGS_ENABLED, havingValue = "true", matchIfMissing = true)
@Path(BrowseResource.RESOURCE_URI)
@Produces(APPLICATION_JSON)
public class BrowseResource
    implements Resource
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  public static final String RESOURCE_URI = V1_API_PREFIX + "/repositories/{repositoryName}/browse";

  private static final String FOLDER = "folder";

  private static final String COMPONENT = "component";

  private static final String ASSET = "asset";

  private final RepositoryManager repositoryManager;

  private final BrowseNodeQueryService browseNodeQueryService;

  private final BrowseNodeConfiguration configuration;

  private final SecurityHelper securityHelper;

  private final ComponentHelper componentHelper;

  @Inject
  public BrowseResource(
      final RepositoryManager repositoryManager,
      final BrowseNodeQueryService browseNodeQueryService,
      final BrowseNodeConfiguration configuration,
      final SecurityHelper securityHelper,
      final ComponentHelper componentHelper)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.browseNodeQueryService = checkNotNull(browseNodeQueryService);
    this.configuration = checkNotNull(configuration);
    this.securityHelper = checkNotNull(securityHelper);
    this.componentHelper = checkNotNull(componentHelper);
  }

  @GET
  @ApiOperation(value = "List browse nodes for a repository path")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Browse nodes returned", response = BrowseNodeXO.class,
          responseContainer = "List"),
      @ApiResponse(code = 403, message = "Insufficient permissions to browse repository"),
      @ApiResponse(code = 404, message = "Repository not found")
  })
  @RequiresAuthentication
  public List<BrowseNodeXO> getBrowseNodes(
      @ApiParam(value = "Repository name", required = true) @PathParam("repositoryName") final String repositoryName,
      @ApiParam(
          value = "Path within the repository (URL-encoded, use '/' as separator). Use '/' for root.") @QueryParam("path") final String path)
  {
    log.debug("Get browse nodes for repository {} at path {}", repositoryName, path);

    Repository repository = getRepository(repositoryName);

    // Check browse permission
    if (!securityHelper.allPermitted(new RepositoryViewPermission(repository, BROWSE))) {
      throw new WebApplicationException("Insufficient permissions to browse repository", FORBIDDEN);
    }

    // Parse path - use "/" as root
    String nodePath = Strings2.isBlank(path) ? "/" : path;
    List<String> pathSegments = parsePathSegments(nodePath);

    Iterable<BrowseNode> browseNodes = browseNodeQueryService.getByPath(
        repository,
        pathSegments,
        configuration.getMaxNodes());

    if (browseNodes == null) {
      return Collections.emptyList();
    }

    return StreamSupport.stream(browseNodes.spliterator(), false)
        .map(browseNode -> toBrowseNodeXO(browseNode, nodePath))
        .collect(Collectors.toList());
  }

  @DELETE
  @ApiOperation(value = "Delete a folder and all its contents")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Folder deletion initiated"),
      @ApiResponse(code = 400, message = "Path parameter is required"),
      @ApiResponse(code = 403, message = "Insufficient permissions to delete folder"),
      @ApiResponse(code = 404, message = "Repository not found")
  })
  @RequiresAuthentication
  @RequiresPermissions("nexus:component:delete")
  public Response deleteFolder(
      @ApiParam(value = "Repository name", required = true) @PathParam("repositoryName") final String repositoryName,
      @ApiParam(value = "Folder path to delete (URL-encoded)", required = true) @QueryParam("path") final String path)
  {
    log.debug("Delete folder in repository {} at path {}", repositoryName, path);

    if (Strings2.isBlank(path)) {
      throw new WebApplicationException("Path parameter is required", BAD_REQUEST);
    }

    Repository repository = getRepository(repositoryName);

    // Check delete permission for this specific folder
    if (!componentHelper.canDeleteFolder(repository, path)) {
      throw new WebApplicationException("Insufficient permissions to delete folder", FORBIDDEN);
    }

    // Initiate async folder deletion
    componentHelper.deleteFolder(repository, path);

    return Response.noContent().build();
  }

  private Repository getRepository(final String repositoryName) {
    Repository repository = repositoryManager.get(repositoryName);
    if (repository == null) {
      throw new WebApplicationException("Repository not found: " + repositoryName, NOT_FOUND);
    }
    return repository;
  }

  private List<String> parsePathSegments(final String path) {
    if ("/".equals(path)) {
      return Collections.emptyList();
    }

    // Remove leading slash and decode each segment
    String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
    if (normalizedPath.isEmpty()) {
      return Collections.emptyList();
    }

    return Arrays.stream(normalizedPath.split("/"))
        .map(EncodingUtil::urlDecode)
        .collect(Collectors.toList());
  }

  private BrowseNodeXO toBrowseNodeXO(final BrowseNode browseNode, final String parentPath) {
    String encodedPath = EncodingUtil.urlEncode(browseNode.getName());
    String nodeId = "/".equals(parentPath) ? encodedPath : (parentPath + "/" + encodedPath);

    return new BrowseNodeXO()
        .withId(nodeId)
        .withType(getNodeType(browseNode))
        .withText(browseNode.getName())
        .withLeaf(browseNode.isLeaf())
        .withComponentId(browseNode.getComponentId() == null ? null : browseNode.getComponentId().getValue())
        .withAssetId(browseNode.getAssetId() == null ? null : browseNode.getAssetId().getValue())
        .withPackageUrl(browseNode.getPackageUrl());
  }

  private String getNodeType(final BrowseNode browseNode) {
    if (browseNode.getAssetId() != null) {
      return ASSET;
    }
    else if (browseNode.getComponentId() != null) {
      return COMPONENT;
    }
    else {
      return FOLDER;
    }
  }
}
