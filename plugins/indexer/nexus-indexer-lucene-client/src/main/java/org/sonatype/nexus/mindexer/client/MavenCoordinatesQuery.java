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
package org.sonatype.nexus.mindexer.client;

public class MavenCoordinatesQuery
    extends Query
{

  public String getGroupId() {
    return getTerms().get("g");
  }

  public void setGroupId(String g) {
    getTerms().put("g", g);
  }

  public String getArtifactId() {
    return getTerms().get("a");
  }

  public void setArtifactId(String a) {
    getTerms().put("a", a);
  }

  public String getVersion() {
    return getTerms().get("v");
  }

  public void setVersion(String v) {
    getTerms().put("v", v);
  }

  public String getClassifier() {
    return getTerms().get("c");
  }

  public void setClassifier(String c) {
    getTerms().put("c", c);
  }

  public String getType() {
    return getTerms().get("p");
  }

  public void setType(String t) {
    getTerms().put("p", t);
  }
}
