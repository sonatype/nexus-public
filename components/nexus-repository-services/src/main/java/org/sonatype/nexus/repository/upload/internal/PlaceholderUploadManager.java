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
package org.sonatype.nexus.repository.upload.internal;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadManager;
import org.sonatype.nexus.repository.upload.UploadResponse;

/**
 * Placeholder {@link UploadManager} when CMA is not deployed.
 *
 * @since 3.next
 */
@Named("placeholder")
@Singleton
public class PlaceholderUploadManager
    extends ComponentSupport
    implements UploadManager
{
  @Override
  public Collection<UploadDefinition> getAvailableDefinitions() {
    return Collections.emptyList();
  }

  @Override
  public UploadDefinition getByFormat(final String format) {
    return null; // no upload definitions
  }

  @Override
  public UploadResponse handle(final Repository repository, final HttpServletRequest request) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void handle(final Repository repository, final File content, final File attributes, final String path)
      throws IOException
  {
    throw new UnsupportedOperationException();
  }
}
