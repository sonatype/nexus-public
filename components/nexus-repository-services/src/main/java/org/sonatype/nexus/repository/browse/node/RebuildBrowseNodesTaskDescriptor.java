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
package org.sonatype.nexus.repository.browse.node;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.FeatureFlags;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.formfields.ItemselectFormField;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

/**
 * Task descriptor for {@link RebuildBrowseNodesTask}.
 *
 * @since 3.6
 */
@Named
@Singleton
public class RebuildBrowseNodesTaskDescriptor
    extends TaskDescriptorSupport
{
  public static final String TYPE_ID = "create.browse.nodes";

  public static final String REPOSITORY_NAME_FIELD_ID = "repositoryName";

  public static final String TASK_NAME = "Repair - Rebuild repository browse";

  @Inject
  public RebuildBrowseNodesTaskDescriptor(
      final NodeAccess nodeAccess,
      final GroupType groupType,
      @Named(FeatureFlags.DATASTORE_CLUSTERED_ENABLED_NAMED) final boolean datastoreClustered)
  {
    super(TYPE_ID, RebuildBrowseNodesTask.class, TASK_NAME, VISIBLE, EXPOSED,
        new ItemselectFormField(REPOSITORY_NAME_FIELD_ID, "Repository",
            "Select the repository(ies) to rebuild browse tree",
            true).withStoreApi("coreui_Repository.readReferencesAddingEntryForAll")
            .withButtons("up", "add", "remove", "down").withFromTitle("Available").withToTitle("Selected")
            .withStoreFilter("type", "!" + groupType.getValue()).withValueAsString(true),
        (nodeAccess.isClustered() && !datastoreClustered) ? newLimitNodeFormField() : null);
  }
}
