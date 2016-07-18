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
package org.sonatype.nexus.upgrade.example;

import org.sonatype.nexus.common.upgrade.Checkpoint;

import static org.mockito.Mockito.mock;

public abstract class CheckpointMock
    implements Checkpoint
{
  public final Checkpoint mock = mock(Checkpoint.class);

  @Override
  public void begin(String version) throws Exception {
    mock.begin(version);
  }

  @Override
  public void commit() throws Exception {
    mock.commit();
  }

  @Override
  public void rollback() throws Exception {
    mock.rollback();
  }

  @Override
  public void end() {
    mock.end();
  }
}
