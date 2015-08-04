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
package org.sonatype.nexus.ruby;


public class MetadataBuilder
    extends AbstractMetadataBuilder
{
  private final StringBuilder xml;

  private boolean closed = false;

  private final DependencyData deps;

  public MetadataBuilder(DependencyData deps) {
    super(deps.modified());
    this.deps = deps;
    xml = new StringBuilder();
    xml.append("<metadata>\n");
    xml.append("  <groupId>rubygems</groupId>\n");
    xml.append("  <artifactId>").append(deps.name()).append("</artifactId>\n");
    xml.append("  <versioning>\n");
    xml.append("    <versions>\n");
  }

  public void appendVersions(boolean isPrerelease) {
    for (String version : deps.versions(isPrerelease)) {
      xml.append("      <version>").append(version);
      if (isPrerelease) {
        xml.append("-SNAPSHOT");
      }
      xml.append("</version>\n");
    }
  }

  public void close() {
    if (!closed) {
      xml.append("    </versions>\n");
      xml.append("    <lastUpdated>")
          .append(timestamp)
          .append("</lastUpdated>\n");
      xml.append("  </versioning>\n");
      xml.append("</metadata>\n");
      closed = true;
    }
  }

  public String toString() {
    close();
    return xml.toString();
  }
}
