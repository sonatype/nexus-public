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
package org.sonatype.nexus.internal.commands;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.app.ApplicationLicense;
import org.sonatype.nexus.common.app.ApplicationVersion;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.karaf.shell.support.ansi.SimpleAnsi.INTENSITY_BOLD;
import static org.apache.karaf.shell.support.ansi.SimpleAnsi.INTENSITY_NORMAL;

/**
 * Display Nexus system information.
 *
 * @since 3.0
 */
@Named
@Command(name = "info", scope = "nexus", description = "Nexus system information")
public class InfoAction
    implements Action
{
  private final ApplicationVersion applicationVersion;

  private final ApplicationLicense applicationLicense;

  @Inject
  public InfoAction(final ApplicationVersion applicationVersion, final ApplicationLicense applicationLicense) {
    this.applicationVersion = checkNotNull(applicationVersion);
    this.applicationLicense = checkNotNull(applicationLicense);
  }

  @Override
  public Object execute() throws Exception {
    printSection("Application");
    printEntry("Version", applicationVersion.getVersion());
    printEntry("Edition", applicationVersion.getEdition());
    printSeparator();

    // build information
    printSection("Build");
    printEntry("Revision", applicationVersion.getBuildRevision());
    printEntry("Timestamp", applicationVersion.getBuildTimestamp());
    printSeparator();

    // license information
    printSection("License");
    printEntry("Valid", applicationLicense.isValid());
    printEntry("Required", applicationLicense.isRequired());
    printEntry("Installed", applicationLicense.isInstalled());
    printEntry("Expired", applicationLicense.isExpired());
    printEntry("Fingerprint", applicationLicense.getFingerprint());
    if (!applicationLicense.getAttributes().isEmpty()) {
      printSeparator();
      printSection("License Attributes");
      applicationLicense.getAttributes().forEach(this::printEntry);
    }

    return null;
  }

  private void printSection(final String name) {
    System.out.println(INTENSITY_BOLD + name + INTENSITY_NORMAL);
  }

  private void printEntry(final String key, final Object value) {
    System.out.printf("  %s: %s%n", key, value);
  }

  private void printSeparator() {
    System.out.println();
  }
}
