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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import org.sonatype.plugin.metadata.GAVCoordinate;
import org.sonatype.sisu.goodies.common.Throwables2;

/**
 * Describes a response from a Nexus plugin concerning a {@link PluginActivationRequest}.
 */
@Deprecated
public final class PluginResponse
{
  // ----------------------------------------------------------------------
  // Constants
  // ----------------------------------------------------------------------

  private static final String LS = System.getProperty("line.separator");

  // ----------------------------------------------------------------------
  // Implementation fields
  // ----------------------------------------------------------------------

  private final GAVCoordinate gav;

  private final PluginActivationRequest request;

  private PluginActivationResult result;

  private PluginDescriptor descriptor;

  private Throwable reason;

  // ----------------------------------------------------------------------
  // Constructors
  // ----------------------------------------------------------------------

  PluginResponse(final GAVCoordinate gav, final PluginActivationRequest request) {
    this.gav = gav;
    this.request = request;
  }

  // ----------------------------------------------------------------------
  // Public methods
  // ----------------------------------------------------------------------

  public GAVCoordinate getPluginCoordinates() {
    return gav;
  }

  public PluginActivationResult getAchievedGoal() {
    return result;
  }

  public PluginDescriptor getPluginDescriptor() {
    return descriptor;
  }

  public Throwable getThrowable() {
    return reason;
  }

  public boolean isSuccessful() {
    return request.isSuccessful(result);
  }

  public String formatAsString(final boolean detailed) {
    final StringBuilder buf = new StringBuilder();

    buf.append("... ").append(gav);
    buf.append(" :: action=").append(request).append(" result=").append(result).append(LS);
    if (!isSuccessful() && null != reason) {
      buf.append("       Reason: ").append(Throwables2.explain(reason)).append(LS);
      if (detailed) {
        final Writer writer = new StringWriter();
        reason.printStackTrace(new PrintWriter(writer));
        buf.append("Stack trace:").append(LS).append(writer).append(LS);
      }
    }

    if (detailed && null != descriptor) {
      buf.append(LS).append(descriptor.formatAsString());
    }

    return buf.toString();
  }

  // ----------------------------------------------------------------------
  // Locally-shared methods
  // ----------------------------------------------------------------------

  void setAchievedGoal(final PluginActivationResult result) {
    this.result = result;
  }

  void setPluginDescriptor(final PluginDescriptor descriptor) {
    this.descriptor = descriptor;
  }

  void setThrowable(final Throwable reason) {
    this.reason = reason;
    result = PluginActivationResult.BROKEN;
  }
}
