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
package org.sonatype.nexus.tasks;

import javax.inject.Named;

import org.sonatype.nexus.proxy.repository.ShadowRepository;
import org.sonatype.nexus.scheduling.AbstractNexusRepositoriesTask;
import org.sonatype.nexus.tasks.descriptors.SynchronizeShadowTaskDescriptor;

import org.codehaus.plexus.util.StringUtils;

/**
 * Publish indexes task.
 */
@Named(SynchronizeShadowTaskDescriptor.ID)
public class SynchronizeShadowsTask
    extends AbstractNexusRepositoriesTask<Object>
{
  /**
   * System event action: shadow sync
   */
  public static final String ACTION = "SYNC_SHADOW";

  @Override
  protected String getRepositoryFieldId() {
    return SynchronizeShadowTaskDescriptor.REPO_FIELD_ID;
  }

  public String getShadowRepositoryId() {
    return getRepositoryId();
  }

  public void setShadowRepositoryId(String shadowRepositoryId) {
    setRepositoryId(shadowRepositoryId);
  }

  @Override
  public String getRepositoryId() {
    return getParameters().get(getRepositoryFieldId());
  }

  @Override
  public void setRepositoryId(String repositoryId) {
    if (!StringUtils.isEmpty(repositoryId)) {
      getParameters().put(getRepositoryFieldId(), repositoryId);
    }
  }

  @Override
  protected Object doRun()
      throws Exception
  {
    ShadowRepository shadow =
        getRepositoryRegistry().getRepositoryWithFacet(getShadowRepositoryId(), ShadowRepository.class);

    shadow.synchronizeWithMaster();

    return null;
  }

  @Override
  protected String getAction() {
    return ACTION;
  }

  @Override
  protected String getMessage() {
    return "Synchronizing virtual repository ID='" + getShadowRepositoryId() + "') with it's master repository.";
  }

}
