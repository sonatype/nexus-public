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
package org.sonatype.nexus.repository.apt.datastore.internal.task;

import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.CheckboxFormField;
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import static org.sonatype.nexus.formfields.FormField.OPTIONAL;
import org.springframework.stereotype.Component;

@AvailabilityVersion(from = "1.0")
@Component
public class RebuildAptMetadataTaskDescriptor
    extends TaskDescriptorSupport
{
  public static final String TYPE_ID = "repository.apt.rebuild.metadata";

  public static final String REPOSITORY_NAME_FIELD_ID = "repositoryName";

  public static final String APT_METADATA_FULL_REBUILD = "rebuildAptMetadataFullRebuild";

  public static final String APT_PROXY_RESET_METADATA = "resetProxyMetadata";

  public RebuildAptMetadataTaskDescriptor() {
    super(TYPE_ID,
        RebuildAptMetadataTask.class,
        "Apt - Rebuild Apt metadata",
        VISIBLE,
        EXPOSED,
        new RepositoryCombobox(
            REPOSITORY_NAME_FIELD_ID,
            "Repository",
            "Select the Apt repository to rebuild metadata",
            true).includingAnyOfFormats(AptFormat.NAME)
                .includingAnyOfTypes(HostedType.NAME, ProxyType.NAME)
                .includeAnEntryForAllRepositories()
                .withListener("select", "updateAptRebuildCheckboxVisibility")
                .withListener("afterrender", "updateAptRebuildCheckboxVisibility"),
        new CheckboxFormField(
            APT_METADATA_FULL_REBUILD,
            "Full rebuild (hosted only)",
            "Repopulate apt_key_value table and execute full rebuild of metadata. Only applies to hosted repositories.",
            OPTIONAL),
        new CheckboxFormField(
            APT_PROXY_RESET_METADATA,
            "Reset proxy metadata",
            "Clear all generated metadata before rebuild. Use this after changing the upstream URL. Only applies to proxy repositories.",
            OPTIONAL));
  }
}
