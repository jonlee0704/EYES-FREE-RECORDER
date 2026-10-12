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

        /** Pro Recording Mode settings (PRD A2). Environment and focus load their defaults. */
        private void bindProRecordingPreferences() {
            String[] keys = {
                    ProRecordingSettings.PREF_ENVIRONMENT,
                    ProRecordingSettings.PREF_FOCUS,
                    ProRecordingSettings.PREF_NOISE_REDUCTION,
                    ProRecordingSettings.PREF_GAIN_MODE,
                    ProRecordingSettings.PREF_GAIN_DB
            };
            for (String key : keys) {
                Preference p = findPreference(key);
                if (p != null) {
                    bindPreferenceSummaryToValue(p);
                }
            }

            Preference proModePref = findPreference(ProRecordingSettings.PREF_PRO_MODE);
            if (proModePref != null) {
                proModePref.setOnPreferenceChangeListener((preference, value) -> {
                    boolean on = Boolean.TRUE.equals(value);
                    vibrate(preference.getContext(), 40);
                    if (on) {
                        ProRecordingSettings pro = ProRecordingSettings.get(preference.getContext());
                        speak("Pro mode on. " + pro.describe());
                    } else {
                        speak("Pro mode off");
                    }
                    return true;
                });
            }

            Preference envPref = findPreference(ProRecordingSettings.PREF_ENVIRONMENT);
            if (envPref != null) {
                envPref.setOnPreferenceChangeListener((preference, value) -> {
                    ProRecordingSettings pro = ProRecordingSettings.get(preference.getContext());
                    pro.setEnvironment(ProRecordingSettings.Environment.fromKey(String.valueOf(value)));
                    setListSummary((ListPreference) preference, String.valueOf(value));
                    refreshProList(ProRecordingSettings.PREF_FOCUS, pro.getFocus().key);
                    refreshProList(ProRecordingSettings.PREF_NOISE_REDUCTION, String.valueOf(pro.getNoiseReduction()));
                    refreshProList(ProRecordingSettings.PREF_GAIN_MODE, pro.getGainMode().key);
                    vibrate(preference.getContext(), 40);
                    speak("Environment: " + pro.describeEnvironmentChange());
                    return true;
                });
            }

            Preference focusPref = findPreference(ProRecordingSettings.PREF_FOCUS);
            if (focusPref != null) {
                focusPref.setOnPreferenceChangeListener((preference, value) -> {
                    ProRecordingSettings pro = ProRecordingSettings.get(preference.getContext());
                    pro.setFocus(ProRecordingSettings.Focus.fromKey(String.valueOf(value)));
                    setListSummary((ListPreference) preference, String.valueOf(value));
                    refreshProList(ProRecordingSettings.PREF_NOISE_REDUCTION, String.valueOf(pro.getNoiseReduction()));
                    vibrate(preference.getContext(), 40);
                    speak(pro.getFocus().spokenName + " focus, noise reduction " + pro.getEffectiveNoiseReduction() + " percent");
                    return true;
                });
            }

            Preference nrPref = findPreference(ProRecordingSettings.PREF_NOISE_REDUCTION);
            if (nrPref != null) {
                nrPref.setOnPreferenceChangeListener((preference, value) -> {
                    setListSummary((ListPreference) preference, String.valueOf(value));
                    ProRecordingSettings pro = ProRecordingSettings.get(preference.getContext());
                    int level = Integer.parseInt(String.valueOf(value));
                    String msg = "Noise reduction " + level + " percent";
                    if (level > pro.getFocus().maxNoiseReduction) {
                        msg += ". " + pro.getFocus().spokenName + " focus is capped at " + pro.getFocus().maxNoiseReduction + " percent";
                    } else if (level == 100 && pro.consumeMaxNoiseReductionWarning()) {
                        msg += ". At maximum, voices may sound thinner";
                    }
                    vibrate(preference.getContext(), 40);
                    speak(msg);
                    return true;
                });
            }

            Preference soundCheckPref = findPreference("pref_pro_sound_check");
            if (soundCheckPref != null) {
                soundCheckPref.setOnPreferenceClickListener(p -> {
                    runSoundCheckFromSettings(p);
                    return true;
                });
            }
        }

        private void setListSummary(ListPreference preference, String value) {
            int index = preference.findIndexOfValue(value);
            if (index >= 0) {
                preference.setSummary(preference.getEntries()[index].toString().replace("%", "%%"));
            }
        }

        private void refreshProList(String key, String value) {
            Preference p = findPreference(key);
            if (p instanceof ListPreference) {
                ((ListPreference) p).setValue(value);
                setListSummary((ListPreference) p, value);
            }
        }

        private void runSoundCheckFromSettings(Preference p) {
            final Context context = requireContext().getApplicationContext();
            final ProRecordingSettings pro = ProRecordingSettings.get(context);
            vibrate(context, 40);
            speak("Sound check. After the vibration, play or speak at your loudest for 5 seconds.");
            p.setSummary("Listening…");
            final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            handler.postDelayed(() -> {
                vibrate(context, 120);
                ProAudioRecorder.runSoundCheck(context, pro, new ProAudioRecorder.SoundCheckCallback() {
                    @Override
                    public void onResult(float peakDbfs, int recommendedChangeDb) {
                        if (!isAdded()) return;
                        vibrate(context, 60);
                        String peak = peakDbfs <= -90f ? "silent" : ProRecordingSettings.formatDb(peakDbfs);
                        if (!pro.getGainMode().isManual()) {
                            String msg = "Peak " + peak + ". Gain is automatic, so the phone sets the level.";
                            p.setSummary(msg);
                            speak(msg);
                            return;
                        }
                        int target = ProRecordingSettings.clampGain(pro.getGainDb() + recommendedChangeDb);
                        int applied = target - pro.getGainDb();
                        if (recommendedChangeDb == 0 || applied == 0) {
                            String msg = "Peak " + peak + ". " + (recommendedChangeDb == 0
                                    ? "Input level is good." : "Input is already at its limit; move the phone.");
                            p.setSummary(msg);
                            speak(msg);
                            return;
                        }
                        String msg = "Peak " + peak + ". Recommend " + (applied < 0 ? "lowering" : "raising")
                                + " input " + Math.abs(applied) + " dB.";
                        p.setSummary(msg);
                        speak(msg + " Choose Apply to set input to " + ProRecordingSettings.formatDb(target) + ".");
                        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("Sound check")
                                .setMessage(msg)
                                .setPositiveButton("Apply", (d, w) -> {
                                    pro.setGainDb(target);
                                    refreshProList(ProRecordingSettings.PREF_GAIN_DB, String.valueOf(target));
                                    speak("Input set to " + ProRecordingSettings.formatDb(target));
                                })
                                .setNegativeButton("Keep current", null)
                                .show();
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded()) return;
                        p.setSummary(message);
                        speak(message);
                    }
                });
            }, 3500);
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
                    "isMATHTESTEnabled",
                    "pref_screen_curtain",
                    ProRecordingSettings.PREF_KEEP_ORIGINAL,
                    ProRecordingSettings.PREF_MONITORING,
                    ProRecordingSettings.PREF_LIMITER_IN_MANUAL
            };
            for (String key : switchPreferenceKeys) {
                Preference p = findPreference(key);
                if (p != null) {
                    p.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
                }
            }

            Preference curtainPref = findPreference("pref_screen_curtain");
            if (curtainPref != null) {
                curtainPref.setOnPreferenceChangeListener((preference, value) -> {
                    boolean handled = sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, value);
                    boolean curtainOn = Boolean.TRUE.equals(value);
                    MainActivity main = MainActivity.getInstance();
                    if (main != null) {
                        if (main.getCommander() != null && main.getCommander().dialView != null) {
                            main.getCommander().dialView.setScreenCurtainEnabled(curtainOn);
                        }
                        main.alwaysSpeak(curtainOn ? "Screen curtain on" : "Screen curtain off");
                    }
                    return handled;
                });
            }

            Preference replayTutorialPref = findPreference("pref_replay_gesture_tutorial");
            if (replayTutorialPref != null) {
                replayTutorialPref.setOnPreferenceClickListener(p -> {
                    speak("Starting gesture tutorial");
                    vibrate(p.getContext(), 40);
                    PreferenceManager.getDefaultSharedPreferences(p.getContext())
                            .edit().putBoolean("pending_replay_gesture_tutorial", true).apply();
                    MainActivity main = MainActivity.getInstance();
                    if (main != null) {
                        main.triggerGestureTutorial();
                    }
                    requireActivity().finish();
                    return true;
                });
            }

            bindProRecordingPreferences();

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
