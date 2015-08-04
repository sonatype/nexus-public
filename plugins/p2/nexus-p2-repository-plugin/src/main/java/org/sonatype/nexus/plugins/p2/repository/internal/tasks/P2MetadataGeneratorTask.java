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
package org.sonatype.nexus.plugins.p2.repository.internal.tasks;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.plugins.p2.repository.P2MetadataGenerator;
import org.sonatype.nexus.scheduling.AbstractNexusRepositoriesPathAwareTask;

@Named(P2MetadataGeneratorTaskDescriptor.ID)
public class P2MetadataGeneratorTask
    extends AbstractNexusRepositoriesPathAwareTask<Object>
{

  private final P2MetadataGenerator p2MetadataGenerator;

  @Inject
  public P2MetadataGeneratorTask(final P2MetadataGenerator p2MetadataGenerator) {
    this.p2MetadataGenerator = p2MetadataGenerator;
  }

  @Override
  protected String getRepositoryFieldId() {
    return P2MetadataGeneratorTaskDescriptor.REPO_OR_GROUP_FIELD_ID;
  }

  @Override
  protected String getRepositoryPathFieldId() {
    return P2MetadataGeneratorTaskDescriptor.RESOURCE_STORE_PATH_FIELD_ID;
  }

  @Override
  protected String getAction() {
    return "REBUILD";
  }

  @Override
  protected String getMessage() {
    if (getRepositoryId() != null) {
      return String.format("Rebuild repository [%s] p2 metadata from path [%s] and bellow", getRepositoryId(),
          getResourceStorePath());
    }
    else {
      return "Rebuild p2 metadata from all repositories (with a P2 Metadata Generator Capability enabled)";
    }
  }

  @Override
  protected Object doRun()
      throws Exception
  {
    final String repositoryId = getRepositoryId();
    if (repositoryId != null) {
      p2MetadataGenerator.scanAndRebuild(repositoryId, getResourceStorePath());
    }
    else {
      p2MetadataGenerator.scanAndRebuild(getResourceStorePath());
    }

    return null;
  }

}
