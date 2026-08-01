import { useEffect, useMemo, useState } from 'react';
import {
  KeyboardAvoidingView,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import * as ImagePicker from 'expo-image-picker';

import { colors } from '../constants/colors';
import { builtinIconChoices } from '../constants/search-targets';
import type { EditableSearchTarget } from '../context/search-targets-context';
import type { SearchLaunchMode, SearchTarget } from '../types/search-targets';
import { copyImageToAppStorageAsync } from '../utils/icon-storage';
import { notify } from '../utils/notify';
import { TargetIcon } from './TargetIcon';

type TargetFormModalProps = {
  onClose: () => void;
  onSubmit: (draft: EditableSearchTarget) => Promise<void>;
  open: boolean;
  target: SearchTarget | null;
};

type DraftState = {
  name: string;
  launchMode: SearchLaunchMode;
  schemeTemplate: string;
  webFallbackTemplate: string;
  androidPackageName: string;
  iconMode: EditableSearchTarget['iconMode'];
  iconValue: string;
  pendingGalleryUri: string;
};

const initialDraft: DraftState = {
  name: '',
  launchMode: 'schemeFirst',
  schemeTemplate: '',
  webFallbackTemplate: '',
  androidPackageName: '',
  iconMode: 'generated',
  iconValue: '',
  pendingGalleryUri: '',
};

export function TargetFormModal({
  onClose,
  onSubmit,
  open,
  target,
}: TargetFormModalProps) {
  const [draft, setDraft] = useState<DraftState>(initialDraft);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    if (!target) {
      setDraft(initialDraft);
      return;
    }

    setDraft({
      name: target.name,
      launchMode: target.launchMode,
      schemeTemplate: target.schemeTemplate ?? '',
      webFallbackTemplate: target.webFallbackTemplate ?? '',
      androidPackageName: target.androidPackageName ?? '',
      iconMode: target.iconMode,
      iconValue: target.iconValue,
      pendingGalleryUri: '',
    });
  }, [open, target]);

  const previewTarget = useMemo(
    () => ({
      iconMode: draft.pendingGalleryUri ? 'gallery' : draft.iconMode,
      iconValue: draft.pendingGalleryUri || draft.iconValue,
      name: draft.name || '搜',
    }),
    [draft.iconMode, draft.iconValue, draft.name, draft.pendingGalleryUri]
  );

  const handlePickImage = async () => {
    const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();

    if (!permission.granted) {
      notify('需要相册权限才能选择自定义图标');
      return;
    }

    const result = await ImagePicker.launchImageLibraryAsync({
      allowsEditing: true,
      aspect: [1, 1],
      mediaTypes: ['images'],
      quality: 0.8,
    });

    if (result.canceled || !result.assets.length) {
      return;
    }

    setDraft((current) => ({
      ...current,
      iconMode: 'gallery',
      pendingGalleryUri: result.assets[0].uri,
    }));
  };

  const handleSave = async () => {
    try {
      setIsSaving(true);

      let iconMode = draft.iconMode;
      let iconValue = draft.iconValue;

      if (draft.pendingGalleryUri) {
        iconMode = 'gallery';
        iconValue = await copyImageToAppStorageAsync(draft.pendingGalleryUri);
      }

      if (!iconValue && iconMode === 'builtin') {
        iconMode = 'generated';
      }

      await onSubmit({
        name: draft.name,
        launchMode: draft.launchMode,
        schemeTemplate: draft.schemeTemplate,
        webFallbackTemplate: draft.webFallbackTemplate,
        androidPackageName: draft.androidPackageName,
        iconMode,
        iconValue,
      });
    } catch (error) {
      const message = error instanceof Error ? error.message : '保存失败，请稍后再试';
      notify(message);
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <Modal animationType="slide" onRequestClose={onClose} transparent visible={open}>
      <View style={styles.backdrop}>
        <KeyboardAvoidingView
          behavior={Platform.select({ ios: 'padding', android: undefined })}
          style={styles.keyboard}
        >
          <View style={styles.sheet}>
            <View style={styles.handle} />
            <Text style={styles.title}>{target ? '编辑搜索项' : '新增搜索项'}</Text>
            <ScrollView
              contentContainerStyle={styles.content}
              keyboardShouldPersistTaps="handled"
              showsVerticalScrollIndicator={false}
            >
              <View style={styles.previewCard}>
                <TargetIcon size={44} target={previewTarget} />
                <View style={styles.previewMeta}>
                  <Text style={styles.previewTitle}>{draft.name || '未命名搜索项'}</Text>
                  <Text style={styles.previewSubtitle}>
                    {draft.launchMode === 'schemeFirst' ? '深链优先' : '网页搜索'}
                  </Text>
                </View>
              </View>

              <View style={styles.field}>
                <Text style={styles.label}>名称</Text>
                <TextInput
                  onChangeText={(value) => setDraft((current) => ({ ...current, name: value }))}
                  placeholder="比如：知乎、微博、豆瓣"
                  placeholderTextColor={colors.muted}
                  style={styles.input}
                  value={draft.name}
                />
              </View>

              <View style={styles.field}>
                <Text style={styles.label}>搜索模式</Text>
                <View style={styles.segment}>
                  {[
                    { label: '深链优先', value: 'schemeFirst' },
                    { label: '纯网页', value: 'webOnly' },
                  ].map((option) => {
                    const active = draft.launchMode === option.value;
                    return (
                      <Pressable
                        key={option.value}
                        onPress={() =>
                          setDraft((current) => ({
                            ...current,
                            launchMode: option.value as SearchLaunchMode,
                          }))
                        }
                        style={[styles.segmentButton, active && styles.segmentButtonActive]}
                      >
                        <Text
                          style={[styles.segmentLabel, active && styles.segmentLabelActive]}
                        >
                          {option.label}
                        </Text>
                      </Pressable>
                    );
                  })}
                </View>
              </View>

              <View style={styles.field}>
                <Text style={styles.label}>深链模板</Text>
                <TextInput
                  autoCapitalize="none"
                  multiline
                  onChangeText={(value) =>
                    setDraft((current) => ({ ...current, schemeTemplate: value }))
                  }
                  placeholder="例如：app://search?keyword={keyword}"
                  placeholderTextColor={colors.muted}
                  style={[styles.input, styles.multilineInput]}
                  value={draft.schemeTemplate}
                />
              </View>

              <View style={styles.field}>
                <Text style={styles.label}>网页模板</Text>
                <TextInput
                  autoCapitalize="none"
                  multiline
                  onChangeText={(value) =>
                    setDraft((current) => ({ ...current, webFallbackTemplate: value }))
                  }
                  placeholder="例如：https://example.com/search?q={keyword}"
                  placeholderTextColor={colors.muted}
                  style={[styles.input, styles.multilineInput]}
                  value={draft.webFallbackTemplate}
                />
              </View>

              <View style={styles.field}>
                <Text style={styles.label}>{'Android \u5305\u540d\uff08\u53ef\u9009\uff09'}</Text>
                <TextInput
                  autoCapitalize="none"
                  autoCorrect={false}
                  onChangeText={(value) =>
                    setDraft((current) => ({ ...current, androidPackageName: value }))
                  }
                  placeholder="例如：com.zhihu.android"
                  placeholderTextColor={colors.muted}
                  style={styles.input}
                  value={draft.androidPackageName}
                />
                <Text style={styles.helperText}>
                  {
                    '\u586b\u5199\u540e\uff0c\u4e4b\u540e\u5982\u679c\u5c0f\u9ed1\u5c4b\u63d0\u4f9b\u7a33\u5b9a\u63a5\u53e3\uff0c\u53ef\u4ee5\u7528\u6765\u8bc6\u522b\u8fd9\u4e2a\u76ee\u6807 App\u3002'
                  }
                </Text>
              </View>

              <View style={styles.field}>
                <Text style={styles.label}>图标</Text>
                <View style={styles.iconActions}>
                  <Pressable onPress={handlePickImage} style={styles.iconActionButton}>
                    <Text style={styles.iconActionLabel}>从相册选择</Text>
                  </Pressable>
                  <Pressable
                    onPress={() =>
                      setDraft((current) => ({
                        ...current,
                        iconMode: 'generated',
                        iconValue: '',
                        pendingGalleryUri: '',
                      }))
                    }
                    style={styles.iconActionButton}
                  >
                    <Text style={styles.iconActionLabel}>自动首字母</Text>
                  </Pressable>
                </View>

                <ScrollView horizontal showsHorizontalScrollIndicator={false}>
                  <View style={styles.builtinList}>
                    {builtinIconChoices.map((choice) => {
                      const selected =
                        !draft.pendingGalleryUri &&
                        draft.iconMode === 'builtin' &&
                        draft.iconValue === choice.key;

                      return (
                        <Pressable
                          key={choice.key}
                          onPress={() =>
                            setDraft((current) => ({
                              ...current,
                              iconMode: 'builtin',
                              iconValue: choice.key,
                              pendingGalleryUri: '',
                            }))
                          }
                          style={[
                            styles.builtinItem,
                            selected && styles.builtinItemSelected,
                          ]}
                        >
                          <TargetIcon
                            size={30}
                            target={{
                              iconMode: 'builtin',
                              iconValue: choice.key,
                              name: choice.label,
                            }}
                          />
                          <Text style={styles.builtinLabel}>{choice.label}</Text>
                        </Pressable>
                      );
                    })}
                  </View>
                </ScrollView>
              </View>
            </ScrollView>

            <View style={styles.footer}>
              <Pressable disabled={isSaving} onPress={onClose} style={styles.cancelButton}>
                <Text style={styles.cancelLabel}>取消</Text>
              </Pressable>
              <Pressable disabled={isSaving} onPress={() => void handleSave()} style={styles.saveButton}>
                <Text style={styles.saveLabel}>{isSaving ? '保存中...' : '保存'}</Text>
              </Pressable>
            </View>
          </View>
        </KeyboardAvoidingView>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(33, 24, 14, 0.25)',
    justifyContent: 'flex-end',
  },
  keyboard: {
    flex: 1,
    justifyContent: 'flex-end',
  },
  sheet: {
    maxHeight: '90%',
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    backgroundColor: colors.surface,
    paddingHorizontal: 18,
    paddingTop: 10,
    paddingBottom: 20,
  },
  handle: {
    alignSelf: 'center',
    width: 52,
    height: 5,
    borderRadius: 999,
    backgroundColor: colors.border,
  },
  title: {
    marginTop: 16,
    color: colors.text,
    fontSize: 22,
    fontWeight: '800',
  },
  content: {
    paddingTop: 16,
    paddingBottom: 14,
    gap: 16,
  },
  previewCard: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    borderRadius: 20,
    backgroundColor: colors.surfaceStrong,
    padding: 14,
  },
  previewMeta: {
    flex: 1,
  },
  previewTitle: {
    color: colors.text,
    fontSize: 17,
    fontWeight: '800',
  },
  previewSubtitle: {
    marginTop: 4,
    color: colors.subtleText,
    fontSize: 13,
  },
  field: {
    gap: 8,
  },
  label: {
    color: colors.text,
    fontSize: 14,
    fontWeight: '700',
  },
  input: {
    borderRadius: 16,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: '#fffdf8',
    color: colors.text,
    fontSize: 15,
    paddingHorizontal: 14,
    paddingVertical: 13,
  },
  multilineInput: {
    minHeight: 84,
    textAlignVertical: 'top',
  },
  helperText: {
    color: colors.subtleText,
    fontSize: 12,
    lineHeight: 18,
  },
  segment: {
    flexDirection: 'row',
    borderRadius: 16,
    backgroundColor: colors.surfaceStrong,
    padding: 4,
  },
  segmentButton: {
    flex: 1,
    borderRadius: 12,
    paddingVertical: 12,
    alignItems: 'center',
  },
  segmentButtonActive: {
    backgroundColor: colors.accent,
  },
  segmentLabel: {
    color: colors.text,
    fontSize: 14,
    fontWeight: '700',
  },
  segmentLabelActive: {
    color: colors.surface,
  },
  iconActions: {
    flexDirection: 'row',
    gap: 10,
  },
  iconActionButton: {
    borderRadius: 14,
    backgroundColor: colors.surfaceStrong,
    paddingHorizontal: 14,
    paddingVertical: 10,
  },
  iconActionLabel: {
    color: colors.text,
    fontSize: 13,
    fontWeight: '700',
  },
  builtinList: {
    flexDirection: 'row',
    gap: 10,
    paddingRight: 12,
  },
  builtinItem: {
    width: 82,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: colors.border,
    paddingVertical: 12,
    paddingHorizontal: 8,
    alignItems: 'center',
    gap: 8,
  },
  builtinItemSelected: {
    borderColor: colors.accent,
    backgroundColor: colors.accentSoft,
  },
  builtinLabel: {
    color: colors.subtleText,
    fontSize: 12,
    fontWeight: '700',
    textAlign: 'center',
  },
  footer: {
    flexDirection: 'row',
    gap: 12,
    paddingTop: 10,
  },
  cancelButton: {
    flex: 1,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: colors.border,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 14,
  },
  cancelLabel: {
    color: colors.text,
    fontSize: 15,
    fontWeight: '700',
  },
  saveButton: {
    flex: 1,
    borderRadius: 18,
    backgroundColor: colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 14,
  },
  saveLabel: {
    color: colors.surface,
    fontSize: 15,
    fontWeight: '800',
  },
});
