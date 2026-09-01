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
package org.sonatype.nexus.script;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.mock;

public class ScriptEventTest
{
  @Test
  public void getScriptReturnsSameInstancePassedToConstructor() {
    Script script = mock(Script.class);

    ScriptEvent underTest = new ScriptEvent(script);

    assertThat(underTest.getScript(), sameInstance(script));
  }

  @Test
  public void getScriptReturnsNullWhenConstructedWithNull() {
    ScriptEvent underTest = new ScriptEvent(null);

    assertThat(underTest.getScript(), nullValue());
  }

  @Test
  public void toStringContainsSimpleClassNameAndScript() {
    Script script = mock(Script.class);

    ScriptEvent underTest = new ScriptEvent(script);

    assertThat(underTest.toString(), startsWith("ScriptEvent{script="));
    assertThat(underTest.toString(), endsWith("}"));
    assertThat(underTest.toString(), equalTo("ScriptEvent{script=" + script + "}"));
  }
}
