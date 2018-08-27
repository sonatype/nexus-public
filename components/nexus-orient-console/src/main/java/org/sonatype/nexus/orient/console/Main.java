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
package org.sonatype.nexus.orient.console;

import java.util.logging.LogManager;

import org.sonatype.nexus.crypto.CryptoHelper;
import org.sonatype.nexus.crypto.PbeCipherFactory;
import org.sonatype.nexus.crypto.internal.CryptoHelperImpl;
import org.sonatype.nexus.crypto.internal.PbeCipherFactoryImpl;
import org.sonatype.nexus.orient.entity.ConflictHook;
import org.sonatype.nexus.orient.internal.PbeCompression;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.orient.console.OConsoleDatabaseApp;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.compression.OCompressionFactory;
import org.eclipse.sisu.wire.WireModule;

/**
 * Boots the Orient console and adds support for PBE compression.
 *
 * @since 3.2.1
 */
public class Main
{
  private Main() {
    // empty
  }

  public static void main(final String[] args) throws Exception {

    // tweak levels to provide a smoother console experience
    LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("/logging.properties"));

    OLogManager.instance().installCustomFormatter();

    // support property-based configuration of PbeCompression
    Injector injector = Guice.createInjector(new WireModule(binder -> {
      binder.bind(PbeCipherFactory.class).to(PbeCipherFactoryImpl.class);
      binder.bind(CryptoHelper.class).to(CryptoHelperImpl.class);
      binder.bind(PbeCompression.class);
    }));

    // register support for PBE compression; needed to work with the security database
    OCompressionFactory.INSTANCE.register(injector.getInstance(PbeCompression.class));

    // register 'ConflictHook' strategy but leave it disabled; needed to load databases that set it as a strategy
    Orient.instance().getRecordConflictStrategy().registerImplementation(ConflictHook.NAME, new ConflictHook(false));

    OConsoleDatabaseApp.main(args);
  }
}
