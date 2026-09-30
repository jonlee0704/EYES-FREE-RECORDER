package com.jonlee.android.SJplayer;

/**
 * Created by jongyeong on 7/12/16.
 */

import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import androidx.core.app.NotificationCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Map;

import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.jonlee.android.common.utils.FileUtils;
import com.jonlee.android.common.utils.MimeUtils;

import static com.jonlee.android.SJplayer.Recorder.SHARED_FOLDER;


public class DriveMainActivity extends AppCompatActivity {
    private static final String TAG = "DriveMainActivity";
    public static final String BACKUP_ROOT_FOLDER = "SJ-RECORDER-DATA";
    public static final String SUFFIX_FOLDER_CREATED_BY_SJPLAYER = "--CREATED-BY-SJPLAYER";
    /**
     * ANY FILES UNDER SHARED_FOLDER will be downloaded to device
     */

    static final int REQUEST_SIGN_IN = 1;
    static final int RESULT_STORE_FILE = 4;
    static final String FOLDER_OTHERS = "Others";

    //RESULT
    static final int SUCCESS = 5;
    static final int FAIL = 6;
    static final int WORKING = 7;
    static final int DONE = 8;

    private static Uri mFileUri;
    private static Drive mService;
    private GoogleSignInClient mGoogleSignInClient;
    private Context mContext;
    private List<File> mResultList;
    private List<BackupFileResult> mBackupFileResult;
    private ListView mListView;
    private RecyclerView recyclerView;
    private TextView mMainTextView;
    private String[] mFileArray;
    private String mDLVal;
    //    private ArrayAdapter<BackupFileResult> mAdapter;
    private SJBackupFileAdapter mSharedFileAdapter;
    private BackupFileResult[] mBackupFileResultArray;
    private BackupFileResult[] mSharedFileResultArray;


//    private List<String>            mFolderNameinLocalStorage;

    private BackupRecorder recorderForBackup;

    private String statusMsg;
    private String downloadButtonStatusMsg;

    private String backupDriveId;
    private String sharedDriveId;

    //Date-Time will be appended.
    private FileWriter fileWriter;
    private final static String LOGFILE = "BACKUP_LOG";
    //LOG File
    private java.io.File gpxfile;

    //SUCCESS Log to know if folder is already backup
    private final static String SUCCESSLOGFILE = "SUCCESS_LOG.txt";
    private java.io.File successLogFile;
    private Map<String, String> successLogMap;
    private FileWriter successLogFileWriter;

    private StringBuffer errorLogs;

    private final int UPLOAD_MODE = 0;
    private final int DOWNLOAD_MODE = 1;
    private int backup_mode = UPLOAD_MODE;
    private boolean isFinishLoadSharedFiles = false;
    private boolean isFinishLoadUploadFiles = false;
    private ProgressBar pb;
    private int progressBarVisibility = ProgressBar.INVISIBLE;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mRecyclerAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private Button button_backup_start;
    private Button button_download_start;
    private TextToSpeech mTTS = null;
    private TextView mAccountInfoTextView;
    private TextView mAccountActionTextView;
    private TextView mModeTitleTextView;
    private View mAccountStatusBar;
    private ImageButton mButtonBack;

    private int isDownloading = 0;

    private Recorder recorder;
    private MainActivity mainActivity;

    /**
     * TODO: Need to know when all the threads are done.
     */
    private int downloadedFileCnt = 0;

    public void speak(String text) {
        if (mTTS != null && text != null && !text.isEmpty()) {
            mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void startSignIn() {
        speak("Opening Google Drive sign in");
        if (mGoogleSignInClient != null) {
            startActivityForResult(mGoogleSignInClient.getSignInIntent(), REQUEST_SIGN_IN);
        }
    }

    private void initDriveService(GoogleSignInAccount googleAccount) {
        if (googleAccount == null) return;
        try {
            GoogleAccountCredential credential =
                    GoogleAccountCredential.usingOAuth2(
                            this, Collections.singleton(DriveScopes.DRIVE));
            credential.setSelectedAccount(googleAccount.getAccount());
            Drive googleDriveService =
                    new Drive.Builder(
                            new NetHttpTransport(),
                            new GsonFactory(),
                            credential)
                            .setApplicationName("SJplayer")
                            .build();

            mService = googleDriveService;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Drive service", e);
        }
    }

    private void updateAccountUi(GoogleSignInAccount account) {
        runOnUiThread(() -> {
            if (mAccountInfoTextView == null || mAccountActionTextView == null) return;
            if (account != null) {
                String email = account.getEmail();
                mAccountInfoTextView.setText("Drive: " + (email != null ? email : "Connected"));
                mAccountInfoTextView.setTextColor(Color.WHITE);
                mAccountActionTextView.setText("SWITCH");
                mAccountActionTextView.setTextColor(Color.parseColor("#80D8FF"));
                if (mAccountStatusBar != null) {
                    mAccountStatusBar.setContentDescription("Connected to Google Drive as " + email + ". Double tap to switch account.");
                }
            } else {
                mAccountInfoTextView.setText("Google Drive: Not connected");
                mAccountInfoTextView.setTextColor(Color.parseColor("#FFA726"));
                mAccountActionTextView.setText("SIGN IN");
                mAccountActionTextView.setTextColor(Color.parseColor("#00E5FF"));
                if (mAccountStatusBar != null) {
                    mAccountStatusBar.setContentDescription("Google Drive not connected. Double tap to sign in.");
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        errorLogs = new StringBuffer();

        // Initialize TextToSpeech for blind users
        mTTS = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                mTTS.setLanguage(Locale.US);
                speak("Cloud Backup. Scanning local recordings.");
            }
        });

        // Setup Google Sign-In Client
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE))
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Check if user is already signed in
        GoogleSignInAccount lastAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (lastAccount != null && GoogleSignIn.hasPermissions(lastAccount, new Scope(DriveScopes.DRIVE))) {
            initDriveService(lastAccount);
        }

        initiateBackupUI();
        updateAccountUi(lastAccount);
    }

    @Override
    protected void onDestroy() {
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
            mTTS = null;
        }
        super.onDestroy();
    }

    private void initiateBackupUI() {
        mContext = getApplicationContext();
        mResultList = new ArrayList<File>();

        setContentView(R.layout.backup_main);

        mButtonBack = (ImageButton) findViewById(R.id.button_back);
        if (mButtonBack != null) {
            mButtonBack.setOnClickListener(v -> finish());
        }

        mAccountStatusBar = findViewById(R.id.account_status_bar);
        mAccountInfoTextView = (TextView) findViewById(R.id.text_account_info);
        mAccountActionTextView = (TextView) findViewById(R.id.text_account_action);
        mModeTitleTextView = (TextView) findViewById(R.id.text_mode_title);
        if (mAccountStatusBar != null) {
            mAccountStatusBar.setOnClickListener(v -> startSignIn());
        }

        mListView = (ListView) findViewById(R.id.listView1);
        mMainTextView = (TextView) findViewById(R.id.backup_main_textView);
        mListView.setAdapter(mSharedFileAdapter);
        OnItemClickListener mMessageClickedHandler = new OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                downloadItemFromList(position);
            }
        };

        mListView.setOnItemClickListener(mMessageClickedHandler);

        mRecyclerView = (RecyclerView) findViewById(R.id.mainRecyclerView);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(mRecyclerAdapter);

        button_backup_start = (Button) findViewById(R.id.button_backup_start);
        button_download_start = (Button) findViewById(R.id.button_download_start);

        button_backup_start.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mService == null) {
                    displayStatus("Google Drive not connected. Tap banner to sign in.");
                    speak("Please connect to Google Drive before backing up.");
                    startSignIn();
                    return;
                }
                if (recorderForBackup == null || !recorderForBackup.isReadyToStart()) {
                    displayStatus("Still scanning local files. Please wait...");
                    speak("Still scanning local files. Please wait.");
                    return;
                }
                mListView.setVisibility(ListView.GONE);
                mRecyclerView.setVisibility(RecyclerView.VISIBLE);
                if (mModeTitleTextView != null) {
                    mModeTitleTextView.setText("LOCAL FOLDERS TO BACKUP");
                }
                speak("Starting upload to Google Drive");
                if (backup_mode == UPLOAD_MODE) {
                    startBackup();
                    backup_mode = UPLOAD_MODE;
                } else {
                    prepareStartBackup();
                    backup_mode = UPLOAD_MODE;
                }
            }
        });

        button_download_start.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mService == null) {
                    displayStatus("Google Drive not connected. Tap banner to sign in.");
                    speak("Please connect to Google Drive before downloading.");
                    startSignIn();
                    return;
                }
                isDownloading = 0;
                downloadedFileCnt = 0;
                if (mModeTitleTextView != null) {
                    mModeTitleTextView.setText("SHARED CLOUD FILES");
                }
                if (backup_mode == UPLOAD_MODE) {
                    backup_mode = DOWNLOAD_MODE;
                    speak("Loading shared files from Google Drive");
                    setSharedDriveId();
                    mRecyclerView.setVisibility(RecyclerView.GONE);
                    mListView.setVisibility(ListView.VISIBLE);
                } else {
                    backup_mode = DOWNLOAD_MODE;
                    speak("Downloading all shared files");
                    downLoadAllSharedFiles();
                }
            }
        });

        // Ready for target folders
        recorderForBackup = new BackupRecorder(this);

        isFinishLoadSharedFiles = false;
        pb = (ProgressBar) findViewById(R.id.progressBar);
        setSJProgressBarVisibility(ProgressBar.VISIBLE);
        mListView.setVisibility(ListView.GONE);
        mRecyclerView.setVisibility(RecyclerView.VISIBLE);

        new Thread(new Runnable() {
            public void run() {
                try {
                    while (!recorderForBackup.isReadyToStart()) {
                        Thread.sleep(500);
                    }
                    Log.i(TAG, "Loading files from local files...");

                    buildBackupFileResultArray();

                    // Logging results of Backup...
                    gpxfile = new java.io.File
                            (recorderForBackup.getRootFolder(), getLogFileName());

                    try {
                        fileWriter = new FileWriter(gpxfile, true);
                    } catch (IOException ie) {
                        Log.i(TAG, "Fail to open log file");
                    }

                    // LOG target numbers
                    logToFile("=== " + getLogFileName() + " ===\n");
                    logToFile("Total scanned file: " + recorderForBackup.getTotalScannedFileCnt() + "\n"
                            + "Total scanned folder: " + recorderForBackup.getTotalScannedFolderCnt() + "\n");

                    // Reload target list
                    initiateListView();

                    int folderCnt = recorderForBackup.getTotalScannedFolderCnt();
                    int fileCnt = recorderForBackup.getTotalScannedFileCnt();
                    String readyMsg = "Ready to backup: " + folderCnt + " folders, " + fileCnt + " files.";
                    displayStatus(readyMsg);
                    speak("Ready to backup " + folderCnt + " folders and " + fileCnt + " audio files. Tap Upload to begin.");

                } catch (InterruptedException ie) {
                    // Nothing
                } finally {
                    setSJProgressBarVisibility(ProgressBar.INVISIBLE);
                }
            }
        }).start();
    }


    private void initiateListView() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerAdapter = new SJplayerListViewAdapter(mBackupFileResultArray);
                mRecyclerView.setAdapter(mRecyclerAdapter);
                mRecyclerAdapter.notifyDataSetChanged();
            }
        });
    }

    private void initiateSharedFileListView() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSharedFileAdapter = new SJBackupFileAdapter(mContext, mSharedFileResultArray);
                mSharedFileAdapter.setNotifyOnChange(true);
                mListView.setAdapter(mSharedFileAdapter);
                mSharedFileAdapter.notifyDataSetChanged();
            }
        });
    }

    private void populateListView() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                mBackupFileResultArray
//                    = new BackupFileResult[recorderForBackup.getDirectories().size()];
//                for(int i=0;recorderForBackup.getDirectories().size() > i;i++)
//                {
////                    mFileArray[i] = recorderForBackup.getDirectories().get(i).getName();
//                    mBackupFileResultArray[i] = new BackupFileResult(
//                            recorderForBackup.getDirectories().get(i).getName());
//                }
//                ArrayAdapter<BackupFileResult> mAdapter = new ArrayAdapter<BackupFileResult>
//                        (mContext, android.R.layout.simple_list_item_1, mBackupFileResultArray);
//                mAdapter.notifyDataSetChanged();
//                mAdapter.setNotifyOnChange(true);
                if (mSharedFileAdapter != null)
                    mSharedFileAdapter.notifyDataSetChanged();
                if (mRecyclerAdapter != null)
                    mRecyclerAdapter.notifyDataSetChanged();
            }
        });
    }

    private void populateBackupListView() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                mBackupFileResultArray
//                    = new BackupFileResult[recorderForBackup.getDirectories().size()];
//                for(int i=0;recorderForBackup.getDirectories().size() > i;i++)
//                {
////                    mFileArray[i] = recorderForBackup.getDirectories().get(i).getName();
//                    mBackupFileResultArray[i] = new BackupFileResult(
//                            recorderForBackup.getDirectories().get(i).getName());
//                }
//                ArrayAdapter<BackupFileResult> mAdapter = new ArrayAdapter<BackupFileResult>
//                        (mContext, android.R.layout.simple_list_item_1, mBackupFileResultArray);
//                mAdapter.notifyDataSetChanged();
//                mAdapter.setNotifyOnChange(true);
                if (mRecyclerAdapter != null)
                    mRecyclerAdapter.notifyDataSetChanged();
            }
        });
    }

    public void buildBackupFileResultArray() {
        mBackupFileResultArray
                = new BackupFileResult[recorderForBackup.getDirectories().size()];
        for (int i = 0; recorderForBackup.getDirectories().size() > i; i++) {
            mBackupFileResultArray[i] =
                    new BackupFileResult(recorderForBackup.getCurrentDirectoryName()
                            , recorderForBackup.getNumberOfAudioFiles(), true);
            Log.i(TAG, "======>" + recorderForBackup.getCurrentDirectoryName() + ":" + recorderForBackup.getNumberOfAudioFiles());
            if (mRecyclerAdapter != null) {
                mRecyclerAdapter.notifyDataSetChanged();
            }
            recorderForBackup.nextFolder();
        }

    }

    public void buildBackupFileResultArrayFromMainRecorder() {
        mBackupFileResultArray
                = new BackupFileResult[recorder.getDirectories().size()];
        recorder.setCurrentDirectoryIndex(0);
        recorder.setCurrentFileIndex(0);
        int fNum;
        String dirName;
        for (int i = 0; recorder.getDirectories().size() > i; i++) {
            fNum = recorder.getNumberOfAudioFiles();
            dirName = recorder.getCurrentDirectoryName();
            Log.i(TAG, "buildBackupFileResultArrayFromMainRecorder======>" + dirName + ":" + fNum);
            mBackupFileResultArray[i] =
                    new BackupFileResult(dirName, fNum, true);
            if (mRecyclerAdapter != null) {
                mRecyclerAdapter.notifyDataSetChanged();
            }
            recorder.nextFolder();
        }
        Log.i(TAG, "buildBackupFileResultArrayFromMainRecorder:DONE for " + recorder.getDirectories().size());
    }

    public void logToFile(String msg) {
        Log.i(TAG, "LogToFile:" + msg);

        try {
            if (fileWriter == null && gpxfile != null) {
                fileWriter = new FileWriter(gpxfile, true);
            }

            if (fileWriter != null) {
                fileWriter.append(msg);
                fileWriter.flush();
            }
        } catch (IOException ie) {
            Log.i(TAG, "Fail to write LOG file: " + ie.getMessage());
        }
    }

    public void logSuccess(String folderName, int fileCnt) {

        try {
            if (successLogFileWriter == null && successLogFile != null) {
                successLogFileWriter = new FileWriter(successLogFile, true);
            }

            if (successLogFileWriter != null) {
                successLogFileWriter.append(folderName + "\t" + fileCnt + "\n");
                successLogFileWriter.flush();
            }
        } catch (IOException ie) {
            Log.i(TAG, "Fail to write SUCCESS_LOG.dat file: " + ie.getMessage());
        }
    }

    private String getLogFileName() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return LOGFILE + "-" + sdf.format(c.getTime()) + ".txt";
    }


    private String getSuccessLogFileName() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return LOGFILE + "-" + sdf.format(c.getTime()) + ".txt";
    }


    //Issue, it's ASync'ed
    public void setMessage(String str) {
        this.mMainTextView.setText(str);
    }

    // Read FileList.Result and retrieving only Filename to check existence.
    public ArrayList<String> getExistingDriveFolderList(FileList fileList) {
        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; fileList.getFiles().size() > i; i++) {
            list.add(fileList.getFiles().get(i).getName());
        }
        return list;
    }

    public HashMap getDriveFolderMap(Drive.Files.List request, String parentFolderId) {
        HashMap map = new HashMap();
        try {
            request.setQ(String.format("trashed = false and "
                    + "mimeType = 'application/vnd.google-apps.folder' and '"
                    + parentFolderId + "' in parents"));
            FileList fileList = request.execute();
            logToFile("Query executed: " + request.getQ());
            String folderName;
            String folderId;
            //Log.i(TAG, "Checking getExstignDriveFolderMap works correcly =======> " + fileList.getFiles().size());
            do {
                for (int i = 0; fileList.getFiles().size() > i; i++) {
                    //What if multiple same folders are existing?
                    //At least it should not a case in SJ Recorder file structure.
                    //map.put(fileList.getFiles().get(i).getName(), fileList.getFiles().get(i));
                    //map.put(fileList.getFiles().get(i).getName(), fileList.getFiles().get(i).getId());
                    folderName = fileList.getFiles().get(i).getName();
                    folderId = fileList.getFiles().get(i).getId();
                    map.put(folderName, folderId);
                    //=======================
                    //if Folder name includes "SUFFIX" created by SJ Player
                    //Then assumming there are folders under the folder
                    //No need to browse all folders, performance issue comes up
                    if (folderName.endsWith(SUFFIX_FOLDER_CREATED_BY_SJPLAYER)) {
                        map.putAll(getDriveFolderMap(request, folderId));
                    }

                }
                //displayStatus("Loading folders in Google drive...");
                //Log.i(TAG, "DriveMainActivity.getExsitingDriveFolderMap() ---> " + map.size());
            } while (request.getPageToken() != null && request.getPageToken().length() > 0);

        } catch (IOException ioe) {
            displayStatus(ioe.toString());
        }
        //displayStatus(map.size() + " folder(files) loaded...");
        return map;
    }

    public HashMap getDriveFileMap(Drive.Files.List request, String parentFolderId) {
        HashMap map = new HashMap();
        try {
            request.setQ(String.format("trashed = false and "
                    + "mimeType != 'application/vnd.google-apps.folder' and '"
                    + parentFolderId + "' in parents"));
            FileList fileList = request.execute();
            logToFile("Query executed: " + request.getQ());
            String folderName;
            String folderId;
            //Log.i(TAG, "Checking getExstignDriveFolderMap works correcly =======> " + fileList.getFiles().size());
            do {
                for (int i = 0; fileList.getFiles().size() > i; i++) {
                    folderName = fileList.getFiles().get(i).getName();
                    folderId = fileList.getFiles().get(i).getId();
                    map.put(folderName, folderId);
                }
                //displayStatus("Loading folders in Google drive...");
                //Log.i(TAG, "DriveMainActivity.getExsitingDriveFolderMap() ---> " + map.size());
            } while (request.getPageToken() != null && request.getPageToken().length() > 0);

        } catch (IOException ioe) {
            displayStatus(ioe.toString());
        }
        //displayStatus(map.size() + " folder(files) loaded...");
        return map;
    }

    //Total file and folder count
    //Running again
    private void sanityCheck() {
        logToFile("===SanityCheck: Should be 0 folder/file created\n===");
        startBackup();
    }

    /**
     *
     */
    private boolean isExistInSuccessLog(String folderName) {
        boolean isExist = false;

        return isExist;
    }

    private void prepareStartBackup() {
        isFinishLoadSharedFiles = false;
        pb = (ProgressBar) findViewById(R.id.progressBar);
        setSJProgressBarVisibility(ProgressBar.VISIBLE);

        initiateListView();
        populateListView();
        setSJProgressBarVisibility(ProgressBar.INVISIBLE);
    }

    private void startBackup() {
        if (mService == null) {
            displayStatus("Google Drive not connected. Tap banner to sign in.");
            speak("Google Drive not connected. Please sign in.");
            startSignIn();
            return;
        }

        if (!recorderForBackup.isReadyToStart()) {
            displayStatus("Still loading local files...");
            speak("Still scanning local files. Please wait a moment.");
            return;
        }

        //Folder success_log
        //FolderName + "\t" + file number
        successLogFile = new java.io.File
                (recorderForBackup.getRootFolder(), SUCCESSLOGFILE);

        try {
            if (!successLogFile.exists())
                successLogFileWriter = new FileWriter(successLogFile, true);
        } catch (IOException ie) {
            Log.i(TAG, "Fail to open log file");
        }

        successLogMap = FileUtils.FileToMap(successLogFile, "\t");

        new Thread(new Runnable() {
            public void run() {
                boolean hasDoneSuccessfully = false;
                FileList fileList = null;
                FileList fileListInFolder = null;
                Drive.Files.List request = null;
                // Count folder/file numbers created by
                int tProcessedFolder = 0;
                int tProcessedFile = 0;
                int tNewFolder = 0;
                int tNewFile = 0;
                int tExistingFolder = 0;
                int tExistingFile = 0;
                int tFailCreateFolder = 0;
                int tFailCreateFile = 0;
                try

                {
                    //displayStatus("Connecting Google Drive...");
                    request = mService.files().list();
                    //TODO when folder increased more than 1K in Drive?
                    //Should create parent folder by year?
                    request.setPageSize(1000);

                    // Query to check if BACK_ROOT_FOLDER exists
                    request.setQ(String.format("trashed = false and "
                            + "mimeType = 'application/vnd.google-apps.folder' and 'root' in parents"));

                    //request.setQ( query );
                    fileList = request.execute();

                    mResultList.addAll(fileList.getFiles());

                    boolean isBackupRootFolderExist = false;

                    for (int i = 0; fileList.getFiles().size() > i; i++) {
                        if (fileList.getFiles().get(i).getName().equalsIgnoreCase(BACKUP_ROOT_FOLDER)) {
                            isBackupRootFolderExist = true;
                            backupDriveId = fileList.getFiles().get(i).getId();
                            displayStatus("Located BACKUP ROOT FOLDER in Google drive: " + backupDriveId);
                            break;
                        }
                    }

                    /**
                     * If (isBackupRootFolderExist == false), create the ROOT Folder
                     */

                    File fileMetadata = new File();
                    if (!isBackupRootFolderExist) {
//                        fileMetadata.setName(BACKUP_ROOT_FOLDER);
//                        fileMetadata.setMimeType("application/vnd.google-apps.folder");
//                        File file = mService.files().create(fileMetadata)
////                                .setFields("id")
//                                .execute();
//                        backupDriveId = file.getId();
                        backupDriveId = createFolder(mService, "", BACKUP_ROOT_FOLDER);
                        logToFile("BackupFolder created: " + backupDriveId + "\n");
                        displayStatus("BackupFolder created: " + backupDriveId);

                    }


                    /**
                     * =======================================================================
                     * FOLDER Work
                     // Query again under Backup ROOT Folder
                     // Query to check if BACK_ROOT_FOLDER exists

                     request.setQ(String.format("trashed = false and "
                     + "mimeType = 'application/vnd.google-apps.folder' and '"
                     + backupDriveId + "' in parents"));
                     fileList = request.execute();
                     logToFile("Query executed: " + request.getQ());
                     */
                    //To double check when duplicated folder created
//                    for(int i = 0; fileList.getFiles().size() > i ; i++) {
//                        logToFile("Folder under BackupFolder:" + fileList.getFiles().get(i).getName() + "\n");
//                    }

                    //prep to compare whether file/folder is existing

                    //ArrayList<String> fileListResult = getExistingDriveFolderList(fileList);
                    HashMap folderListMap = getDriveFolderMap(request, backupDriveId);
                    //========================================
                    // After getting Backup ROOT folder, get targetFolder list
                    String localFolderName = "";
                    String driveFolderName = "";
                    boolean isDriveFolderExistInLocalFolderList = false;
                    String newFolderId = null;

                    boolean isFileExistInDriveFolderList = false;
                    boolean isSuccessToCreateFolder = true;
                    boolean isExistSuccessLog;


                    // Reading local directories
                    // if there is 0 foler found, then nothing's going on
                    for (int i = 0; recorderForBackup.getDirectories().size() > i; i++) {
                        isExistSuccessLog = false;
                        localFolderName = recorderForBackup.getCurrentDirectoryName();
                        mBackupFileResultArray[i].setStatus(BackupFileResult.PROGRESS);
                        mBackupFileResultArray[i].setBackupResultDescription("Uploading...");
                        populateBackupListView();
                        displayStatus("Uploading... " + localFolderName);
                        HashMap fileMapInGoogleDrive = new HashMap();

                        // If localFolderName exists in SUCCESS_LOG.dat, then it moves to the next folder
                        // Make sense?
                        // @TODO just in case, sanity check is required.

                        if (successLogMap.containsKey(localFolderName) &&
                                Integer.parseInt(successLogMap.get(localFolderName)) == recorderForBackup.getNumberOfAudioFiles()) {
                            Log.i(TAG, "SUCCESS FOLDER in Log ===> " + localFolderName +
                                    ":" + recorderForBackup.getNumberOfAudioFiles() + ":" + tProcessedFile);
                            //Not to Log again
                            isExistSuccessLog = true;
                            tExistingFile = tExistingFile + recorderForBackup.getNumberOfAudioFiles();
                            tExistingFolder++;
                            tProcessedFile = tProcessedFile + Integer.parseInt(successLogMap.get(localFolderName));
                            updateUploadButton(tProcessedFile + "", WORKING);
                            mBackupFileResultArray[i].setStatus(BackupFileResult.SUCCESS);
                            downloadedFileCnt++;
//                            mBackupFileResultArray[i].setBackupResultDescription(
//                                    new String(new String(Character.toChars(0x2714)) //Thumb up
//                                            + " " + successLogMap.get(localFolderName) + " files in backup log"
//                                    ));
                            mBackupFileResultArray[i].setBackupResultDescription(
                                    new String(successLogMap.get(localFolderName) + " files in backup log"));
                            displayStatus(mBackupFileResultArray[i].getBackupResultDescription());
                            recorderForBackup.nextFolder();
                        } else {
                            String yearOfFolder = getYearFolderName(localFolderName);
                            String yearOfFolderId;
                            try {
                                if (folderListMap.containsKey(localFolderName)) {
                                    newFolderId = (String) folderListMap.get(localFolderName);
                                } else {
                                    //Checking if localFile/Folder exists in Drive
                                    //folderListMap = Folder list in Google Driver / BACKUP FOLDRER

                                    if (!folderListMap.containsKey(yearOfFolder)) {
                                        yearOfFolderId = createFolder(mService, backupDriveId, yearOfFolder);
                                        folderListMap.put(yearOfFolder, yearOfFolderId);
                                        //=============================================================
                                        //Create a new folder as local folder name
                                        //                                fileMetadata.setName(localFolderName);
                                        //                                fileMetadata.setMimeType("application/vnd.google-apps.folder");
                                        //                                fileMetadata.setParents(Collections.singletonList(backupDriveId));
                                        //                                File newFileFolder = mService.files().create(fileMetadata)
                                        //                                        .setFields("id, parents")
                                        //                                        .execute();
                                        //
                                        //                                newFolderId = newFileFolder.getId();
                                        newFolderId = createFolder(mService, yearOfFolderId, localFolderName);

                                        //TODO Should I add the new folder into folderListMap?
                                        //folderListMap.put(yearOfFolder, newFolderId);


                                    } else if (folderListMap.containsKey(yearOfFolder) &&
                                            !folderListMap.containsKey(localFolderName)) {
                                        newFolderId = createFolder(mService,
                                                (String) folderListMap.get(yearOfFolder),
                                                localFolderName);
                                        //folderListMap.put(localFolderName, newFolderId);
                                        //
                                        //NOTHING TO DO when folder exists already
                                    } else {
                                        newFolderId = (String) folderListMap.get(localFolderName);
                                        //TODO 서브폴더 이름을 folderListMap 에 로드해야함
                                        //String existingFolderId = (String) folderListMap.get(yearOfFolder);
                                        //                                if(!folderListMap.containsKey(localFolderName)) {
                                        //                                    existingFolderId = createFolder(mService, existingFolderId, localFolderName);
                                        //                                    folderListMap.put(localFolderName, existingFolderId);
                                        //                                    tExistingFolder++;
                                        //                                }

                                        //                                //Copying files
                                        //                                logToFile("Existing folder:" + localFolderName + "\n");
                                        //                                Log.i(TAG,"Existing folder:" + localFolderName + "\n");

                                        //newFolderId = existingFolderId;
                                        //Query files only when folder exists already
                                    }
                                }
                            } catch (Exception e) {
                                displayStatus(e.toString());
                                if (newFolderId == null) {
                                    isSuccessToCreateFolder = false;
                                    logToFile("Failed to create folder:" + localFolderName);
                                } else {
                                    logToFile("Folder created: " + localFolderName + "@Local, "
                                            + localFolderName + "@Drive : "
                                            + newFolderId + "@Drive\n");
                                    ;
                                }
                            } finally {
                                displayStatus(mBackupFileResultArray[i].getBackupResultDescription());
                            }

                            /**
                             * Create file list in Google Drive
                             */

                            if (isSuccessToCreateFolder) {
                                tNewFolder++;
                                //List up existing files@Drive under newFolderId
                                fileMapInGoogleDrive = getDriveFileMap(request, newFolderId);
                                // Only inside of a specific folder.
                                // Total count is in
                                int newInFolder = 0;
                                int skipInFolder = 0;
                                int failInFolder = 0;
                                for (int n = 0; recorderForBackup.getNumberOfAudioFiles() > n; n++) {

                                    // TODO When md5CheckSum added, it should be compared too.
                                    // Creating file only when file does not exist
//                                    Log.i(TAG, "DriveMainActivity.CreateFiles ---> "+
//                                            recorderForBackup.getCurrentFileName() +
//                                            ":" + fileMapInGoogleDrive.size() +
//                                            ":" + fileMapInGoogleDrive.containsKey(recorderForBackup.getCurrentFileName())
//                                    );
                                    displayStatus(mBackupFileResultArray[i].getBackupResultDescription());
                                    if (!fileMapInGoogleDrive.containsKey(recorderForBackup.getCurrentFileName())) {
                                        fileMetadata = new File();
                                        //Get MimeType from filename
                                        FileContent mediaContent =
                                                new FileContent(MimeUtils.guessMimeTypeFromFullFilePath(
                                                        recorderForBackup.getCurrentFileFullPath()),
                                                        new java.io.File(recorderForBackup.getCurrentFileFullPath()));

                                        fileMetadata.setName(recorderForBackup.getCurrentFileName());
                                        fileMetadata.setMimeType(MimeUtils.guessMimeTypeFromFullFilePath(
                                                recorderForBackup.getCurrentFileFullPath()));
                                        fileMetadata.setParents(Collections.singletonList(newFolderId));

                                        try {
                                            displayStatus("Uploading... " + recorderForBackup.getCurrentFileName() + "(" +
                                                    String.format("%.2f", (double) recorderForBackup.getCurrentFile().length() / 1000000) + "MB)");

//                                            File uploadedFile =
//                                                    mService.files().create(fileMetadata, mediaContent).execute();

                                            //==============================
                                            newInFolder++;

                                        } catch (Exception e) {
                                            displayStatus(e.toString());
                                            failInFolder++;
                                            // Do nothing and move next;
                                            logToFile("Failed to create file: " + recorderForBackup.getCurrentFileName()
                                                    + ":" + e.getMessage() + "]\n");

                                        }
                                    } else {
                                        skipInFolder++;
                                    }

                                    /**
                                     * TODO Separate MSG part into Custom ListView/Adapter
                                     * TODO This MUST be updated later.
                                     */
                                    mBackupFileResultArray[i].setStatus(BackupFileResult.PROGRESS);
//                                    mBackupFileResultArray[i].setBackupResultDescription(
//                                            new String(new String(Character.toChars(0x1F4E4))
//                                                    + " total:" + mBackupFileResultArray[i].getSize()
//                                                    + ", new:" + newInFolder + ", skip:" + skipInFolder
//                                                    + ", fail:" + failInFolder));
                                    mBackupFileResultArray[i].setBackupResultDescription(
                                            new String("total:" + mBackupFileResultArray[i].getSize()
                                                    + ", new:" + newInFolder + ", skip:" + skipInFolder
                                                    + ", fail:" + failInFolder));
                                    populateBackupListView();
                                    recorderForBackup.nextSong();
                                    tProcessedFile = tProcessedFile + 1;
                                    //n == cnt File in the loop
                                    //i == cnt Folder in the loop
                                    updateUploadButton(tProcessedFile + "", WORKING);
                                    displayStatus(mBackupFileResultArray[i].getBackupResultDescription());
                                }


                                // ==============================================================
                                // ONLY TO DISPLAY Backup result
                                // If new+skip != total, then SOMETHING WRONG
                                if (newInFolder + skipInFolder == mBackupFileResultArray[i].getSize()) {

                                    if (!isExistSuccessLog) {
                                        //Log localFolderName and created or skipped files
                                        logSuccess(localFolderName, newInFolder + skipInFolder);
                                    }
                                    mBackupFileResultArray[i].setStatus(BackupFileResult.SUCCESS);
                                    downloadedFileCnt++;
                                    mBackupFileResultArray[i].setBackupResultDescription(
                                            new String(
                                                    //new String(Character.toChars(0x2714)) //Thumb up
                                                    "total:" + mBackupFileResultArray[i].getSize()
                                                            + ", new:" + newInFolder + ", skip:" + skipInFolder
                                                            + ", fail:" + failInFolder));
                                } else {
                                    mBackupFileResultArray[i].setStatus(BackupFileResult.FAIL);
                                    mBackupFileResultArray[i].setBackupResultDescription(
                                            new String(
                                                    //new String(Character.toChars(0x2753)) //Question?
                                                    "total:" + mBackupFileResultArray[i].getSize()
                                                            + ", new:" + newInFolder + ", skip:" + skipInFolder
                                                            + ", fail:" + failInFolder));

                                }
                                populateBackupListView();
                                displayStatus(mBackupFileResultArray[i].getBackupResultDescription());
                                tNewFile = tNewFile + newInFolder;
                                tExistingFile = tExistingFile + skipInFolder;
                                tFailCreateFile = tFailCreateFile + failInFolder;
                                //tProcessedFolder = tExistingFolder + tNewFolder;
                                //tProcessedFile = tProcessedFile + (newInFolder + skipInFolder + failInFolder);
                                //recorderForBackup.nextFolder();

                                //FAILED to create folder
                            } else {
                                //TODO Something different icon in case folder itself failed.
                                tFailCreateFolder++;
                                mBackupFileResultArray[i].setStatus(BackupFileResult.FAIL);
                                mBackupFileResultArray[i].setBackupResultDescription(
                                        //new String(new String(Character.toChars(0x2753)) //Question?
                                        "total:" + mBackupFileResultArray[i].getSize()
                                                + " Failed to create folder");

                                populateBackupListView();

                                //recorderForBackup.nextFolder();
                                //break;
                            }
                            recorderForBackup.nextFolder();
                            populateBackupListView();
                            displayStatus(mBackupFileResultArray[i].getBackupResultDescription());
                        }

                        tProcessedFolder = tProcessedFolder + 1;

                    }//END OF LOOP
                    //For Loop

//
//                    // Query to check if BACK_ROOT_FOLDER exists
//                    request.setQ(String.format("trashed = false and "
//                            + "mimeType = 'application/vnd.google-apps.folder' and '"
//                            + backupDriveId + "' in parents"));
//
//                    //request.setQ( query );
//                    fileList = request.execute();
//                    logToFile("Total folders in Drive:" + fileList.getFiles().size()+"\n");
//
//                    // Query to check if BACK_ROOT_FOLDER exists
//                    request.setQ(String.format("trashed = false"));
//                           // + "mimeType = 'application/vnd.google-apps.folder' and '"
////                            + "'" + backupDriveId + "' in parents"));
//
//                    //request.setQ( query );
//                    fileList = request.execute();
//                    logToFile("Total files in Drive:" + fileList.getFiles().size()+"\n");
                    // Display highlevel progress in status bar.
//                    displayStatus("Folder: " + tProcessedFolder + "/" + recorderForBackup.getTotalScannedFolderCnt()
//                            + ", File: " + tProcessedFile
//                            + "/" + recorderForBackup.getTotalScannedFileCnt());

                    logToFile("RESULT:\n" + tNewFolder + " new folders\n" + tNewFile + " new files\n");
                    logToFile(tExistingFolder + " existing folders\n" + tExistingFile + " existing files\n");
                    logToFile("=== " + recorderForBackup.createFileName() + " ===\n\n");

                    hasDoneSuccessfully = true;

//                } catch (UserRecoverableAuthIOException e) {
//                    //startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
//                    errorLogs.append(e.getMessage());
//                    logToFile("Exception:" + e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                    errorLogs.append(e.toString());
                    displayStatus("STOPPED by Exceptions:" + errorLogs.toString());
                    logToFile("=== STOPPED by Exceptions ===\n" + errorLogs.toString());
                } finally {
                    populateListView();
                    logToFile("RESULT:\n" + tNewFolder + " new folders\n" + tNewFile + " new files\n"
                            + tExistingFolder + " existing folders\n" + tExistingFile + " existing files\n");

                    logToFile("=== " + getLogFileName() + "-" + recorderForBackup.createFileName() + " ===\n\n");

                    try {
                        if (fileWriter != null)
                            fileWriter.close();
                        if (successLogFileWriter != null)
                            successLogFileWriter.close();
                    } catch (IOException ie) {
                        Log.i(TAG, "Fail to close LOG fileWriter:" + ie.getMessage());
                    } finally {
                        fileWriter = null;
                        successLogFileWriter = null;
                    }
                    // Start again until newFile == 0 after 1min waiting
//                    try {
//                        Thread.sleep(1000*60);
//                    }catch(InterruptedException ie) {
//                        // Nothing
//                    }
//                    if( tProcessedFolder < recorderForBackup.getTotalScannedFolderCnt() ){
//                        Log.i(TAG, "===RESTART BACKUP AGENT===");
//                        displayStatus("===RESTART BACKUP AGENT===");
//                        //startBackup();
//                    }

                    //displayNotification("RESTARTED");
                }

                displayStatus(errorLogs.toString() + "\n" + tProcessedFile + "/" + recorderForBackup.getTotalScannedFileCnt() +
                        " files processed. " + tNewFile + " new files and " + tNewFolder + " new folders.");

                if (hasDoneSuccessfully) {
                    updateUploadButton("UPLOAD", DONE);
                    speak("Backup completed successfully. " + tProcessedFile + " files processed.");
                } else {
                    updateUploadButton("UPLOAD", FAIL);
                    speak("Backup finished with some errors.");
                }
            }
        }).start();


    }

    /**
     * @param src is Folder name created by SJ Player, e.g. YYYY-MM-DD
     * @return Returning only YYYY
     */
    public String getYearFolderName(String src) {
        String yearOfFolder;
        try {
            //Assumption is FolderName starts with 2016-...
            // which is the standard format of SJ recorder
            // Only when src can be converted DATE format
            //Calendar c = Calendar.getInstance();

            SimpleDateFormat sdf;
            //To minimize Exception throw, have SimpleDateFormat line duplicated.
            if (src.length() == 7) {
                sdf = new SimpleDateFormat("yyyy-MM");
                yearOfFolder = new SimpleDateFormat("yyyy").format(sdf.parse(src)) + SUFFIX_FOLDER_CREATED_BY_SJPLAYER;
            } else if (src.length() == 4) {
                sdf = new SimpleDateFormat("yyyy");
                yearOfFolder = new SimpleDateFormat("yyyy").format(sdf.parse(src)) + SUFFIX_FOLDER_CREATED_BY_SJPLAYER;
            } else if (src.length() == 10) {
                //othersise assuming yyyy-mm-dd
                sdf = new SimpleDateFormat("yyyy-MM-dd");
                yearOfFolder = new SimpleDateFormat("yyyy").format(sdf.parse(src)) + SUFFIX_FOLDER_CREATED_BY_SJPLAYER;
            } else {
                yearOfFolder = "OTHERS" + SUFFIX_FOLDER_CREATED_BY_SJPLAYER;
            }
        } catch (Exception ee) {
            displayStatus(ee.toString());
            yearOfFolder = "OTHERS" + SUFFIX_FOLDER_CREATED_BY_SJPLAYER;
        }
        Log.i(TAG, "getYearFolderName ===> " + src + ":" + yearOfFolder);
        return yearOfFolder;
    }

    public String createFolder(Drive mService, String parentFolderId, String folderNameId) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(folderNameId);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");
        //"" ===> Creating a folder under ROOT
        if (parentFolderId.length() > 0)
            fileMetadata.setParents(Collections.singletonList(parentFolderId));
        File newFileFolder = mService.files().create(fileMetadata)
                .setFields("id, parents")
                .execute();

        return newFileFolder.getId();
    }

    public void updateUploadButton(String msg, int status) {
        if (status == DONE) {
            downloadButtonStatusMsg = new String(Character.toChars(0x2714)) + " " + msg;
        } else if (status == WORKING) {
            downloadButtonStatusMsg = new String(Character.toChars(0x1F4E4)) + " " + msg;
        } else if (status == FAIL) {
            downloadButtonStatusMsg = new String(Character.toChars(0x2753)) + " " + msg;
        } else {
            downloadButtonStatusMsg = msg; //NOTHING
        }

        mUploadButtonHandler.sendMessage(mUploadButtonHandler.obtainMessage());
    }

    private Handler mUploadButtonHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            button_backup_start.setText(downloadButtonStatusMsg);
        }
    };

    //mode: UPLOAD or DOWNLOAD
    //success:

    public void setItemInList(int mode, int itemNumber, int status, String msg) {
//        String isOk;
//
//        if (status == SUCCESS)
//            isOk = new String(Character.toChars(0x2714));
//        else if (status == FAIL)
//            isOk = new String(Character.toChars(0x2753));
//        else if (status == WORKING)
//            isOk = new String(Character.toChars(0x1F4E5));
//        else
//            isOk = new String(Character.toChars(0x2753));
//
//        msg = isOk + " " + msg;

        if (mode == UPLOAD_MODE) {
            mBackupFileResultArray[itemNumber].setBackupResultDescription(msg);
        } else if (mode == DOWNLOAD_MODE) {
            mSharedFileResultArray[itemNumber].setBackupResultDescription(msg);
        }
        /**
         * new String(Character.toChars(0x2753) ---> Question
         * new String(Character.toChars(0x1F4C1)) ---> Folder
         * new String(Character.toChars(0x1F197)) ---> Thumb up
         * new String(Character.toChars(U+1F4E5)) ---> Donwload
         *  new String(Character.toChars(U+1F4E4)) ---> Upload
         *  U+1F4BF	---> CD Disck
         *  U+1F4BE	---> Floppy
         *  U+1F196	---> NG
         *  U+1F44D	 ----> Thumb up
         * E,g,
         new String(Character.toChars(0x1F4C1)) //Folder
         + " " + localFolderName
         + "\n" + new String(Character.toChars(0x1F4E4))
         + " total:" + mBackupFileResultArray[i].getSize()
         + ", new:" + newInFolder + ", skip:" + skipInFolder
         + ", fail:" + failInFolder
         */
    }

    public void displayStatus(String msg) {
        statusMsg = msg;
        mHandler.sendMessage(mHandler.obtainMessage());
    }

    /**
     * msg=="invisible progressbar
     * dissmissProgressBar("INVISIBLE_PROGRESSBAR")
     *
     * @param
     */
    public void setSJProgressBarVisibility(int i) {
        progressBarVisibility = i;
        mHandler.sendMessage(mHandler.obtainMessage());
    }

    // To solve the issue that Static UI obj can't access from thread
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            pb.setVisibility(progressBarVisibility);
            mMainTextView.setText(statusMsg);
            //mMainTextView.scrollTo(1, mMainTextView.getLineCount());
        }
    };

    public void isReadyToClickButtons(int i) {
        progressBarVisibility = i;
        mButtonHandler.sendMessage(mHandler.obtainMessage());
    }

    // To solve the issue that Static UI obj can't access from thread
    private Handler mButtonHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (isFinishLoadSharedFiles) {
                button_backup_start.setEnabled(true);
            } else {
                button_backup_start.setEnabled(false);
            }

        }
    };


//    public FileList getRootFolderList(){
//
//        Thread t = new Thread(new Runnable()
//        {
//            com.google.api.services.drive.model.FileList fileList = null;
//            com.google.api.services.drive.Drive.Files f1 = mService.files();
//            com.google.api.services.drive.Drive.Files.List request = null;
//            @Override
//            public void run()
//            {
//
//                try
//                {
//                    request = f1.list();
//
//                    request.setQ(String.format("trashed = false and "
//                            + "mimeType = 'application/vnd.google-apps.folder' and 'root' in parents"));
//
//                    //request.setQ( query );
//                    fileList = request.execute();
//
//                    mResultList.addAll(fileList.getFiles());
//
//                } catch (UserRecoverableAuthIOException e) {
//                    startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                    }
//        });
//        t.start();
//        return fileList;
//    }

    /**
     * Keep checking if it's good to make The progressbar(pb) INVISIBLE
     * AND Enable DOWNLOAD button
     */
    private void dismissDownloadProgressBar() {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    while (!isFinishLoadSharedFiles) {
//                        //Nothing
//                        Thread.sleep(1000);
//                    }
//                    ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(ProgressBar.INVISIBLE);
//                }catch(InterruptedException ie){
//                    ie.printStackTrace();
//                }finally {
//                }
//            }
//        });
        new Thread(new Runnable() {
            public void run() {
                try {
                    while (!isFinishLoadSharedFiles) {
                        //Nothing
                        Thread.sleep(1000);
                    }
                    ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(ProgressBar.INVISIBLE);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                } finally {
                }
            }
        }).start();
    }

    /**
     * ANY FILES UNDER SHARED_FOLDER will be downloaded to device
     */
    private void setSharedDriveId() {
        if (mService == null) {
            displayStatus("Google Drive not connected. Tap banner to sign in.");
            speak("Google Drive not connected. Please sign in.");
            startSignIn();
            return;
        }

        isFinishLoadSharedFiles = false;
        setSJProgressBarVisibility(ProgressBar.VISIBLE);
        // run a background job and once complete

        new Thread(new Runnable() {
            public void run() {
                List<BackupFileResult> sharedFiles =
                        new ArrayList<BackupFileResult>();
                FileList fileList = null;
                FileList folderList = null;
                FileList allFileList = null;
                FileList tAllFileList = null;
                Drive.Files.List request = null;
                String pFolderName;

                try {
                    /**
                     * Waiting until recorderFoBackup Instance is ready
                     */
                    if (!recorderForBackup.isReadyToStart()) {
                        displayStatus("Still loading local files...");
                        return;
                    }


                    request = mService.files().list();
                    //TODO when folder increased more than 1K in Drive?
                    //Should create parent folder by year?
                    //request.setPageSize(1000);

                    // Query to check if BACK_ROOT_FOLDER exists
                    request.setQ(String.format("trashed = false and "
                            + "mimeType = 'application/vnd.google-apps.folder' and 'root' in parents"));

                    fileList = request.execute();
                    boolean isSharedFolderExist = false;
                    for (int i = 0; fileList.getFiles().size() > i; i++) {
                        if (fileList.getFiles().get(i).getName().equalsIgnoreCase(SHARED_FOLDER)) {
                            isSharedFolderExist = true;
                            sharedDriveId = fileList.getFiles().get(i).getId();
                            Log.i(TAG, "DriveMainActivity.setSharedFiles (1) ---> " + fileList.getFiles().get(i).getName());
                            break;
                        }
                    }

                    // If (isBackupRootFolderExist == false), create the ROOT Folder
                    File fileMetadata = new File();
                    if (!isSharedFolderExist) {
                        fileMetadata.setName(SHARED_FOLDER);
                        fileMetadata.setMimeType("application/vnd.google-apps.folder");
                        File file = mService.files().create(fileMetadata)
//                                .setFields("id")
                                .execute();
                        sharedDriveId = file.getId();
                        logToFile("SHARED_FOLDER created: " + sharedDriveId + "\n");
                    }

                    /**
                     * After creating and get SHARED FOLDER
                     * START downloading files
                     */

                    //request = mService.files().list();
                    File tFile;
                    do {
                        try {
                            //Getting all Files and Folders
                            request.setQ(String.format("trashed = false and '"
                                    //+ "mimeType = 'application/vnd.google-apps.folder' and '"
                                    + sharedDriveId + "' in parents"));
                            allFileList = request.execute();
                            //sharedFiles.addAll((folderList.getFiles()));

                            for (int i = 0; allFileList.getFiles().size() > i; i++) {
                                tFile = allFileList.getFiles().get(i);
                                if (tFile.getMimeType().contains("folder")) {
                                    pFolderName = tFile.getName();
                                    // No recursive, only files under the first folder
                                    request.setQ(String.format("trashed = false and "
                                            + "mimeType != 'application/vnd.google-apps.folder' and '"
                                            + tFile.getId() + "' in parents"));
                                    tAllFileList = request.execute();
                                    for (int j = 0; tAllFileList.getFiles().size() > j; j++) {
                                        sharedFiles.add(new BackupFileResult(tAllFileList.getFiles().get(j), pFolderName));
                                        logToFile("Query executed&size() to find SubFolders: " +
                                                ":FOLDER NAME:" + pFolderName + ":" +
                                                request.getQ() + tAllFileList.getFiles().size());
                                    }
                                } else {
                                    //If it's a file under the SHARE FOLDER ROOT, it's blank.
                                    sharedFiles.add(new BackupFileResult(tFile, "SHARED ROOT"));
                                    logToFile("Query executed&size() to find ROOT FOLDER: " + request.getQ() + allFileList.getFiles().size());

                                }
                                displayStatus("Loaded " + tFile.getName() + "...");
                                populateListView();
//                                pFolderName = folderList.getFiles().get(i).getName();
//                                        request.setQ(String.format("trashed = false and "
//                                        + "mimeType != 'application/vnd.google-apps.folder' and '"
//                                        + folderList.getFiles().get(i).getId() + "' in parents"));
//                                fileList = request.execute();
//                                logToFile("Query executed&size() to find SubFolders: " + request.getQ() + fileList.getFiles().size());
//                                for(int j = 0; fileList.getFiles().size() > j; j++){
//                                    fileList.getFiles().get(j).set("parent_folder", folderList.getFiles().get(i));
//                                    sharedFiles.add(fileList.getFiles().get(j));
//                                }
                                //sharedFiles.addAll((fileList.getFiles()));
                            }

                            //application/vnd.google-apps.file
                            //Query all files under 'SHARED FOLDER'
//                            request.setQ(String.format("trashed = false and "
//                                    + "mimeType != 'application/vnd.google-apps.folder' and '"
//                                    + sharedDriveId + "' in parents"));
//
//                            fileList = request.execute();
//                            logToFile("Query executed&size(): " + request.getQ() + fileList.getFiles().size());
//                            sharedFiles.addAll((fileList.getFiles()));
                            request.setPageToken(fileList.getNextPageToken());
                        } catch (UserRecoverableAuthIOException e) {
                            startActivityForResult(e.getIntent(), 2);
                        } catch (IOException e) {
                            e.printStackTrace();
                            if (request != null) {
                                request.setPageToken(null);
                            }
                        } finally {
                            displayStatus(sharedFiles.size() + " files loaded.");
                        }
                    } while (request.getPageToken() != null && request.getPageToken().length() > 0);


                    //Create Array for display the list
                    mSharedFileResultArray
                            = new BackupFileResult[sharedFiles.size()];
                    /**
                     * Create Array to display target Files in ListVIew
                     */
                    // Display target files in List View
                    initiateSharedFileListView();

                    for (int i = 0; sharedFiles.size() > i; i++) {
                        mSharedFileResultArray[i] =
                                new BackupFileResult(sharedFiles.get(i).getFile(), sharedFiles.get(i).getParentFolderName());
                        populateListView();
                        Log.i(TAG, "DriveMainActivity.setSharedFiles ---> " + sharedFiles.get(i).getFile().getName() + ":" +
                                sharedFiles.get(i).getParentFolderName());
                    }


                    // Download file stream
//                    OutputStream outputStream = new ByteArrayOutputStream();
//                    //Start sync up
//                    for(int i = 0 ; i < sharedFiles.size() ; i++){
//                        mService.files().get(sharedFiles.get(i).getId())
//                                .executeMediaAndDownloadTo(outputStream);
//                    }

                } catch (UserRecoverableAuthIOException e) {
                    startActivityForResult(e.getIntent(), 2);
                } catch (IOException e) {
                    e.printStackTrace();
                    if (request != null) {
                        request.setPageToken(null);
                    }
                } catch (NullPointerException ne) {
                    ne.printStackTrace();

                } finally {
                    isFinishLoadSharedFiles = true;
                    setSJProgressBarVisibility(ProgressBar.INVISIBLE);
                }
            }
        }).start();

    }

    private List<File> getSharedFiles() {

        List<File> sharedFiles = new ArrayList<File>();
        if (mService == null) {
            return sharedFiles;
        }
        FileList fileList = null;
        FileList folderList = null;
        Drive.Files.List request = null;

        //Set 'sharedDriveId' variable
        setSharedDriveId();

        try {
            request = mService.files().list();

            do {
                try {
                    //application/vnd.google-apps.file
                    //Query all files under 'SHARED FOLDER'
//                    request.setQ(String.format("trashed = false and "
//                            + "mimeType = 'application/vnd.google-apps.file' and '"
//                            + sharedDriveId + "' in parents"));

                    //Read -1 down folders only
//                    request.setQ(String.format("trashed = false and "
//                            + "mimeType = 'application/vnd.google-apps.folder' and '"
//                            + sharedDriveId + "' in parents"));
//
//                    folderList = request.execute();
//                    for(int i = 0; folderList.getFiles().size() > i; i++){
//                        logToFile("Query executed to search sub-folders in SharedFolder: " + request.getQ());
//
//                        request.setQ(String.format("trashed = false and "
//                            + "mimeType = 'application/vnd.google-apps.file' and '"
//                            + folderList.getFiles().get(i).getId() + "' in parents"));
//                        fileList = request.execute();
//                        sharedFiles.addAll((fileList.getFiles()));
//                    }

                    request.setQ(String.format("trashed = false and "
                            + "mimeType = 'application/vnd.google-apps.file' and '"
                            + sharedDriveId + "' in parents"));
                    fileList = request.execute();
                    logToFile("Query executed: " + request.getQ());
                    sharedFiles.addAll((fileList.getFiles()));
                    request.setPageToken(fileList.getNextPageToken());
                } catch (UserRecoverableAuthIOException e) {
                    startActivityForResult(e.getIntent(), 2);
                } catch (IOException e) {
                    e.printStackTrace();
                    if (request != null) {
                        request.setPageToken(null);
                    }
                }
            } while (request.getPageToken() != null && request.getPageToken().length() > 0);


            /**
             * Create Array to display target Files in ListVIew
             */
            mSharedFileResultArray
                    = new BackupFileResult[sharedFiles.size()];
            for (int i = 0; sharedFiles.size() > i; i++) {
                mSharedFileResultArray[i] =
                        new BackupFileResult(sharedFiles.get(i).getName()
                                , sharedFiles.get(i).size(), false);
            }
//            ArrayAdapter<BackupFileResult> mAdapter = new ArrayAdapter<BackupFileResult>(mContext, android.R.layout.simple_list_item_1, mSharedFileResultArray);
//            mAdapter.notifyDataSetChanged();
//            mAdapter.setNotifyOnChange(true);
//            mAdapter.notifyDataSetChanged();

            // Display target files in List View
            initiateSharedFileListView();

            // Download file stream
            OutputStream outputStream = new ByteArrayOutputStream();
            //Start sync up
            for (int i = 0; i < sharedFiles.size(); i++) {
                mService.files().get(sharedFiles.get(i).getId())
                        .executeMediaAndDownloadTo(outputStream);
            }


        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        populateListView();
        return sharedFiles;
    }

    public void buildSharedFileResultArray() {
        mSharedFileResultArray
                = new BackupFileResult[recorderForBackup.getDirectories().size()];
        for (int i = 0; recorderForBackup.getDirectories().size() > i; i++) {
            mSharedFileResultArray[i] =
                    new BackupFileResult(SHARED_FOLDER
                            , recorderForBackup.getNumberOfAudioFiles(), true);
            recorderForBackup.nextFolder();
        }

    }

    private void getDriveContents() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                mResultList = new ArrayList<File>();
                com.google.api.services.drive.Drive.Files f1 = mService.files();
                com.google.api.services.drive.Drive.Files.List request = null;

                do {
                    try {
                        request = f1.list();

                        request.setQ(String.format("trashed = false and "
                                + "mimeType = 'application/vnd.google-apps.folder' and 'root' in parents"));

                        com.google.api.services.drive.model.FileList fileList = request.execute();

                        mResultList.addAll(fileList.getFiles());
                        request.setPageToken(fileList.getNextPageToken());
                    } catch (UserRecoverableAuthIOException e) {
                        startActivityForResult(e.getIntent(), 2);
                    } catch (IOException e) {
                        e.printStackTrace();
                        if (request != null) {
                            request.setPageToken(null);
                        }
                    }
                } while (request.getPageToken() != null && request.getPageToken().length() > 0);

                populateListView();
            }
        });
        t.start();
    }

    private void downloadFilesFromSharedFolder() {

        //2. update ListView
        initiateSharedFileListView();

        List<File> sharedFiles = getSharedFiles();

        if (sharedFiles.isEmpty()) {
            return;
        } else {

        }
    }

    private void downLoadAllSharedFiles() {
        for (int i = 0; mSharedFileResultArray.length > i; i++) {
            downloadItemFromList(i);
        }
        //displayStatus(downloadedFileCnt + " files downloaded.");
    }

    private void downloadItemFromList(final int position) {
        //Exit when it's not DOWNLOAD MODE
        if (backup_mode != DOWNLOAD_MODE) {
            showToast(recorderForBackup.getTotalScannedFileCnt() + " files and " +
                    recorderForBackup.getTotalScannedFolderCnt() + " folders being uploaded.");
            isDownloading = 0;
            return;
        }

        final BackupFileResult fResult = (BackupFileResult) mListView.getItemAtPosition(position);
        fResult.setStatus(BackupFileResult.PROGRESS);
        //fResult.setBackupResultDescription("");
        populateListView();


        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                /**
                 * Manage simultanious thread numbers.
                 */
                try {
                    while (isDownloading > 3) {
                        //Nothing
                        Thread.sleep(3000);
                        Log.i(TAG, "Waiting===>" + isDownloading + " in side of Sleep(). " + fResult.getFileName());
                    }
                } catch (InterruptedException ie) {
                    fResult.setStatus(BackupFileResult.FAIL);
                    fResult.setBackupResultDescription(ie.toString());
                    ie.printStackTrace();
                    displayStatus(ie.toString());
                }

                try {
                    isDownloading++;
                    String fileId = fResult.getFile().getId();

                    Log.i(TAG, "DriveMainAct.downloadItemFromList--->" + fileId + ":"
                            + mService.files().get(fileId));


//                    try {
//                                    final java.io.File file = new java.io.File(Environment.getExternalStoragePublicDirectory(
//                                            Environment.DIRECTORY_MUSIC), "/SJ-RECORDER-SHARED/"+fResult.getFile().getName());
                    java.io.File targetFolder =
                            new java.io.File(recorderForBackup.getSharedFolder().getPath() + "/" + fResult.getParentFolderName());
                    if (!targetFolder.exists()) {
                        targetFolder.mkdir();
                    }

                    final java.io.File file = new java.io.File(recorderForBackup.getSharedFolder().getPath() + "/" + fResult.getParentFolderName(),
                            fResult.getFile().getName());
                    displayStatus("Downloading... " + file.getName());

                    Log.i(TAG, "Downloading: " + fResult.getFile().getName() + ":FOLDER===>(" + fResult.getParentFolderName() +
                            ") to " + recorderForBackup.getSharedFolder().getPath());
                    if (file.exists()) {
                        fResult.setStatus(BackupFileResult.SUCCESS);
                        fResult.setBackupResultDescription("Skipped, file exists");
                        displayStatus("Skipped existing file: " + file.getName());
                    } else {
                        //resp.download(new FileOutputStream(file));
                        //mService.files().get(fileId).executeMediaAndDownloadTo(new FileOutputStream(file));
                        InputStream iStream = mService.files().get(fileId).executeMediaAsInputStream();
                        boolean resultOfDownload = storeFile(file, iStream);
                        if (resultOfDownload) {
                            fResult.setStatus(BackupFileResult.SUCCESS);
                            downloadedFileCnt++;
                            fResult.setBackupResultDescription("Downloaded");
                            displayStatus("Downloaded: " + file.getName());
                        } else {
                            fResult.setStatus(BackupFileResult.FAIL);
                            fResult.setBackupResultDescription("Unknown reason");
                        }
                    }
////                                com.google.api.client.http.HttpResponse resp =
////                                        mService.getRequestFactory()
////                                                .buildGetRequest()
////                                                .execute();
//
//                                Log.i(TAG, "downloadfromlist.Headers--->" + resp.getHeaders());
//                    }catch(Exception ee){
//                        fResult.setStatus(BackupFileResult.FAIL);
//                        fResult.setBackupResultDescription(ee.toString());
//                    } finally {
//                        //outputStream.close();
//                        isDownloading--;
//                        populateListView();
//                    }
                    //InputStream iStream = resp.getContent();
                } catch (Exception e) {
                    displayStatus(e.toString());
                    fResult.setStatus(BackupFileResult.FAIL);
                    fResult.setBackupResultDescription(e.toString());
                } finally {
                    isDownloading--;
                    populateListView();
                }
            }
        });
        t.start();

    }


//
//
//    private void populateListViewTest(String str)
//    {
//        runOnUiThread(new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                mBackupFileResultArray[0].setFileName(str);
//            }
//        });
//    }

    private boolean storeFile(java.io.File file, InputStream iStream) {

        boolean result = false;
        try {
            final OutputStream oStream = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(oStream);
            BufferedInputStream bis = new BufferedInputStream(iStream);
            try {
                try {
                    final byte[] buffer = new byte[2048];
                    int read;
                    int total = 0;
                    //while ((read = iStream.read(buffer)) > -1)
                    while ((read = bis.read(buffer)) != -1) {
                        //Log.i(TAG, "storeFile--->" + read);
                        total += read;
                        bos.write(buffer, 0, read);
                    }
                    bos.flush();
                    result = true;
                    //displayStatus("Downloaded:" + file.getAbsolutePath());
                    Log.i(TAG, "Downloaded:" + file.getAbsolutePath() + "/" + file.getName() + "(" + total + ")");
                } finally {
                    bos.close();
                }
            } catch (Exception e) {
                displayStatus(e.toString());
                e.printStackTrace();
            }
        } catch (IOException e) {
            result = false;
            displayStatus(e.toString());
        }
        return result;
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_SIGN_IN:
                if (resultCode == RESULT_OK && data != null) {
                    handleSignInResult(data);
                } else {
                    updateAccountUi(null);
                    speak("Google Drive sign in was cancelled.");
                    displayStatus("Sign in cancelled. Tap Google Drive banner to sign in.");
                }
                break;
            case RESULT_STORE_FILE:
                break;
        }
    }

    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(googleAccount -> {
                    Log.d(TAG, "Signed in as " + googleAccount.getEmail());
                    initDriveService(googleAccount);
                    updateAccountUi(googleAccount);
                    speak("Connected to Google Drive as " + googleAccount.getEmail());
                    displayStatus("Connected as " + googleAccount.getEmail());
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Unable to sign in.", exception);
                    updateAccountUi(null);
                    speak("Google Drive sign in failed.");
                    displayStatus("Sign in failed. Tap banner to retry.");
                });
    }


//
//    private void saveFileToDrive()
//    {
//        Thread t = new Thread(new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                try
//                {
//                    // Create URI from real path
//                    String path;
//                    path = getPathFromUri(mFileUri);
//                    mFileUri = Uri.fromFile(new java.io.File(path));
//
//                    ContentResolver cR = DriveMainActivity.this.getContentResolver();
//
//                    // File's binary content
//                    java.io.File fileContent = new java.io.File(mFileUri.getPath());
//                    FileContent mediaContent = new FileContent(cR.getType(mFileUri), fileContent);
//
//                    showToast("Selected " + mFileUri.getPath() + "to upload");
//
//                    // File's meta data.
//                    File body = new File();
//                    body.setName(fileContent.getName());
//                    body.setMimeType(cR.getType(mFileUri));
//
//                    com.google.api.services.drive.Drive.Files f1 = mService.files();
//                    com.google.api.services.drive.Drive.Files.Create i1 = f1.create(body, mediaContent);
//                    File file = i1.execute();
//
//                    if (file != null)
//                    {
//                        showToast("Uploaded: " + file.getName());
//                    }
//                } catch (UserRecoverableAuthIOException e) {
//                    startActivityForResult(e.getIntent(), 2);
//                    errorLogs.append(e.getMessage());
//                } catch (IOException e) {
//                    errorLogs.append(e.getMessage());
//                    showToast("Transfer ERROR: " + e.toString());
//                }
//            }
//        });
//        t.start();
//    }

    @Override
    protected void onStop() {
        super.onStop();
        logToFile("=== APP onStop() ===\n");
        logToFile(errorLogs.toString() + "\n\n");
        errorLogs = new StringBuffer("");
    }

    @Override
    protected void onPause() {
        super.onPause();
        logToFile("=== APP onPause() ===\n");

    }

    public void showToast(final String toast) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public String getPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    public void displayNotification(String msg, int imageR) {
        try {
            NotificationManager mNotificationManager =
                    (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            if (mNotificationManager == null) return;

            String channelId = "sjplayer_channel_id";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                android.app.NotificationChannel channel = new android.app.NotificationChannel(
                        channelId,
                        "SJplayer Notification",
                        NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription("SJplayer Backup Notifications");
                mNotificationManager.createNotificationChannel(channel);
            }

            int notificationID = 100;
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this, channelId);

            mBuilder.setContentTitle("SJ Backup Agent");
            mBuilder.setContentText(msg);
            mBuilder.setSmallIcon(imageR);

            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(this.getPackageManager().getLaunchIntentForPackage(this.getPackageName()).getComponent());

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent intent = PendingIntent.getActivity(this, 0, i, flags);
            mBuilder.setContentIntent(intent);

            mNotificationManager.notify(notificationID, mBuilder.build());
        } catch (Exception e) {
            Log.w(TAG, "displayNotification failed: " + e.getMessage());
        }
    }

    public boolean createDriveFolder(String driveFolder) {
        boolean result = false;
        return result;
    }

    /**
     * @param folderName 2016-02-11 or anystring starting with 4 digit string and '-', then returning first 4 digits
     *                   e.g. 2016-01-01, 2015-11, 2015, 2015-10-10-daddy-in
     * @return
     */
    public String getYearFromFolderName(String folderName) {
        // Exception by failed to convert from string to int,
        // Exception by OutofIndex
        // then returning "others" folder name
        try {
            Log.i(TAG, "getYearFromFolderName==>" + folderName + ":" + folderName.substring(0, 4));
            return Integer.parseInt(folderName.substring(0, 4)) + "";
        } catch (Exception e) {
            e.printStackTrace();
            return FOLDER_OTHERS;
        }

    }
}
