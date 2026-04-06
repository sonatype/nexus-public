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
package org.sonatype.nexus.repository.content.tasks;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class CreateComponentIndexTaskTest
    extends TestSupport
{
  @Mock
  private CreateComponentIndexService createComponentIndexService;

  @Test
  public void testGetMessage() {
    CreateComponentIndexTask underTest = new CreateComponentIndexTask(createComponentIndexService, false);
    assertThat(underTest.getMessage(), containsString("indexes"));
  }

  @Test
  public void testExecuteCallsService() throws Exception {
    CreateComponentIndexTask underTest = new CreateComponentIndexTask(createComponentIndexService, false);
    Object result = underTest.execute();
    assertThat(result, is(nullValue()));
    verify(createComponentIndexService).recreateComponentIndexes();
  }

  @Test(expected = TaskInterruptedException.class)
  public void testExecuteThrowsWhenDisabled() throws Exception {
    CreateComponentIndexTask underTest = new CreateComponentIndexTask(createComponentIndexService, true);
    underTest.execute();
  }

  @Test
  public void testExecuteDoesNotCallServiceWhenDisabled() throws Exception {
    CreateComponentIndexTask underTest = new CreateComponentIndexTask(createComponentIndexService, true);
    try {
      underTest.execute();
    }
    catch (TaskInterruptedException expected) {
      // expected
    }
    verifyNoInteractions(createComponentIndexService);
  }

  @Test(expected = NullPointerException.class)
  public void testNullServiceRejected() {
    new CreateComponentIndexTask(null, false);
  }
}
