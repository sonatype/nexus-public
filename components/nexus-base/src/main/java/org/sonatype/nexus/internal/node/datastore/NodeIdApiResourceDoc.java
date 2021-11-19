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
package org.sonatype.nexus.internal.node.datastore;

import org.sonatype.nexus.internal.node.datastore.NodeIdApiResource.NodeInformation;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * REST API to reset the stored Node ID. This is intended for use when cloning a system.
 *
 * @since 3.37
 */
@Api(value = "System: Nodes")
public interface NodeIdApiResourceDoc
{
  @ApiOperation("Get information about this node")
  @ApiResponses(value = {@ApiResponse(code = 403, message = "Insufficient permissions to update settings")})
  NodeInformation getNodeId();

  @ApiOperation("Reset the ID for this node. Takes effect after restart and should only be used when cloning an instance")
  @ApiResponses(value = {@ApiResponse(code = 403, message = "Insufficient permissions to update settings")})
  void clear();
}
