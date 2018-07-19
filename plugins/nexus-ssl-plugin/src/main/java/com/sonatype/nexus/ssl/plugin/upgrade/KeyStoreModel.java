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
package com.sonatype.nexus.ssl.plugin.upgrade;

/**
 * Holds information about the 'keystore' model defined by this plugin.
 *
 * This model is stored in the 'config' database under its own schema. While previous upgrades
 * used the generic 'config' model name, future upgrades should use this more specific model.
 *
 * Upgrades should depend on the 'config' model and version at the time the upgrade was written.
 *
 * @since 3.13
 */
public interface KeyStoreModel
{
  String NAME = "keystore";
}
