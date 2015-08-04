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
package org.sonatype.nexus.testsuite.p2.pack200effort0;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.sonatype.nexus.testsuite.p2.AbstractNexusProxyP2IT;

import org.junit.Test;

public class Pack200Effort0IT
    extends AbstractNexusProxyP2IT
{

  public Pack200Effort0IT() {
    super("pack200effort0");
  }

  @Test
  public void pack200effort0()
      throws IOException
  {
    downloadFile(
        new URL(getNexusTestRepoUrl() + "empty.jar.pack.gz"),
        new File("target/pack200effort0.jar.pack.gz").getCanonicalPath()
    );
  }

}
