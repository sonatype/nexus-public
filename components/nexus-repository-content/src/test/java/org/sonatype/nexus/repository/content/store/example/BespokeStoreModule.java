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
package org.sonatype.nexus.repository.content.store.example;

import javax.inject.Named;

import org.sonatype.nexus.repository.content.store.AssetBlobStore;
import org.sonatype.nexus.repository.content.store.BespokeFormatStoreModule;
import org.sonatype.nexus.repository.content.store.ComponentStore;
import org.sonatype.nexus.repository.content.store.ContentRepositoryStore;

/**
 * Bespoke store module for a bespoke format that uses a custom asset store with extra features.
 */
@Named("bespoke")
public class BespokeStoreModule
    extends BespokeFormatStoreModule<ContentRepositoryStore<TestContentRepositoryDAO>,
                                     ComponentStore<TestComponentDAO>,
                                     TestAssetStore, // adds support for browseFlaggedAssets
                                     AssetBlobStore<TestAssetBlobDAO>>
{
  // nothing to add...
}
