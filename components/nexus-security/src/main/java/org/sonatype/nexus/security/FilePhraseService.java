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
package org.sonatype.nexus.security;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.crypto.AbstractPhraseService;
import org.sonatype.nexus.crypto.PhraseService;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Files.asCharSource;

/**
 * File-backed {@link PhraseService}.
 *
 * @since 3.8
 */
@Named("file")
@Singleton
public class FilePhraseService
    extends AbstractPhraseService
{
  private final Supplier<String> masterPhraseSupplier;

  @Inject
  public FilePhraseService(@Named("${nexus.security.masterPhraseFile}") @Nullable final File masterPhraseFile) {
    super(masterPhraseFile != null);
    this.masterPhraseSupplier = Suppliers.memoize(new Supplier<String>()
    {
      @Override
      public String get() {
        try {
          return asCharSource(masterPhraseFile, UTF_8).read().trim();
        }
        catch (IOException e) {
          throw Throwables.propagate(e);
        }
      }
    });
  }

  @Override
  protected String getMasterPhrase() {
    return masterPhraseSupplier.get();
  }
}
