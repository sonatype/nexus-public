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
package org.sonatype.nexus.script.plugin.internal.orient;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.script.Script;
import org.sonatype.nexus.script.plugin.internal.ScriptStore;

import com.google.common.collect.ImmutableList;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SCHEMAS;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;

/**
 * Orient based {@link ScriptStore} implementation.
 *
 * @since 3.0
 */
@Named("orient")
@Priority(Integer.MAX_VALUE)
@ManagedLifecycle(phase = SCHEMAS)
@Singleton
public class OrientScriptStore
    extends StateGuardLifecycleSupport
    implements ScriptStore
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final OrientScriptEntityAdapter entityAdapter;

  @Inject
  public OrientScriptStore(@Named(DatabaseInstanceNames.CONFIG) final Provider<DatabaseInstance> databaseInstance,
                           final OrientScriptEntityAdapter entityAdapter)
  {
    this.databaseInstance = checkNotNull(databaseInstance);
    this.entityAdapter = checkNotNull(entityAdapter);
  }

  @Override
  protected void doStart() throws Exception {
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      entityAdapter.register(db);
    }
  }

  @Override
  public Script newScript() {
    return new OrientScript();
  }

  @Override
  @Guarded(by = STARTED)
  public List<Script> list() {
    return inTx(databaseInstance).call(db -> ImmutableList.copyOf(entityAdapter.browse(db)));
  }

  @Nullable
  @Override
  public Script get(final String name) {
    for (Script script : list()) {
      if (Objects.equals(script.getName(), name)) {
        return script;
      }
    }
    return null;
  }

  @Override
  @Guarded(by = STARTED)
  public void create(final Script script) {
    checkNotNull(script);
    checkArgument(script instanceof OrientScript, "Script is not an OrientScript");
    inTxRetry(databaseInstance).run(db -> entityAdapter.addEntity(db, (OrientScript) script));
  }

  @Override
  @Guarded(by = STARTED)
  public void update(final Script script) {
    checkNotNull(script);
    checkArgument(script instanceof OrientScript, "Script is not an OrientScript");
    inTxRetry(databaseInstance).run(db -> entityAdapter.editEntity(db, (OrientScript) script));
  }

  @Override
  @Guarded(by = STARTED)
  public void delete(final Script script) {
    checkNotNull(script);
    checkArgument(script instanceof OrientScript, "Script is not an OrientScript");
    inTxRetry(databaseInstance).run(db -> entityAdapter.deleteEntity(db, (OrientScript) script));
  }

}
