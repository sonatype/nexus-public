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
package org.sonatype.nexus.rest.repotargets;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import javax.inject.Inject;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.registry.RepositoryTypeRegistry;
import org.sonatype.nexus.proxy.targets.Target;
import org.sonatype.nexus.proxy.targets.TargetRegistry;
import org.sonatype.nexus.rest.AbstractNexusPlexusResource;
import org.sonatype.nexus.rest.model.RepositoryTargetResource;

import org.restlet.data.Request;

public abstract class AbstractRepositoryTargetPlexusResource
    extends AbstractNexusPlexusResource
{
  private TargetRegistry targetRegistry;

  private RepositoryTypeRegistry repositoryTypeRegistry;

  @Inject
  public void setTargetRegistry(final TargetRegistry targetRegistry) {
    this.targetRegistry = targetRegistry;
  }

  @Inject
  public void setRepositoryTypeRegistry(final RepositoryTypeRegistry repositoryTypeRegistry) {
    this.repositoryTypeRegistry = repositoryTypeRegistry;
  }

  protected TargetRegistry getTargetRegistry() {
    return targetRegistry;
  }

  protected RepositoryTypeRegistry getRepositoryTypeRegistry() {
    return repositoryTypeRegistry;
  }

  protected RepositoryTargetResource getNexusToRestResource(Target target, Request request) {
    RepositoryTargetResource resource = new RepositoryTargetResource();

    resource.setId(target.getId());

    resource.setName(target.getName());

    resource.setResourceURI(request.getResourceRef().getPath());

    resource.setContentClass(target.getContentClass().getId());

    List<String> patterns = new ArrayList<String>(target.getPatternTexts());

    for (String pattern : patterns) {
      resource.addPattern(pattern);
    }

    return resource;
  }

  protected Target getRestToNexusResource(RepositoryTargetResource resource)
      throws ConfigurationException, PatternSyntaxException
  {
    ContentClass cc = getRepositoryTypeRegistry().getContentClasses().get(resource.getContentClass());

    if (cc == null) {
      throw new ConfigurationException("Content class with ID=\"" + resource.getContentClass()
          + "\" does not exists!");
    }

    Target target = new Target(resource.getId(), resource.getName(), cc, resource.getPatterns());

    return target;
  }

  protected boolean validate(boolean isNew, RepositoryTargetResource resource) {
    if (isNew) {
      if (resource.getId() == null) {
        resource.setId(Long.toHexString(System.nanoTime()));
      }
    }

    if (resource.getId() == null) {
      return false;
    }

    return true;
  }

}
