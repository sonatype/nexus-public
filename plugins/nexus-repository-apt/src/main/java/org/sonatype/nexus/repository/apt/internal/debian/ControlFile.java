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
package org.sonatype.nexus.repository.apt.internal.debian;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @since 3.17
 */
public class ControlFile
{
  public static class Builder
  {
    private List<Paragraph> paragraphs;

    private Builder(final List<Paragraph> paragraphs) {
      super();
      this.paragraphs = paragraphs;
    }

    public Builder removeParagraphs(final Predicate<Paragraph> p) {
      paragraphs = paragraphs.stream().filter(p).collect(Collectors.toList());
      return this;
    }

    public Builder addParagraph(final Paragraph p) {
      paragraphs.add(p);
      return this;
    }

    public Builder replaceParagraph(final Predicate<Paragraph> predicate, final Paragraph p) {
      paragraphs = Stream
          .concat(paragraphs.stream().filter(predicate), Stream.of(p))
          .collect(Collectors.toList());
      return this;
    }

    public Builder transformParagraphs(final Predicate<Paragraph> predicate, final Function<Paragraph, Paragraph> transform) {
      paragraphs = paragraphs.stream()
          .filter(predicate)
          .map(transform)
          .collect(Collectors.toList());
      return this;
    }

    public ControlFile build() {
      return new ControlFile(paragraphs);
    }
  }

  public static Builder newBuilder() {
    return new Builder(new ArrayList<>());
  }

  private final List<Paragraph> paragraphs;

  public ControlFile(final List<Paragraph> paragraphs) {
    super();
    this.paragraphs = new ArrayList<>(paragraphs);
  }

  public Builder builder() {
    return new Builder(new ArrayList<>(paragraphs));
  }

  public List<Paragraph> getParagraphs() {
    return paragraphs;
  }

  public Optional<ControlField> getField(final String name) {
    if (paragraphs.isEmpty()) {
      return Optional.empty();
    }

    return paragraphs.get(0).getField(name);
  }

  public List<ControlField> getFields() {
    if (paragraphs.isEmpty()) {
      return Collections.emptyList();
    }

    return paragraphs.get(0).getFields();
  }

  public static class Paragraph
  {
    private final List<ControlField> fields;

    public Paragraph(final List<ControlField> fields) {
      super();
      this.fields = new ArrayList<>(fields);
    }

    public Optional<ControlField> getField(final String name) {
      for (ControlField f : fields) {
        if (f.key.equals(name)) {
          return Optional.of(f);
        }
      }

      return Optional.empty();
    }

    public List<ControlField> getFields() {
      return Collections.unmodifiableList(fields);
    }

    public Paragraph withFields(final List<ControlField> updateFields) {
      Map<String, ControlField> index = updateFields.stream().collect(Collectors.toMap(f -> f.key, f -> f));
      return new Paragraph(Stream
          .concat(fields.stream().filter(f -> !index.containsKey(f.key)), updateFields.stream())
          .collect(Collectors.toList()));
    }

    @Override
    public String toString() {
      return fields.stream()
          .map(f -> f.key + ": " + f.value)
          .collect(Collectors.joining("\n"));
    }
  }

  public static class ControlField
  {
    public final String key;

    public final String value;

    public ControlField(final String key, final String value) {
      super();
      this.key = key;
      this.value = value;
    }

    public String foldedValue() {
      return Arrays.stream(value.split("\n"))
          .map(s -> s.trim())
          .collect(Collectors.joining());
    }

    public List<String> listValue() {
      return Arrays.asList(value.trim().split("\\s+"));
    }
  }
}
