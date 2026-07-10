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
package org.sonatype.nexus.api.rest.selfhosted.security.realm;

import java.util.List;

import org.sonatype.nexus.api.rest.selfhosted.security.realm.model.RealmApiXO;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @since 3.20
 */
@Tag(name = "Security management: realms")
public interface RealmApiResourceDoc
{
  @Operation(summary = "List the available realms")
  List<RealmApiXO> getRealms();

  @Operation(summary = "List the active realm IDs in order")
  List<String> getActiveRealms();

  @Operation(summary = "Set the active security realms in the order they should be used")
  void setActiveRealms(@Parameter(description = "The realm IDs") List<String> realmIds);
}
