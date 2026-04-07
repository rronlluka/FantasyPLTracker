/**
 * Auth-gated entry point.
 * Redirects to /login if no manager ID is saved, otherwise to the main tabs.
 */
import { useEffect } from 'react';
import { View, ActivityIndicator } from 'react-native';
import { useRouter } from 'expo-router';
import { Storage } from '@/utils/storage';
import { Colors } from '@/constants/theme';

export default function IndexScreen() {
  const router = useRouter();

  useEffect(() => {
    let mounted = true;
    Storage.getManagerId().then((id) => {
      if (!mounted) return;
      if (id) {
        router.replace('/(tabs)/leagues');
      } else {
        router.replace('/login');
      }
    });
    return () => { mounted = false; };
  }, []);

  return (
    <View style={{ flex: 1, backgroundColor: Colors.background, justifyContent: 'center', alignItems: 'center' }}>
      <ActivityIndicator color={Colors.primary} size="large" />
    </View>
  );
}
