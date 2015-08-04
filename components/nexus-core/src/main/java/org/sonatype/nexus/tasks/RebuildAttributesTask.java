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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.scheduling.AbstractNexusRepositoriesPathAwareTask;
import org.sonatype.nexus.tasks.descriptors.RebuildAttributesTaskDescriptor;

/**
 * Rebuild attributes task.
 */
@Named(RebuildAttributesTaskDescriptor.ID)
public class RebuildAttributesTask
    extends AbstractNexusRepositoriesPathAwareTask<Object>
{
  /**
   * System event action: rebuildAttributes
   */
  public static final String ACTION = "REBUILDATTRIBUTES";

  @Override
  protected String getRepositoryFieldId() {
    return RebuildAttributesTaskDescriptor.REPO_OR_GROUP_FIELD_ID;
  }

  @Override
  protected String getRepositoryPathFieldId() {
    return RebuildAttributesTaskDescriptor.RESOURCE_STORE_PATH_FIELD_ID;
  }

  @Override
  public Object doRun()
      throws Exception
  {
    ResourceStoreRequest req = new ResourceStoreRequest(getResourceStorePath());

    Map<String, String> initialData = new HashMap<String, String>();

    if (getRepositoryId() != null) {
      getRepositoryRegistry().getRepository(getRepositoryId()).recreateAttributes(req, initialData);
    }
    else {
      List<Repository> reposes = getRepositoryRegistry().getRepositories();

      for (Repository repo : reposes) {
        repo.recreateAttributes(req, null);
      }
    }

    return null;
  }

  @Override
  protected String getAction() {
    return ACTION;
  }

  @Override
  protected String getMessage() {
    if (getRepositoryId() != null) {
      return "Rebuilding attributes of repository " + getRepositoryName() + " from path "
          + getResourceStorePath() + " and below.";
    }
    else {
      return "Rebuilding attributes of all registered repositories from path " + getResourceStorePath()
          + " and below.";
    }
  }

}
