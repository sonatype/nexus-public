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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import org.junit.Test;

import static com.fasterxml.jackson.databind.SerializationFeature.FLUSH_AFTER_WRITE_VALUE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class StreamingObjectMapperTest
{
  private UntypedObjectDeserializerSerializer serializer;

  private StreamingObjectMapper underTest = new StreamingObjectMapper()
  {
    @Override
    protected UntypedObjectDeserializerSerializer untypedObjectDeserializer(final JsonGenerator generator) {
      return serializer = spy(new UntypedObjectDeserializerSerializer(generator));
    }
  };

  @Test
  public void should_Write_Exactly_What_Was_Read() throws IOException {
    String json = "{}";
    ByteArrayInputStream input = new ByteArrayInputStream(json.getBytes());
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    underTest.readAndWrite(input, output);

    assertThat(json, equalTo(new String(output.toByteArray())));

    json = "{\"_id\":\"simple\",\"name\":\"simple\",\"description\":\"simplestuff\"}";
    input = new ByteArrayInputStream(json.getBytes());
    output = new ByteArrayOutputStream();
    underTest.readAndWrite(input, output);

    assertThat(json, equalTo(new String(output.toByteArray())));
  }

  @Test
  public void should_Write_MinimizedJson_Of_What_Was_Read() throws IOException {
    String prettyJson =
        "{\n\"_id\": \"simple\",\n\"name\": \"simple\",\n\"description\": \"simplestuff\"}";

    ByteArrayInputStream input = new ByteArrayInputStream(prettyJson.getBytes());
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    underTest.readAndWrite(input, output);

    String minifiedJson = prettyJson.replaceAll("\n", "").replace(" ", "");
    assertThat(minifiedJson, equalTo(new String(output.toByteArray())));

    verify(serializer, times(3 /* entries in map */)).deserialize(any(), any());
  }

  @Test
  public void testEdgeCases() throws IOException {
    String prettyJson =
        "{\n\"versions\": [\"simple\"],\n\"name\": null,\n\"description\": {\"description\":\"simplestuff\", \"null\" : null}}";

    ByteArrayInputStream input = new ByteArrayInputStream(prettyJson.getBytes());
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    underTest.readAndWrite(input, output);

    verify(serializer, times(5 /* entries in map */)).deserialize(any(), any());
  }

  @Test
  public void should_Configure_SerializationFeature() {
    assertTrue(underTest.isEnabled(FLUSH_AFTER_WRITE_VALUE));

    underTest.configure(FLUSH_AFTER_WRITE_VALUE, false);

    assertFalse(underTest.isEnabled(FLUSH_AFTER_WRITE_VALUE));
  }

  @Test
  public void serializerProvider_IsReusedWithinSingleRequest() throws IOException {
    // The active ThreadLocal cache must be populated during readAndWrite and cleared after it.
    // This guards against the ThreadLocal-leak regression where pooled threads accumulate stale
    // provider references because onReadAndWriteComplete() was not called (or didn't clear).
    CachingStreamingObjectMapper mapper = new CachingStreamingObjectMapper();
    String json = "{\"a\":\"1\",\"b\":\"2\",\"c\":\"3\"}";

    // Track the provider observed mid-call (set by onSerializerProviderCreated) and compare
    // against what getCachedSerializerProvider() returns after completion (must be null).
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    mapper.readAndWrite(new ByteArrayInputStream(json.getBytes()), output);
    assertThat(new String(output.toByteArray()), equalTo(json));

    // Verify a provider was created during the call.
    DefaultSerializerProvider provider = mapper.getLastCreatedProvider();
    assertThat("Provider must have been created during readAndWrite", provider, notNullValue());

    // The active cache MUST be null after readAndWrite: onReadAndWriteComplete() cleared it.
    // If this assertion fails, it means ThreadLocals are leaking on pooled threads.
    assertThat("Active cache must be cleared after readAndWrite (ThreadLocal-leak regression check)",
        mapper.getActiveProvider(), nullValue());

    // A second call must allocate a fresh provider (not re-use the stale one from before),
    // since the cache was cleared. The new provider may be the same class but must go through
    // the creation hook again.
    output = new ByteArrayOutputStream();
    mapper.readAndWrite(new ByteArrayInputStream(json.getBytes()), output);
    assertThat(new String(output.toByteArray()), equalTo(json));

    DefaultSerializerProvider providerSecondCall = mapper.getLastCreatedProvider();
    assertThat("Second call must have created a provider", providerSecondCall, notNullValue());
    // After the second call the active cache must again be cleared.
    assertThat("Active cache must be cleared after second readAndWrite",
        mapper.getActiveProvider(), nullValue());
  }

  @Test
  public void serializerProvider_IsDifferentAcrossThreads() throws Exception {
    // Each thread must get its own DefaultSerializerProvider$Impl — no cross-thread sharing.
    int threads = 8;
    String json = "{\"key\":\"value\"}";
    CachingStreamingObjectMapper mapper = new CachingStreamingObjectMapper();
    CyclicBarrier barrier = new CyclicBarrier(threads);
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    List<Future<DefaultSerializerProvider>> futures = new ArrayList<>();

    try {
      for (int i = 0; i < threads; i++) {
        futures.add(pool.submit(() -> {
          barrier.await();
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          mapper.readAndWrite(new ByteArrayInputStream(json.getBytes()), out);
          assertThat(new String(out.toByteArray()), equalTo(json));
          return mapper.getLastCreatedProvider();
        }));
      }

      List<DefaultSerializerProvider> providers = new ArrayList<>();
      for (Future<DefaultSerializerProvider> f : futures) {
        DefaultSerializerProvider p = f.get();
        if (p != null) {
          providers.add(p);
        }
      }

      // Every thread must have its own distinct provider instance
      for (int i = 0; i < providers.size(); i++) {
        for (int j = i + 1; j < providers.size(); j++) {
          assertThat(
              "Threads must not share a DefaultSerializerProvider$Impl",
              providers.get(i),
              not(sameInstance(providers.get(j))));
        }
      }
    }
    finally {
      pool.shutdown();
    }
  }

  /**
   * Test subclass that implements the full cache lifecycle (populate + clear) to verify
   * both within-call reuse and post-call cleanup (ThreadLocal-leak regression guard).
   */
  private static class CachingStreamingObjectMapper
      extends StreamingObjectMapper
  {
    private final ThreadLocal<DefaultSerializerProvider> activeCache = new ThreadLocal<>();

    private final ThreadLocal<DefaultSerializerProvider> lastCreatedProvider = new ThreadLocal<>();

    @Override
    protected void onSerializerProviderCreated(final DefaultSerializerProvider provider) {
      activeCache.set(provider);
      lastCreatedProvider.set(provider);
    }

    @Override
    protected DefaultSerializerProvider threadLocalSerializerProvider() {
      return activeCache.get();
    }

    @Override
    protected void onReadAndWriteComplete() {
      activeCache.remove();
    }

    /** Returns the active (within-call) cached provider; null after readAndWrite completes. */
    DefaultSerializerProvider getActiveProvider() {
      return activeCache.get();
    }

    /** Returns the provider created during the most recent readAndWrite call. */
    DefaultSerializerProvider getLastCreatedProvider() {
      return lastCreatedProvider.get();
    }
  }
}
