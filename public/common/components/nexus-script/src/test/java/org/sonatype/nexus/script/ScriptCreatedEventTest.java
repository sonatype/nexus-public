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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;

public class ScriptCreatedEventTest
{
  @Test
  public void getScriptReturnsSameInstancePassedToConstructor() {
    Script script = mock(Script.class);

    ScriptCreatedEvent underTest = new ScriptCreatedEvent(script);

    assertThat(underTest.getScript(), sameInstance(script));
  }

  @Test
  public void getScriptReturnsNullWhenConstructedWithNull() {
    ScriptCreatedEvent underTest = new ScriptCreatedEvent(null);

    assertThat(underTest.getScript(), nullValue());
  }

  @Test
  public void eventIsInstanceOfScriptEvent() {
    Script script = mock(Script.class);

    ScriptCreatedEvent underTest = new ScriptCreatedEvent(script);

    assertThat(underTest, instanceOf(ScriptEvent.class));
  }

  @Test
  public void toStringUsesSubclassSimpleName() {
    Script script = mock(Script.class);

    ScriptCreatedEvent underTest = new ScriptCreatedEvent(script);

    assertThat(underTest.toString(), equalTo("ScriptCreatedEvent{script=" + script + "}"));
  }
}
