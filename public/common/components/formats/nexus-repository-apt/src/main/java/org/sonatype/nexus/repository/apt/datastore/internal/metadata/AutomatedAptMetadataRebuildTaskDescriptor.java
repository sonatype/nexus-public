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
package org.sonatype.nexus.repository.apt.datastore.internal.metadata;

import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import org.springframework.stereotype.Component;

/**
 * Base class for automated APT metadata rebuild task descriptors.
 * Provides common configuration for both hosted and proxy repository metadata rebuild tasks.
 * <p>
 * These tasks are NOT_VISIBLE and NOT_EXPOSED - they are only used internally by the scheduler
 * to automatically rebuild metadata in response to repository events.
 */
@AvailabilityVersion(from = "1.0")
@Component
public class AutomatedAptMetadataRebuildTaskDescriptor
    extends TaskDescriptorSupport
{
  public static final String TYPE_ID = "repository.apt.rebuild-metadata.automatic";

  private static final String NAME = "Automated APT repository Metadata Rebuild";

  /**
   * Constructs a task descriptor for automated APT metadata rebuilds.
   */
  protected AutomatedAptMetadataRebuildTaskDescriptor() {
    super(
        TYPE_ID,
        AutomatedAptMetadataRebuildTask.class,
        NAME,
        NOT_VISIBLE,
        NOT_EXPOSED,
        REQUEST_RECOVERY,
        new RepositoryCombobox(
            RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID,
            "Repository",
            "Select the APT repository to rebuild metadata for",
            true).includingAnyOfFormats(AptFormat.NAME).includingAnyOfTypes(ProxyType.NAME, HostedType.NAME));
  }
}
