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
package org.sonatype.nexus.repository.json;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.FormatSchema;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.async.NonBlockingInputFeeder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.RequestPayload;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper class to be able to decorate an existing {@link JsonParser} with new functionality and state. This decorator
 * prevents other decorators from having to implement all methods.
 *
 * @since 3.16
 */
public class JsonParserDecorator
    extends JsonParser
{
  private JsonParser jsonParser;

  public JsonParserDecorator(final JsonParser jsonParser) {
    this.jsonParser = checkNotNull(jsonParser);
  }

  @Override
  public ObjectCodec getCodec() {
    return jsonParser.getCodec();
  }

  @Override
  public void setCodec(final ObjectCodec c) {
    jsonParser.setCodec(c);
  }

  @Override
  public Object getInputSource() {
    return jsonParser.getInputSource();
  }

  @Override
  public Object getCurrentValue() {
    return jsonParser.getCurrentValue();
  }

  @Override
  public void setCurrentValue(final Object v) {
    jsonParser.setCurrentValue(v);
  }

  @Override
  public void setRequestPayloadOnError(final RequestPayload payload) {
    jsonParser.setRequestPayloadOnError(payload);
  }

  @Override
  public void setRequestPayloadOnError(final byte[] payload, final String charset) {
    jsonParser.setRequestPayloadOnError(payload, charset);
  }

  @Override
  public void setRequestPayloadOnError(final String payload) {
    jsonParser.setRequestPayloadOnError(payload);
  }

  @Override
  public void setSchema(final FormatSchema schema) {
    jsonParser.setSchema(schema);
  }

  @Override
  public FormatSchema getSchema() {
    return jsonParser.getSchema();
  }

  @Override
  public boolean canUseSchema(final FormatSchema schema) {
    return jsonParser.canUseSchema(schema);
  }

  @Override
  public boolean requiresCustomCodec() {
    return jsonParser.requiresCustomCodec();
  }

  @Override
  public boolean canParseAsync() {
    return jsonParser.canParseAsync();
  }

  @Override
  public NonBlockingInputFeeder getNonBlockingInputFeeder() {
    return jsonParser.getNonBlockingInputFeeder();
  }

  @Override
  public Version version() {
    return jsonParser.version();
  }

  @Override
  public void close() throws IOException {
    jsonParser.close();
  }

  @Override
  public boolean isClosed() {
    return jsonParser.isClosed();
  }

  @Override
  public JsonStreamContext getParsingContext() {
    return jsonParser.getParsingContext();
  }

  @Override
  public JsonLocation getTokenLocation() {
    return jsonParser.getTokenLocation();
  }

  @Override
  public JsonLocation getCurrentLocation() {
    return jsonParser.getCurrentLocation();
  }

  @Override
  public int releaseBuffered(final OutputStream out) throws IOException {
    return jsonParser.releaseBuffered(out);
  }

  @Override
  public int releaseBuffered(final Writer w) throws IOException {
    return jsonParser.releaseBuffered(w);
  }

  @Override
  public JsonParser enable(final Feature f) {
    return jsonParser.enable(f);
  }

  @Override
  public JsonParser disable(final Feature f) {
    return jsonParser.disable(f);
  }

  @Override
  public JsonParser configure(final Feature f, final boolean state) {
    return jsonParser.configure(f, state);
  }

  @Override
  public boolean isEnabled(final Feature f) {
    return jsonParser.isEnabled(f);
  }

  @Override
  public int getFeatureMask() {
    return jsonParser.getFeatureMask();
  }

  @Override
  @Deprecated
  public JsonParser setFeatureMask(final int mask) {
    return jsonParser.setFeatureMask(mask);
  }

  @Override
  public JsonParser overrideStdFeatures(final int values, final int mask) {
    return jsonParser.overrideStdFeatures(values, mask);
  }

  @Override
  public int getFormatFeatures() {
    return jsonParser.getFormatFeatures();
  }

  @Override
  public JsonParser overrideFormatFeatures(final int values, final int mask) {
    return jsonParser.overrideFormatFeatures(values, mask);
  }

  @Override
  public JsonToken nextToken() throws IOException {
    return jsonParser.nextToken();
  }

  @Override
  public JsonToken nextValue() throws IOException {
    return jsonParser.nextValue();
  }

  @Override
  public boolean nextFieldName(final SerializableString str) throws IOException {
    return jsonParser.nextFieldName(str);
  }

  @Override
  public String nextFieldName() throws IOException {
    return jsonParser.nextFieldName();
  }

  @Override
  public String nextTextValue() throws IOException {
    return jsonParser.nextTextValue();
  }

  @Override
  public int nextIntValue(final int defaultValue) throws IOException {
    return jsonParser.nextIntValue(defaultValue);
  }

  @Override
  public long nextLongValue(final long defaultValue) throws IOException {
    return jsonParser.nextLongValue(defaultValue);
  }

  @Override
  public Boolean nextBooleanValue() throws IOException {
    return jsonParser.nextBooleanValue();
  }

  @Override
  public JsonParser skipChildren() throws IOException {
    return jsonParser.skipChildren();
  }

  @Override
  public void finishToken() throws IOException {
    jsonParser.finishToken();
  }

  @Override
  public JsonToken currentToken() {
    return jsonParser.currentToken();
  }

  @Override
  public int currentTokenId() {
    return jsonParser.currentTokenId();
  }

  @Override
  public JsonToken getCurrentToken() {
    return jsonParser.getCurrentToken();
  }

  @Override
  public int getCurrentTokenId() {
    return jsonParser.getCurrentTokenId();
  }

  @Override
  public boolean hasCurrentToken() {
    return jsonParser.hasCurrentToken();
  }

  @Override
  public boolean hasTokenId(final int id) {
    return jsonParser.hasTokenId(id);
  }

  @Override
  public boolean hasToken(final JsonToken t) {
    return jsonParser.hasToken(t);
  }

  @Override
  public boolean isExpectedStartArrayToken() {
    return jsonParser.isExpectedStartArrayToken();
  }

  @Override
  public boolean isExpectedStartObjectToken() {
    return jsonParser.isExpectedStartObjectToken();
  }

  @Override
  public boolean isNaN() throws IOException {
    return jsonParser.isNaN();
  }

  @Override
  public void clearCurrentToken() {
    jsonParser.clearCurrentToken();
  }

  @Override
  public JsonToken getLastClearedToken() {
    return jsonParser.getLastClearedToken();
  }

  @Override
  public void overrideCurrentName(final String name) {
    jsonParser.overrideCurrentName(name);
  }

  @Override
  public String getCurrentName() throws IOException {
    return jsonParser.getCurrentName();
  }

  @Override
  public String getText() throws IOException {
    return jsonParser.getText();
  }

  @Override
  public int getText(final Writer writer) throws IOException {
    return jsonParser.getText(writer);
  }

  @Override
  public char[] getTextCharacters() throws IOException {
    return jsonParser.getTextCharacters();
  }

  @Override
  public int getTextLength() throws IOException {
    return jsonParser.getTextLength();
  }

  @Override
  public int getTextOffset() throws IOException {
    return jsonParser.getTextOffset();
  }

  @Override
  public boolean hasTextCharacters() {
    return jsonParser.hasTextCharacters();
  }

  @Override
  public Number getNumberValue() throws IOException {
    return jsonParser.getNumberValue();
  }

  @Override
  public NumberType getNumberType() throws IOException {
    return jsonParser.getNumberType();
  }

  @Override
  public byte getByteValue() throws IOException {
    return jsonParser.getByteValue();
  }

  @Override
  public short getShortValue() throws IOException {
    return jsonParser.getShortValue();
  }

  @Override
  public int getIntValue() throws IOException {
    return jsonParser.getIntValue();
  }

  @Override
  public long getLongValue() throws IOException {
    return jsonParser.getLongValue();
  }

  @Override
  public BigInteger getBigIntegerValue() throws IOException {
    return jsonParser.getBigIntegerValue();
  }

  @Override
  public float getFloatValue() throws IOException {
    return jsonParser.getFloatValue();
  }

  @Override
  public double getDoubleValue() throws IOException {
    return jsonParser.getDoubleValue();
  }

  @Override
  public BigDecimal getDecimalValue() throws IOException {
    return jsonParser.getDecimalValue();
  }

  @Override
  public boolean getBooleanValue() throws IOException {
    return jsonParser.getBooleanValue();
  }

  @Override
  public Object getEmbeddedObject() throws IOException {
    return jsonParser.getEmbeddedObject();
  }

  @Override
  public byte[] getBinaryValue(final Base64Variant bv) throws IOException {
    return jsonParser.getBinaryValue(bv);
  }

  @Override
  public byte[] getBinaryValue() throws IOException {
    return jsonParser.getBinaryValue();
  }

  @Override
  public int readBinaryValue(final OutputStream out) throws IOException {
    return jsonParser.readBinaryValue(out);
  }

  @Override
  public int readBinaryValue(final Base64Variant bv, final OutputStream out) throws IOException {
    return jsonParser.readBinaryValue(bv, out);
  }

  @Override
  public int getValueAsInt() throws IOException {
    return jsonParser.getValueAsInt();
  }

  @Override
  public int getValueAsInt(final int def) throws IOException {
    return jsonParser.getValueAsInt(def);
  }

  @Override
  public long getValueAsLong() throws IOException {
    return jsonParser.getValueAsLong();
  }

  @Override
  public long getValueAsLong(final long def) throws IOException {
    return jsonParser.getValueAsLong(def);
  }

  @Override
  public double getValueAsDouble() throws IOException {
    return jsonParser.getValueAsDouble();
  }

  @Override
  public double getValueAsDouble(final double def) throws IOException {
    return jsonParser.getValueAsDouble(def);
  }

  @Override
  public boolean getValueAsBoolean() throws IOException {
    return jsonParser.getValueAsBoolean();
  }

  @Override
  public boolean getValueAsBoolean(final boolean def) throws IOException {
    return jsonParser.getValueAsBoolean(def);
  }

  @Override
  public String getValueAsString() throws IOException {
    return jsonParser.getValueAsString();
  }

  @Override
  public String getValueAsString(final String def) throws IOException {
    return jsonParser.getValueAsString(def);
  }

  @Override
  public boolean canReadObjectId() {
    return jsonParser.canReadObjectId();
  }

  @Override
  public boolean canReadTypeId() {
    return jsonParser.canReadTypeId();
  }

  @Override
  public Object getObjectId() throws IOException {
    return jsonParser.getObjectId();
  }

  @Override
  public Object getTypeId() throws IOException {
    return jsonParser.getTypeId();
  }

  @Override
  public <T> T readValueAs(final Class<T> valueType) throws IOException {
    return jsonParser.readValueAs(valueType);
  }

  @Override
  public <T> T readValueAs(final TypeReference<?> valueTypeRef) throws IOException {
    return jsonParser.readValueAs(valueTypeRef);
  }

  @Override
  public <T> Iterator<T> readValuesAs(final Class<T> valueType) throws IOException {
    return jsonParser.readValuesAs(valueType);
  }

  @Override
  public <T> Iterator<T> readValuesAs(final TypeReference<?> valueTypeRef) throws IOException {
    return jsonParser.readValuesAs(valueTypeRef);
  }

  @Override
  public <T extends TreeNode> T readValueAsTree() throws IOException {
    return jsonParser.readValueAsTree();
  }
}
