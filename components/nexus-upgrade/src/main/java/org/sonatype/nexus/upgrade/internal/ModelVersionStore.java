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
package org.sonatype.nexus.upgrade.internal;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.property.PropertiesFile;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;

import com.google.common.annotations.VisibleForTesting;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;

/**
 * Store for model versions.
 * 
 * Starts in UPGRADE phase (managed by {@link UpgradeServiceImpl}) rather than the usual SCHEMAS phase.
 * 
 * @since 3.1
 */
@Named
@Singleton
public class ModelVersionStore
    extends StateGuardLifecycleSupport
{
  @VisibleForTesting
  static final String MODEL_PROPERTIES = "model.properties";

  private final UpgradeManager upgradeManager;

  private final Provider<DatabaseInstance> databaseInstance;

  private final ClusteredModelVersionsEntityAdapter entityAdapter;

  private final PropertiesFile localModelVersions;

  private ClusteredModelVersions clusteredModelVersions;

  private boolean newInstance;

  @Inject
  public ModelVersionStore(final UpgradeManager upgradeManager,
                           @Named(DatabaseInstanceNames.CONFIG) final Provider<DatabaseInstance> databaseInstance,
                           final ClusteredModelVersionsEntityAdapter entityAdapter,
                           final ApplicationDirectories applicationDirectories)
  {
    this.upgradeManager = checkNotNull(upgradeManager);
    this.databaseInstance = checkNotNull(databaseInstance);
    this.entityAdapter = checkNotNull(entityAdapter);
    localModelVersions = new PropertiesFile(new File(applicationDirectories.getWorkDirectory("db"), MODEL_PROPERTIES));
  }

  @Override
  protected void doStart() throws Exception {
    if (localModelVersions.exists()) {
      localModelVersions.load();
    }
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      // if we're first to register the 'model_versions' type then that implies this is a new instance;
      // however we also need to check if the 'repository' type is registered to account for the first
      // few NXRM 3.x releases that didn't ship with upgrade, but did have 'repository' - at this point
      // in startup (before SCHEMAS) if neither type is registered then we consider it a new instance
      entityAdapter.register(db, () -> newInstance = !db.getMetadata().getSchema().existsClass("repository"));

      clusteredModelVersions = entityAdapter.get(db); // NOSONAR
    }
  }

  @Guarded(by = STARTED)
  public boolean isNewInstance() {
    return newInstance;
  }

  @Guarded(by = STARTED)
  public synchronized Map<String, String> load() {
    Map<String, String> modelVersions = new HashMap<>();
    load(modelVersions, upgradeManager.getLocalModels(), localModelVersions::getProperty);
    load(modelVersions, upgradeManager.getClusteredModels(), clusteredModelVersions::get);
    return modelVersions;
  }

  private void load(final Map<String, String> mergedModelVersions,
                    final Set<String> models,
                    final Function<String, String> versionLookup)
  {
    for (String model : models) {
      String version = versionLookup.apply(model);
      if (version != null) {
        mergedModelVersions.put(model, version);
      }
    }
  }

  @Guarded(by = STARTED)
  public synchronized void save(Map<String, String> modelVersions) {
    checkNotNull(modelVersions);
    save(modelVersions, upgradeManager.getLocalModels(), localModelVersions::setProperty);
    save(modelVersions, upgradeManager.getClusteredModels(), clusteredModelVersions::put);
    try {
      localModelVersions.store();
      // avoid touching database if the clustered versions haven't changed
      if (clusteredModelVersions.isDirty()) {
        inTxRetry(databaseInstance).run(db -> entityAdapter.set(db, clusteredModelVersions));
        clusteredModelVersions.clearDirty();
      }
    }
    catch (IOException e) {
      throw new RuntimeException("Could not save upgraded model versions: " + modelVersions, e);
    }
  }

  private void save(final Map<String, String> mergedModelVersions,
                    final Set<String> models,
                    final BiConsumer<String, String> versionSetter)
  {
    for (String model : models) {
      String version = mergedModelVersions.get(model);
      if (version != null) {
        versionSetter.accept(model, version);
      }
    }
  }
}
