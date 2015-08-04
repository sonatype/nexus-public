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
package org.sonatype.nexus.plugins.rrb.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

abstract class RemoteRepositoryParserTestAbstract
{

  /**
   * Extract the content of a file from the classpath.
   */
  protected String getExampleFileContent(String exampleFileName)
      throws IOException
  {
    URL exampleFileURL = this.getClass().getResource(exampleFileName);
    File file = new File(exampleFileURL.getPath());

    StringBuilder content = new StringBuilder();
    BufferedReader input = new BufferedReader(new FileReader(file));
    try {
      String line = null;
      while ((line = input.readLine()) != null) {
        content.append(line);
        content.append(System.getProperty("line.separator"));
      }
    }
    finally {
      input.close();
    }

    return content.toString();
  }

}
