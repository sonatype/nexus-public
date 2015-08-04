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
package org.sonatype.nexus.rest.groups;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStore;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.rest.AbstractResourceStoreContentPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.restlet.data.Request;
import org.restlet.resource.ResourceException;

/**
 * @author tstevens
 */
@Named
@Singleton
public class RepositoryGroupContentPlexusResource
    extends AbstractResourceStoreContentPlexusResource
{
  public static final String GROUP_ID_KEY = "groupId";

  public RepositoryGroupContentPlexusResource() {
    setRequireStrictChecking(false);
  }

  @Override
  public Object getPayloadInstance() {
    // group content is read only
    return null;
  }

  @Override
  public String getResourceUri() {
    return "/repo_groups/{" + GROUP_ID_KEY + "}/content";
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/repo_groups/*/content/**", "authcBasic,tgperms");
  }

  @Override
  protected ResourceStore getResourceStore(final Request request)
      throws NoSuchRepositoryException, ResourceException
  {
    return getRepositoryRegistry().getRepositoryWithFacet(request.getAttributes().get(GROUP_ID_KEY).toString(),
        GroupRepository.class);
  }

}
