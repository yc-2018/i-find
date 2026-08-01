import { Image, StyleSheet, Text, View } from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import type { SvgProps } from 'react-native-svg';

import BilibiliIcon from '../../icon/bilibili.svg';
import DouyinIcon from '../../icon/douyin.svg';
import JdIcon from '../../icon/jd.svg';
import MeituanIcon from '../../icon/meituan.svg';
import PddIcon from '../../icon/pdd.svg';
import TaobaoIcon from '../../icon/taobao.svg';
import XhsIcon from '../../icon/xhs.svg';
import { colors } from '../constants/colors';
import { resolveGeneratedColor } from '../utils/search-targets';
import type { SearchTarget } from '../types/search-targets';

type TargetIconProps = {
  size?: number;
  target: Pick<SearchTarget, 'iconMode' | 'iconValue' | 'name'>;
};

const svgIconMap: Record<string, React.FC<SvgProps>> = {
  'asset:bilibili': BilibiliIcon,
  'asset:douyin': DouyinIcon,
  'asset:jd': JdIcon,
  'asset:meituan': MeituanIcon,
  'asset:pdd': PddIcon,
  'asset:taobao': TaobaoIcon,
  'asset:xhs': XhsIcon,
};

export function TargetIcon({ size = 28, target }: TargetIconProps) {
  if (target.iconMode === 'gallery' && target.iconValue) {
    return (
      <Image
        source={{ uri: target.iconValue }}
        style={[styles.image, { width: size, height: size, borderRadius: size / 3.2 }]}
      />
    );
  }

  if (target.iconMode === 'builtin') {
    const SvgIcon = svgIconMap[target.iconValue];

    if (SvgIcon) {
      return <SvgIcon height={size} width={size} />;
    }

    const iconName = target.iconValue.startsWith('mdi:')
      ? target.iconValue.replace('mdi:', '')
      : 'magnify';

    return (
      <View style={[styles.fallbackCircle, { width: size, height: size, borderRadius: size / 2 }]}>
        <MaterialCommunityIcons color={colors.accent} name={iconName as never} size={size * 0.72} />
      </View>
    );
  }

  const letter = Array.from(target.name.trim())[0] ?? '?';
  const backgroundColor = resolveGeneratedColor(target.iconValue || target.name);

  return (
    <View
      style={[
        styles.generatedCircle,
        {
          width: size,
          height: size,
          borderRadius: size / 2,
          backgroundColor,
        },
      ]}
    >
      <Text style={[styles.generatedLabel, { fontSize: size * 0.44 }]}>{letter}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  image: {
    resizeMode: 'cover',
  },
  fallbackCircle: {
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.accentSoft,
  },
  generatedCircle: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  generatedLabel: {
    color: '#fff',
    fontWeight: '800',
  },
});
