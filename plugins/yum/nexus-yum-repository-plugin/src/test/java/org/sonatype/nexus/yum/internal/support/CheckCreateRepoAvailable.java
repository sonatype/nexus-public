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
package org.sonatype.nexus.yum.internal.support;

import java.io.File;
import java.io.FilenameFilter;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.io.File.pathSeparator;
import static java.lang.System.getenv;
import static org.junit.Assert.fail;

public class CheckCreateRepoAvailable
{

  private static final Logger LOG = LoggerFactory.getLogger(CheckCreateRepoAvailable.class);

  @Test
  public void shouldHaveCreaterepoInPath()
      throws Exception
  {
    String[] paths = getenv("PATH").split(pathSeparator);
    for (String path : paths) {
      LOG.debug("Search for createrepo in {} ...", path);

      String[] files = new File(path).list(new FilenameFilter()
      {
        public boolean accept(File dir, String name) {
          return "createrepo".equals(name);
        }
      });
      if (files.length > 0) {
        LOG.debug("Found createrepo in {} !", path);
        return;
      }
    }
    fail("Createrepo not found.");
  }

}
