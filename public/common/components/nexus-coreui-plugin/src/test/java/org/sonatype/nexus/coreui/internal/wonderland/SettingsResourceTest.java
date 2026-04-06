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
package org.sonatype.nexus.coreui.internal.wonderland;

import java.util.List;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class SettingsResourceTest
    extends Test5Support
{
  @Test
  void getReturnsKeepAliveProperty() {
    SettingsResource underTest = new SettingsResource();

    List<PropertyXO> properties = underTest.get();

    assertThat(properties, is(notNullValue()));
    assertThat(properties, hasSize(1));
    assertThat(properties.get(0).getKey(), is("keepAlive"));
    // Default value for nexus.ui.keepAlive is true
    assertThat(properties.get(0).getValue(), is("true"));
  }

  @Test
  void getReturnsNonEmptyList() {
    SettingsResource underTest = new SettingsResource();

    List<PropertyXO> properties = underTest.get();

    assertThat(properties.isEmpty(), is(false));
  }
}
