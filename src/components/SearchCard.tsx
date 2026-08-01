import { StyleSheet, Text, TouchableOpacity } from 'react-native';

import { colors } from '../constants/colors';
import { TargetIcon } from './TargetIcon';
import type { SearchTarget } from '../types/search-targets';

type SearchCardProps = {
  disabled?: boolean;
  onPress: () => void;
  target: SearchTarget;
};

export function SearchCard({ disabled, onPress, target }: SearchCardProps) {
  return (
    <TouchableOpacity
      activeOpacity={0.85}
      disabled={disabled}
      onPress={onPress}
      style={[styles.card, disabled && styles.cardDisabled]}
    >
      <TargetIcon size={28} target={target} />
      <Text numberOfLines={1} style={styles.label}>
        {target.name}
      </Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  card: {
    width: '23%',
    minHeight: 92,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    borderRadius: 22,
    backgroundColor: colors.surface,
    paddingHorizontal: 4,
    paddingVertical: 12,
    shadowColor: colors.shadow,
    shadowOpacity: 0.06,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 8 },
    elevation: 2,
  },
  cardDisabled: {
    opacity: 0.55,
  },
  label: {
    maxWidth: '100%',
    color: colors.subtleText,
    fontSize: 12,
    fontWeight: '700',
    textAlign: 'center',
  },
});
