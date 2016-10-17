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
package org.sonatype.nexus.quartz.internal.orient;

import org.sonatype.nexus.orient.OClassNameBuilder;

/**
 * {@link TestMarshalledEntity} entity-adapter.
 */
public class TestMarshalledEntityAdapter
  extends MarshalledEntityAdapter<TestMarshalledEntity>
{
  private static final String DB_CLASS = new OClassNameBuilder()
      .prefix("test")
      .type("marshalled_entity")
      .build();

  public TestMarshalledEntityAdapter(final Marshaller marshaller,
                                     final ClassLoader classLoader)
  {
    super(DB_CLASS, marshaller, classLoader);
  }

  @Override
  protected TestMarshalledEntity newEntity() {
    return new TestMarshalledEntity();
  }
}
