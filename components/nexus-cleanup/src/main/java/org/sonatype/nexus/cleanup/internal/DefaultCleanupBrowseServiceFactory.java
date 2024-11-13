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
package org.sonatype.nexus.cleanup.internal;

import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.cleanup.content.search.CleanupBrowseServiceFactory;
import org.sonatype.nexus.cleanup.content.search.CleanupComponentBrowse;
import org.sonatype.nexus.cleanup.content.search.DefaultCleanupComponentBrowse;
import org.sonatype.nexus.repository.Format;

@Named
@Singleton
public class DefaultCleanupBrowseServiceFactory
    implements CleanupBrowseServiceFactory
{
  private final DefaultCleanupComponentBrowse defaultCleanupComponentBrowse;

  @Inject
  public DefaultCleanupBrowseServiceFactory(final DefaultCleanupComponentBrowse defaultCleanupComponentBrowse) {
    this.defaultCleanupComponentBrowse = Objects.requireNonNull(defaultCleanupComponentBrowse);
  }

  @Override
  public CleanupComponentBrowse get(final Format format) {
    return defaultCleanupComponentBrowse;
  }

  @Override
  public CleanupComponentBrowse getPreviewService() {
    return defaultCleanupComponentBrowse;
  }
}
