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
package org.sonatype.nexus.testsuite.repo.nexus4594;

import org.sonatype.nexus.testsuite.repo.nexus4539.AutoBlockITSupport;

import org.hamcrest.Matchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

public class Blocked2NFCITSupport
    extends AutoBlockITSupport
{

  /**
   * Check that the Nexus made remote requests to our fake repository.
   */
  protected void verifyNexusWentRemote() {
    assertThat(pathsTouched, not(Matchers.<String>empty()));
    assertThat(pathsTouched, hasItem("/repository/foo/bar/5.0/bar-5.0.jar"));
    pathsTouched.clear();
  }

  /**
   * Check that the Nexus did not made remote requests to our fake repository.
   */
  protected void verifyNexusDidNotWentRemote() {
    assertThat(pathsTouched, Matchers.<String>empty());
  }

}
