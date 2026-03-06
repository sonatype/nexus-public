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
package org.sonatype.nexus.repository.search.sql.store.upgrade.task;

import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.repository.search.sql.store.upgrade.task.ReIndexSearchFilterByPatternTask.FILTER_CONDITION_FIELD_ID;

/**
 * Task descriptor for {@link ReIndexSearchFilterByPatternTask}.
 *
 * This task re-indexes search records for components matching a SQL filter condition.
 * The filter condition is a SQL WHERE clause fragment that is applied to the component table.
 *
 * Example filter conditions:
 * - "namespace LIKE '%_%' OR version LIKE '%_%'" for components with underscores
 */
@AvailabilityVersion(from = "1.0")
@Component
public class ReIndexSearchFilterByPatternTaskDescriptor
    extends TaskDescriptorSupport
{
  public static final String TASK_VISIBLE_NAMED = "${nexus.reindex.search.by.pattern.visible:false}";

  @Autowired
  public ReIndexSearchFilterByPatternTaskDescriptor(
      @Value(TASK_VISIBLE_NAMED) final boolean visible)
  {
    super(ReIndexSearchFilterByPatternTask.TYPE_ID, ReIndexSearchFilterByPatternTask.class,
        "Admin - Reindex Search Tables By Pattern", visible, visible,
        new StringTextFormField(
            FILTER_CONDITION_FIELD_ID,
            "Filter Condition",
            "SQL filter condition to match components (e.g., \"namespace LIKE '%_%' OR version LIKE '%_%'\")",
            FormField.MANDATORY));
  }
}
