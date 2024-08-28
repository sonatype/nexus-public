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
package org.sonatype.nexus.crypto;

import com.google.common.annotations.VisibleForTesting;

/**
 * Supplies pass-phrases to the various password helpers.
 *
 * @since 3.8
 */
public interface PhraseService
{
  /**
   * Returns currently configured phrase; falls back to the default if none is configured.
   */
  String getPhrase(String defaultPhrase);

  /**
   * Marks the encoded value to make it easier to distinguish it from legacy encoded values.
   */
  String mark(String encoded);

  /**
   * Was the given value encoded using the legacy phrase?
   *
   * @see #mark(String)
   */
  boolean usesLegacyEncoding(String encoded);

  /**
   * Legacy {@link PhraseService} that always returns the default phrase.
   */
  @VisibleForTesting
  PhraseService LEGACY_PHRASE_SERVICE = new AbstractPhraseService(false)
  {
    @Override
    protected String getMasterPhrase() {
      return null;
    }
  };
}
