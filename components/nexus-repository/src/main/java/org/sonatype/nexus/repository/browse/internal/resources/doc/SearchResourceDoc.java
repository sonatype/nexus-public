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
package org.sonatype.nexus.repository.browse.internal.resources.doc;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.sonatype.nexus.repository.browse.api.ComponentXO;
import org.sonatype.nexus.repository.browse.internal.resources.SearchResource;
import org.sonatype.nexus.rest.Page;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * Swagger documentation for {@link SearchResource}
 *
 * @since 3.4
 */
@Api(value = "search")
public interface SearchResourceDoc
{
  @ApiOperation("Search components")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "q", value = "Query by keyword", dataType = "string", paramType = "query"),
      // common
      @ApiImplicitParam(name = "repository", value = "Repository name", dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "format", value = "Query by format", dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "group", value = "Component group", dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "name", value = "Component name", dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "version", value = "Component version", dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "md5", value = "Specific MD5 hash of component's asset", dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "sha1", value = "Specific SHA-1 hash of component's asset", dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "sha256", value = "Specific SHA-256 hash of component's asset", dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "sha512", value = "Specific SHA-512 hash of component's asset", dataType = "string", paramType = "query"),
      // Maven specific
      @ApiImplicitParam(name = "maven.groupId", value = "Maven groupId", dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "maven.artifactId", value = "Maven artifactId", dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "maven.baseVersion", value = "Maven base version", dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "maven.extension", value = "Maven extension of component's asset", dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "maven.classifier", value = "Maven classifier of component's asset", dataType = "string", paramType = "query"),
      // Nuget specific
      @ApiImplicitParam(name = "nuget.id", value = "Nuget id", dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "nuget.tags", value = "Nuget tags", dataType = "string", paramType = "query"),
      // NPM specific
      @ApiImplicitParam(name = "npm.scope", value = "NPM scope", dataType = "string", paramType = "query"),
      // Docker specific
      @ApiImplicitParam(name = "docker.imageName", value = "Docker image name", dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "docker.imageTag", value = "Docker image tag", dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "docker.layerId", value = "Docker layer ID", dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "docker.contentDigest", value = "Docker content digest", dataType = "string", paramType = "query"),
      // PyPi specific
      @ApiImplicitParam(name = "pypi.classifiers", value = "PyPi classifiers", dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "pypi.description", value = "PyPi description", dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "pypi.keywords", value = "PyPi keywords", dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "pypi.summary", value = "PyPi summary", dataType = "string", paramType = "query"),
      // RubyGems specific
      @ApiImplicitParam(name = "rubygems.description", value = "RubyGems description", dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "rubygems.platform", value = "RubyGems platform", dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "rubygems.summary", value = "RubyGems summary", dataType = "string", paramType = "query")
  })
  Page<ComponentXO> search(
      @ApiParam(value = "A token returned by a prior request. If present, the next page of results are returned")
      final String continuationToken,
      @Context final UriInfo uriInfo);
}
