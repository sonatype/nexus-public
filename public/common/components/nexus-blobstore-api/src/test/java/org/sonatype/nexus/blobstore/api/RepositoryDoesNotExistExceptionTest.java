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
package org.sonatype.nexus.blobstore.api;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for {@link RepositoryDoesNotExistException}.
 */
public class RepositoryDoesNotExistExceptionTest
{
  @Test
  public void testConstructorAndMessage() {
    RepositoryDoesNotExistException exception = new RepositoryDoesNotExistException();

    assertThat(exception.getMessage(), is("Repository does not exist"));
    assertThat(exception.getCause(), is(nullValue()));
  }

  @Test
  public void testIsCheckedException() {
    RepositoryDoesNotExistException exception = new RepositoryDoesNotExistException();

    assertThat(exception instanceof Exception, is(true));
    assertThat(RuntimeException.class.isInstance(exception), is(false));
  }
}
