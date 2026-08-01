const { withGradleProperties } = require('@expo/config-plugins');

function upsertGradleProperty(modResults, key, value) {
  const propertyIndex = modResults.findIndex(
    (item) => item.type === 'property' && item.key === key
  );

  if (!value) {
    if (propertyIndex >= 0) {
      modResults.splice(propertyIndex, 1);
    }

    return modResults;
  }

  const property = {
    type: 'property',
    key,
    value,
  };

  if (propertyIndex >= 0) {
    modResults[propertyIndex] = property;
    return modResults;
  }

  modResults.push(property);
  return modResults;
}

module.exports = function withReactNativeArchitectures(config, props = {}) {
  const architectures = Array.isArray(props.architectures)
    ? props.architectures.map((value) => value.trim()).filter(Boolean)
    : [];

  return withGradleProperties(config, (modConfig) => {
    modConfig.modResults = upsertGradleProperty(
      modConfig.modResults,
      'reactNativeArchitectures',
      architectures.join(',')
    );

    return modConfig;
  });
};
