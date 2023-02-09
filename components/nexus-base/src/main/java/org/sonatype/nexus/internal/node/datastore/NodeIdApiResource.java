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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.node.datastore.NodeHeartbeatManager;
import org.sonatype.nexus.node.datastore.NodeIdStore;
import org.sonatype.nexus.rest.Resource;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;


/**
 * REST API to reset the stored Node ID. This is intended for use when cloning a system.
 *
 * @since 3.37
 */
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(NodeIdApiResource.PATH)
@Named
@Singleton
public class NodeIdApiResource
    extends ComponentSupport
    implements Resource, NodeIdApiResourceDoc
{
  public static final String PATH = V1_API_PREFIX + "/system/node";

  private final NodeIdStore nodeIdStore;

  private final NodeHeartbeatManager nodeHeartbeatManager;

  @Inject
  public NodeIdApiResource(final NodeIdStore nodeIdStore,
                           final NodeHeartbeatManager nodeHeartbeatManager) {
    this.nodeIdStore = nodeIdStore;
    this.nodeHeartbeatManager = checkNotNull(nodeHeartbeatManager);
  }

  @Override
  @GET
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  public Map<String, Map<String, Object>> getNodesInformation() {
    return nodeHeartbeatManager.getSystemInformationForNodes();
  }

  @Override
  @DELETE
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  public void clear() {
    nodeIdStore.clear();
  }
}
