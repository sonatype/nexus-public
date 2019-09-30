import * as Enzyme from 'enzyme';

declare global {
    var shallow: typeof Enzyme.shallow;
    var mount: typeof Enzyme.mount;
    var render: typeof Enzyme.render;
}
