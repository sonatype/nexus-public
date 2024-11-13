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

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.crypto.PhraseService;

/**
 * Common support for marking and checking customized pass-phrases.
 *
 * @since 3.8
 */
public abstract class AbstractPhraseService
    extends ComponentSupport
    implements PhraseService
{
  private final boolean hasMasterPhrase;

  public AbstractPhraseService(final boolean hasMasterPhrase) {
    this.hasMasterPhrase = hasMasterPhrase;
  }

  protected abstract String getMasterPhrase();

  @Override
  public String getPhrase(final String defaultPhrase) {
    return hasMasterPhrase ? getMasterPhrase() : defaultPhrase;
  }

  @Override
  public String mark(final String value) {
    if (hasMasterPhrase && value != null) {
      if (!value.contains("~{")) {
        return value.replace("{", "~{").replace("}", "}~");
      }
    }
    return value;
  }

  @Override
  public boolean usesLegacyEncoding(final String value) {
    if (hasMasterPhrase && value != null) {
      if (value.contains("~{")) {
        return false;
      }
    }
    return true;
  }
}
