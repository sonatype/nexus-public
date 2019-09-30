import deprecate from '../utils/deprecate';
import toHaveDisplayName from './toHaveDisplayName';
import colors from '../utils/colors';

const toHaveTagName = deprecate(
  (enzymeWrapper: EnzymeObject, tag: string): Matcher => {
    return toHaveDisplayName(enzymeWrapper, tag);
  },
  `Matcher ${colors.red(
    'toHaveTagName'
  )} is deprecated and will be removed in a future release. ` +
    `Use the replacement, ${colors.blue('toHaveDisplayName')} instead.`
);

export default toHaveTagName;
