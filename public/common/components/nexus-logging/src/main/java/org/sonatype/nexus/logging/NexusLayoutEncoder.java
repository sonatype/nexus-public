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

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.PatternLayoutEncoderBase;

/**
 * Adds the ability to use {@code %node} in an encoder pattern, and replaces the default
 * {@code %m}/{@code %msg}/{@code %message} converters with {@link MaskingMessageConverter}
 * so that sensitive cookie values (e.g. Keycloak tokens) are redacted from all log output.
 */
public class NexusLayoutEncoder
    extends PatternLayoutEncoderBase<ILoggingEvent>
{
  @Override
  public void start() {
    PatternLayout patternLayout = new PatternLayout();
    // logback 1.6.x removed the mutable Map<String,String> getDefaultConverterMap() in favor of a
    // per-instance Map<String, Supplier<DynamicConverter>> exposed via getInstanceConverterMap().
    patternLayout.getInstanceConverterMap().put("node", NexusNodeNameConverter::new);
    patternLayout.getInstanceConverterMap().put("m", MaskingMessageConverter::new);
    patternLayout.getInstanceConverterMap().put("msg", MaskingMessageConverter::new);
    patternLayout.getInstanceConverterMap().put("message", MaskingMessageConverter::new);
    patternLayout.setContext(context);
    patternLayout.setPattern(getPattern());
    patternLayout.start();
    this.layout = patternLayout;
    super.start();
  }
}
