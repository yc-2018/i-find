import type { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';

import { colors } from '../constants/colors';

type ScreenHeaderProps = {
  onBack?: () => void;
  rightAction?: ReactNode;
  subtitle?: string;
  title: string;
};

export function ScreenHeader({
  onBack,
  rightAction,
  subtitle,
  title,
}: ScreenHeaderProps) {
  return (
    <View style={styles.container}>
      <View style={styles.row}>
        {onBack ? (
          <Pressable onPress={onBack} style={styles.backButton}>
            <MaterialCommunityIcons color={colors.text} name="chevron-left" size={24} />
          </Pressable>
        ) : (
          <View style={styles.backPlaceholder} />
        )}
        {rightAction ?? <View style={styles.rightPlaceholder} />}
      </View>
      <Text style={styles.title}>{title}</Text>
      {subtitle ? <Text style={styles.subtitle}>{subtitle}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    marginBottom: 18,
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  backButton: {
    height: 40,
    width: 40,
    borderRadius: 20,
    backgroundColor: colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
  },
  backPlaceholder: {
    width: 40,
  },
  rightPlaceholder: {
    width: 40,
  },
  title: {
    color: colors.text,
    fontSize: 28,
    fontWeight: '800',
  },
  subtitle: {
    marginTop: 6,
    color: colors.subtleText,
    fontSize: 14,
    lineHeight: 21,
  },
});
