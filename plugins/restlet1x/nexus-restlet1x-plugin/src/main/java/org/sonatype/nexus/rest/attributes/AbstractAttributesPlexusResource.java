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
package org.sonatype.nexus.rest.attributes;

import org.sonatype.nexus.rest.restore.AbstractRestorePlexusResource;
import org.sonatype.nexus.tasks.RebuildAttributesTask;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;

public abstract class AbstractAttributesPlexusResource
    extends AbstractRestorePlexusResource
{
  @Override
  public Object getPayloadInstance() {
    return null;
  }

  @Override
  public void delete(Context context, Request request, Response response)
      throws ResourceException
  {
    RebuildAttributesTask task = getNexusScheduler().createTaskInstance(RebuildAttributesTask.class);

    String repositoryId = getRepositoryId(request);
    if (repositoryId == null) {
      repositoryId = getRepositoryGroupId(request);
    }
    task.setRepositoryId(repositoryId);

    task.setResourceStorePath(getResourceStorePath(request));

    this.handleDelete(task, request);
  }

}
