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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Provider;

import org.sonatype.nexus.orient.DatabaseExternalizer;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.supportzip.GeneratedContentSourceSupport;
import org.sonatype.nexus.common.io.SanitizingJsonOutputStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Specialized {@link GeneratedContentSourceSupport} that blanks out known password fields in JSON.
 *
 * @since 3.0
 */
class PasswordSanitizedJsonSource
    extends GeneratedContentSourceSupport
{
  private static final List<String> FIELDS = Arrays.asList(
      "applicationPassword", "password", "systemPassword", "secret", "secretAccessKey", "sessionToken", "aptSigning");

  private static final Set<String> EXCLUDED_CLASSES = Collections.unmodifiableSet(
      new HashSet<>(Arrays.asList("api_key", "usertoken_record")));

  private static final String REPLACEMENT = "**REDACTED**";

  private final Provider<DatabaseInstance> databaseInstance;

  /**
   * Constructor.
   */
  public PasswordSanitizedJsonSource(final Type type,
                                     final String path,
                                     final Provider<DatabaseInstance> databaseInstance)
  {
    super(type, path, Priority.REQUIRED);
    this.databaseInstance = checkNotNull(databaseInstance);
  }

  @Override
  protected void generate(final File file) throws Exception {
    try (OutputStream output = new SanitizingJsonOutputStream(
        new BufferedOutputStream(new FileOutputStream(file)), FIELDS, REPLACEMENT)) {
      DatabaseExternalizer externalizer = databaseInstance.get().externalizer();
      externalizer.export(output, EXCLUDED_CLASSES);
    }
  }
}
