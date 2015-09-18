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
package org.sonatype.nexus.internal.orient;

import org.sonatype.nexus.orient.DatabaseManager;
import org.sonatype.nexus.orient.DatabaseServer;
import org.sonatype.nexus.orient.EncryptedRecordIdObfuscator;
import org.sonatype.nexus.orient.RecordIdObfuscator;

import com.google.inject.AbstractModule;

/**
 * Orient module.
 *
 * @since 3.0
 */
public class OrientModule
  extends AbstractModule
{
  @Override
  protected void configure() {
    preloadOrientEngine();

    // configure default implementations
    bind(DatabaseServer.class).to(DatabaseServerImpl.class);
    bind(DatabaseManager.class).to(DatabaseManagerImpl.class);
    bind(RecordIdObfuscator.class).to(EncryptedRecordIdObfuscator.class);
  }

  /**
   * Pre-loads OrientDB using empty (system) TCCL so javax.script engines can be found.
   */
  private static void preloadOrientEngine() {
    ClassLoader tccl = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(null);
      com.orientechnologies.orient.core.Orient.instance();
    }
    finally {
      Thread.currentThread().setContextClassLoader(tccl);
    }
  }
}
