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

import ch.qos.logback.classic.spi.ILoggingEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaskingMessageConverterTest
{
  @Test
  void testConvert_masksKeycloakIdentityCookie() {
    ILoggingEvent event = mock(ILoggingEvent.class);
    when(event.getFormattedMessage()).thenReturn(
        "DEBUG org.apache.http.headers - Set-Cookie: KEYCLOAK_IDENTITY=eyJhbGciOiJ.abc.def;"
            + " Version=1; Path=/auth; HttpOnly");

    MaskingMessageConverter converter = new MaskingMessageConverter();
    String result = converter.convert(event);

    assertThat(result.contains("eyJhbGciOiJ.abc.def"), is(false));
    assertThat(result.contains("Set-Cookie: KEYCLOAK_IDENTITY=****"), is(true));
    assertThat(result.contains("; Version=1; Path=/auth; HttpOnly"), is(true));
  }
}
