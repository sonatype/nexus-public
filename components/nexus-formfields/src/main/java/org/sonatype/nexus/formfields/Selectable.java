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
package org.sonatype.nexus.formfields;

import java.util.Map;

/**
 * Implemented by {@link FormField}s whose value should be selected from a data store (combobox).
 * The data store should return collections of records that have "id" and "name" fields.
 *
 * @since 2.7
 */
public interface Selectable
{

  /**
   * Returns Ext.Direct API name used to configure Ext proxy.
   * E.g.
   * "coreui_RepositoryTarget.read"
   *
   * @since 3.0
   */
  String getStoreApi();

  /**
   * @return Filters to be applied to store
   * @since 3.0
   */
  Map<String, String> getStoreFilters();

  /**
   * Returns the name of the property that should be considered as an record id. Defaults to "id";
   */
  String getIdMapping();

  /**
   * Returns the name of the property that should be considered as an record description. Defaults to "name";
   */
  String getNameMapping();

}
