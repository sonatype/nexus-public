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
package org.sonatype.nexus.api.rest.selfhosted.lifecycle;

// NEXUS-46395 sample migration: Swagger 1.x annotations \u2192 OpenAPI 3.x (Flavor B in the
// resteasy-spike-notes.md). This file is the proof-of-concept for the 390-file annotation
// sweep that the full Phase 3 migration must perform. Mapping applied here:
//
//   @Tag(name = "X")                            \u2192 @Tag(name = "X")
//   @Operation(summary = "summary")             \u2192 @Operation(summary = "summary")
//   @Operation(summary = , notes=)        \u2192 @Operation(summary=, description=)
//   @Parameter(description = "description")             \u2192 @Parameter(description = "description")
//
// All replacement annotations live under io.swagger.v3.oas.annotations.* (provided by
// io.swagger.core.v3:swagger-annotations-jakarta).
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST API to manage the Nexus application lifecycle.
 *
 * @since 3.16
 */
@Tag(name = "Lifecycle")
public interface ManagedLifecycleResourceDoc
{
  @Operation(summary = "Get current lifecycle phase")
  String getPhase();

  @Operation(summary = "Move to new lifecycle phase")
  void setPhase(@Parameter(description = "The phase to move to") final String phase);

  @Operation(summary = "Bounce lifecycle phase",
      description = "Re-runs all phases from the given phase to the current phase")
  void bounce(@Parameter(description = "The phase to bounce") final String phase);
}
