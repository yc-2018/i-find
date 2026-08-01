import { useEffect, useMemo, useState } from 'react';
import { Alert, Pressable, StyleSheet, Switch, Text, View } from 'react-native';
import { useRouter } from 'expo-router';
import DraggableFlatList, { RenderItemParams } from 'react-native-draggable-flatlist';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';

import { ScreenHeader } from '../src/components/ScreenHeader';
import { SettingsTargetRow } from '../src/components/SettingsTargetRow';
import { TargetFormModal } from '../src/components/TargetFormModal';
import { colors } from '../src/constants/colors';
import {
  type EditableSearchTarget,
  useSearchTargets,
} from '../src/context/search-targets-context';
import { useStopappSettings } from '../src/context/stopapp-context';
import type { SearchTarget } from '../src/types/search-targets';
import { notify } from '../src/utils/notify';
import { sortTargetsBySortOrder } from '../src/utils/search-targets';

const COPY = {
  title: '\u914d\u7f6e\u641c\u7d22\u9879',
  subtitle:
    '\u62d6\u62fd\u6392\u5e8f\u3001\u9690\u85cf\u6216\u7f16\u8f91\u641c\u7d22\u5165\u53e3\uff0c\u540c\u65f6\u7ba1\u7406 Shizuku \u76f4\u89e3\u51bb\u3002',
  add: '\u65b0\u589e',
  restoreDefaults: '\u6062\u590d\u9ed8\u8ba4',
  addTarget: '\u6dfb\u52a0\u641c\u7d22\u9879',
  sectionTitle: '\u641c\u7d22\u9879\u5217\u8868',
  shizukuTitle: 'Shizuku \u76f4\u89e3\u51bb',
  statusChecking: '\u68c0\u6d4b\u4e2d',
  statusNotInstalled: '\u672a\u5b89\u88c5',
  statusUnavailable: '\u4e0d\u53ef\u7528',
  statusReady: '\u5df2\u5c31\u7eea',
  statusNeedPermission: '\u7b49\u5f85\u6388\u6743',
  statusNeedService: '\u672a\u542f\u52a8',
  statusCheckingBody: '\u6b63\u5728\u68c0\u6d4b Shizuku \u7684\u5b89\u88c5\u3001\u670d\u52a1\u548c\u6388\u6743\u72b6\u6001\u3002',
  statusNotInstalledBody:
    '\u672a\u68c0\u6d4b\u5230 Shizuku\u3002\u5b89\u88c5\u540e\uff0c\u5c31\u53ef\u4ee5\u7528 ADB \u6216 root \u6388\u6743\u6765\u5c1d\u8bd5\u76f4\u63a5\u89e3\u51bb App\u3002',
  statusUnavailableBody:
    '\u5f53\u524d\u8fd0\u884c\u73af\u5883\u8fd8\u6ca1\u52a0\u8f7d Android \u539f\u751f Shizuku \u6a21\u5757\uff0c\u9700\u8981\u7528 dev build \u6216 APK \u9a8c\u8bc1\u3002',
  statusNeedServiceBody:
    'Shizuku \u5df2\u5b89\u88c5\uff0c\u4f46\u8fd8\u6ca1\u6709\u542f\u52a8\u670d\u52a1\u3002\u6253\u5f00 Shizuku \u540e\uff0c\u5148\u6309\u5b83\u7684\u63d0\u793a\u542f\u52a8\u670d\u52a1\u3002',
  statusNeedPermissionBody:
    'Shizuku \u670d\u52a1\u5df2\u7ecf\u8fd0\u884c\uff0c\u4f46 I find \u8fd8\u6ca1\u62ff\u5230 Shizuku \u6388\u6743\u3002\u6388\u6743\u540e\u624d\u80fd\u76f4\u63a5\u53d1\u9001\u89e3\u51bb\u547d\u4ee4\u3002',
  statusReadyBody:
    'Shizuku \u5df2\u5c31\u7eea\u3002\u5f00\u542f\u540e\uff0c\u70b9\u51fb\u6df1\u94fe\u641c\u7d22\u4f1a\u5148\u5c1d\u8bd5\u76f4\u63a5\u89e3\u51bb\u76ee\u6807 App\u3002',
  autoDefrost: '\u81ea\u52a8\u89e3\u51bb',
  autoDefrostHintUnavailable:
    '\u53ea\u6709\u5728 Shizuku \u670d\u52a1\u5df2\u8fd0\u884c\u4e14\u6388\u6743\u6210\u529f\u540e\uff0c\u624d\u80fd\u5f00\u542f\u8fd9\u4e2a\u5f00\u5173\u3002',
  autoDefrostHintReady:
    '\u5f00\u542f\u540e\uff0c\u6bcf\u6b21\u70b9\u51fb\u6df1\u94fe\u641c\u7d22\u90fd\u4f1a\u5148\u5c1d\u8bd5 unsuspend / unhide / enable \u547d\u4ee4\u3002',
  refreshStatus: '\u5237\u65b0\u72b6\u6001',
  openShizuku: '\u6253\u5f00 Shizuku',
  requestPermission: '\u6388\u6743 Shizuku',
  packageName: 'moe.shizuku.privileged.api',
  packageLabel: 'Package',
  versionLabel: '\u7248\u672c',
  launcherLabel: '\u7ba1\u7406\u5668',
  supportLabel: '\u89e3\u51bb\u80fd\u529b',
  backendLabel: '\u5de5\u4f5c\u6a21\u5f0f',
  available: '\u53ef\u6253\u5f00',
  unavailable: '\u4e0d\u53ef\u6253\u5f00',
  supported: '\u5df2\u53ef\u7528',
  unsupported: '\u672a\u5c31\u7eea',
  backendAdb: 'ADB',
  backendRoot: 'root',
  backendUnknown: '\u672a\u77e5',
  backendUnavailable: '--',
  permissionGranted: '\u5df2\u6388\u6743',
  permissionMissing: '\u672a\u6388\u6743',
  serviceRunning: '\u8fd0\u884c\u4e2d',
  serviceStopped: '\u672a\u8fd0\u884c',
  permissionLabel: '\u6388\u6743',
  serviceLabel: '\u670d\u52a1',
  saveUpdated: '\u641c\u7d22\u9879\u5df2\u66f4\u65b0',
  saveAdded: '\u641c\u7d22\u9879\u5df2\u6dfb\u52a0',
  deleteTitle: '\u5220\u9664\u641c\u7d22\u9879',
  deleteMessagePrefix: '\u786e\u5b9a\u8981\u5220\u9664\u201c',
  deleteMessageSuffix: '\u201d\u5417\uff1f',
  cancel: '\u53d6\u6d88',
  delete: '\u5220\u9664',
  deleted: '\u641c\u7d22\u9879\u5df2\u5220\u9664',
  restoreTitle: '\u6062\u590d\u9ed8\u8ba4\u641c\u7d22\u9879',
  restoreMessage:
    '\u8fd9\u4f1a\u6e05\u7a7a\u5f53\u524d\u81ea\u5b9a\u4e49\u6392\u5e8f\u548c\u65b0\u589e\u9879\uff0c\u786e\u5b9a\u7ee7\u7eed\u5417\uff1f',
  restore: '\u6062\u590d',
  restored: '\u5df2\u6062\u590d\u9ed8\u8ba4\u641c\u7d22\u9879',
  openShizukuFailed: '\u6ca1\u6709\u6210\u529f\u6253\u5f00 Shizuku',
  permissionGrantedToast: 'Shizuku \u6388\u6743\u6210\u529f',
  permissionDeniedToast: 'Shizuku \u6388\u6743\u672a\u6210\u529f',
  enableAutoDefrostUnavailable:
    '\u8bf7\u5148\u786e\u4fdd Shizuku \u670d\u52a1\u5df2\u542f\u52a8\uff0c\u5e76\u4e14 I find \u5df2\u6388\u6743',
  emptyReady: '\u8fd8\u6ca1\u6709\u641c\u7d22\u9879',
  emptyLoading: '\u6b63\u5728\u8bfb\u53d6\u4f60\u7684\u641c\u7d22\u9879...',
};

function getStatusPresentation(
  isReady: boolean,
  status: ReturnType<typeof useStopappSettings>['status']
) {
  if (!isReady) {
    return {
      label: COPY.statusChecking,
      body: COPY.statusCheckingBody,
      color: colors.accent,
      backgroundColor: colors.accentSoft,
    };
  }

  if (status.reason === 'native_module_unavailable') {
    return {
      label: COPY.statusUnavailable,
      body: COPY.statusUnavailableBody,
      color: colors.danger,
      backgroundColor: '#f7dfde',
    };
  }

  if (status.availability === 'not_installed') {
    return {
      label: COPY.statusNotInstalled,
      body: COPY.statusNotInstalledBody,
      color: colors.subtleText,
      backgroundColor: colors.surfaceStrong,
    };
  }

  if (!status.serviceRunning) {
    return {
      label: COPY.statusNeedService,
      body: COPY.statusNeedServiceBody,
      color: colors.accent,
      backgroundColor: colors.accentSoft,
    };
  }

  if (!status.permissionGranted) {
    return {
      label: COPY.statusNeedPermission,
      body: COPY.statusNeedPermissionBody,
      color: colors.accent,
      backgroundColor: colors.accentSoft,
    };
  }

  return {
    label: COPY.statusReady,
    body: COPY.statusReadyBody,
    color: colors.success,
    backgroundColor: '#d7ecdf',
  };
}

function resolveBackendLabel(backendMode: ReturnType<typeof useStopappSettings>['status']['backendMode']) {
  if (backendMode === 'adb') {
    return COPY.backendAdb;
  }

  if (backendMode === 'root') {
    return COPY.backendRoot;
  }

  if (backendMode === 'unknown') {
    return COPY.backendUnknown;
  }

  return COPY.backendUnavailable;
}

export default function SettingsScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const {
    isReady,
    targets,
    deleteTarget,
    restoreDefaults,
    saveTarget,
    setTargetHidden,
    reorderTargets,
  } = useSearchTargets();
  const {
    isReady: isShizukuReady,
    autoDefrostEnabled,
    canAutoDefrost,
    openManager,
    refreshStatus,
    requestPermission,
    setAutoDefrostEnabled,
    status,
  } = useStopappSettings();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingTarget, setEditingTarget] = useState<SearchTarget | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [draftTargets, setDraftTargets] = useState<SearchTarget[]>(() =>
    sortTargetsBySortOrder(targets)
  );

  const sortedTargets = useMemo(() => sortTargetsBySortOrder(targets), [targets]);
  const statusPresentation = useMemo(
    () => getStatusPresentation(isShizukuReady, status),
    [isShizukuReady, status]
  );
  const listBottomSpacing = useMemo(() => Math.max(insets.bottom, 20) + 52, [insets.bottom]);
  const needsPermission = status.serviceRunning && !status.permissionGranted;
  const canOpenShizuku = status.installed && status.launchable;
  const actionLabel = needsPermission ? COPY.requestPermission : COPY.openShizuku;

  useEffect(() => {
    if (isDragging) {
      return;
    }

    setDraftTargets(sortedTargets);
  }, [isDragging, sortedTargets]);

  useEffect(() => {
    void refreshStatus();
  }, [refreshStatus]);

  const openCreateModal = () => {
    setEditingTarget(null);
    setIsModalOpen(true);
  };

  const openEditModal = (target: SearchTarget) => {
    setEditingTarget(target);
    setIsModalOpen(true);
  };

  const closeModal = () => {
    setEditingTarget(null);
    setIsModalOpen(false);
  };

  const handleSave = async (draft: EditableSearchTarget) => {
    await saveTarget(draft, editingTarget?.id ?? null);
    notify(editingTarget ? COPY.saveUpdated : COPY.saveAdded);
    closeModal();
  };

  const handleDelete = (target: SearchTarget) => {
    Alert.alert(
      COPY.deleteTitle,
      `${COPY.deleteMessagePrefix}${target.name}${COPY.deleteMessageSuffix}`,
      [
        { text: COPY.cancel, style: 'cancel' },
        {
          text: COPY.delete,
          style: 'destructive',
          onPress: () => {
            deleteTarget(target.id);
            notify(COPY.deleted);
          },
        },
      ]
    );
  };

  const handleRestoreDefaults = () => {
    Alert.alert(COPY.restoreTitle, COPY.restoreMessage, [
      { text: COPY.cancel, style: 'cancel' },
      {
        text: COPY.restore,
        style: 'destructive',
        onPress: () => {
          restoreDefaults();
          notify(COPY.restored);
        },
      },
    ]);
  };

  const handleToggleAutoDefrost = async (enabled: boolean) => {
    if (enabled && !canAutoDefrost) {
      notify(COPY.enableAutoDefrostUnavailable);
      return;
    }

    await setAutoDefrostEnabled(enabled);
  };

  const handlePrimaryAction = async () => {
    if (needsPermission) {
      const result = await requestPermission();
      notify(result.granted ? COPY.permissionGrantedToast : COPY.permissionDeniedToast);
      return;
    }

    const result = await openManager();
    if (!result.opened) {
      notify(COPY.openShizukuFailed);
    }
  };

  const renderItem = ({ item, drag, isActive }: RenderItemParams<SearchTarget>) => (
    <SettingsTargetRow
      isActive={isActive}
      isDragging={isDragging}
      onDelete={() => handleDelete(item)}
      onDragStart={drag}
      onEdit={() => openEditModal(item)}
      onToggleHidden={(nextHidden) => setTargetHidden(item.id, nextHidden)}
      target={item}
    />
  );

  return (
    <SafeAreaView edges={['top', 'bottom']} style={styles.safeArea}>
      <View style={styles.container}>
        <DraggableFlatList
          activationDistance={12}
          autoscrollSpeed={40}
          autoscrollThreshold={28}
          containerStyle={styles.list}
          contentContainerStyle={[styles.listContent, { paddingBottom: listBottomSpacing }]}
          data={draftTargets}
          dragItemOverflow={false}
          keyExtractor={(item) => item.id}
          ListEmptyComponent={
            <View style={styles.emptyState}>
              <Text style={styles.emptyTitle}>
                {isReady ? COPY.emptyReady : COPY.emptyLoading}
              </Text>
            </View>
          }
          ListFooterComponent={<View style={styles.footerSpacer} />}
          ListHeaderComponent={
            <View style={styles.headerContent}>
              <ScreenHeader
                onBack={() => router.back()}
                rightAction={
                  <Pressable onPress={openCreateModal} style={styles.headerAction}>
                    <Text style={styles.headerActionLabel}>{COPY.add}</Text>
                  </Pressable>
                }
                subtitle={COPY.subtitle}
                title={COPY.title}
              />

              <View style={styles.statusCard}>
                <View style={styles.statusTopRow}>
                  <Text style={styles.statusTitle}>{COPY.shizukuTitle}</Text>
                  <View
                    style={[
                      styles.statusPill,
                      { backgroundColor: statusPresentation.backgroundColor },
                    ]}
                  >
                    <Text style={[styles.statusPillLabel, { color: statusPresentation.color }]}>
                      {statusPresentation.label}
                    </Text>
                  </View>
                </View>

                <Text style={styles.statusBody}>{statusPresentation.body}</Text>

                <View style={styles.switchRow}>
                  <View style={styles.switchMeta}>
                    <Text style={styles.switchTitle}>{COPY.autoDefrost}</Text>
                    <Text style={styles.switchHint}>
                      {canAutoDefrost
                        ? COPY.autoDefrostHintReady
                        : COPY.autoDefrostHintUnavailable}
                    </Text>
                  </View>
                  <Switch
                    disabled={!canAutoDefrost}
                    onValueChange={(value) => {
                      void handleToggleAutoDefrost(value);
                    }}
                    thumbColor={colors.surface}
                    trackColor={{ false: colors.hidden, true: colors.success }}
                    value={autoDefrostEnabled}
                  />
                </View>

                <View style={styles.actionRow}>
                  <Pressable
                    onPress={() => {
                      void refreshStatus();
                    }}
                    style={styles.secondaryButton}
                  >
                    <Text style={styles.secondaryButtonLabel}>{COPY.refreshStatus}</Text>
                  </Pressable>
                  <Pressable
                    disabled={!needsPermission && !canOpenShizuku}
                    onPress={() => {
                      void handlePrimaryAction();
                    }}
                    style={[
                      styles.primaryButton,
                      !needsPermission && !canOpenShizuku && styles.disabledButton,
                    ]}
                  >
                    <Text style={styles.primaryButtonLabel}>{actionLabel}</Text>
                  </Pressable>
                </View>

                <View style={styles.diagnosticsGrid}>
                  <View style={styles.diagnosticItem}>
                    <Text style={styles.diagnosticLabel}>{COPY.packageLabel}</Text>
                    <Text numberOfLines={1} style={styles.diagnosticValue}>
                      {COPY.packageName}
                    </Text>
                  </View>
                  <View style={styles.diagnosticItem}>
                    <Text style={styles.diagnosticLabel}>{COPY.versionLabel}</Text>
                    <Text style={styles.diagnosticValue}>{status.versionName ?? '--'}</Text>
                  </View>
                  <View style={styles.diagnosticItem}>
                    <Text style={styles.diagnosticLabel}>{COPY.launcherLabel}</Text>
                    <Text style={styles.diagnosticValue}>
                      {status.launchable ? COPY.available : COPY.unavailable}
                    </Text>
                  </View>
                  <View style={styles.diagnosticItem}>
                    <Text style={styles.diagnosticLabel}>{COPY.supportLabel}</Text>
                    <Text style={styles.diagnosticValue}>
                      {status.publicDefrostSupported ? COPY.supported : COPY.unsupported}
                    </Text>
                  </View>
                  <View style={styles.diagnosticItem}>
                    <Text style={styles.diagnosticLabel}>{COPY.serviceLabel}</Text>
                    <Text style={styles.diagnosticValue}>
                      {status.serviceRunning ? COPY.serviceRunning : COPY.serviceStopped}
                    </Text>
                  </View>
                  <View style={styles.diagnosticItem}>
                    <Text style={styles.diagnosticLabel}>{COPY.permissionLabel}</Text>
                    <Text style={styles.diagnosticValue}>
                      {status.permissionGranted ? COPY.permissionGranted : COPY.permissionMissing}
                    </Text>
                  </View>
                  <View style={styles.diagnosticItem}>
                    <Text style={styles.diagnosticLabel}>{COPY.backendLabel}</Text>
                    <Text style={styles.diagnosticValue}>
                      {resolveBackendLabel(status.backendMode)}
                    </Text>
                  </View>
                  <View style={styles.diagnosticItem}>
                    <Text style={styles.diagnosticLabel}>Reason</Text>
                    <Text numberOfLines={2} style={styles.diagnosticValue}>
                      {status.reason}
                    </Text>
                  </View>
                </View>
              </View>

              <View style={styles.quickActions}>
                <Pressable onPress={handleRestoreDefaults} style={styles.secondaryButton}>
                  <Text style={styles.secondaryButtonLabel}>{COPY.restoreDefaults}</Text>
                </Pressable>
                <Pressable onPress={openCreateModal} style={styles.primaryButton}>
                  <Text style={styles.primaryButtonLabel}>{COPY.addTarget}</Text>
                </Pressable>
              </View>

              <Text style={styles.sectionTitle}>{COPY.sectionTitle}</Text>
            </View>
          }
          onDragBegin={() => setIsDragging(true)}
          onDragEnd={({ data }) => {
            setIsDragging(false);
            setDraftTargets(data);
            reorderTargets(data);
          }}
          removeClippedSubviews={false}
          renderItem={renderItem}
          showsVerticalScrollIndicator={false}
        />

        <TargetFormModal
          onClose={closeModal}
          onSubmit={handleSave}
          open={isModalOpen}
          target={editingTarget}
        />
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: colors.canvas,
  },
  container: {
    flex: 1,
    paddingHorizontal: 16,
    paddingTop: 10,
  },
  list: {
    flex: 1,
  },
  listContent: {
    paddingBottom: 12,
  },
  headerContent: {
    paddingBottom: 10,
  },
  headerAction: {
    borderRadius: 999,
    backgroundColor: colors.surfaceStrong,
    paddingHorizontal: 14,
    paddingVertical: 8,
  },
  headerActionLabel: {
    color: colors.accent,
    fontSize: 14,
    fontWeight: '700',
  },
  statusCard: {
    borderRadius: 24,
    backgroundColor: colors.surface,
    padding: 16,
    shadowColor: colors.shadow,
    shadowOpacity: 0.06,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 8 },
    elevation: 2,
  },
  statusTopRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 12,
  },
  statusTitle: {
    flex: 1,
    color: colors.text,
    fontSize: 18,
    fontWeight: '800',
  },
  statusPill: {
    borderRadius: 999,
    paddingHorizontal: 12,
    paddingVertical: 6,
  },
  statusPillLabel: {
    fontSize: 12,
    fontWeight: '800',
  },
  statusBody: {
    marginTop: 10,
    color: colors.subtleText,
    fontSize: 14,
    lineHeight: 21,
  },
  switchRow: {
    marginTop: 16,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 12,
  },
  switchMeta: {
    flex: 1,
    paddingRight: 8,
  },
  switchTitle: {
    color: colors.text,
    fontSize: 15,
    fontWeight: '800',
  },
  switchHint: {
    marginTop: 4,
    color: colors.subtleText,
    fontSize: 12,
    lineHeight: 18,
  },
  actionRow: {
    flexDirection: 'row',
    gap: 12,
    marginTop: 16,
  },
  quickActions: {
    flexDirection: 'row',
    gap: 12,
    marginTop: 14,
    marginBottom: 16,
  },
  secondaryButton: {
    flex: 1,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 14,
  },
  secondaryButtonLabel: {
    color: colors.text,
    fontSize: 14,
    fontWeight: '700',
  },
  primaryButton: {
    flex: 1,
    borderRadius: 18,
    backgroundColor: colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 14,
  },
  primaryButtonLabel: {
    color: colors.surface,
    fontSize: 14,
    fontWeight: '800',
  },
  disabledButton: {
    opacity: 0.45,
  },
  diagnosticsGrid: {
    marginTop: 16,
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 10,
  },
  diagnosticItem: {
    width: '47%',
    borderRadius: 16,
    backgroundColor: colors.surfaceStrong,
    paddingHorizontal: 12,
    paddingVertical: 10,
  },
  diagnosticLabel: {
    color: colors.subtleText,
    fontSize: 12,
    fontWeight: '700',
  },
  diagnosticValue: {
    marginTop: 4,
    color: colors.text,
    fontSize: 13,
    fontWeight: '700',
  },
  sectionTitle: {
    color: colors.text,
    fontSize: 17,
    fontWeight: '800',
    marginBottom: 12,
  },
  emptyState: {
    marginTop: 56,
    alignItems: 'center',
  },
  emptyTitle: {
    color: colors.text,
    fontSize: 17,
    fontWeight: '700',
    textAlign: 'center',
  },
  footerSpacer: {
    height: 12,
  },
});
