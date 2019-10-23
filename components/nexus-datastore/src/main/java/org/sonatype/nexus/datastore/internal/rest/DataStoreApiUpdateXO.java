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
package org.sonatype.nexus.datastore.internal.rest;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.datastore.api.DataStoreConfiguration;

import io.swagger.annotations.ApiModelProperty;

/**
 * REST API model for updating Data Stores.
 *
 * @since 3.next
 */
public class DataStoreApiUpdateXO
    extends DataStoreApiXO
{
  @ApiModelProperty(value = "The password to use for the JDBC connection.", example = "")
  private String password;

  public DataStoreApiUpdateXO() {
    // do nothing
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(final String password) {
    this.password = password;
  }

  @Override
  public DataStoreConfiguration toDataStoreConfiguration() {
    DataStoreConfiguration configuration = super.toDataStoreConfiguration();
    if (!Strings2.isBlank(password)) {
      configuration.getAttributes().put("password", password);
    }

    return configuration;
  }
}
