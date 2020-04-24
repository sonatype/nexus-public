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
package org.sonatype.nexus.repository.npm.internal.tasks;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchFacet;

import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.repository.npm.internal.tasks.orient.OrientReindexNpmRepositoryTask;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

/**
 * Task descriptor for {@link OrientReindexNpmRepositoryTask}.
 *
 * @since 3.7
 */
@Named
@Singleton
public class ReindexNpmRepositoryTaskDescriptor
    extends TaskDescriptorSupport
{
  public static final String TYPE_ID = "repository.npm.reindex";

  public static final String REPOSITORY_NAME_FIELD_ID = "repositoryName";

  @Inject
  public ReindexNpmRepositoryTaskDescriptor(final NodeAccess nodeAccess) {
    super(TYPE_ID,
        OrientReindexNpmRepositoryTask.class,
        "Repair - Reconcile npm /-/v1/search metadata",
        VISIBLE,
        EXPOSED,
        new RepositoryCombobox(
            REPOSITORY_NAME_FIELD_ID,
            "Repository",
            "Select the npm repository to reconcile",
            true
        ).includingAnyOfFacets(NpmSearchFacet.class).includeAnEntryForAllRepositories(),

        nodeAccess.isClustered() ? newMultinodeFormField().withInitialValue(true) : null
    );
  }
}
