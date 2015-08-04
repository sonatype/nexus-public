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
package org.sonatype.nexus.testsuite.repo.nexus4539;

import java.io.IOException;

/**
 * Do as same as {@link NEXUS4539AutoBlockIT}, but instead of trying to download something from nexus to speed things
 * up
 * it just relax an wait.
 */
public class NEXUS4539AutoBlockNoPingIT
    extends NEXUS4539AutoBlockIT
{
  @Override
  protected void shakeNexus()
      throws IOException
  {
    // do not ping
  }

}
