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
package org.sonatype.nexus.configuration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.file.Files;

import org.sonatype.nexus.configuration.ModelUtils.CharacterModelReader;
import org.sonatype.nexus.configuration.ModelUtils.CharacterModelUpgrader;
import org.sonatype.nexus.configuration.ModelUtils.CharacterModelWriter;
import org.sonatype.nexus.configuration.ModelUtils.CorruptModelException;
import org.sonatype.nexus.configuration.ModelUtils.Versioned;
import org.sonatype.nexus.configuration.ModelloUtils.VersionedInFieldXmlModelloModelHelper;
import org.sonatype.nexus.util.file.FileSupport;
import org.sonatype.sisu.goodies.testsupport.TestSupport;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.sonatype.sisu.goodies.testsupport.hamcrest.FileMatchers.exists;

/**
 * UT for {@link ModelUtils}.
 *
 * @since 2.7.0
 */
public class ModelUtilsTest
    extends TestSupport
{
  public static class DomReader
      extends CharacterModelReader<Xpp3Dom>
      implements Versioned
  {
    private final VersionedInFieldXmlModelloModelHelper versionedModelloModelHelper;

    public DomReader() {
      this.versionedModelloModelHelper = new VersionedInFieldXmlModelloModelHelper(charset, "version");
    }

    @Override
    public Xpp3Dom read(final Reader reader) throws IOException, CorruptModelException {
      try {
        return Xpp3DomBuilder.build(reader);
      }
      catch (XmlPullParserException e) {
        throw new CorruptModelException(e.getMessage(), e);
      }
    }

    @Override
    public String readVersion(final InputStream input) throws IOException, CorruptModelException {
      return versionedModelloModelHelper.readVersion(input);
    }
  }

  public static final DomReader DOM_READER = new DomReader();

  public static final CharacterModelWriter<Xpp3Dom> DOM_WRITER = new CharacterModelWriter<Xpp3Dom>()
  {
    @Override
    public void write(final Writer writer, final Xpp3Dom model) throws IOException {
      Xpp3DomWriter.write(writer, model);
      writer.flush();
    }
  };

  public static final CharacterModelUpgrader V1_V2_UPGRADER = new CharacterModelUpgrader("1", "2")
  {
    @Override
    public void upgrade(final Reader reader, final Writer writer) throws IOException, CorruptModelException {
      final Xpp3Dom dom = DOM_READER.read(reader);
      dom.getChild("version").setValue(toVersion());
      final Xpp3Dom newnode = new Xpp3Dom("v2field");
      newnode.setValue("foo");
      dom.addChild(newnode);
      DOM_WRITER.write(writer, dom);
    }
  };

  public static final CharacterModelUpgrader V2_V3_UPGRADER = new CharacterModelUpgrader("2", "3")
  {
    @Override
    public void upgrade(final Reader reader, final Writer writer) throws IOException, CorruptModelException {
      final Xpp3Dom dom = DOM_READER.read(reader);
      dom.getChild("version").setValue(toVersion());
      final Xpp3Dom newnode = new Xpp3Dom("v3field");
      newnode.setValue("bar");
      dom.addChild(newnode);
      DOM_WRITER.write(writer, dom);
    }
  };

  public static final CharacterModelUpgrader V2_V3_UPGRADER_FAILING = new CharacterModelUpgrader("2", "3")
  {
    @Override
    public void upgrade(final Reader reader, final Writer writer) throws IOException, CorruptModelException {
      throw new CorruptModelException(getClass().getSimpleName());
    }
  };

  @Test
  public void versioning() throws Exception {
    final String payload = "<foo><version>1</version></foo>";
    final File file = util.createTempFile();
    FileSupport.writeFile(file.toPath(), payload);

    final String version;
    try(final InputStream input = Files.newInputStream(file.toPath())) {
      version = DOM_READER.readVersion(input);
    }
    assertThat(version, equalTo("1"));
  }

  @Test(expected = CorruptModelException.class)
  public void versioningFieldWrong() throws Exception {
    final String payload = "<foo><versionField>1</versionField></foo>";
    final File file = util.createTempFile();
    FileSupport.writeFile(file.toPath(), payload);

    final String version;
    try(final InputStream input = Files.newInputStream(file.toPath())) {
      version = DOM_READER.readVersion(input);
    }
  }

  @Test
  public void plainUpgrade() throws Exception {
    final String payload = "<foo><version>1</version></foo>";
    final File file = util.createTempFile();
    FileSupport.writeFile(file.toPath(), payload);

    final Xpp3Dom dom = ModelUtils.load("3", file, DOM_READER, V1_V2_UPGRADER, V2_V3_UPGRADER);

    assertThat(normalizeLineEndings(dom.toString()), equalTo(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<foo>\n  <version>3</version>\n  <v2field>foo</v2field>\n  <v3field>bar</v3field>\n</foo>"));
  }

  @Test
  public void intermediateUpgrade() throws Exception {
    final String payload = "<foo><version>1</version></foo>";
    final File file = util.createTempFile();
    FileSupport.writeFile(file.toPath(), payload);

    final Xpp3Dom dom = ModelUtils.load("2", file, DOM_READER, V1_V2_UPGRADER, V2_V3_UPGRADER);

    assertThat(normalizeLineEndings(dom.toString()), equalTo(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<foo>\n  <version>2</version>\n  <v2field>foo</v2field>\n</foo>"));
  }

  @Test
  public void intermediateNoUpgrade() throws Exception {
    final String payload = "<foo><version>1</version></foo>";
    final File file = util.createTempFile();
    FileSupport.writeFile(file.toPath(), payload);

    final Xpp3Dom dom = ModelUtils.load("1", file, DOM_READER, V1_V2_UPGRADER, V2_V3_UPGRADER);

    assertThat(normalizeLineEndings(dom.toString()), equalTo(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<foo>\n  <version>1</version>\n</foo>"));
  }

  @Test(expected = CorruptModelException.class)
  public void noVersionNode() throws Exception {
    final String payload = "<foo></foo>";
    final File file = util.createTempFile();
    FileSupport.writeFile(file.toPath(), payload);

    ModelUtils.load("1", file, DOM_READER, V1_V2_UPGRADER, V2_V3_UPGRADER);
  }

  @Test(expected = CorruptModelException.class)
  public void emptyVersionNode() throws Exception {
    final String payload = "<foo><version/></foo>";
    final File file = util.createTempFile();
    FileSupport.writeFile(file.toPath(), payload);

    ModelUtils.load("1", file, DOM_READER, V1_V2_UPGRADER, V2_V3_UPGRADER);
  }

  @Test(expected = CorruptModelException.class)
  public void corruptXmlModel() throws Exception {
    final String payload = "<foo version/></foo>";
    final File file = util.createTempFile();
    FileSupport.writeFile(file.toPath(), payload);

    ModelUtils.load("1", file, DOM_READER, V1_V2_UPGRADER, V2_V3_UPGRADER);
  }

  @Test(expected = CorruptModelException.class)
  public void corruptXmlModelDuringUpgrade() throws Exception {
    final String payload = "<foo><version>1</version></foo>";
    final File file = util.createTempFile();
    FileSupport.writeFile(file.toPath(), payload);

    ModelUtils.load("3", file, DOM_READER, V1_V2_UPGRADER, V2_V3_UPGRADER_FAILING);
  }

  @Test
  public void upgradeNoConverter() throws Exception {
    final String payload = "<foo><version>1</version></foo>";
    final File file = util.createTempFile();
    FileSupport.writeFile(file.toPath(), payload);

    try {
      final Xpp3Dom dom = ModelUtils.load("99", file, DOM_READER, V1_V2_UPGRADER, V2_V3_UPGRADER);
    }
    catch (IOException e) {
      assertThat(e.getMessage(), startsWith("Could not upgrade model"));
      // TODO: assertiom against message makes test fragile, but this is what "tail" should be
      // assertThat(e.getMessage(), endsWith("to version 99, is upgraded to 3, originally was 1, available upgraders exists for versions [2, 1]"));
    }
  }

  @Test
  public void plainSave() throws Exception {
    final String payload = "<foo><version>1</version></foo>";
    final File file = util.createTempFile();
    final File backupFile = new File(file.getParentFile(), file.getName() + ".bak");
    Files.delete(file.toPath());
    final Xpp3Dom model = Xpp3DomBuilder.build(new StringReader(payload));

    ModelUtils.save(model, file, DOM_WRITER);

    assertThat(file, exists());
    assertThat(FileSupport.readFile(file.toPath()), equalTo(
        "<foo>\n  <version>1</version>\n</foo>"));
    assertThat(backupFile, not(exists()));

    ModelUtils.save(model, file, DOM_WRITER);

    assertThat(file, exists());
    assertThat(FileSupport.readFile(file.toPath()), equalTo(
        "<foo>\n  <version>1</version>\n</foo>"));
    assertThat(backupFile, exists());
    assertThat(FileSupport.readFile(backupFile.toPath()), equalTo(
        "<foo>\n  <version>1</version>\n</foo>"));

    final Xpp3Dom newnode = new Xpp3Dom("second");
    newnode.setValue("foo");
    model.addChild(newnode);

    ModelUtils.save(model, file, DOM_WRITER);

    assertThat(file, exists());
    assertThat(FileSupport.readFile(file.toPath()), equalTo(
        "<foo>\n  <version>1</version>\n  <second>foo</second>\n</foo>"));
    assertThat(backupFile, exists());
    assertThat(FileSupport.readFile(backupFile.toPath()), equalTo(
        "<foo>\n  <version>1</version>\n</foo>"));
  }

  /**
   * Normalize OS-specific differences in line endings.
   */
  private String normalizeLineEndings(String input) {
    return input == null ? null : input.replaceAll("\r\n", "\n");
  }
}
