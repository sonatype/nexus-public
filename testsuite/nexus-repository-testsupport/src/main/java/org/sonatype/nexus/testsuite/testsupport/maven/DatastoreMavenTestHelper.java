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
package org.sonatype.nexus.testsuite.testsupport.maven;

import java.io.IOException;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.view.Payload;

@Named
@Singleton
public class DatastoreMavenTestHelper
    extends MavenTestHelper
{
  @Override
  public void verifyHashesExistAndCorrect(final Repository repository, final String path) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeWithoutValidation(
      final Repository repository,
      final String path,
      final Payload payload) throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void write(final Repository repository, final String path, final Payload payload) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Payload read(final Repository repository, final String path) throws IOException {
    throw new UnsupportedOperationException();
  }
}
