export * from './IndicatorFactory';
export * from './Indicator';

declare const require: any;

// Require all indicators. Needed by webpack
require('./Global');