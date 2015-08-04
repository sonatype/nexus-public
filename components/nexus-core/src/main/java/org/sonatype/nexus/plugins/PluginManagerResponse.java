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
package org.sonatype.nexus.plugins;

import java.util.ArrayList;
import java.util.List;

import org.sonatype.plugin.metadata.GAVCoordinate;

/**
 * Describes a response from the {@link NexusPluginManager} concerning a {@link PluginActivationRequest}.
 */
@Deprecated
public final class PluginManagerResponse
{
  // ----------------------------------------------------------------------
  // Constants
  // ----------------------------------------------------------------------

  private static final String LS = System.getProperty("line.separator");

  // ----------------------------------------------------------------------
  // Implementation fields
  // ----------------------------------------------------------------------

  private final GAVCoordinate originator;

  private final PluginActivationRequest request;

  private final List<PluginResponse> responses = new ArrayList<PluginResponse>(5);

  // ----------------------------------------------------------------------
  // Constructors
  // ----------------------------------------------------------------------

  PluginManagerResponse(final GAVCoordinate originator, final PluginActivationRequest request) {
    this.originator = originator;
    this.request = request;
  }

  // ----------------------------------------------------------------------
  // Public methods
  // ----------------------------------------------------------------------

  public GAVCoordinate getOriginator() {
    return originator;
  }

  public PluginActivationRequest getRequest() {
    return request;
  }

  public boolean isSuccessful() {
    for (final PluginResponse r : responses) {
      if (!r.isSuccessful()) {
        return false;
      }
    }
    return true;
  }

  public String formatAsString(final boolean detailed) {
    final StringBuilder buf = new StringBuilder();
    final boolean successful = isSuccessful();

    buf.append("Plugin manager request \"").append(request).append("\" on plugin \"").append(originator);
    buf.append(successful ? "\" was successful." : "\" FAILED!");

    if (detailed || !successful) {
      buf.append(LS).append("The following plugins were processed:").append(LS);
      for (final PluginResponse r : responses) {
        buf.append(r.formatAsString(detailed));
      }
    }

    return buf.toString();
  }

  // ----------------------------------------------------------------------
  // Locally-shared methods
  // ----------------------------------------------------------------------

  void addPluginResponse(final PluginResponse response) {
    responses.add(response);
  }

  void addPluginManagerResponse(final PluginManagerResponse managerResponse) {
    responses.addAll(managerResponse.responses);
  }
}
