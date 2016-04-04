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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.orient.DatabaseExternalizer;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseManager;
import org.sonatype.nexus.supportzip.SupportBundle;
import org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type;
import org.sonatype.nexus.supportzip.SupportBundleCustomizer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Shared {@code config} database components.
 *
 * @since 3.0
 */
@SuppressWarnings("UnusedDeclaration")
public class ConfigDatabase
{
  private ConfigDatabase() {
    // empty
  }

  public static final String NAME = "config";

  /**
   * Shared {@code config} database instance provider.
   */
  @Named(NAME)
  @Singleton
  public static class ProviderImpl
      implements Provider<DatabaseInstance>
  {
    private final DatabaseManager databaseManager;

    @Inject
    public ProviderImpl(final DatabaseManager databaseManager) {
      this.databaseManager = checkNotNull(databaseManager);
    }

    @Override
    public DatabaseInstance get() {
      return databaseManager.instance(NAME);
    }
  }

  /**
   * Includes export of the {@code config} database in support-zip.
   */
  @Named
  @Singleton
  public static class SupportBundleCustomizerImpl
      extends ComponentSupport
      implements SupportBundleCustomizer
  {
    private final Provider<DatabaseInstance> databaseInstance;

    @Inject
    public SupportBundleCustomizerImpl(@Named(NAME) final Provider<DatabaseInstance> databaseInstance) {
      this.databaseInstance = checkNotNull(databaseInstance);
    }

    @Override
    public void customize(final SupportBundle supportBundle) {
      String path = String.format("work/%s/%s/%s",
          DatabaseManagerImpl.WORK_PATH,
          databaseInstance.get().getName(),
          DatabaseExternalizer.EXPORT_FILENAME
      );

      supportBundle.add(new PasswordSanitizedJsonSource(Type.CONFIG, path, databaseInstance));
    }
  }
}
