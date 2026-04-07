/**
 * Simple persistent key-value store backed by expo-file-system.
 * Matches the PreferencesManager in the Android Kotlin app.
 */
import { File, Paths } from 'expo-file-system';

const PREFS_FILE = new File(Paths.document, 'fpl_prefs.json');

type PrefsData = {
  managerId?: string;
  backendUrl?: string;
  favoriteLeagueId?: string;
  favoriteLeagueName?: string;
  selectedLeagueId?: string;
  selectedLeagueName?: string;
};

async function readPrefs(): Promise<PrefsData> {
  try {
    if (!PREFS_FILE.exists) return {};
    const raw = await PREFS_FILE.text();
    return JSON.parse(raw) as PrefsData;
  } catch {
    return {};
  }
}

async function writePrefs(data: PrefsData): Promise<void> {
  if (!PREFS_FILE.exists) {
    PREFS_FILE.create({ intermediates: true, overwrite: true });
  }
  PREFS_FILE.write(JSON.stringify(data));
}

export const Storage = {
  async getManagerId(): Promise<number | null> {
    const prefs = await readPrefs();
    return prefs.managerId ? parseInt(prefs.managerId, 10) : null;
  },

  async saveManagerId(id: number): Promise<void> {
    const prefs = await readPrefs();
    await writePrefs({ ...prefs, managerId: String(id) });
  },

  async getBackendUrl(): Promise<string | null> {
    const prefs = await readPrefs();
    return prefs.backendUrl ?? null;
  },

  async saveBackendUrl(url: string): Promise<void> {
    const prefs = await readPrefs();
    await writePrefs({ ...prefs, backendUrl: url });
  },

  async getFavoriteLeagueId(): Promise<number | null> {
    const prefs = await readPrefs();
    return prefs.favoriteLeagueId ? parseInt(prefs.favoriteLeagueId, 10) : null;
  },

  async getFavoriteLeagueName(): Promise<string | null> {
    const prefs = await readPrefs();
    return prefs.favoriteLeagueName ?? null;
  },

  async saveFavoriteLeague(id: number, name: string): Promise<void> {
    const prefs = await readPrefs();
    await writePrefs({ ...prefs, favoriteLeagueId: String(id), favoriteLeagueName: name });
  },

  async removeFavoriteLeague(): Promise<void> {
    const prefs = await readPrefs();
    const { favoriteLeagueId: _a, favoriteLeagueName: _b, ...rest } = prefs;
    await writePrefs(rest);
  },

  async getSelectedLeagueId(): Promise<number | null> {
    const prefs = await readPrefs();
    return prefs.selectedLeagueId ? parseInt(prefs.selectedLeagueId, 10) : null;
  },

  async getSelectedLeagueName(): Promise<string | null> {
    const prefs = await readPrefs();
    return prefs.selectedLeagueName ?? null;
  },

  async saveSelectedLeague(id: number, name: string): Promise<void> {
    const prefs = await readPrefs();
    await writePrefs({ ...prefs, selectedLeagueId: String(id), selectedLeagueName: name });
  },

  async clearSelectedLeague(): Promise<void> {
    const prefs = await readPrefs();
    const { selectedLeagueId: _a, selectedLeagueName: _b, ...rest } = prefs;
    await writePrefs(rest);
  },

  async clearAll(): Promise<void> {
    await writePrefs({});
  },
};
