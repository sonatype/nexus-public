/*
 * Copyright (c) 2007-2014 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.bolyuba.nexus.plugin.npm.service.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.configuration.application.ApplicationDirectories;
import org.sonatype.nexus.proxy.item.AbstractContentLocator;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.StringContentLocator;
import org.sonatype.nexus.util.DigesterUtils;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.bolyuba.nexus.plugin.npm.NpmRepository;
import com.bolyuba.nexus.plugin.npm.service.NpmBlob;
import com.bolyuba.nexus.plugin.npm.service.PackageRoot;
import com.bolyuba.nexus.plugin.npm.service.PackageVersion;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Metadata parser and producer component, parses out of "raw" (streamed or not) source to entities and other way
 * around.
 */
@Singleton
@Named
public class MetadataParser
    extends ComponentSupport
{
  private final File temporaryDirectory;

  private final ObjectMapper objectMapper;

  @Inject
  public MetadataParser(final ApplicationDirectories applicationDirectories) {
    this(applicationDirectories.getTemporaryDirectory());
  }

  @VisibleForTesting
  public MetadataParser(final File temporaryDirectory) {
    this.temporaryDirectory = checkNotNull(temporaryDirectory);
    this.objectMapper = new ObjectMapper(); // this parses registry JSON
  }

  public File getTemporaryDirectory() {
    return temporaryDirectory;
  }

  // Parse API

  public PackageRootIterator parseRegistryRoot(final String repositoryId, final ContentLocator contentLocator)
      throws IOException
  {
    checkNotNull(repositoryId);
    checkNotNull(contentLocator);
    checkArgument(NpmRepository.JSON_MIME_TYPE.equals(contentLocator.getMimeType()), "JSON is expected inout!");
    return new ParsingPackageRootIterator(repositoryId,
        objectMapper.getFactory().createParser(contentLocator.getContent()));
  }

  public PackageRoot parsePackageRoot(final String repositoryId, final ContentLocator contentLocator)
      throws IOException
  {
    checkNotNull(repositoryId);
    checkNotNull(contentLocator);
    checkArgument(NpmRepository.JSON_MIME_TYPE.equals(contentLocator.getMimeType()), "JSON is expected inout!");
    try (final JsonParser parser = objectMapper.getFactory().createParser(contentLocator.getContent())) {
      final PackageRoot packageRoot = parsePackageRoot(repositoryId, parser);
      checkArgument(!packageRoot.isIncomplete(),
          "Wrong API use, incomplete package roots should not be consumed this way!");
      return packageRoot;
    }
  }

  // Produce API

  public RegistryRootContentLocator produceRegistryRoot(final PackageRootIterator packageRootIterator)
      throws IOException
  {
    checkNotNull(packageRootIterator);
    return new RegistryRootContentLocator(this, packageRootIterator);
  }

  @Nullable
  public StringContentLocator produceShrinkedPackageRoot(final PackageRoot root) throws IOException {
    if (root == null) {
      return null;
    }
    final String jsonString = objectMapper.writeValueAsString(root.getRaw());
    return new StringContentLocator(jsonString, NpmRepository.JSON_MIME_TYPE);
  }

  @Nullable
  public StringContentLocator producePackageRoot(final PackageRoot root) throws IOException {
    if (root == null) {
      return null;
    }
    final String jsonString = objectMapper.writeValueAsString(root.getRaw());
    return new StringContentLocator(jsonString, NpmRepository.JSON_MIME_TYPE);
  }

  @Nullable
  public StringContentLocator producePackageVersion(final PackageVersion version)
      throws IOException
  {
    if (version == null) {
      return null;
    }
    final String jsonString = objectMapper.writeValueAsString(version.getRaw());
    return new StringContentLocator(jsonString, NpmRepository.JSON_MIME_TYPE);
  }

  // ==

  private PackageRoot parsePackageRoot(final String repositoryId, final JsonParser parser) throws IOException {
    final Map<String, Object> raw = Maps.newHashMap();
    final Map<String, NpmBlob> attachments = Maps.newHashMap();
    checkArgument(parser.nextToken() == JsonToken.START_OBJECT, "Unexpected input %s, expected %s",
        parser.getCurrentToken(), JsonToken.START_OBJECT);
    while (parser.nextToken() == JsonToken.FIELD_NAME) {
      final String fieldName = parser.getCurrentName();
      if ("_attachments".equals(fieldName)) {
        parsePackageAttachments(parser, attachments);
        continue;
      }
      final JsonToken token = parser.nextValue();
      if (token == JsonToken.START_OBJECT) {
        raw.put(fieldName, parser.readValueAs(new TypeReference<Map<String, Object>>() {}));
      }
      else if (token == JsonToken.START_ARRAY) {
        raw.put(fieldName, parser.readValueAs(new TypeReference<List<Object>>() {}));
      }
      else {
        switch (token) {
          case VALUE_NULL: {
            raw.put(fieldName, null);
            break;
          }
          case VALUE_FALSE: {
            raw.put(fieldName, Boolean.FALSE);
            break;
          }
          case VALUE_TRUE: {
            raw.put(fieldName, Boolean.TRUE);
            break;
          }
          case VALUE_NUMBER_INT: {
            raw.put(fieldName, parser.getValueAsInt());
            break;
          }
          case VALUE_NUMBER_FLOAT: {
            raw.put(fieldName, parser.getValueAsDouble());
            break;
          }
          case VALUE_STRING: {
            raw.put(fieldName, parser.getValueAsString());
            break;
          }
          default: {
            throw new IllegalArgumentException("Unexpected token: " + token);
          }
        }
        raw.put(fieldName, parser.getValueAsString());
      }
    }
    final PackageRoot result = new PackageRoot(repositoryId, raw);
    if (!attachments.isEmpty()) {
      result.getAttachments().putAll(attachments);
    }
    return result;
  }

  @VisibleForTesting
  void parsePackageAttachments(final JsonParser parser,
                               final Map<String, NpmBlob> attachments) throws IOException
  {
    checkArgument(parser.nextToken() == JsonToken.START_OBJECT, "Unexpected input %s, expected %s",
        parser.getCurrentToken(), JsonToken.START_OBJECT);
    while (parser.nextToken() == JsonToken.FIELD_NAME) {
      final NpmBlob attachment = parsePackageAttachment(parser);
      if (attachment != null) {
        attachments.put(attachment.getName(), attachment);
      }
    }
  }

  /**
   * Parses CouchDB attachment, if any, and returns it. Returns {@code null} if attachment is
   * incomplete or "stub". Still, parser will consume whole attachment object and will be located
   * on next token.
   *
   * @see <a href="http://wiki.apache.org/couchdb/HTTP_Document_API#Inline_Attachments">Attachments</a>
   */
  private @Nullable NpmBlob parsePackageAttachment(final JsonParser parser) throws IOException {
    String name = parser.getCurrentName();
    String contentType = "application/octet-stream";
    long length = ContentLocator.UNKNOWN_LENGTH;
    String sha1hash = null;
    File file = null;
    boolean stub = false;
    checkArgument(parser.nextToken() == JsonToken.START_OBJECT, "Unexpected input %s, expected %s",
        parser.getCurrentToken(), JsonToken.START_OBJECT);
    while (parser.nextToken() == JsonToken.FIELD_NAME) {
      final String fieldName = parser.getCurrentName();
      parser.nextValue();
      if ("content_type".equals(fieldName)) {
        contentType = parser.getValueAsString();
      }
      else if ("stub".equals(fieldName)) {
        stub = parser.getValueAsBoolean();
      }
      else if ("length".equals(fieldName)) {
        length = parser.getValueAsLong();
      }
      else if ("data".equals(fieldName)) {
        file = File.createTempFile("npm_attachment", "temp", temporaryDirectory);
        final byte[] binaryValue = parser.getBinaryValue();
        sha1hash = DigesterUtils.getSha1Digest(binaryValue);
        // TODO: can Jackson stream binary? I doubt...
        Files.write(parser.getBinaryValue(), file);
      }
    }
    if (stub || file == null) {
      // This happens with "stub" attachments, so just return null
      return null;
    }
    if (length != ContentLocator.UNKNOWN_LENGTH) {
      checkArgument(file.length() == length, "Invalid content length!");
    }
    return new NpmBlob(file, contentType, name, sha1hash);
  }

  // ==

  private class ParsingPackageRootIterator
      implements PackageRootIterator
  {
    private final String repositoryId;

    private final JsonParser parser;

    private PackageRoot nextPackageRoot;

    private ParsingPackageRootIterator(final String repositoryId, final JsonParser parser) {
      this.repositoryId = repositoryId;
      this.parser = parser;
      nextPackageRoot = getNext();
    }

    @Override
    public void close() throws IOException {
      parser.close();
    }

    @Override
    public boolean hasNext() {
      final boolean hasNext = nextPackageRoot != null;
      if (!hasNext) {
        try {
          close();
        }
        catch (IOException e) {
          throw Throwables.propagate(e);
        }
      }
      return hasNext;
    }

    @Override
    public PackageRoot next() {
      final PackageRoot next = nextPackageRoot;
      nextPackageRoot = getNext();
      return next;
    }

    private PackageRoot getNext() {
      try {
        while (parser.nextToken() != null) {
          if (parser.getCurrentToken() != JsonToken.FIELD_NAME) {
            continue;
          }
          // we are at field name, skip any field starting with underscores
          if (parser.getCurrentName().startsWith("_")) {
            continue; // skip it
          }
          return parsePackageRoot(repositoryId, parser);
        }
        return null;
      }
      catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Remove unsupported");
    }
  }

  // ==

  /**
   * A content locator that streams the potentially huge registry root JSON document out of all of the package
   * documents.
   */
  private static class RegistryRootContentLocator
      extends AbstractContentLocator
      implements Iterator<ByteSource>
  {
    private final MetadataParser metadataParser;

    private final PackageRootIterator packageRootIterator;

    private boolean first;

    protected RegistryRootContentLocator(final MetadataParser metadataParser,
                                         final PackageRootIterator packageRootIterator)
    {
      super(NpmRepository.JSON_MIME_TYPE, false, ContentLocator.UNKNOWN_LENGTH);
      this.metadataParser = metadataParser;
      this.packageRootIterator = packageRootIterator;
      this.first = true;
    }

    @Override
    public InputStream getContent() throws IOException {
      return ByteSource.concat(this).openStream();
    }

    @Override
    public boolean hasNext() {
      return first || packageRootIterator.hasNext();
    }

    @Override
    public ByteSource next() {
      try {
        final List<ByteSource> sources = Lists.newArrayList();
        if (first) {
          first = false;
          sources.add(ByteSource.wrap("{".getBytes(Charsets.UTF_8)));
        }
        if (packageRootIterator.hasNext()) {
          final PackageRoot packageRoot = packageRootIterator.next();
          sources.add(ByteSource.wrap(("\"" + packageRoot.getName() + "\":").getBytes(Charsets.UTF_8)));
          sources.add(ByteSource.wrap(metadataParser.produceShrinkedPackageRoot(packageRoot).getByteArray()));
        }
        if (!packageRootIterator.hasNext()) {
          sources.add(ByteSource.wrap("}".getBytes(Charsets.UTF_8)));
        }
        else {
          sources.add(ByteSource.wrap(",".getBytes(Charsets.UTF_8)));
        }
        return ByteSource.concat(sources);
      }
      catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Remove not supported");
    }
  }
}
