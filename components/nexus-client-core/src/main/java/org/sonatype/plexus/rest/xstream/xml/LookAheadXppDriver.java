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
package org.sonatype.plexus.rest.xstream.xml;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.XmlFriendlyNameCoder;
import com.thoughtworks.xstream.io.xml.XppDriver;

/**
 * COPIED FROM plexus-restlet-bridge to cease the dependency on it (as it would pull in Restlet and many other
 * dependencies).
 * <p/>
 * A HierarchicalStreamDriver that loads the {@link LookAheadXppReader}.
 *
 * @since 2.3
 */
public class LookAheadXppDriver
    extends XppDriver
{

  private static boolean xppLibraryPresent;

  public LookAheadXppDriver() {
    super(new XmlFriendlyNameCoder());
  }

  public HierarchicalStreamReader createReader(Reader xml) {
    loadLibrary();
    return new LookAheadXppReader(xml, getNameCoder());
  }

  public HierarchicalStreamReader createReader(InputStream in) {
    return createReader(new InputStreamReader(in));
  }

  private void loadLibrary() {
    if (!xppLibraryPresent) {
      try {
        getClass().getClassLoader().loadClass("io.github.xstream.mxparser.MXParser");
      }
      catch (ClassNotFoundException e) {
        throw new IllegalArgumentException(
            "XPP3 pull parser library not present. Specify another driver."
                + " For example: new XStream(new DomDriver())", e
        );
      }
      xppLibraryPresent = true;
    }
  }

  public HierarchicalStreamWriter createWriter(Writer out) {
    return new PrettyPrintWriter(out, getNameCoder());
  }

  public HierarchicalStreamWriter createWriter(OutputStream out) {
    return createWriter(new OutputStreamWriter(out));
  }

}
