package com.jonlee.android.SJplayer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    public static final String KEY_PREF_SYNC_CONN = "pref_syncConnectionType";

    private static TextToSpeech sTts = null;
    private static Vibrator sVibrator = null;
    private static boolean sIsInitialized = false;

    public static void speak(String text) {
        if (sTts != null && text != null && !text.isEmpty()) {
            sTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    public static void vibrate(Context context, long ms) {
        try {
            if (sVibrator == null && context != null) {
                sVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (sVibrator != null && sVibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    sVibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    sVibrator.vibrate(ms);
                }
            }
        } catch (Exception ignored) {}
    }

    private static final Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = (preference, value) -> {
        String stringValue = value != null ? value.toString() : "";

        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(stringValue);
            String entry = index >= 0 ? listPreference.getEntries()[index].toString() : null;
            if (entry != null) {
                preference.setSummary(entry.replace("%", "%%"));
                if (sIsInitialized) {
                    speak(preference.getTitle() + " set to " + entry);
                    vibrate(preference.getContext(), 40);
                }
            } else {
                preference.setSummary(null);
            }
        } else if (preference instanceof SwitchPreference) {
            boolean checked = Boolean.TRUE.equals(value);
            if (sIsInitialized) {
                speak(preference.getTitle() + (checked ? " turned ON" : " turned OFF"));
                vibrate(preference.getContext(), 40);
            }
        } else if (preference instanceof EditTextPreference) {
            preference.setSummary(stringValue);
            if (sIsInitialized) {
                speak(preference.getTitle() + " set to " + stringValue);
                vibrate(preference.getContext(), 40);
            }
        } else {
            preference.setSummary(stringValue);
        }
        return true;
    };

    private static void bindPreferenceSummaryToValue(@NonNull Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
        String storedValue = sharedPreferences.getString(preference.getKey(), "");
        if ((storedValue == null || storedValue.isEmpty()) && preference instanceof ListPreference) {
            storedValue = ((ListPreference) preference).getValue();
        }
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, storedValue != null ? storedValue : "");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        setupActionBar();

        sIsInitialized = false;

        // Initialize TextToSpeech for self-voicing accessibility
        sTts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                sTts.setLanguage(Locale.US);
                speak("Settings opened. Explore options or swipe down to return.");
            }
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, new GeneralPreferenceFragment())
                    .commit();
        }
    }

    @Override
    protected void onDestroy() {
        if (sTts != null) {
            sTts.stop();
            sTts.shutdown();
            sTts = null;
        }
        sIsInitialized = false;
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        speak("Settings closed. Returning to player.");
        vibrate(this, 50);
        super.onBackPressed();
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.title_activity_settings);
        }
    }

    public static class GeneralPreferenceFragment extends PreferenceFragmentCompat {

        private ActivityResultLauncher<Intent> folderPickerLauncher;
        private ActivityResultLauncher<Intent> importFolderPickerLauncher;
        private Preference targetFoldersPreference;
        private Preference importFolderPreference;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            folderPickerLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                            Intent data = result.getData();
                            if (data != null) {
                                Uri uri = data.getData();
                                if (uri != null) {
                                    final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                                    requireContext().getContentResolver().takePersistableUriPermission(uri, takeFlags);

                                    String folderPath = uri.toString();
                                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
                                    sharedPreferences.edit().putString("target_folders", folderPath).apply();

                                    MainActivity main = MainActivity.getInstance();
                                    if (main != null && main.getRecorder() != null) {
                                        main.getRecorder().readDirectories();
                                        targetFoldersPreference.setSummary(main.getRecorder().getAllTargetFoldersSummary());
                                    } else {
                                        targetFoldersPreference.setSummary(folderPath);
                                    }
                                    speak("Folder selected: " + folderPath);
                                }
                            }
                        }
                    });

            importFolderPickerLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                            Intent data = result.getData();
                            if (data != null) {
                                Uri uri = data.getData();
                                if (uri != null) {
                                    final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                                    requireContext().getContentResolver().takePersistableUriPermission(uri, takeFlags);

                                    MainActivity main = MainActivity.getInstance();
                                    if (main != null && main.getRecorder() != null) {
                                        main.getRecorder().importFolder(uri);
                                    }
                                    speak("Importing selected folder.");
                                }
                            }
                        }
                    });
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireContext());
            if (!sp.contains("pref_poking_eyes_default_off_migrated")) {
                String currentPoking = sp.getString("poking_eye_warning", "0");
                if ("31".equals(currentPoking) || "10".equals(currentPoking) || !sp.contains("poking_eye_warning")) {
                    sp.edit().putString("poking_eye_warning", "0").apply();
                }
                sp.edit().putBoolean("pref_poking_eyes_default_off_migrated", true).apply();
            }
            if (!sp.contains("pref_math_test_default_off_migrated")) {
                sp.edit()
                        .putBoolean("isMATHTESTEnabled", false)
                        .putBoolean("pref_math_test_default_off_migrated", true)
                        .apply();
            }

            setPreferencesFromResource(R.xml.pref_general, rootKey);
            setHasOptionsMenu(true);

            // Group 1: Voice & Audio Recording
            Preference welcomePref = findPreference("pref_download_welcome");
            if (welcomePref != null) {
                welcomePref.setOnPreferenceClickListener(p -> {
                    speak("Download Spoken Welcome Guide");
                    vibrate(p.getContext(), 30);
                    Toast.makeText(requireContext(), "Downloading welcome message...", Toast.LENGTH_SHORT).show();
                    return true;
                });
            }

            // Bind ListPreferences
            String[] listPreferenceKeys = {
                    "pref_audio_quality",
                    "max_volume",
                    "poking_eye_warning",
                    "sync_frequency",
                    "mathtest_mode",
                    "mathtest_frequency",
                    "mathtest_count_on_start",
                    "mathtest_count_after_start",
                    "pref_visualizer_mode_str"
            };
            for (String key : listPreferenceKeys) {
                Preference p = findPreference(key);
                if (p != null) {
                    bindPreferenceSummaryToValue(p);
                }
            }

            // Visualizer & Animation mode live listener
            Preference visualizerModePref = findPreference("pref_visualizer_mode_str");
            if (visualizerModePref != null) {
                visualizerModePref.setOnPreferenceChangeListener((preference, value) -> {
                    boolean handled = sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, value);
                    try {
                        int mode = Integer.parseInt(value.toString());
                        PreferenceManager.getDefaultSharedPreferences(preference.getContext())
                                .edit().putInt("pref_visualizer_mode", mode).apply();
                        MainActivity main = MainActivity.getInstance();
                        if (main != null && main.getCommander() != null && main.getCommander().dialView != null) {
                            main.getCommander().dialView.setVisualizerMode(mode);
                            main.updateVisualizerModeUI(mode);
                        }
                    } catch (Exception ignored) {}
                    return handled;
                });
            }

            // Stereo Recording Preference
            Preference stereoPref = findPreference("pref_stereo_recording");
            if (stereoPref != null) {
                boolean stereoSupported = AudioMetadataHelper.isStereoRecordingSupported(requireContext());
                if (stereoSupported) {
                    stereoPref.setSummary("Supported by device • Multiple microphones detected");
                } else {
                    stereoPref.setSummary(getString(R.string.pref_summary_stereo_not_supported));
                    stereoPref.setEnabled(false);
                }
            }

            // Bind SwitchPreferences for change feedback
            String[] switchPreferenceKeys = {
                    "isTTSEnabled",
                    "pref_stereo_recording",
                    "pref_noise_suppression",
                    "isNavModeEnabled",
                    "isAutoSyncEnabled",
                    "isMATHTESTEnabled"
            };
            for (String key : switchPreferenceKeys) {
                Preference p = findPreference(key);
                if (p != null) {
                    p.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
                }
            }

            // Bind EditTextPreferences
            String[] editTextKeys = {
                    "mathtest_starting_number",
                    "mathtest_ending_number",
                    "mathtest_multiplytest_scope"
            };
            for (String key : editTextKeys) {
                Preference p = findPreference(key);
                if (p != null) {
                    bindPreferenceSummaryToValue(p);
                }
            }

            // Group 3: Storage & Cloud Backup
            Preference manualBackupPref = findPreference("manual_backup_action");
            if (manualBackupPref != null) {
                manualBackupPref.setOnPreferenceClickListener(p -> {
                    speak("Opening Google Drive Backup.");
                    vibrate(p.getContext(), 40);
                    startActivity(new Intent(requireContext(), DriveMainActivity.class));
                    return true;
                });
            }

            targetFoldersPreference = findPreference("target_folders");
            if (targetFoldersPreference != null) {
                MainActivity main = MainActivity.getInstance();
                if (main != null && main.getRecorder() != null) {
                    targetFoldersPreference.setSummary(main.getRecorder().getAllTargetFoldersSummary());
                }
                targetFoldersPreference.setOnPreferenceClickListener(preference -> {
                    speak("Opening folder selection.");
                    vibrate(preference.getContext(), 30);
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    folderPickerLauncher.launch(intent);
                    return true;
                });
            }

            importFolderPreference = findPreference("import_folder");
            if (importFolderPreference != null) {
                importFolderPreference.setOnPreferenceClickListener(preference -> {
                    speak("Opening folder import.");
                    vibrate(preference.getContext(), 30);
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    importFolderPickerLauncher.launch(intent);
                    return true;
                });
            }

            // Group 6: Legal & Privacy
            Preference tosPref = findPreference("pref_view_tos");
            if (tosPref != null) {
                tosPref.setOnPreferenceClickListener(preference -> {
                    speak("Opening Terms of Service.");
                    vibrate(preference.getContext(), 30);
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(MainActivity.TOS_ONLINE_URL));
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Could not open browser: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    return true;
                });
            }

            Preference privacyPref = findPreference("pref_view_privacy_policy");
            if (privacyPref != null) {
                privacyPref.setOnPreferenceClickListener(preference -> {
                    speak("Opening Privacy Policy.");
                    vibrate(preference.getContext(), 30);
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(MainActivity.PRIVACY_ONLINE_URL));
                    startActivity(intent);
                    return true;
                });
            }
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            // Allow initial population before enabling speech on changes
            view.postDelayed(() -> sIsInitialized = true, 500);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                speak("Settings closed. Returning to player.");
                vibrate(requireContext(), 50);
                getActivity().onBackPressed();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
