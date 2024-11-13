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
package org.sonatype.nexus.pax.logging;

/**
 * Interface for uploading log files to a remote location.
 */
public interface RollingPolicyUploader
{
  /**
   * Rollover the given path with the given parameters.
   *
   * @param context     the context prefix to use in the remote location for the given paths
   * @param datePath the log date path to use in the remote location for the given paths
   * @param path        the path to  the file to upload
   * Note: This method is invoked by the {@link RemoteTimeBasedRollingPolicy} when a rollover occurs.
   */
  void rollover(final String context, final String datePath, final String path);
}
