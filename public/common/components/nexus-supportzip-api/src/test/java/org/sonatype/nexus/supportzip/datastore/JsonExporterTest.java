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
package org.sonatype.nexus.supportzip.datastore;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThrows;
import static org.sonatype.nexus.supportzip.PasswordSanitizing.REPLACEMENT;

/**
 * Tests for {@link JsonExporter}
 */
public class JsonExporterTest
{
  private static final String EMPTY_JSON = "{}";

  private static final String SECRET = "supersecret";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private final JsonExporter underTest = new JsonExporter();

  @Test
  public void exportToJsonNullListWritesEmptyJson() throws Exception {
    File file = temporaryFolder.newFile("null-list.json");

    underTest.exportToJson(null, file);

    assertThat(readFile(file), is(EMPTY_JSON));
  }

  @Test
  public void exportToJsonEmptyListWritesEmptyJson() throws Exception {
    File file = temporaryFolder.newFile("empty-list.json");

    underTest.exportToJson(Collections.<TestData>emptyList(), file);

    assertThat(readFile(file), is(EMPTY_JSON));
  }

  @Test
  public void exportToJsonNonEmptyListWritesSanitizedJson() throws Exception {
    File file = temporaryFolder.newFile("list.json");

    underTest.exportToJson(Collections.singletonList(new TestData("myname", SECRET, 7)), file);

    String content = readFile(file);
    assertThat(content, containsString("\"name\":\"myname\""));
    assertThat(content, containsString("\"count\":7"));
    // the sensitive password field itself must hold the replacement, not the secret
    assertThat(content, containsString("\"password\":\"" + REPLACEMENT + "\""));
    assertThat(content, not(containsString(SECRET)));
  }

  @Test
  public void exportToJsonNullFileThrowsNpe() {
    assertThrows(NullPointerException.class,
        () -> underTest.exportToJson(Collections.singletonList(new TestData("a", "b", 1)), null));
  }

  @Test
  public void exportObjectToJsonNullObjectWritesEmptyJson() throws Exception {
    File file = temporaryFolder.newFile("null-object.json");

    underTest.exportObjectToJson(null, file);

    assertThat(readFile(file), is(EMPTY_JSON));
  }

  @Test
  public void exportObjectToJsonNonNullObjectWritesSanitizedJson() throws Exception {
    File file = temporaryFolder.newFile("object.json");

    underTest.exportObjectToJson(new TestData("myname", SECRET, 7), file);

    String content = readFile(file);
    assertThat(content, containsString("\"name\":\"myname\""));
    assertThat(content, containsString("\"count\":7"));
    // the sensitive password field itself must hold the replacement, not the secret
    assertThat(content, containsString("\"password\":\"" + REPLACEMENT + "\""));
    assertThat(content, not(containsString(SECRET)));
  }

  @Test
  public void exportObjectToJsonNullFileThrowsNpe() {
    assertThrows(NullPointerException.class, () -> underTest.exportObjectToJson(new TestData("a", "b", 1), null));
  }

  @Test
  public void importFromJsonEmptyJsonReturnsEmptyList() throws Exception {
    File file = temporaryFolder.newFile("import-empty.json");
    underTest.exportToJson(null, file);

    List<TestData> result = underTest.importFromJson(file, TestData.class);

    assertThat(result, is(empty()));
  }

  @Test
  public void importFromJsonBlankFileReturnsEmptyList() throws Exception {
    File file = temporaryFolder.newFile("import-blank.json");
    Files.write(file.toPath(), "   \n\t  ".getBytes(StandardCharsets.UTF_8));

    List<TestData> result = underTest.importFromJson(file, TestData.class);

    assertThat(result, is(empty()));
  }

  @Test
  public void importFromJsonArrayReturnsList() throws Exception {
    File file = temporaryFolder.newFile("round-trip-list.json");
    List<TestData> exported = Arrays.asList(new TestData("first", SECRET, 1), new TestData("second", SECRET, 2));
    underTest.exportToJson(exported, file);

    List<TestData> result = underTest.importFromJson(file, TestData.class);

    assertThat(result, hasSize(2));
    assertThat(result.get(0).getName(), is("first"));
    assertThat(result.get(0).getCount(), is(1));
    // password was sanitized during export, so the round-trip value is the replacement
    assertThat(result.get(0).getPassword(), is(REPLACEMENT));
    assertThat(result.get(1).getName(), is("second"));
    assertThat(result.get(1).getCount(), is(2));
    // every element's sensitive password must have been sanitized during export
    assertThat(result.get(1).getPassword(), is(REPLACEMENT));
  }

  @Test
  public void importFromJsonNullFileThrowsNpe() {
    assertThrows(NullPointerException.class, () -> underTest.importFromJson(null, TestData.class));
  }

  @Test
  public void importFromJsonNullClazzThrowsNpe() throws Exception {
    File file = temporaryFolder.newFile("import-null-clazz.json");

    assertThrows(NullPointerException.class, () -> underTest.importFromJson(file, null));
  }

  @Test
  public void importObjectFromJsonEmptyJsonReturnsEmpty() throws Exception {
    File file = temporaryFolder.newFile("import-object-empty.json");
    underTest.exportObjectToJson(null, file);

    Optional<TestData> result = underTest.importObjectFromJson(file, TestData.class);

    assertThat(result.isPresent(), is(false));
  }

  @Test
  public void importObjectFromJsonBlankFileReturnsEmpty() throws Exception {
    File file = temporaryFolder.newFile("import-object-blank.json");
    Files.write(file.toPath(), "    ".getBytes(StandardCharsets.UTF_8));

    Optional<TestData> result = underTest.importObjectFromJson(file, TestData.class);

    assertThat(result.isPresent(), is(false));
  }

  @Test
  public void importObjectFromJsonValidJsonReturnsObject() throws Exception {
    File file = temporaryFolder.newFile("round-trip-object.json");
    underTest.exportObjectToJson(new TestData("myname", SECRET, 42), file);

    Optional<TestData> result = underTest.importObjectFromJson(file, TestData.class);

    assertThat(result.isPresent(), is(true));
    assertThat(result.get().getName(), is("myname"));
    assertThat(result.get().getCount(), is(42));
    // password was sanitized during export, so the round-trip value is the replacement
    assertThat(result.get().getPassword(), is(REPLACEMENT));
  }

  @Test
  public void importObjectFromJsonNullFileThrowsNpe() {
    assertThrows(NullPointerException.class, () -> underTest.importObjectFromJson(null, TestData.class));
  }

  @Test
  public void importObjectFromJsonNullClazzThrowsNpe() throws Exception {
    File file = temporaryFolder.newFile("import-object-null-clazz.json");

    assertThrows(NullPointerException.class, () -> underTest.importObjectFromJson(file, null));
  }

  @Test
  public void importFromJsonExplicitEmptyJsonLiteralReturnsEmptyList() throws Exception {
    File file = temporaryFolder.newFile("import-literal-empty.json");
    Files.write(file.toPath(), EMPTY_JSON.getBytes(StandardCharsets.UTF_8));

    assertThat(underTest.importFromJson(file, TestData.class), is(empty()));
  }

  @Test
  public void importFromJsonEmptyFileReturnsEmptyList() throws Exception {
    File file = temporaryFolder.newFile("import-empty-file.json");

    // a zero-byte file yields a blank string, which is distinct from the "{}" literal branch
    assertThat(underTest.importFromJson(file, TestData.class), is(empty()));
  }

  @Test
  public void importObjectFromJsonEmptyFileReturnsEmpty() throws Exception {
    File file = temporaryFolder.newFile("import-object-empty-file.json");

    Optional<TestData> result = underTest.importObjectFromJson(file, TestData.class);

    assertThat(result.isPresent(), is(false));
  }

  @Test
  public void importObjectFromJsonExplicitEmptyJsonLiteralReturnsEmpty() throws Exception {
    File file = temporaryFolder.newFile("import-object-literal-empty.json");
    Files.write(file.toPath(), EMPTY_JSON.getBytes(StandardCharsets.UTF_8));

    Optional<TestData> result = underTest.importObjectFromJson(file, TestData.class);

    assertThat(result.isPresent(), is(false));
  }

  @Test
  public void exportObjectToJsonEmptySensitiveValueIsNotRedacted() throws Exception {
    File file = temporaryFolder.newFile("empty-secret.json");

    underTest.exportObjectToJson(new TestData("myname", "", 1), file);

    // sanitization only replaces non-empty values, so an empty password is left as-is
    String content = readFile(file);
    assertThat(content, containsString("\"password\":\"\""));
    assertThat(content, not(containsString(REPLACEMENT)));

    Optional<TestData> result = underTest.importObjectFromJson(file, TestData.class);
    assertThat(result.isPresent(), is(true));
    assertThat(result.get().getPassword(), is(""));
  }

  @Test
  public void roundTripPreservesNonSensitiveOrdering() throws Exception {
    File file = temporaryFolder.newFile("ordering.json");
    underTest.exportToJson(Arrays.asList(new TestData("alpha", "x", 0), new TestData("beta", "y", 0)), file);

    List<TestData> result = underTest.importFromJson(file, TestData.class);

    List<String> names = result.stream().map(TestData::getName).collect(java.util.stream.Collectors.toList());
    assertThat(names, contains("alpha", "beta"));
  }

  private static String readFile(final File file) throws Exception {
    return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
  }

  /**
   * Simple POJO used for (de)serialization round-trips, including a sensitive {@code password} field.
   */
  public static class TestData
  {
    private String name;

    private String password;

    private int count;

    public TestData() {
      // for Jackson deserialization
    }

    public TestData(final String name, final String password, final int count) {
      this.name = name;
      this.password = password;
      this.count = count;
    }

    public String getName() {
      return name;
    }

    public void setName(final String name) {
      this.name = name;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(final String password) {
      this.password = password;
    }

    public int getCount() {
      return count;
    }

    public void setCount(final int count) {
      this.count = count;
    }
  }
}
