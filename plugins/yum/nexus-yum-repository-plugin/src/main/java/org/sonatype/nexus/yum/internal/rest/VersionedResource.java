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
package org.sonatype.nexus.yum.internal.rest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Path;

import org.sonatype.nexus.yum.Yum;
import org.sonatype.nexus.yum.YumHosted;
import org.sonatype.nexus.yum.YumRegistry;
import org.sonatype.nexus.yum.YumRepository;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

import org.restlet.data.Request;
import org.restlet.resource.ResourceException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.restlet.data.Status.CLIENT_ERROR_BAD_REQUEST;

/**
 * @since yum 3.0
 */
@Path(VersionedResource.RESOURCE_URI)
@Named
@Singleton
public class VersionedResource
    extends AbstractYumRepositoryResource
    implements PlexusResource
{

  private static final String YUM_REPO_PREFIX_NAME = "yum/repos";

  private static final String YUM_REPO_PREFIX = "/" + YUM_REPO_PREFIX_NAME;

  private static final String VERSION_URL_PARAM = "version";

  private static final String REPOSITORY_URL_PARAM = "repository";

  private static final int SEGMENTS_AFTER_REPO_PREFIX = 3;

  public static final String RESOURCE_URI = YUM_REPO_PREFIX + "/{" + REPOSITORY_URL_PARAM + "}/{" + VERSION_URL_PARAM
      + "}";

  private final YumRegistry yumRegistry;

  @Inject
  public VersionedResource(final YumRegistry yumRegistry) {
    this.yumRegistry = checkNotNull(yumRegistry);
    setRequireStrictChecking(false);
  }

  @Override
  protected String getUrlPrefixName() {
    return "yum";
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor(YUM_REPO_PREFIX + "/**",
        "authcBasic,perms[nexus:yumVersionedRepositories]");
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  protected YumRepository getYumRepository(Request request, UrlPathInterpretation interpretation)
      throws Exception
  {
    final String repositoryId = request.getAttributes().get(REPOSITORY_URL_PARAM).toString();
    final String version = request.getAttributes().get(VERSION_URL_PARAM).toString();

    final Yum yum = yumRegistry.get(repositoryId);
    if (yum == null) {
      throw new ResourceException(CLIENT_ERROR_BAD_REQUEST,
          "Couldn't find repository with id : " + repositoryId);
    }

    if (!(yum instanceof YumHosted)) {
      throw new ResourceException(
          CLIENT_ERROR_BAD_REQUEST, "Repository " + repositoryId + " does not support versions"
      );
    }

    YumHosted yumHosted = (YumHosted) yum;

    String aliasVersion = yumHosted.getVersion(version);
    if (aliasVersion == null) {
      aliasVersion = version;
    }

    return yumHosted.getYumRepository(aliasVersion);
  }

  @Override
  protected int getSegmentCountAfterPrefix() {
    return SEGMENTS_AFTER_REPO_PREFIX;
  }

}
