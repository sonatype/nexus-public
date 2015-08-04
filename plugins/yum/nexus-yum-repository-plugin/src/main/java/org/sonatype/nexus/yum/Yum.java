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
package org.sonatype.nexus.yum;

import java.io.File;

import org.sonatype.nexus.proxy.repository.Repository;

/**
 * Provides access to Yum functionality around a Nexus repository.
 *
 * @since yum 3.0
 */
public interface Yum
{

  static final long DEFAULT_DELETE_PROCESSING_DELAY = 10;

  String PATH_OF_REPODATA = "repodata";

  String NAME_OF_REPOMD_XML = "repomd.xml";

  String PATH_OF_REPOMD_XML = PATH_OF_REPODATA + "/" + NAME_OF_REPOMD_XML;

  /**
   * @return associated Nexus repository (never null)
   */
  Repository getNexusRepository();

  YumRepository getYumRepository()
      throws Exception;

  File getBaseDir();

}
