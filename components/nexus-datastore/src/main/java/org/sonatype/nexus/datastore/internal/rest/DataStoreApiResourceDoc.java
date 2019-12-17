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

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * @since 3.20
 */
@Api(value = "Data Stores", description = "Operations to manage data stores")
public interface DataStoreApiResourceDoc
{
  @ApiOperation("List data stores")
  List<DataStoreApiXO> getDataStores() throws Exception;

  @ApiOperation("Create new data store")
  Response createDataStore(@Valid final DataStoreApiUpdateXO apiDataStore) throws Exception;

  @ApiOperation("Update an existing data store")
  void updateDataStore(
      @ApiParam("The name of the data store to update") @NotNull String dataStoreName,
      @Valid final DataStoreApiUpdateXO apiDataStore) throws Exception;

  @ApiOperation("Delete a data store")
  void deleteDataStore(
      @ApiParam("The name of the data store to delete") @NotNull final String dataStoreName) throws Exception;
}
