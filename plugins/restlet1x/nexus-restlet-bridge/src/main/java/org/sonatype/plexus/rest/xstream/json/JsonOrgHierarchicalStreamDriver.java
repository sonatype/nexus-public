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
package org.sonatype.plexus.rest.xstream.json;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;

import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * JSON.org based StreamDriver.
 *
 * @author cstamas
 */
public class JsonOrgHierarchicalStreamDriver
    implements HierarchicalStreamDriver
{
  private ClassHintProvider classHintProvider;

  public JsonOrgHierarchicalStreamDriver() {
    this(null);
  }

  public JsonOrgHierarchicalStreamDriver(ClassHintProvider classHintProvider) {
    super();
    this.classHintProvider = classHintProvider;
  }

  public HierarchicalStreamReader createReader(Reader in) {
    if (classHintProvider != null) {
      return new JsonOrgHierarchicalStreamReader(in, false, classHintProvider);
    }
    else {
      return new JsonOrgHierarchicalStreamReader(in, true);
    }
  }

  public HierarchicalStreamReader createReader(InputStream in) {
    return createReader(new InputStreamReader(in));
  }

  /**
   * @since restlet-bridge 1.25
   */
  public HierarchicalStreamReader createReader(URL in) {
    try {
      return createReader(in.openStream());
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @since restlet-bridge 1.25
   */
  public HierarchicalStreamReader createReader(File in) {
    try {
      return createReader(new BufferedReader(new FileReader(in)));
    }
    catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public HierarchicalStreamWriter createWriter(Writer out) {
    return new JsonOrgHierarchicalStreamWriter(out, false);
  }

  public HierarchicalStreamWriter createWriter(OutputStream out) {
    return createWriter(new OutputStreamWriter(out));
  }
}
