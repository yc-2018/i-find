import { Pressable, StyleSheet, Switch, Text, View } from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';

import { colors } from '../constants/colors';
import { TargetIcon } from './TargetIcon';
import type { SearchTarget } from '../types/search-targets';

type SettingsTargetRowProps = {
  isActive: boolean;
  isDragging: boolean;
  onDelete: () => void;
  onDragStart: () => void;
  onEdit: () => void;
  onToggleHidden: (hidden: boolean) => void;
  target: SearchTarget;
};

export function SettingsTargetRow({
  isActive,
  isDragging,
  onDelete,
  onDragStart,
  onEdit,
  onToggleHidden,
  target,
}: SettingsTargetRowProps) {
  return (
    <Pressable
      disabled={isDragging}
      onPress={onEdit}
      style={[styles.card, isActive && styles.cardActive]}
    >
      <View style={styles.left}>
        <TargetIcon size={34} target={target} />
        <View style={styles.meta}>
          <Text numberOfLines={1} style={[styles.name, target.hidden && styles.hiddenName]}>
            {target.name}
          </Text>
          <Text numberOfLines={1} style={styles.detail}>
            {target.launchMode === 'schemeFirst' ? '深链优先' : '网页搜索'}
            {target.hidden ? ' · 已隐藏' : ' · 首页可见'}
          </Text>
        </View>
      </View>

      <View style={styles.actions}>
        <Switch
          onValueChange={(value) => onToggleHidden(!value)}
          thumbColor={colors.surface}
          trackColor={{ false: colors.hidden, true: colors.success }}
          value={!target.hidden}
        />
        <Pressable
          accessibilityLabel={`拖拽排序 ${target.name}`}
          delayLongPress={120}
          hitSlop={10}
          onLongPress={onDragStart}
          style={styles.dragButton}
        >
          <MaterialCommunityIcons color={colors.subtleText} name="drag-horizontal" size={40} />
        </Pressable>
        <Pressable onPress={onDelete} style={styles.iconButton}>
          <MaterialCommunityIcons color={colors.danger} name="trash-can-outline" size={20} />
        </Pressable>
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {
    marginBottom: 12,
    borderRadius: 22,
    backgroundColor: colors.surface,
    paddingHorizontal: 16,
    paddingVertical: 14,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    shadowColor: colors.shadow,
    shadowOpacity: 0.06,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 8 },
    elevation: 2,
  },
  cardActive: {
    opacity: 0.9,
  },
  left: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingRight: 12,
  },
  meta: {
    flex: 1,
  },
  name: {
    color: colors.text,
    fontSize: 16,
    fontWeight: '800',
  },
  hiddenName: {
    color: colors.hidden,
  },
  detail: {
    marginTop: 4,
    color: colors.subtleText,
    fontSize: 13,
  },
  actions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  dragButton: {
    minWidth: 66,
    minHeight: 66,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 20,
    backgroundColor: colors.surfaceStrong,
  },
  iconButton: {
    padding: 8,
  },
});
