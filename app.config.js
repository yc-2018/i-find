const { expo: baseConfig } = require('./app.json');

const buildArchs = process.env.BUILD_ARCHS
  ? process.env.BUILD_ARCHS.split(',').map((value) => value.trim()).filter(Boolean)
  : [];

const plugins = [...(baseConfig.plugins ?? [])];

plugins.push('./plugins/withStopappQueries');

if (buildArchs.length > 0) {
  plugins.push([
    './plugins/withReactNativeArchitectures',
    {
      architectures: buildArchs,
    },
  ]);
}

module.exports = {
  expo: {
    ...baseConfig,
    plugins,
  },
};
