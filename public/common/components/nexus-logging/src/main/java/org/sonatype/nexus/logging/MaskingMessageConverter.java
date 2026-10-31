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
package org.sonatype.nexus.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Replaces the default {@code %m}/{@code %msg}/{@code %message} pattern converter to redact sensitive
 * cookie values at log-write time, covering both live logs and rolled {@code .log.gz} files.
 *
 * <p>
 * Masking is delegated to {@link SensitiveCookieMasker}, which redacts Keycloak and related
 * session cookie values (e.g. {@code KEYCLOAK_IDENTITY}, {@code KEYCLOAK_SESSION},
 * {@code AUTH_SESSION_ID}) appearing in {@code Set-Cookie} headers logged by HTTP client libraries.
 *
 * <p>
 * Registered in {@link NexusLayoutEncoder} under the {@code m}, {@code msg}, and
 * {@code message} conversion words.
 */
public class MaskingMessageConverter
    extends MessageConverter
{
  // Shared singleton: logback may create a converter instance per encoder (and again on config
  // reload), so a static masker enforces SensitiveCookieMasker's "read nexus.log.mask.cookies once
  // at startup" contract regardless of how many converter instances exist. The masker is stateless
  // (an immutable compiled Pattern) and therefore thread-safe to share.
  private static final SensitiveCookieMasker MASKER = new SensitiveCookieMasker();

  @Override
  public String convert(final ILoggingEvent event) {
    return MASKER.mask(event.getFormattedMessage());
  }
}
