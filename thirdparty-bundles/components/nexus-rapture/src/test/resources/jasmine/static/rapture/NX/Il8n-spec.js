describe('I18n', function() {
  beforeAll(function(done) {
    Ext.onReady(function() {
      NX.I18n.register({
        keys: {
          _test_key: 'value',
          _test_reference: '@_test_key',
          _test_missing_reference: '@_test_missing_key',
          _test_formatted: '{0}'
        },
        bundles: {
          _test_bundle: {
            _test_key: 'value',
            _test_reference: '@_test_bundle:_test_key',
            _test_missing_reference: '@_test_bundle:_test_missing_reference_target'
          }
        }
      });

      done();
    });
  });

  describe('get', function() {
    it('by key', function() {
      expect(NX.I18n.get('_test_key')).toBe('value');
    });

    it('by reference', function() {
      expect(NX.I18n.get('_test_reference')).toBe('value');
    });

    it('missing by key', function() {
      expect(NX.I18n.get('_test_missing')).toBe('MISSING_I18N:_test_missing');
    });

    it('missing by reference', function() {
      expect(NX.I18n.get('_test_missing_reference')).toBe('MISSING_I18N:_test_missing_key');
    });
  });

  describe('format', function() {
    it('by key', function() {
      expect(NX.I18n.format('_test_formatted', 'test')).toBe('test');
    });

    it('missing by key', function() {
      expect(NX.I18n.format('_test_missing', 'test')).toBe('MISSING_I18N:_test_missing');
    });
  });

  describe('render', function() {
    it('matching key', function() {
      expect(NX.I18n.render('_test_bundle', '_test_key')).toBe('value');
    });

    it('matching reference', function() {
      expect(NX.I18n.render('_test_bundle', '_test_reference')).toBe('value');
    });

    it('missing by key', function() {
      expect(NX.I18n.render('_test_bundle', '_test_missing_key')).toBe('MISSING_I18N:_test_bundle:_test_missing_key');
    });

    it('missing by reference', function() {
      expect(NX.I18n.render('_test_bundle', '_test_missing_reference')).toBe('MISSING_I18N:_test_bundle:_test_missing_reference_target');
    });
  });
});