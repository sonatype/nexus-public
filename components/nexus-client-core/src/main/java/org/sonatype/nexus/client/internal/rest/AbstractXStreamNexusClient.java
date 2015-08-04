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
package org.sonatype.nexus.client.internal.rest;

import org.sonatype.nexus.client.internal.util.Check;
import org.sonatype.nexus.client.rest.ConnectionInfo;

import com.thoughtworks.xstream.XStream;

/**
 * Basically AbstractNexusClient with some extra fluff: it maintains reference to XStream used by serialization
 * (whatever underlying class would use), to make it able to pass XStream around (toward subsystems) to apply needed
 * XStream configuration. As Nexus currently is married to XStream, this will probably change, hence, this class, as
 * one
 * of the implementations keeps the fact of XStream use encapsulated, I did not want to proliferate it through all of
 * Nexus Client.
 *
 * @since 2.1
 */
public abstract class AbstractXStreamNexusClient
    extends AbstractNexusClient
{

  private final XStream xstream;

  protected AbstractXStreamNexusClient(final ConnectionInfo connectionInfo, final XStream xstream) {
    super(connectionInfo);
    this.xstream = Check.notNull(xstream, XStream.class);
  }

  public XStream getXStream() {
    return xstream;
  }
}
