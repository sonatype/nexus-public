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
package org.sonatype.nexus.extjs;

import java.io.File;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.codehaus.plexus.util.DirectoryScanner;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link ClassDefScanner}.
 */
public class ClassDefScannerTest
    extends TestSupport
{
  private ClassDefScanner scanner;

  @Before
  public void setUp() throws Exception {
    Log log = new SystemStreamLog()
    {
      @Override
      public boolean isDebugEnabled() {
        return true;
      }
    };
    this.scanner = new ClassDefScanner(log);
  }

  private List<ClassDef> scan(final File basedir) throws Exception {
    DirectoryScanner files = new DirectoryScanner();
    files.setBasedir(basedir);
    files.setIncludes(new String[]{"**/*.js"});

    List<ClassDef> classes = scanner.scan(files);
    log("Classes:");
    for (ClassDef def : classes) {
      log("  {} [priority: {}]", def.getName(), def.getPriority());
      for (String dep : def.getDependencies()) {
        log("    + {}", dep);
      }
    }
    return classes;
  }

  @Test
  public void basicOrder() throws Exception {
    File basedir = util.resolveFile("src/test/resources/basic");
    List<ClassDef> classes = scan(basedir);

    assertThat(classes, hasSize(3));

    ClassDef baz = classes.get(0);
    assertThat(baz.getName(), is("Baz"));

    ClassDef bar = classes.get(1);
    assertThat(bar.getName(), is("Bar"));
    assertThat(bar.getDependencies(), contains("Baz"));

    ClassDef foo = classes.get(2);
    assertThat(foo.getName(), is("Foo"));
    assertThat(foo.getDependencies(), contains("Bar"));
  }

  @Test
  public void priorityOrder() throws Exception {
    File basedir = util.resolveFile("src/test/resources/priority");
    List<ClassDef> classes = scan(basedir);

    assertThat(classes, hasSize(5));

    ClassDef poo = classes.get(0);
    assertThat(poo.getName(), is("Poo"));

    ClassDef ick = classes.get(1);
    assertThat(ick.getName(), is("Ick"));
    assertThat(ick.getDependencies(), contains("Poo"));

    ClassDef baz = classes.get(2);
    assertThat(baz.getName(), is("Baz"));

    ClassDef bar = classes.get(3);
    assertThat(bar.getName(), is("Bar"));
    assertThat(bar.getDependencies(), contains("Baz"));

    ClassDef foo = classes.get(4);
    assertThat(foo.getName(), is("Foo"));
    assertThat(foo.getDependencies(), contains("Bar"));
  }

  @Test
  public void altClassNameOrder() throws Exception {
    File basedir = util.resolveFile("src/test/resources/altclassname");
    List<ClassDef> classes = scan(basedir);

    assertThat(classes, hasSize(3));

    ClassDef baz = classes.get(0);
    assertThat(baz.getName(), is("Baz"));

    ClassDef bar = classes.get(1);
    assertThat(bar.getName(), is("Bar"));
    assertThat(bar.getAlternateClassName(), containsInAnyOrder("Test.Bar", "Other.Bar"));
    assertThat(bar.getDependencies(), contains("Baz"));

    ClassDef foo = classes.get(2);
    assertThat(foo.getName(), is("Foo"));
    assertThat(foo.getAlternateClassName(), containsInAnyOrder("Test.Foo"));
    assertThat(foo.getDependencies(), contains("Test.Bar"));
  }

  @Test
  public void mvcOrder() throws Exception {
    File basedir = util.resolveFile("src/test/resources/mvc");
    scanner.setNamespace("Test");
    List<ClassDef> classes = scan(basedir);

    assertThat(classes, hasSize(5));

    assertThat(classes.get(4).getName(), is("Test.Application"));
    assertThat(classes.get(4).getDependencies(), containsInAnyOrder("Ext.app.Application", "Test.controller.Fun"));
  }
}
