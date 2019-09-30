import getFunctionName from 'function.prototype.name';

export default function getComponentName(Component) {
  if (typeof Component === 'string') {
    return Component;
  }
  if (typeof Component === 'function') {
    return Component.displayName || getFunctionName(Component);
  }
  return null;
}
