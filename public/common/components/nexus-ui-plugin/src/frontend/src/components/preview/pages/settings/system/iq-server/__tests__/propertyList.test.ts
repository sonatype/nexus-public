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

import { parsePropertiesString, serializeProperties, validateProperties } from '../propertyList';
import { IqProperty } from '../types';

describe('parsePropertiesString', () => {
  it('returns no properties and no dropped lines for an empty string', () => {
    expect(parsePropertiesString('')).toEqual({ properties: [], droppedLineCount: 0 });
    expect(parsePropertiesString('   ')).toEqual({ properties: [], droppedLineCount: 0 });
  });

  it('parses newline-separated name=value pairs', () => {
    const { properties, droppedLineCount } = parsePropertiesString('proxy.host=proxy.example.com\nproxy.port=8080');
    expect(droppedLineCount).toBe(0);
    expect(properties).toHaveLength(2);
    expect(properties[0]).toMatchObject({ name: 'proxy.host', value: 'proxy.example.com' });
    expect(properties[1]).toMatchObject({ name: 'proxy.port', value: '8080' });
  });

  it('parses CRLF-separated pairs', () => {
    const { properties } = parsePropertiesString('a=1\r\nb=2');
    expect(properties).toHaveLength(2);
  });

  it('keeps values that themselves contain "="', () => {
    const { properties } = parsePropertiesString('param=value=with=equals');
    expect(properties[0]).toMatchObject({ name: 'param', value: 'value=with=equals' });
  });

  it('trims whitespace around name and value', () => {
    const { properties } = parsePropertiesString('  foo = bar  ');
    expect(properties[0]).toMatchObject({ name: 'foo', value: 'bar' });
  });

  it('keeps a row with an empty value rather than dropping it', () => {
    const { properties, droppedLineCount } = parsePropertiesString('foo=');
    expect(droppedLineCount).toBe(0);
    expect(properties[0]).toMatchObject({ name: 'foo', value: '' });
  });

  it('counts a comment line as dropped', () => {
    const { properties, droppedLineCount } = parsePropertiesString('# a comment\nfoo=bar');
    expect(droppedLineCount).toBe(1);
    expect(properties).toHaveLength(1);
  });

  it('counts a line with no "=" as dropped', () => {
    const { droppedLineCount } = parsePropertiesString('justsometext');
    expect(droppedLineCount).toBe(1);
  });

  it('counts a line with an empty name before "=" as dropped', () => {
    const { droppedLineCount } = parsePropertiesString('=novalue');
    expect(droppedLineCount).toBe(1);
  });

  it('does not count blank lines as dropped', () => {
    const { droppedLineCount } = parsePropertiesString('foo=bar\n\n\nbaz=qux');
    expect(droppedLineCount).toBe(0);
  });

  it('generates unique ids for each row', () => {
    const { properties } = parsePropertiesString('a=1\nb=2');
    expect(properties[0].id).not.toBe(properties[1].id);
  });
});

describe('serializeProperties', () => {
  it('returns empty string for empty array', () => {
    expect(serializeProperties([])).toBe('');
  });

  it('serializes rows with newline separator', () => {
    const props: IqProperty[] = [
      { id: '1', name: 'proxy.host', value: 'proxy.example.com' },
      { id: '2', name: 'proxy.port', value: '8080' },
    ];
    expect(serializeProperties(props)).toBe('proxy.host=proxy.example.com\nproxy.port=8080');
  });

  it('excludes rows with an empty name or value', () => {
    const props: IqProperty[] = [
      { id: '1', name: '', value: 'x' },
      { id: '2', name: 'y', value: '' },
      { id: '3', name: 'z', value: 'w' },
    ];
    expect(serializeProperties(props)).toBe('z=w');
  });
});

describe('validateProperties', () => {
  it('returns no errors for valid properties', () => {
    const props: IqProperty[] = [{ id: '1', name: 'proxy.host', value: 'x' }];
    const result = validateProperties(props);
    expect(result.hasBlockingErrors).toBe(false);
    expect(result.validations).toEqual([]);
  });

  it('errors on an empty name', () => {
    const props: IqProperty[] = [{ id: '1', name: '', value: 'x' }];
    const result = validateProperties(props);
    expect(result.hasBlockingErrors).toBe(true);
    expect(result.validations).toContainEqual(expect.objectContaining({ id: '1', error: 'Parameter name is required' }));
  });

  it('errors on an empty value', () => {
    const props: IqProperty[] = [{ id: '1', name: 'x', value: '' }];
    const result = validateProperties(props);
    expect(result.hasBlockingErrors).toBe(true);
    expect(result.validations).toContainEqual(expect.objectContaining({ id: '1', error: 'Value is required' }));
  });

  it('errors on a case-insensitive duplicate name', () => {
    const props: IqProperty[] = [
      { id: '1', name: 'Proxy.Host', value: 'a' },
      { id: '2', name: 'proxy.host', value: 'b' },
    ];
    const result = validateProperties(props);
    expect(result.hasBlockingErrors).toBe(true);
    expect(result.validations).toContainEqual(expect.objectContaining({ id: '2', error: 'Duplicate parameter name' }));
  });
});
