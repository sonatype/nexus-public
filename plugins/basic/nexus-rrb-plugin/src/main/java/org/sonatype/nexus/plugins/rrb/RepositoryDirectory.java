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
package org.sonatype.nexus.plugins.rrb;

import javax.xml.bind.annotation.XmlType;

@XmlType(name = "node")
public class RepositoryDirectory
{
  @Override
  public String toString() {
    return "RepositoryDirectory [lastModified=" + lastModified + ", leaf=" + leaf + ", relativePath="
        + relativePath + ", resourceURI=" + resourceURI + ", sizeOnDisk=" + sizeOnDisk + ", text=" + text + "]";
  }

  private String resourceURI = "";

  private String relativePath = "";

  private String text = "";

  private boolean leaf;

  private String lastModified = "";

  private int sizeOnDisk = -1;

  public String getResourceURI() {
    return resourceURI;
  }

  public void setResourceURI(String baseUrl) {
    this.resourceURI = baseUrl;
  }

  public String getText() {
    return text;
  }

  public void setText(String name) {
    this.text = name;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((resourceURI == null) ? 0 : resourceURI.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final RepositoryDirectory other = (RepositoryDirectory) obj;
    if (resourceURI == null) {
      if (other.resourceURI != null) {
        return false;
      }
    }
    else if (!resourceURI.equals(other.resourceURI)) {
      return false;
    }
    return true;
  }

  public String getRelativePath() {
    return relativePath;
  }

  public void setRelativePath(String relativePath) {
    this.relativePath = relativePath;
  }

  public boolean isLeaf() {
    return leaf;
  }

  public void setLeaf(boolean leaf) {
    this.leaf = leaf;
  }

  public String getLastModified() {
    return lastModified;
  }

  public void setLastModified(String lastModified) {
    this.lastModified = lastModified;
  }

  public int getSizeOnDisk() {
    return sizeOnDisk;
  }

  public void setSizeOnDisk(int sizeOnDisk) {
    this.sizeOnDisk = sizeOnDisk;
  }
}