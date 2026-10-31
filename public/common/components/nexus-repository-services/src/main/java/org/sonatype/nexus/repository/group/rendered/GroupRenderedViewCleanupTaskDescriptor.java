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
package org.sonatype.nexus.repository.group.rendered;

import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.NumberTextFormField;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import org.springframework.stereotype.Component;

/**
 * Task descriptor for the "Remove unused rendered group views" cleanup task.
 */
@AvailabilityVersion(from = "1.0")
@Component
public class GroupRenderedViewCleanupTaskDescriptor
    extends TaskDescriptorSupport
{
  public static final String TYPE_ID = "repository.group.rendered-view.cleanup";

  static final String DAYS_HORIZON_FIELD_ID = "daysHorizon";

  private static final Number DEFAULT_DAYS_HORIZON = 14;

  /**
   * Smallest accepted horizon. Zero or a negative value would make {@code Duration.ofDays(n)} zero
   * or negative and prune every rendered view on each run, including ones downloaded seconds
   * earlier, turning a storage-bounding task into a cache-wipe. One day is the tightest horizon
   * that still means "unused".
   */
  private static final Number MINIMUM_DAYS_HORIZON = 1;

  public GroupRenderedViewCleanupTaskDescriptor() {
    super(TYPE_ID,
        GroupRenderedViewCleanupTask.class,
        "Remove unused rendered group views",
        VISIBLE,
        EXPOSED,
        new NumberTextFormField(
            DAYS_HORIZON_FIELD_ID,
            "Days horizon",
            "Delete rendered-view assets not downloaded within this many days",
            FormField.OPTIONAL)
                .withInitialValue(DEFAULT_DAYS_HORIZON)
                .withMinimumValue(MINIMUM_DAYS_HORIZON));
  }
}
