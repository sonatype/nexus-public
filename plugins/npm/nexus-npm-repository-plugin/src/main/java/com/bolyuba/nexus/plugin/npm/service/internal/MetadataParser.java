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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import org.sonatype.security.SecuritySystem;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserNotFoundException;
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
import org.apache.shiro.subject.Subject;

import static com.fasterxml.jackson.core.JsonToken.END_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.END_OBJECT;
import static com.fasterxml.jackson.core.JsonToken.FIELD_NAME;
import static com.fasterxml.jackson.core.JsonToken.START_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.START_OBJECT;
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
  private static final String VERSIONS_KEY = "versions";

  private static final String ATTACHMENTS = "_attachments";

  private static final String MAINTAINERS_KEY = "maintainers";

  private static final String NPM_USER = "_npmUser";

  private static final String NAME = "name";

  private final File temporaryDirectory;

  private final SecuritySystem securitySystem;

  private final ObjectMapper objectMapper;

  @Inject
  public MetadataParser(final ApplicationDirectories applicationDirectories,
                        final SecuritySystem securitySystem) {
    this(applicationDirectories.getTemporaryDirectory(), securitySystem);
  }

  @VisibleForTesting
  public MetadataParser(final File temporaryDirectory,
                        final SecuritySystem securitySystem) {
    this.temporaryDirectory = checkNotNull(temporaryDirectory);
    this.securitySystem = checkNotNull(securitySystem);
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
    return parsePackageRoot(repositoryId, contentLocator, false);
  }

  public PackageRoot parsePackageRoot(final String repositoryId,
                                      final ContentLocator contentLocator,
                                      final boolean abbreviateMetadata) throws IOException
  {
    checkNotNull(repositoryId);
    checkNotNull(contentLocator);
    checkArgument(NpmRepository.JSON_MIME_TYPE.equals(contentLocator.getMimeType()), "JSON is expected inout!");
    try (final JsonParser parser = objectMapper.getFactory().createParser(contentLocator.getContent())) {
      final PackageRoot packageRoot = parsePackageRoot(repositoryId, parser, abbreviateMetadata);
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
    return new StringContentLocator(jsonString, NpmRepository.JSON_MIME_TYPE, root.getModified());
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

  private PackageRoot parsePackageRoot(final String repositoryId,
                                       final JsonParser parser,
                                       final boolean abbreviateMetadata) throws IOException
  {
    final Map<String, Object> raw = Maps.newHashMap();
    final Map<String, NpmBlob> attachments = Maps.newHashMap();
    final String currentUserId = getCurrentUserId();
    checkArgument(parser.nextToken() == JsonToken.START_OBJECT, "Unexpected input %s, expected %s",
        parser.getCurrentToken(), JsonToken.START_OBJECT);
    parser.nextToken();
    while (!END_OBJECT.equals(parser.getCurrentToken())) {
      final String fieldName = parseFieldName(parser);
      if (ATTACHMENTS.equals(fieldName)) {
        parsePackageAttachments(parser, attachments);
      } else if (VERSIONS_KEY.equals(fieldName)) {
        raw.put(fieldName, parseVersions(parser, currentUserId));
      } else if (MAINTAINERS_KEY.equals(fieldName)) {
        raw.put(fieldName, parseMaintainers(parser, currentUserId));
      } else if (NPM_USER.equals(fieldName)) {
        raw.put(fieldName, parseMaintainer(parser, currentUserId));
      } else {
        raw.put(fieldName, parseValue(parser));
      }
    }
    final PackageRoot result = new PackageRoot(repositoryId, raw);
    result.setAbbreviated(abbreviateMetadata);
    result.setModified(System.currentTimeMillis());
    if (!attachments.isEmpty()) {
      result.getAttachments().putAll(attachments);
    }
    return result;
  }

  @Nullable
  private String getCurrentUserId() {
    Subject subject = securitySystem.getSubject();
    if(subject == null) {
      return null;
    }

    try {
      User user = securitySystem.getUser(subject.getPrincipal().toString());
      return user.getUserId();
    }
    catch (UserNotFoundException e) { // NOSONAR
      log.debug("Unable to find current user");
      return null;
    }
  }

  private String parseFieldName(final JsonParser parser) throws IOException {
    assert parser.getCurrentToken().equals(FIELD_NAME);
    String currentName = parser.getCurrentName();
    parser.nextToken();
    return currentName;
  }

  private Object parseValue(final JsonParser parser)
      throws IOException
  {
    switch (parser.getCurrentToken()) {
      case START_OBJECT:
        return parseObject(parser);
      case START_ARRAY:
        return parseArray(parser);
      case VALUE_NULL: {
        parser.nextToken();
        return null;
      }
      case VALUE_FALSE: {
        parser.nextToken();
        return Boolean.FALSE;
      }
      case VALUE_TRUE: {
        parser.nextToken();
        return Boolean.TRUE;
      }
      case VALUE_NUMBER_INT: {
        BigInteger value = parser.getBigIntegerValue();
        parser.nextToken();
        return value;
      }
      case VALUE_NUMBER_FLOAT: {
        double value = parser.getValueAsDouble();
        parser.nextToken();
        return value;
      }
      case VALUE_STRING: {
        String value = parser.getValueAsString();
        parser.nextToken();
        return value;
      }
      default: {
        throw new IllegalArgumentException("Unexpected token: " + parser.getCurrentToken());
      }
    }
  }

  private Object parseArray(final JsonParser parser) throws IOException {
    Object value = parser.readValueAs(new TypeReference<List<Object>>() { });
    parser.nextToken();
    return value;
  }

  private Object parseObject(final JsonParser parser) throws IOException {
    Object value = parser.readValueAs(new TypeReference<Map<String, Object>>() { });
    parser.nextToken();
    return value;
  }

  /**
   * Parses the versions object in JSON
   */
  private Object parseVersions(final JsonParser parser,
                               @Nullable final String currentUserId) throws IOException {
    parser.nextToken();
    Map<String, Object> versions = new LinkedHashMap<>();
    while (!END_OBJECT.equals(parser.getCurrentToken())) {
      String name = parseFieldName(parser);
      if(!START_OBJECT.equals(parser.getCurrentToken())) {
        versions.put(name, parseValue(parser));
      } else {
        Map<String, Object> attachment = parseVersion(parser, currentUserId);
        versions.put(name, attachment);
      }
    }
    parser.nextToken();
    return versions;
  }

  /**
   * Parses the versions object and replaces maintainer and npmUser
   * name elements with the currently logged in userId
   */
  private Map<String, Object> parseVersion(final JsonParser parser,
                                           @Nullable final String currentUserId) throws IOException {
    parser.nextToken();
    Map<String, Object> version = new LinkedHashMap<>();
    while (!END_OBJECT.equals(parser.getCurrentToken())) {
      String name = parseFieldName(parser);
      if (MAINTAINERS_KEY.equals(name)) {
        version.put(name, parseMaintainers(parser, currentUserId));
      } else if (NPM_USER.equals(name)) {
        version.put(name, parseMaintainer(parser, currentUserId));
      } else {
        version.put(name, parseValue(parser));
      }
    }
    parser.nextToken();
    return version;
  }

  /**
   * Parses the maintainers objects in the JSON
   */
  private List<Object> parseMaintainers(final JsonParser parser, @Nullable final String currentUserId) throws IOException {
    if(START_ARRAY.equals(parser.getCurrentToken())) {
      return parseMaintainersAsArray(parser, currentUserId);
    } else {
      List<Object> entries = new ArrayList<>();
      entries.add(parseStringMaintainer(parser, currentUserId));
      return entries;
    }
  }

  private Object parseStringMaintainer(final JsonParser parser, final String currentUserId) throws IOException {
    if (currentUserId != null && !currentUserId.isEmpty()) {
      parser.nextToken();
      return currentUserId;
    } else {
      return parseValue(parser);
    }
  }

  private List<Object> parseMaintainersAsArray(final JsonParser parser,
                                               @Nullable final String currentUserId) throws IOException
  {
    List<Object> entries = new ArrayList<>();
    parser.nextToken();
    while (!END_ARRAY.equals(parser.getCurrentToken())) {
      if(START_OBJECT.equals(parser.getCurrentToken())) {
        entries.add(parseMaintainer(parser, currentUserId));
      } else {
        entries.add(parseStringMaintainer(parser, currentUserId));
      }
    }
    parser.nextToken();
    return entries;
  }

  /**
   * Parses a maintainer object and overwrites the name element
   * with the currently logged in user (i.e. publisher)
   */
  private Map<String, Object> parseMaintainer(final JsonParser parser, @Nullable final String currentUserId) throws IOException {
    parser.nextToken();
    Map<String, Object> entries = new LinkedHashMap<>();
    while (!END_OBJECT.equals(parser.getCurrentToken())) {
      String key = parseFieldName(parser);
      if (currentUserId != null && !currentUserId.isEmpty() && NAME.equals(key)) {
        entries.put(key, currentUserId);
        parser.nextToken();
      }
      else {
        entries.put(key, parseValue(parser));
      }
    }
    parser.nextToken();
    return entries;
  }

  @VisibleForTesting
  void parsePackageAttachments(final JsonParser parser,
                               final Map<String, NpmBlob> attachments) throws IOException
  {
    checkArgument(parser.getCurrentToken() == JsonToken.START_OBJECT, "Unexpected input %s, expected %s",
        parser.getCurrentToken(), JsonToken.START_OBJECT);
    while (parser.nextToken() == JsonToken.FIELD_NAME) {
      final NpmBlob attachment = parsePackageAttachment(parser);
      if (attachment != null) {
        attachments.put(attachment.getName(), attachment);
      }
    }
    parser.nextToken();
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
          return parsePackageRoot(repositoryId, parser, false);
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
