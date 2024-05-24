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
package org.sonatype.nexus.repository.internal.search.index.task;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

/**
 * Task descriptor for {@link SearchUpdateTask}.
 *
 * @since 3.37
 */
@AvailabilityVersion(from = "1.0")
@Named
@Singleton
public class SearchUpdateTaskDescriptor
    extends TaskDescriptorSupport
{
  public static final String TYPE_ID = "repository.search.update";

  public static final String REPOSITORY_NAMES_FIELD_ID = "repositoryNames";

  private static final Messages messages = I18N.create(Messages.class);

  @Inject
  public SearchUpdateTaskDescriptor() {
    super(TYPE_ID,
        SearchUpdateTask.class,
        messages.name(),
        NOT_VISIBLE,
        NOT_EXPOSED
    );
  }

  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("Update repository search indexes")
    String name();
  }
}
