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
package org.sonatype.nexus.internal.capability;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import javax.inject.Provider;
import javax.validation.ValidationException;
import javax.validation.Validator;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.capability.Capability;
import org.sonatype.nexus.capability.CapabilityDescriptor;
import org.sonatype.nexus.capability.CapabilityDescriptor.ValidationMode;
import org.sonatype.nexus.capability.CapabilityDescriptorRegistry;
import org.sonatype.nexus.capability.CapabilityEvent;
import org.sonatype.nexus.capability.CapabilityFactory;
import org.sonatype.nexus.capability.CapabilityFactoryRegistry;
import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.capability.CapabilityNotFoundException;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.formfields.Encrypted;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.PasswordFormField;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorage;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItem;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItemData;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.capability.CapabilityIdentity.capabilityIdentity;
import static org.sonatype.nexus.capability.CapabilityType.capabilityType;

/**
 * {@link DefaultCapabilityRegistry} UTs.
 *
 * @since capabilities 2.0
 */
public class DefaultCapabilityRegistryTest
    extends TestSupport
{

  static final CapabilityType CAPABILITY_TYPE = capabilityType("test");

  private final Random random = new Random(System.currentTimeMillis());

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Mock
  private EventManager eventManager;

  @Mock
  private CapabilityStorage capabilityStorage;

  @Mock
  private CapabilityDescriptorRegistry capabilityDescriptorRegistry;

  @Mock
  private CapabilityDescriptor capabilityDescriptor;

  private DefaultCapabilityRegistry underTest;

  private ArgumentCaptor<CapabilityEvent> rec;

  private Map<Integer, Secret> secrets = new HashMap<>();

  @Mock
  private SecretsService secretsService;

  private MockedStatic<SecurityUtils> mockStatic;

  @Before
  public final void setUpCapabilityRegistry() throws Exception {
    mockStatic = mockStatic(SecurityUtils.class);
    Subject subject = subject("testuser");
    mockStatic.when(() -> SecurityUtils.getSubject()).thenReturn(subject);

    when(secretsService.encryptMaven(any(), any(), any())).thenAnswer(invocation -> {
      Secret secret = mock(Secret.class);
      when(secret.getId()).thenReturn("" + secrets.size());
      when(secret.decrypt()).thenReturn(invocation.getArgument(1, char[].class));
      secrets.put(secrets.size(), secret);
      return secret;
    });
    when(secretsService.from(any()))
        .thenAnswer(invocation -> secrets.get(Integer.valueOf(invocation.getArgument(0, String.class))));

    final CapabilityFactory factory = mock(CapabilityFactory.class);
    when(factory.create()).thenAnswer(new Answer<Capability>()
    {
      @Override
      public Capability answer(final InvocationOnMock invocation)
          throws Throwable
      {
        return mock(Capability.class);
      }

    });

    final CapabilityFactoryRegistry capabilityFactoryRegistry = mock(CapabilityFactoryRegistry.class);
    when(capabilityFactoryRegistry.get(CAPABILITY_TYPE)).thenReturn(factory);

    when(capabilityDescriptorRegistry.get(CAPABILITY_TYPE)).thenReturn(capabilityDescriptor);

    final ActivationConditionHandlerFactory achf = mock(ActivationConditionHandlerFactory.class);
    when(achf.create(Mockito.<DefaultCapabilityReference>any())).thenReturn(
        mock(ActivationConditionHandler.class)
    );
    final ValidityConditionHandlerFactory vchf = mock(ValidityConditionHandlerFactory.class);
    when(vchf.create(Mockito.<DefaultCapabilityReference>any())).thenReturn(
        mock(ValidityConditionHandler.class)
    );

    when(capabilityStorage.add(Mockito.<CapabilityStorageItem>any())).thenAnswer(
        new Answer<CapabilityIdentity>()
        {
          @Override
          public CapabilityIdentity answer(final InvocationOnMock invocationOnMock) throws Throwable {
            return capabilityIdentity(String.valueOf(random.nextLong()));
          }
        }
    );

    when(capabilityStorage.newStorageItem(anyInt(), any(), anyBoolean(), any(), any()))
        .thenAnswer(i -> {
          Object[] args = i.getArguments();
          final CapabilityStorageItem item = new CapabilityStorageItemData();
          item.setVersion((Integer) args[0]);
          item.setType((String) args[1]);
          item.setEnabled((Boolean) args[2]);
          item.setProperties((Map) args[4]);
          item.setNotes((String) args[3]);

          return item;
        });

    ValidatorProvider validatorProvider = mock(ValidatorProvider.class);
    when(validatorProvider.get()).thenReturn(mock(Validator.class));

    underTest = new DefaultCapabilityRegistry(
        capabilityStorage,
        capabilityFactoryRegistry,

        capabilityDescriptorRegistry,
        eventManager,
        achf,
        vchf,
        secretsService,
        validatorProvider
    );

    rec = ArgumentCaptor.forClass(CapabilityEvent.class);
  }

  @After
  public void teardown() {
    mockStatic.close();
  }

  /**
   * Create capability creates a non null reference and posts create event.
   *
   * @throws Exception unexpected
   */
  @Test
  public void create()
      throws Exception
  {
    final CapabilityReference reference = underTest.add(CAPABILITY_TYPE, true, null, null);
    assertThat(reference, is(not(nullValue())));

    verify(eventManager, atLeastOnce()).post(rec.capture());
    assertThat(rec.getValue(), is(instanceOf(CapabilityEvent.Created.class)));
    assertThat(rec.getValue().getReference(), is(equalTo(reference)));
  }

  /**
   * Remove an existent capability posts remove event.
   *
   * @throws Exception unexpected
   */
  @Test
  public void remove()
      throws Exception
  {
    final CapabilityReference reference = underTest.add(CAPABILITY_TYPE, true, null, null);
    final CapabilityReference reference1 = underTest.remove(reference.context().id());

    assertThat(reference1, is(equalTo(reference)));

    verify(eventManager, atLeastOnce()).post(rec.capture());
    assertThat(rec.getAllValues().get(0), is(instanceOf(CapabilityEvent.Created.class)));
    assertThat(rec.getAllValues().get(0).getReference(), is(equalTo(reference1)));
    assertThat(rec.getAllValues().get(1), is(instanceOf(CapabilityEvent.AfterRemove.class)));
    assertThat(rec.getAllValues().get(1).getReference(), is(equalTo(reference1)));
  }

  /**
   * Remove an inexistent capability does nothing and does not post remove event.
   *
   * @throws Exception unexpected
   */
  @Test
  public void removeInexistent()
      throws Exception
  {
    underTest.add(CAPABILITY_TYPE, true, null, null);

    thrown.expect(CapabilityNotFoundException.class);
    underTest.remove(capabilityIdentity("foo"));
  }

  /**
   * Get a created capability.
   *
   * @throws Exception unexpected
   */
  @Test
  public void get()
      throws Exception
  {
    final CapabilityReference reference = underTest.add(CAPABILITY_TYPE, true, null, null);
    final CapabilityReference reference1 = underTest.get(reference.context().id());

    assertThat(reference1, is(not(nullValue())));
  }

  /**
   * Get an inexistent capability.
   *
   * @throws Exception unexpected
   */
  @Test
  public void getInexistent()
      throws Exception
  {
    underTest.add(CAPABILITY_TYPE, true, null, null);
    final CapabilityReference reference = underTest.get(capabilityIdentity("foo"));

    assertThat(reference, is(nullValue()));
  }

  /**
   * Get all created capabilities.
   *
   * @throws Exception unexpected
   */
  @Test
  public void getAll()
      throws Exception
  {
    underTest.add(CAPABILITY_TYPE, true, null, null);
    underTest.add(CAPABILITY_TYPE, true, null, null);
    final Collection<? extends CapabilityReference> references = underTest.getAll();

    assertThat(references, hasSize(2));
  }

  private interface ValidatorProvider
      extends Provider<Validator>
  {

  }

  /**
   * On load if version did not change conversion should not be performed.
   *
   * @throws Exception unexpected
   */
  @Test
  public void load()
      throws Exception
  {
    final Map<String, String> oldProps = Maps.newHashMap();
    oldProps.put("p1", "v1");
    oldProps.put("p2", "v2");

    final CapabilityStorageItem item = new CapabilityStorageItemData();
    item.setVersion(0);
    item.setType(CAPABILITY_TYPE.toString());
    item.setEnabled(true);
    item.setProperties(oldProps);
    CapabilityIdentity fooId = capabilityIdentity("foo");
    when(capabilityStorage.getAll()).thenReturn(ImmutableMap.of(fooId, item));

    final CapabilityDescriptor descriptor = mock(CapabilityDescriptor.class);
    when(capabilityDescriptorRegistry.get(CAPABILITY_TYPE)).thenReturn(descriptor);
    when(descriptor.version()).thenReturn(0);

    underTest.load();

    verify(capabilityStorage).getAll();
    verify(descriptor).version();
    verify(descriptor).formFields();
    verify(descriptor).validate(fooId, oldProps, ValidationMode.LOAD);
    verifyNoMoreInteractions(descriptor, capabilityStorage);
  }

  /**
   * On load if version changed conversion should be performed and new properties stored.
   *
   * @throws Exception unexpected
   */
  @Test
  public void loadWhenVersionChanged()
      throws Exception
  {
    final Map<String, String> oldProps = Maps.newHashMap();
    oldProps.put("p1", "v1");
    oldProps.put("p2", "v2");

    final CapabilityStorageItem item = new CapabilityStorageItemData();
    item.setVersion(0);
    item.setType(CAPABILITY_TYPE.toString());
    item.setEnabled(true);
    item.setProperties(oldProps);

    when(capabilityStorage.getAll()).thenReturn(ImmutableMap.of(capabilityIdentity("foo"), item));

    final CapabilityDescriptor descriptor = mock(CapabilityDescriptor.class);
    when(capabilityDescriptorRegistry.get(CAPABILITY_TYPE)).thenReturn(descriptor);
    when(descriptor.version()).thenReturn(1);

    final Map<String, String> newProps = Maps.newHashMap();
    oldProps.put("p1", "v1-converted");
    oldProps.put("p3", "v3");

    when(descriptor.convert(oldProps, 0)).thenReturn(newProps);

    underTest.load();

    verify(capabilityStorage).getAll();
    verify(descriptor, atLeastOnce()).version();
    verify(descriptor).convert(oldProps, 0);
    final ArgumentCaptor<CapabilityStorageItem> captor = ArgumentCaptor.forClass(CapabilityStorageItem.class);
    verify(capabilityStorage).update(Mockito.eq(capabilityIdentity("foo")), captor.capture());
    assertThat(captor.getValue(), is(notNullValue()));

    final Map<String, String> actualNewProps = captor.getValue().getProperties();
    assertThat(newProps, is(equalTo(actualNewProps)));
  }

  /**
   * On load if version changed conversion should be performed if conversion fails load is skipped.
   *
   * @throws Exception unexpected
   */
  @Test
  public void loadWhenVersionChangedAndConversionFails()
      throws Exception
  {
    final Map<String, String> oldProps = Maps.newHashMap();
    oldProps.put("p1", "v1");
    oldProps.put("p2", "v2");

    final CapabilityStorageItem item = new CapabilityStorageItemData();
    item.setVersion(0);
    item.setType(CAPABILITY_TYPE.toString());
    item.setEnabled(true);
    item.setProperties(oldProps);

    when(capabilityStorage.getAll()).thenReturn(ImmutableMap.of(capabilityIdentity("foo"), item));

    final CapabilityDescriptor descriptor = mock(CapabilityDescriptor.class);
    when(capabilityDescriptorRegistry.get(CAPABILITY_TYPE)).thenReturn(descriptor);
    when(descriptor.version()).thenReturn(1);

    when(descriptor.convert(oldProps, 0)).thenThrow(new RuntimeException("expected"));

    underTest.load();

    verify(capabilityStorage).getAll();
    verify(descriptor, atLeastOnce()).version();
    verify(descriptor).convert(oldProps, 0);
    verify(descriptor).formFields();

    verifyNoMoreInteractions(descriptor, capabilityStorage);
  }

  /**
   * On load, if type is unknown (no descriptor for that type), the entry is skipped.
   *
   * @throws Exception unexpected
   */
  @Test
  public void loadWhenTypeIsUnknown()
      throws Exception
  {
    final CapabilityStorageItem item = new CapabilityStorageItemData();
    item.setVersion(0);
    item.setType(CAPABILITY_TYPE.toString());
    item.setEnabled(true);
    item.setProperties(null);

    when(capabilityStorage.getAll()).thenReturn(ImmutableMap.of(capabilityIdentity("foo"), item));

    when(capabilityDescriptorRegistry.get(CAPABILITY_TYPE)).thenReturn(null);

    underTest.load();

    verify(capabilityStorage).getAll();
    final Collection<DefaultCapabilityReference> references = underTest.getAll();
    assertThat(references.size(), is(0));
  }

  /**
   * Verify that value is stored encrypted when corresponding field is marked with {@link Encrypted}.
   */
  @Test
  public void createWithEncryptedProperty()
      throws Exception
  {
    createCapabilityWithSecret("bar");
    ArgumentCaptor<CapabilityStorageItem> csiRec = ArgumentCaptor.forClass(CapabilityStorageItem.class);

    verify(capabilityStorage).add(csiRec.capture());
    CapabilityStorageItem item = csiRec.getValue();
    assertThat(item, is(notNullValue()));
    String fooValue = item.getProperties().get("foo");
    assertThat(fooValue, is("0"));
    verify(secretsService).encryptMaven("capabilities", "bar".toCharArray(), "testuser");
  }

  /**
   * Verify that value is decrypted when corresponding field is marked with {@link Encrypted}.
   */
  @Test
  public void loadWithEncryptedProperty()
      throws Exception
  {
    Map<String, String> properties = Maps.newHashMap();
    properties.put("foo", secretsService.encryptMaven("", "bar".toCharArray(), "").getId());

    final CapabilityStorageItem item = new CapabilityStorageItemData();
    item.setVersion(0);
    item.setType(CAPABILITY_TYPE.toString());
    item.setEnabled(true);
    item.setProperties(properties);
    when(capabilityStorage.getAll()).thenReturn(ImmutableMap.of(capabilityIdentity("foo"), item));

    final CapabilityDescriptor descriptor = mock(CapabilityDescriptor.class);
    when(capabilityDescriptorRegistry.get(CAPABILITY_TYPE)).thenReturn(descriptor);
    when(descriptor.version()).thenReturn(0);
    when(descriptor.formFields()).thenReturn(Arrays.<FormField>asList(
        new PasswordFormField("foo", "foo", "?", FormField.OPTIONAL)
    ));

    underTest.load();

    ArgumentCaptor<Object> ebRec = ArgumentCaptor.forClass(Object.class);

    verify(eventManager, atLeastOnce()).post(ebRec.capture());
    assertThat(
        ((CapabilityEvent) ebRec.getAllValues().get(0)).getReference().context().properties().get("foo"), is("bar")
    );
  }

  /**
   * Confirm thread safety for concurrent calls to {@link DefaultCapabilityRegistry#getAll()} and
   * {@link DefaultCapabilityRegistry#add(CapabilityType, boolean, String, Map)}.
   *
   * This test would fail with a {@link java.util.ConcurrentModificationException} unless
   * {@link DefaultCapabilityRegistry#getAll()} returns a copy of the internal references values.
   */
  @Test
  public void getAllReturnNotAffectedByConcurrentAdd() {
    // guarantee we have at least 2 instances in the DefaultCapabilityRegistry under test
    underTest.add(CAPABILITY_TYPE, true, "note1", null);
    underTest.add(CAPABILITY_TYPE, true, "note2", null);

    Collection<DefaultCapabilityReference> references = underTest.getAll();
    Iterator<DefaultCapabilityReference> iterator = references.iterator();

    iterator.next();
    underTest.add(CAPABILITY_TYPE, true, "note3", null);
    assertThat(iterator.next(), notNullValue());
  }

  /**
   * On load, if properties are invalid, the entry is kept but marked as failed.
   */
  @Test
  public void loadWhenPropertiesAreInvalid()
      throws Exception
  {
    final Map<String, String> oldProps = Maps.newHashMap();
    oldProps.put("bad", "data");
    final CapabilityStorageItem item = new CapabilityStorageItemData();
    item.setVersion(0);
    item.setType(CAPABILITY_TYPE.toString());
    item.setEnabled(true);
    item.setProperties(oldProps);


    CapabilityIdentity fooId = capabilityIdentity("foo");
    when(capabilityStorage.getAll()).thenReturn(ImmutableMap.of(fooId, item));

    final CapabilityDescriptor descriptor = mock(CapabilityDescriptor.class);
    when(capabilityDescriptorRegistry.get(CAPABILITY_TYPE)).thenReturn(descriptor);
    when(descriptor.version()).thenReturn(0);

    doThrow(new ValidationException("Bad data!"))
        .when(descriptor).validate(fooId, oldProps, ValidationMode.LOAD);

    assertThat(underTest.getAll(), hasSize(0));

    underTest.load();

    assertThat(underTest.getAll(), hasSize(1));

    Iterator<DefaultCapabilityReference> itr = underTest.getAll().iterator();
    assertThat(itr.next().hasFailure(), is(true));
    assertThat(itr.hasNext(), is(false));
  }

  /**
   * On load, if capability is not unique, the entry is kept but not marked as a failure.
   * This is because the separate 'HasNoDuplicates' capability condition will track it.
   */
  @Test
  public void loadWhenCapabilityIsNotUnique()
      throws Exception
  {
    final Map<String, String> oldProps = Maps.newHashMap();
    oldProps.put("duplicate", "data");

    final CapabilityStorageItem item = new CapabilityStorageItemData();
    item.setVersion(0);
    item.setType(CAPABILITY_TYPE.toString());
    item.setEnabled(true);
    item.setProperties(oldProps);

    final CapabilityStorageItem duplicate = item;

    CapabilityIdentity fooId = capabilityIdentity("foo");
    CapabilityIdentity barId = capabilityIdentity("bar");
    when(capabilityStorage.getAll()).thenReturn(ImmutableMap.of(fooId, item, barId, duplicate));

    final CapabilityDescriptor descriptor = mock(CapabilityDescriptor.class);
    when(capabilityDescriptorRegistry.get(CAPABILITY_TYPE)).thenReturn(descriptor);
    when(descriptor.version()).thenReturn(0);

    doNothing().when(descriptor).validate(fooId, oldProps, ValidationMode.LOAD);

    assertThat(underTest.getAll(), hasSize(0));

    underTest.load();

    assertThat(underTest.getAll(), hasSize(2));

    Iterator<DefaultCapabilityReference> itr = underTest.getAll().iterator();
    assertThat(itr.next().hasFailure(), is(false));
    assertThat(itr.next().hasFailure(), is(false));
    assertThat(itr.hasNext(), is(false));
  }

  @Test
  public void refreshReferencesOnDemand() {
    CapabilityDescriptor descriptor = mock(CapabilityDescriptor.class);
    when(capabilityDescriptorRegistry.get(CAPABILITY_TYPE)).thenReturn(descriptor);
    when(descriptor.formFields()).thenReturn(Collections.singletonList(
        new PasswordFormField("password", "password", "Sensitive field", FormField.MANDATORY)
    ));

    final Map<String, String> oldProps = Maps.newHashMap();
    oldProps.put("p1", "v1");
    oldProps.put("p2", "v2");
    oldProps.put("password", secretsService.encryptMaven("", "admin123".toCharArray(), "").getId());

    final CapabilityStorageItem item = new CapabilityStorageItemData();
    item.setVersion(0);
    item.setType(CAPABILITY_TYPE.toString());
    item.setEnabled(true);
    item.setProperties(oldProps);
    CapabilityIdentity fooId = capabilityIdentity("foo");
    when(capabilityStorage.getAll()).thenReturn(ImmutableMap.of(fooId, item));

    underTest.load();
    Collection<DefaultCapabilityReference> references = underTest.getAll();
    assertThat(references, hasSize(1));
    Map<String, String> properties = references.stream().findFirst().get().properties();
    assertEquals("v1", properties.get("p1"));
    assertEquals("v2", properties.get("p2"));

    oldProps.put("p1", "v1a");
    oldProps.put("p2", "v2a");
    underTest.pullAndRefreshReferencesFromDB();

    assertThat(underTest.getAll(), hasSize(1));
    properties = references.stream().findFirst().get().properties();
    assertEquals("v1a", properties.get("p1"));
    assertEquals("v2a", properties.get("p2"));
    assertEquals("admin123", properties.get("password"));
  }

  @Test
  public void migrateCapabilityWithSecrets() {
    CapabilityReference reference = createCapabilityWithSecret("my-secret");

    ArgumentCaptor<CapabilityStorageItem> csiRec = ArgumentCaptor.forClass(CapabilityStorageItem.class);

    verify(capabilityStorage).add(csiRec.capture());
    CapabilityStorageItem initial = csiRec.getValue();
    assertThat(initial, is(notNullValue()));
    String foo = initial.getProperties().get("foo");
    assertThat(foo, is("0"));
    verify(secretsService).encryptMaven("capabilities", "my-secret".toCharArray(), "testuser");

    //re encrypting the reference and force to re encrypt the secret
    underTest.migrateSecrets(reference, (secret) -> true);

    verify(capabilityStorage).update(any(CapabilityIdentity.class), csiRec.capture());
    CapabilityStorageItem updated = csiRec.getAllValues().get(csiRec.getAllValues().size() -1); // get the last value

    //verify we only modified secrets
    assertThat(updated, is(notNullValue()));
    assertThat(updated.getType() , is(initial.getType()));
    assertThat(updated.getNotes(), is(initial.getNotes()));
    assertThat(updated.isEnabled(), is(initial.isEnabled()));

    foo = updated.getProperties().get("foo");
    assertThat(foo, is("1")); // we re-encrypted the secret
    verify(secretsService, times(2)).encryptMaven("capabilities", "my-secret".toCharArray(), "testuser");

    //re encrypt again but this time force to not re encrypt anything
    underTest.migrateSecrets(reference, (secret) -> false);

    //we didn't update this time, since maps are equal
    verify(capabilityStorage).update(any(CapabilityIdentity.class), csiRec.capture());
    CapabilityStorageItem nonUpdated = csiRec.getAllValues().get(csiRec.getAllValues().size() -1); // get the last value

    assertThat(nonUpdated, is(notNullValue()));

    foo = updated.getProperties().get("foo");
    assertThat(foo, is("1")); //  we did not re-encrypt the secret
    verify(secretsService, times(2)).encryptMaven("capabilities", "my-secret".toCharArray(), "testuser");
  }

  private CapabilityReference createCapabilityWithSecret(final String secretValue) {
    final CapabilityDescriptor descriptor = mock(CapabilityDescriptor.class);
    when(capabilityDescriptorRegistry.get(CAPABILITY_TYPE)).thenReturn(descriptor);
    when(descriptor.formFields()).thenReturn(Collections.singletonList(
        new PasswordFormField("foo", "foo", "?", FormField.OPTIONAL)
    ));

    Map<String, String> properties = Maps.newHashMap();
    properties.put("foo", secretValue);

    return underTest.add(CAPABILITY_TYPE, true, null, properties);
  }

  private static Subject subject(final String principal) {
    Subject subject = mock(Subject.class);
    when(subject.getPrincipal()).thenReturn(principal);
    return subject;
  }
}
