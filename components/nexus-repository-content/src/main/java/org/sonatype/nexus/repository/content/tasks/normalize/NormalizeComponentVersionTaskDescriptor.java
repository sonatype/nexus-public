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
package org.sonatype.nexus.repository.content.tasks.normalize;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

/**
 * Descriptor for {@link NormalizeComponentVersionTask} which populates the normalized_version column on the {format}_component tables
 */
@AvailabilityVersion(from = "1.0")
@Named
@Singleton
public class NormalizeComponentVersionTaskDescriptor
    extends TaskDescriptorSupport
{
  public static final String TYPE_ID = "component.normalize.version";

  public static final String EXPOSED_FLAG = "${nexus.component.normalize.expose:-false}";

  public static final String VISIBLE_FLAG = "${nexus.component.normalize.visible:-false}";

  @Inject
  public NormalizeComponentVersionTaskDescriptor(
      @Named(EXPOSED_FLAG) final boolean exposed,
      @Named(VISIBLE_FLAG) final boolean visible)
  {
    super(TYPE_ID,
        NormalizeComponentVersionTask.class,
        "Components - Normalize versions",
        visible,
        exposed);
  }
}
