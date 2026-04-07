import React, { type ReactNode, useEffect, useMemo, useRef } from 'react';
import { View } from 'react-native';
import type { StyleProp, ViewStyle } from 'react-native';
import {
  BottomSheetBackdrop,
  BottomSheetModal,
  type BottomSheetDefaultBackdropProps,
} from '@gorhom/bottom-sheet';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Colors } from '@/constants/theme';

type AppBottomSheetProps = {
  visible: boolean;
  onClose: () => void;
  children: ReactNode;
  snapPoints?: (string | number)[];
  backgroundStyle?: StyleProp<ViewStyle>;
  contentContainerStyle?: StyleProp<ViewStyle>;
  stackBehavior?: 'push' | 'switch' | 'replace';
};

export function AppBottomSheet({
  visible,
  onClose,
  children,
  snapPoints = ['85%'],
  backgroundStyle,
  contentContainerStyle,
  stackBehavior,
}: AppBottomSheetProps) {
  const modalRef = useRef<BottomSheetModal>(null);
  const insets = useSafeAreaInsets();
  const resolvedSnapPoints = useMemo(() => snapPoints, [snapPoints]);

  useEffect(() => {
    if (visible) {
      modalRef.current?.present();
      return;
    }

    modalRef.current?.dismiss();
  }, [visible]);

  const renderBackdrop = (props: BottomSheetDefaultBackdropProps) => (
    <BottomSheetBackdrop
      {...props}
      appearsOnIndex={0}
      disappearsOnIndex={-1}
      opacity={0.6}
      pressBehavior="close"
    />
  );

  return (
    <BottomSheetModal
      ref={modalRef}
      index={0}
      snapPoints={resolvedSnapPoints}
      enableDynamicSizing={false}
      topInset={insets.top}
      enablePanDownToClose
      onDismiss={onClose}
      stackBehavior={stackBehavior}
      backdropComponent={renderBackdrop}
      handleIndicatorStyle={{
        backgroundColor: Colors.outlineVariant,
        width: 40,
        height: 4,
      }}
      backgroundStyle={[
        {
          backgroundColor: Colors.surfaceContainer,
          borderTopLeftRadius: 24,
          borderTopRightRadius: 24,
        },
        backgroundStyle,
      ]}
    >
      <View style={[{ flex: 1, minHeight: 0 }, contentContainerStyle]}>
        {children}
      </View>
    </BottomSheetModal>
  );
}
