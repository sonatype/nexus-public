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
package org.sonatype.nexus.repository.upload;

import java.io.IOException;
import java.util.Collection;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;

/**
 * @since 3.next
 */
public interface UploadHandler
{

  /**
   * Adds a component to the repository at the described location. May fail or have unexpected behavior if the
   * repository format does not match this upload instance.
   *
   * @param repository the {@link Repository} to add the component to
   * @param upload the upload
   * @return the {@link Asset Assets} created by the operation
   */
  Collection<String> handle(Repository repository, ComponentUpload upload) throws IOException;

  /**
   * The {@link UploadDefinition} used by this format.
   */
  UploadDefinition getDefinition();
}
