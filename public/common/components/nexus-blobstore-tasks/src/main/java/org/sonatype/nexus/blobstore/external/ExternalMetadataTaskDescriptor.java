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
package org.sonatype.nexus.blobstore.external;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * This task should only be run after an upgrade to 3.79 is finalized as its intended to be invoked in the context of an
 * upgrade step in the same context as the database upgrade we use 1.0 for the availability version.
 */
@AvailabilityVersion(from = "1.0")
@Component
public class ExternalMetadataTaskDescriptor
    extends TaskDescriptorSupport
{
  public static final String TYPE_ID = "external.blobstore.metadata";

  private static final String NAME = "Retrieve external blobstore metadata";

  @Autowired
  public ExternalMetadataTaskDescriptor(
      @Value("${external.blobstore.metadata:false}") final boolean visibleExposed)
  {
    super(TYPE_ID,
        ExternalMetadataTask.class,
        NAME,
        visibleExposed,
        visibleExposed,
        REQUEST_RECOVERY,
        new RepositoryCombobox(
            RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID,
            "Repository",
            "Select the repository to retrieve metadata for",
            true),
        new StringTextFormField(ExternalMetadataTask.FORMAT_FIELD_ID,
            "Repository Format",
            "Specify the repository format to retrieve metadata for",
            false));
  }
}
