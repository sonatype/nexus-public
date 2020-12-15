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
package org.sonatype.nexus.supportzip;

import java.io.File;
import java.io.IOException;

/**
 * Should be used by the Support ZIP to export
 * {@link org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type#SECURITY} DB data.
 *
 * @since 3.29
 */
public interface ExportSecurityData
{
  /**
   * Export DB data to a Json file.
   *
   * @param file where data will be stored.
   * @throws IOException for any issue during writing a file.
   */
  void export(final File file) throws IOException;
}
