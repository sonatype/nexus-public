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
package org.sonatype.nexus.script.plugin.internal;

import java.util.Collection;
import java.util.List;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.script.Script;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.common.collect.MoreCollections.toImmutableList;

/**
 * Tests {@link ScriptManagerImpl#browse()}.
 */
public class ScriptManagerImplTest
{
  private ScriptStore scriptStore;

  private ScriptManagerImpl underTest;

  @Before
  public void setup() throws Exception {
    scriptStore = mock(ScriptStore.class);
    underTest = new ScriptManagerImpl(mock(EventManager.class), scriptStore, false, false);
    underTest.start();
  }

  @Test
  public void browseReturnsTheStoreListWithoutCopyingItAgain() {
    List<Script> scripts = toImmutableList(List.of(createScript("script_1"), createScript("script_2")));
    when(scriptStore.list()).thenReturn(scripts);

    assertSame(scripts, underTest.browse());
  }

  @Test
  public void browseResultToleratesNullQueries() {
    when(scriptStore.list()).thenReturn(toImmutableList(List.of(createScript("script_1"))));

    Collection<?> result = (Collection<?>) underTest.browse();

    assertThat(result.contains(null), is(false));
  }

  private static Script createScript(final String name) {
    ScriptData script = new ScriptData();
    script.setName(name);
    script.setType("groovy");
    script.setContent("log.info('world')");
    return script;
  }
}
