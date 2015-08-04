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
package org.sonatype.nexus.rest.mirrors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.proxy.repository.Mirror;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.rest.AbstractNexusPlexusResource;
import org.sonatype.nexus.rest.model.MirrorResource;

import org.apache.commons.lang.StringUtils;
import org.restlet.data.Request;

public abstract class AbstractRepositoryMirrorPlexusResource
    extends AbstractNexusPlexusResource
{
  /**
   * Key to store Repo with which we work against.
   */
  public static final String REPOSITORY_ID_KEY = "repositoryId";

  /**
   * Key to store Mirror with which we work against.
   */
  public static final String MIRROR_ID_KEY = "mirrorId";

  protected String getRepositoryId(Request request) {
    return request.getAttributes().get(REPOSITORY_ID_KEY).toString();
  }

  protected String getMirrorId(Request request) {
    return request.getAttributes().get(MIRROR_ID_KEY).toString();
  }

  protected List<MirrorResource> nexusToRestModel(List<Mirror> mirrors) {
    List<MirrorResource> sortedList = new ArrayList<MirrorResource>();

    for (Mirror mirror : mirrors) {
      sortedList.add(nexusToRestModel(mirror));
    }

    return sortedList;
  }

  protected MirrorResource nexusToRestModel(Mirror mirror) {
    MirrorResource resource = new MirrorResource();

    resource.setId(mirror.getId());
    resource.setUrl(mirror.getUrl());

    return resource;
  }

  protected List<Mirror> restToNexusModel(List<MirrorResource> resources) {
    List<Mirror> sortedList = new ArrayList<Mirror>();

    for (MirrorResource resource : resources) {
      sortedList.add(restToNexusModel(resource));
    }

    return sortedList;
  }

  protected Mirror restToNexusModel(MirrorResource resource) {
    Mirror mirror = new Mirror(resource.getId(), resource.getUrl());

    return mirror;
  }

  protected List<Mirror> getMirrors(Repository repository) {
    return repository.getPublishedMirrors().getMirrors();
  }

  protected void setMirrors(Repository repository, List<Mirror> mirrors)
      throws IOException
  {
    //populate ids if not set
    for (Mirror mirror : mirrors) {
      if (StringUtils.isEmpty(mirror.getId())) {
        mirror.setId(mirror.getUrl());
      }
    }

    repository.getPublishedMirrors().setMirrors(mirrors);

    getNexusConfiguration().saveConfiguration();
  }
}
