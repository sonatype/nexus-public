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
package org.sonatype.nexus.rest.routes;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.mapping.RepositoryPathMapping.MappingType;
import org.sonatype.nexus.proxy.mapping.RequestRepositoryMapper;
import org.sonatype.nexus.rest.AbstractNexusPlexusResource;
import org.sonatype.nexus.rest.NoSuchRepositoryAccessException;
import org.sonatype.nexus.rest.model.RepositoryRouteMemberRepository;
import org.sonatype.nexus.rest.model.RepositoryRouteResource;

import org.restlet.data.Reference;
import org.restlet.data.Request;

/**
 * Abstract base class for route resource handlers.
 *
 * @author cstamas
 */
public abstract class AbstractRepositoryRoutePlexusResource
    extends AbstractNexusPlexusResource
{
  public static final String ROUTE_ID_KEY = "routeId";

  private RequestRepositoryMapper repositoryMapper;

  @Inject
  public void setRepositoryMapper(final RequestRepositoryMapper repositoryMapper) {
    this.repositoryMapper = repositoryMapper;
  }

  protected RequestRepositoryMapper getRepositoryMapper() {
    return repositoryMapper;
  }

  /**
   * Creating a list of member reposes. Since this method is used in two Resource subclasses too, and those are
   * probably mapped to different bases, a listBase param is needed to generate a correct URI, from the actual
   * subclass effective mapping.
   */
  protected List<RepositoryRouteMemberRepository> getRepositoryRouteMemberRepositoryList(Reference listBase,
                                                                                         List<String> reposList,
                                                                                         Request request,
                                                                                         String mapId)
      throws NoSuchRepositoryAccessException
  {
    List<RepositoryRouteMemberRepository> members =
        new ArrayList<RepositoryRouteMemberRepository>(reposList.size());

    for (String repoId : reposList) {
      RepositoryRouteMemberRepository member = new RepositoryRouteMemberRepository();

      if ("*".equals(repoId)) {
        member.setId("*");

        member.setName("ALL");

        member.setResourceURI(null);
      }
      else {
        member.setId(repoId);

        try {
          member.setName(getRepositoryRegistry().getRepository(repoId).getName());
        }
        catch (NoSuchRepositoryAccessException e) {
          throw e;
        }
        catch (NoSuchRepositoryException e) {
          getLogger().warn(
              "Cannot find repository '" + repoId + "' declared within route: + '" + mapId + "'!", e);
          continue;
        }

        member.setResourceURI(createChildReference(request, this, repoId).toString());
      }

      members.add(member);
    }

    return members;
  }

  protected MappingType resource2configType(String type) {
    if (RepositoryRouteResource.INCLUSION_RULE_TYPE.equals(type)) {
      return MappingType.INCLUSION;
    }
    else if (RepositoryRouteResource.EXCLUSION_RULE_TYPE.equals(type)) {
      return MappingType.EXCLUSION;
    }
    else if (RepositoryRouteResource.BLOCKING_RULE_TYPE.equals(type)) {
      return MappingType.BLOCKING;
    }
    else {
      return null;
    }
  }

  protected String config2resourceType(MappingType type) {
    if (MappingType.INCLUSION.equals(type)) {
      return RepositoryRouteResource.INCLUSION_RULE_TYPE;
    }
    else if (MappingType.EXCLUSION.equals(type)) {
      return RepositoryRouteResource.EXCLUSION_RULE_TYPE;
    }
    else if (MappingType.BLOCKING.equals(type)) {
      return RepositoryRouteResource.BLOCKING_RULE_TYPE;
    }
    else {
      return null;
    }
  }

}
