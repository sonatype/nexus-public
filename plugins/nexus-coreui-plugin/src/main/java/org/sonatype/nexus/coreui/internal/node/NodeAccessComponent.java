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
package org.sonatype.nexus.coreui.internal.node;

import java.util.List;
import java.util.Map.Entry;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.extdirect.DirectComponentSupport;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

/**
 * NodeAccessComponent {@link DirectComponentSupport}.
 */
@Named
@Singleton
@DirectAction(action = "node_NodeAccess")
public class NodeAccessComponent
    extends DirectComponentSupport
{
  private final NodeAccess nodeAccess;

  @Inject
  public NodeAccessComponent(final NodeAccess nodeAccess) {
    this.nodeAccess = checkNotNull(nodeAccess);
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  public List<NodeInfoXO> nodes() {
    return nodeAccess.getMemberAliases().entrySet().stream().map(this::asNodeInfoXO).collect(toList());
  }

  private NodeInfoXO asNodeInfoXO(final Entry<String, String> entry) {
    NodeInfoXO nodeInfoXO = new NodeInfoXO();
    nodeInfoXO.setName(entry.getKey());
    nodeInfoXO.setLocal(entry.getKey().equals(nodeAccess.getId()));
    nodeInfoXO.setDisplayName(entry.getValue());
    return nodeInfoXO;
  }
}
