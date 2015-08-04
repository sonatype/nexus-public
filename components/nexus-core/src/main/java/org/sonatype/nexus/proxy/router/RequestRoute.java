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
package org.sonatype.nexus.proxy.router;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.Repository;

/**
 * Request route holds the information how will be an incoming request processed.
 *
 * @author cstamas
 */
public class RequestRoute
{
  private Repository targetedRepository;

  private String repositoryPath;

  private String strippedPrefix;

  private String originalRequestPath;

  private int requestDepth;

  private ResourceStoreRequest resourceStoreRequest;

  public boolean isRepositoryHit() {
    return targetedRepository != null;
  }

  public Repository getTargetedRepository() {
    return targetedRepository;
  }

  public void setTargetedRepository(Repository targetedRepository) {
    this.targetedRepository = targetedRepository;
  }

  public String getRepositoryPath() {
    return repositoryPath;
  }

  public void setRepositoryPath(String repositoryPath) {
    this.repositoryPath = repositoryPath;
  }

  public String getStrippedPrefix() {
    return strippedPrefix;
  }

  public void setStrippedPrefix(String strippedPrefix) {
    this.strippedPrefix = strippedPrefix;
  }

  public String getOriginalRequestPath() {
    return originalRequestPath;
  }

  public void setOriginalRequestPath(String originalRequestPath) {
    this.originalRequestPath = originalRequestPath;
  }

  public int getRequestDepth() {
    return requestDepth;
  }

  public void setRequestDepth(int depth) {
    this.requestDepth = depth;
  }

  public ResourceStoreRequest getResourceStoreRequest() {
    return resourceStoreRequest;
  }

  public void setResourceStoreRequest(ResourceStoreRequest resourceStoreRequest) {
    this.resourceStoreRequest = resourceStoreRequest;
  }

  // ==

  @Override
  public String toString() {
    return "RequestRoute [targetedRepository=" + targetedRepository + ", repositoryPath=" + repositoryPath
        + ", strippedPrefix=" + strippedPrefix + ", originalRequestPath=" + originalRequestPath + ", requestDepth="
        + requestDepth + ", resourceStoreRequest=" + resourceStoreRequest + "]";
  }
}
