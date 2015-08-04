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

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "rrbresponse")
public class MavenRepositoryReaderResponse
    implements Serializable
{
  private static final long serialVersionUID = 3969716837308011475L;

  List<RepositoryDirectory> data;

  public MavenRepositoryReaderResponse() {
    super();
  }

  @XmlElementWrapper(name = "data")
  @XmlElement(name = "node")
  public List<RepositoryDirectory> getData() {
    return data;
  }

  public void setData(List<RepositoryDirectory> data) {
    this.data = data;
  }

  @Override
  public String toString() {
    return "MavenRepositoryReaderResponse [data=" + data + "]";
  }

}