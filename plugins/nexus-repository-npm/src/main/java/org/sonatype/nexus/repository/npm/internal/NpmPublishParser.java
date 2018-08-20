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
package org.sonatype.nexus.repository.npm.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.thread.io.StreamCopier;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import static com.fasterxml.jackson.core.JsonToken.*;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;

/**
 * Parses incoming "npm publish" and "npm unpublish" JSON using the streaming API so that attachments are not read into
 * memory as part of the parse operation. The implementation is very similar to a traditional recursive-descent parser.
 *
 * All records except for the attachment data are read into a {@code NestedAttributesMap} as usual. The {@code data}
 * field is replaced with a string that can be used to look up the associated temporary blob.
 *
 * Note that during the parse operation all temporary blobs are owned by this class and will be disposed of in the event
 * that something goes wrong. Once the parse operation has succeeded, the ownership of the temp blobs is assigned to the
 * returned result.
 *
 * Do not reuse instances of this class.
 *
 * @since 3.4
 */
public class NpmPublishParser
    extends ComponentSupport
{
  private static final String ATTACHMENTS_KEY = "_attachments";

  private static final String ATTACHMENTS_DATA_KEY = "data";

  private static final String MAINTAINERS_KEY = "maintainers";

  private static final String VERSIONS_KEY = "versions";

  private static final String NPM_USER = "_npmUser";

  public static final String NAME = "name";

  private static final String DIST_TAGS = "dist-tags";

  private final JsonParser jsonParser;

  private final StorageFacet storageFacet;

  private final List<HashAlgorithm> hashAlgorithms;

  private final Map<String, TempBlob> tempBlobs = new LinkedHashMap<>();

  /**
   * @param jsonParser     json parser containing the content
   * @param storageFacet   storage facet for creating temp blobs (if needed)
   * @param hashAlgorithms hash algorithms to apply to the temp blobs (if any are created)
   */
  public NpmPublishParser(final JsonParser jsonParser,
                          final StorageFacet storageFacet,
                          final List<HashAlgorithm> hashAlgorithms)
  {
    this.jsonParser = checkNotNull(jsonParser);
    this.storageFacet = checkNotNull(storageFacet);
    this.hashAlgorithms = checkNotNull(hashAlgorithms);
  }

  /**
   * Parses the {@code JsonParser}'s content into a {@code NpmPublishOrDeleteRequest}. Temp blobs will be created if
   * necessary using the {@code StorageFacet} and with the provided {@code HashAlgorithm}s.
   *
   * This method will manage the temp blobs for the lifetime of the parse operation (and will dispose accordingly in
   * event of a failure). After returning, the parsed result must be managed carefully so as not to leak temp blobs.
   * @param currentUserId currently logged in userId
   */
  public NpmPublishRequest parse(@Nullable final String currentUserId) throws IOException {
    try {
      NestedAttributesMap packageRoot = parsePackageRoot();

      if(currentUserId != null && !currentUserId.isEmpty()) {
        updateMaintainer(packageRoot, currentUserId);
      }
      return new NpmPublishRequest(packageRoot, tempBlobs);
    }
    catch (Throwable t) { //NOSONAR
      for (TempBlob tempBlob : tempBlobs.values()) {
        tempBlob.close();
      }
      throw t;
    }
  }

  private void updateMaintainer(final NestedAttributesMap packageRoot, final String currentUserId) {
    String distVersion = getDistTagVersion(packageRoot);

    if (distVersion != null && packageRoot.contains(VERSIONS_KEY)) {
      NestedAttributesMap versionsMap = packageRoot.child(VERSIONS_KEY);

      if (versionsMap.contains(distVersion)) {
        NestedAttributesMap versionToUpdate = versionsMap.child(distVersion);

        String npmUser = getNpmUser(versionToUpdate);
        if (isUserTokenBasedPublish(currentUserId, npmUser)) {
          updateNpmUser(packageRoot, currentUserId);
          updateMaintainerList(packageRoot, currentUserId);

          updateNpmUser(versionToUpdate, currentUserId);
          updateMaintainerList(versionToUpdate, currentUserId);
        }
      }
    } else {
      log.warn("Version(s) attribute not found in package root");
    }
  }

  private boolean isUserTokenBasedPublish(final String currentUserId, final String npmUser) {
    return npmUser != null && !currentUserId.equals(npmUser);
  }

  private String getNpmUser(final NestedAttributesMap packageEntry) {
    if (packageEntry.contains(NPM_USER)) {
      NestedAttributesMap npmUser = packageEntry.child(NPM_USER);
      return npmUser.get(NAME, String.class);
    }
    return null;
  }

  private String getDistTagVersion(final NestedAttributesMap packageRoot) {
    if (packageRoot.contains(DIST_TAGS)) {
      NestedAttributesMap distTag = packageRoot.child(DIST_TAGS);
      return (String) distTag.iterator().next().getValue();
    }
    return null;
  }

  private void updateMaintainerList(final NestedAttributesMap versionToUpdate, final String currentUserId) {
    Object maintainersObject = versionToUpdate.get(MAINTAINERS_KEY);

    if (nonNull(maintainersObject)) {
      if(maintainersObject instanceof List) {
        List maintainers = (List) maintainersObject;

        Object maintainer = maintainers.get(0);
        if (maintainer instanceof Map) {
          updateMaintainerAsMap(maintainers, currentUserId);
        }
        else if (maintainer instanceof String) {
          updateMaintainerAsString(maintainers, currentUserId);
        }
      }
      else if (maintainersObject instanceof String) {
        versionToUpdate.set(MAINTAINERS_KEY, currentUserId);
      }
    }
  }

  private void updateMaintainerAsMap(final List<Map<String, String>> maintainers, final String currentUserId) {
    Map<String, String> latestEntry = maintainers.get(0);
    latestEntry.put(NAME, currentUserId);
  }

  private void updateMaintainerAsString(final List<String> maintainers, String currentId) {
    maintainers.set(0, currentId);
  }

  private void updateNpmUser(final NestedAttributesMap packageEntry, final String currentUserId) {
    if (packageEntry.contains(NPM_USER)) {
      NestedAttributesMap npmUser = packageEntry.child(NPM_USER);
      npmUser.set(NAME, currentUserId);
    }
  }

  /**
   * Parses the package root, which is the starting point of the parsing process for the incoming request.
   */
  private NestedAttributesMap parsePackageRoot() throws IOException {
    Map<String, Object> backing = new LinkedHashMap<>();
    consumeToken();
    consumeToken();
    while (!END_OBJECT.equals(currentToken())) {
      String key = parseFieldName();
      final Object value;
      if (ATTACHMENTS_KEY.equals(key)) {
        value = parseAttachments();
      } else {
        value = parseValue();
      }
      backing.put(key, value);
    }
    return new NestedAttributesMap(String.valueOf(backing.get(NpmMetadataUtils.NAME)), backing);
  }

  /**
   * Parses a JSON value into the appropriate Java type, delegating based on the type of the token being parsed.
   */
  @Nullable
  private Object parseValue() throws IOException {
    switch (currentToken()) {
      case START_OBJECT:
        return parseObject();
      case START_ARRAY:
        return parseArray();
      case VALUE_STRING:
        return parseString();
      case VALUE_NUMBER_INT:
        return parseIntegerValue();
      case VALUE_NUMBER_FLOAT:
        return parseFloatValue();
      case VALUE_TRUE:
        return parseBooleanTrueValue();
      case VALUE_FALSE:
        return parseBooleanFalseValue();
      case VALUE_NULL:
        return parseNullValue();
      default:
        throw new IllegalStateException(
            "Unexpected token " + currentToken() + " at " + jsonParser.getCurrentLocation());
    }
  }

  /**
   * Parses a field name.
   */
  private String parseFieldName() throws IOException {
    requireToken(FIELD_NAME);
    String fieldName = jsonParser.getCurrentName();
    consumeToken();
    return fieldName;
  }

  /**
   * Parses a JSON object made of zero or more entries, returning the result as a map.
   */
  private Map<String, Object> parseObject() throws IOException {
    requireToken(START_OBJECT);
    consumeToken();
    Map<String, Object> entries = new LinkedHashMap<>();
    while (!END_OBJECT.equals(currentToken())) {
      String key = parseFieldName();
      Object value = parseValue();
      entries.put(key, value);
    }
    consumeToken();
    return entries;
  }

  /**
   * Parses an array literal made up of zero or more values, returning the results as a list of (Java) objects.
   */
  private List<Object> parseArray() throws IOException {
    requireToken(START_ARRAY);
    consumeToken();
    List<Object> entries = new ArrayList<>();
    while (!END_ARRAY.equals(currentToken())) {
      entries.add(parseValue());
    }
    consumeToken();
    return entries;
  }

  /**
   * Parses a JSON string literal, consuming the token.
   */
  private String parseString() throws IOException {
    requireToken(VALUE_STRING);
    final String value = jsonParser.getValueAsString();
    consumeToken();
    return value;
  }

  /**
   * Parses a JSON integer, consuming the token.
   */
  private BigInteger parseIntegerValue() throws IOException {
    requireToken(VALUE_NUMBER_INT);
    final BigInteger value = jsonParser.getBigIntegerValue();
    consumeToken();
    return value;
  }

  /**
   * Parses a JSON float, consuming the token.
   */
  private BigDecimal parseFloatValue() throws IOException {
    requireToken(VALUE_NUMBER_FLOAT);
    final BigDecimal value = jsonParser.getDecimalValue();
    consumeToken();
    return value;
  }

  /**
   * Parses a JSON boolean true, consuming the token.
   */
  private Boolean parseBooleanTrueValue() throws IOException {
    requireToken(VALUE_TRUE);
    final Boolean value = jsonParser.getBooleanValue();
    consumeToken();
    return value;
  }

  /**
   * Parses a JSON boolean false, consuming the token.
   */
  private Boolean parseBooleanFalseValue() throws IOException {
    requireToken(VALUE_FALSE);
    final Boolean value = jsonParser.getBooleanValue();
    consumeToken();
    return value;
  }

  /**
   * Parses a JSON null value, consuming the token.
   */
  @Nullable
  private Object parseNullValue() throws IOException {
    requireToken(VALUE_NULL);
    consumeToken();
    return null;
  }

  /**
   * Parses the attachment objects in the JSON.
   */
  private Map<String, Object> parseAttachments() throws IOException {
    requireToken(START_OBJECT);
    consumeToken();
    Map<String, Object> attachments = new LinkedHashMap<>();
    while (!END_OBJECT.equals(currentToken())) {
      String name = parseFieldName();
      Map<String, Object> attachment = parseAttachment();
      attachments.put(name, attachment);
    }
    consumeToken();
    return attachments;
  }

  /**
   * Parses an attachment, including special handling for the data field which must be handled differently because of
   * the potentially large size of the field.
   */
  private Map<String, Object> parseAttachment() throws IOException {
    requireToken(START_OBJECT);
    consumeToken();
    Map<String, Object> entries = new LinkedHashMap<>();
    while (!END_OBJECT.equals(currentToken())) {
      String key = parseFieldName();
      Object value;
      if (ATTACHMENTS_DATA_KEY.equals(key)) {
        value = parseAttachmentData();
      }
      else {
        value = parseValue();
      }
      entries.put(key, value);
    }
    consumeToken();
    return entries;
  }

  /**
   * Parses the data field of an attachment in the JSON. The data is stored in a temp blob, and the blob ID is returned
   * as a string.
   */
  private String parseAttachmentData() throws IOException {
    try {
      TempBlob tempBlob = readBinaryValueIntoTempBlob();
      String id = tempBlob.getBlob().getId().toString();
      tempBlobs.put(id, tempBlob);
      return id;
    }
    catch (Exception e) {
      throw new IOException("failed to process attachment data", e);
    }
  }

  private TempBlob readBinaryValueIntoTempBlob() {
    return new StreamCopier<>(this::readBinaryValue, this::createTempBlob).read();
  }

  private void readBinaryValue(final OutputStream outputStream) {
    try {
      jsonParser.readBinaryValue(outputStream);
      consumeToken();
    }
    catch (IOException e) {
      throw new RuntimeException("Unable to read binary value", e);
    }
  }

  private TempBlob createTempBlob(final InputStream inputStream) {
    return storageFacet.createTempBlob(inputStream, hashAlgorithms);
  }

  /**
   * Requires that the current token is of a specified type.
   */
  private void requireToken(final JsonToken token) throws IOException {
    if (!token.equals(currentToken())) {
      throw new IllegalStateException(
          "Expected " + token + " but found " + currentToken() + " at " + jsonParser.getCurrentLocation());
    }
  }

  /**
   * Consumes a token from the underlying parser.
   */
  private void consumeToken() throws IOException {
    jsonParser.nextToken();
  }

  /**
   * Returns the current token type without consuming the token from the parser.
   */
  private JsonToken currentToken() {
    return jsonParser.currentToken();
  }
}
