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
package org.sonatype.nexus.script.plugin.internal

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.common.app.ManagedLifecycle
import org.sonatype.nexus.common.event.EventManager
import org.sonatype.nexus.common.stateguard.Guarded
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport
import org.sonatype.nexus.script.Script
import org.sonatype.nexus.script.ScriptCreatedEvent
import org.sonatype.nexus.script.ScriptDeletedEvent
import org.sonatype.nexus.script.ScriptManager
import org.sonatype.nexus.script.ScriptUpdatedEvent

import com.google.common.collect.ImmutableList
import groovy.transform.CompileStatic

import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED

/**
 * Default {@link ScriptManager}.
 *
 * @since 3.0
 */
@Named
@ManagedLifecycle(phase = SERVICES)
@Singleton
@CompileStatic
class ScriptManagerImpl
    extends StateGuardLifecycleSupport
    implements ScriptManager
{
  @Inject
  EventManager eventManager

  @Inject
  ScriptStore scriptStore

  @Inject
  @Named('${nexus.scripts.allowCreation:-false}')
  boolean allowCreation

  @Override
  @Guarded(by = STARTED)
  Iterable<Script> browse() {
    return ImmutableList.copyOf(scriptStore.list())
  }

  @Override
  @Guarded(by = STARTED)
  Script get(final String name) {
    return scriptStore.get(name)
  }

  @Override
  @Guarded(by = STARTED)
  Script create(final String name, final String content, final String type) {
    validateCreationIsAllowed()

    Script script = scriptStore.newScript()
    script.name = name
    script.content = content
    script.type = type
    scriptStore.create(script)
    eventManager.post(new ScriptCreatedEvent(script))
    return script
  }

  @Override
  @Guarded(by = STARTED)
  Script update(final String name, final String content) {
    validateCreationIsAllowed()

    Script script = scriptStore.get(name)
    if (script == null) {
      return null
    }
    script.content = content
    scriptStore.update(script)
    eventManager.post(new ScriptUpdatedEvent(script))
    return script
  }

  @Override
  @Guarded(by = STARTED)
  void delete(final String name) {
    Script script = scriptStore.get(name)
    if (script != null) {
      scriptStore.delete(script)
      eventManager.post(new ScriptDeletedEvent(script))
    }
  }

  void validateCreationIsAllowed() {
    if(!allowCreation){
      throw new ScriptingDisabledException('Creating and updating scripts is disabled')
    }
  }
}
