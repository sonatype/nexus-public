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
package org.sonatype.nexus.datastore;

import java.util.List;

import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DataStoreConfiguration;
import org.sonatype.nexus.formfields.FormField;

/**
 * Describes a {@link DataStore} type.
 *
 * @since 3.19
 */
public interface DataStoreDescriptor
{
  /**
   * A user friendly name of the data store type to be presented in the UI.
   */
  String getName();

  /**
   * Form fields to configure the data store.
   */
  List<FormField<?>> getFormFields();

  /**
   * Validate the given configuration.
   */
  default void validate(DataStoreConfiguration configuration) {
    // no validation
  }

  /**
   * Is this data store type available for use?
   */
  default boolean isEnabled() {
    return true;
  }
}
