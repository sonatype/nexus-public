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
package org.sonatype.nexus.pax.logging;

import ch.qos.logback.access.PatternLayout;
import ch.qos.logback.access.pattern.RemoteUserConverter;
import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.pattern.PatternLayoutEncoderBase;

/**
 * Encoder that configures {@code %u} and {@code %user} converter
 * patterns to be converted by {@link NexusUserIdConverter} instead of {@link RemoteUserConverter}.
 *
 * @since 3.0
 */
public class AccessPatternLayoutEncoder
    extends PatternLayoutEncoderBase<IAccessEvent>
{
  @Override
  public void start() {
    PatternLayout patternLayout = new PatternLayout();
    patternLayout.getDefaultConverterMap().put("u", NexusUserIdConverter.class.getName());
    patternLayout.getDefaultConverterMap().put("user", NexusUserIdConverter.class.getName());
    patternLayout.setContext(context);
    patternLayout.setPattern(getPattern());
    patternLayout.start();
    this.layout = patternLayout;
    super.start();
  }
}
