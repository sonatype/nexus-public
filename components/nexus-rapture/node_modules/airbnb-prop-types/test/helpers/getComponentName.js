import { expect } from 'chai';

import getComponentName from '../../build/helpers/getComponentName';

describe('getComponentName', () => {
  it('given a string, returns the string', () => {
    expect(getComponentName('foo')).to.equal('foo');
  });

  it('given a function, returns displayName or name', () => {
    function Foo() {}

    expect(getComponentName(Foo)).to.equal(Foo.name);

    Foo.displayName = 'Bar';
    expect(getComponentName(Foo)).to.equal(Foo.displayName);
  });

  it('given anything else, returns null', () => {
    expect(getComponentName()).to.equal(null);
    expect(getComponentName(null)).to.equal(null);
    expect(getComponentName(undefined)).to.equal(null);
    expect(getComponentName([])).to.equal(null);
    expect(getComponentName({})).to.equal(null);
    expect(getComponentName(42)).to.equal(null);
    expect(getComponentName(true)).to.equal(null);
  });
});
