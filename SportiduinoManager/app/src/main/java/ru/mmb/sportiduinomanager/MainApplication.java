package ru.mmb.sportiduinomanager;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.acra.ACRA;
import org.acra.BuildConfig;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraHttpSender;
import org.acra.annotation.AcraToast;
import org.acra.data.StringFormat;
import org.acra.sender.HttpSender;

import ru.mmb.sportiduinomanager.model.Distance;
import ru.mmb.sportiduinomanager.model.Station;

/**
 * Keeps all persistent activity data for application lifetime.
 */

// ACRA exception reporting system
@AcraCore(buildConfigClass = BuildConfig.class,
        reportFormat = StringFormat.JSON,
        reportContent = {org.acra.ReportField.REPORT_ID,
                org.acra.ReportField.APP_VERSION_CODE,
                org.acra.ReportField.APP_VERSION_NAME,
                org.acra.ReportField.PACKAGE_NAME,
                org.acra.ReportField.FILE_PATH,
                org.acra.ReportField.PHONE_MODEL,
                org.acra.ReportField.BRAND,
                org.acra.ReportField.PRODUCT,
                org.acra.ReportField.ANDROID_VERSION,
                org.acra.ReportField.BUILD,
                org.acra.ReportField.TOTAL_MEM_SIZE,
                org.acra.ReportField.AVAILABLE_MEM_SIZE,
                org.acra.ReportField.CUSTOM_DATA,
                org.acra.ReportField.STACK_TRACE,
                org.acra.ReportField.INITIAL_CONFIGURATION,
                org.acra.ReportField.CRASH_CONFIGURATION,
                org.acra.ReportField.DISPLAY,
                org.acra.ReportField.USER_APP_START_DATE,
                org.acra.ReportField.USER_CRASH_DATE,
                org.acra.ReportField.LOGCAT,
                org.acra.ReportField.EVENTSLOG,
                org.acra.ReportField.RADIOLOG,
                org.acra.ReportField.INSTALLATION_ID,
                org.acra.ReportField.DEVICE_FEATURES,
                org.acra.ReportField.ENVIRONMENT,
                org.acra.ReportField.SETTINGS_SYSTEM,
                org.acra.ReportField.SETTINGS_SECURE,
                org.acra.ReportField.SETTINGS_GLOBAL,
                org.acra.ReportField.THREAD_DETAILS,
                org.acra.ReportField.BUILD_CONFIG})
@AcraHttpSender(uri = "http://mmb.progressor.ru/php/mmbscripts/acra.php",
        httpMethod = HttpSender.Method.POST)
@AcraToast(resText = R.string.acra_toast_text)

@SuppressWarnings("WeakerAccess")
public final class MainApplication extends Application {

    /**
     * Name of local SQLite database.
     */
    static final String DB_NAME = "mmb.sqlite";
    /**
     * Alpha for disabled buttons appearance in the application.
     */
    static final float DISABLED_BUTTON = .5f;
    /**
     * Alpha for enabled buttons appearance in the application.
     */
    static final float ENABLED_BUTTON = 1f;
    /**
     * Copy of main application context.
     */
    private Context mContext;
    /**
     * Handler of SQLite database.
     */
    private SQLiteDatabase mDb;
    /**
     * Description of error occurred during application startup (if any).
     */
    private String mStartupError = "";
    /**
     * Current status of SQLite database.
     */
    private int mDbStatus = Distance.DB_STATE_UNKNOWN;

    /**
     * Id of distance download background process.
     */
    private long mDistanceDlId = -1L;
    /**
     * Email of the user who authorized (or tried to authorize) at the site.
     */
    private String mUserEmail = "";
    /**
     * Password of the user who authorized (or tried to authorize) at the site.
     */
    private String mUserPassword = "";
    /**
     * Which website database we are using, main of test.
     */
    private int mTestSite;
    /**
     * Distance downloaded from site or loaded from local database.
     */
    private Distance mDistance;

    /**
     * List of previously discovered Bluetooth devices.
     */
    private List<BluetoothDevice> mBTDeviceList = new ArrayList<>();
    /**
     * Connected Bluetooth station.
     */
    private Station mStation;

    /**
     * Current team number at chip initialization.
     */
    private int mTeamNumber;

    /**
     * Current members selection mask at chip initialization.
     */
    private int mTeamMask;

    /**
     * Get main application context.
     *
     * @return context
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * Get database object.
     *
     * @return Database handler
     */
    public SQLiteDatabase getDatabase() {
        return mDb;
    }

    /**
     * Get startup error message (if any).
     *
     * @return Error message string
     */
    public String getStartupError() {
        return mStartupError;
    }

    /**
     * Get local SQLite database status.
     *
     * @return The status
     */
    public int getDbStatus() {
        if (mDbStatus == Distance.DB_STATE_UNKNOWN) updateDbStatus();
        return mDbStatus;
    }

    /**
     * Update local SQLite database status.
     */
    public void updateDbStatus() {
        mDbStatus = Distance.getDbStatus(mDb);
    }

    /**
     * Get id of distance download process.
     *
     * @return Process id
     */
    public long getDistanceDownloadId() {
        return mDistanceDlId;
    }

    /**
     * Save id of distance download process.
     *
     * @param downloadId Process id
     */
    public void setDistanceDownloadId(final long downloadId) {
        mDistanceDlId = downloadId;
    }

    /**
     * Get the email of the user who authorized (or tried to authorize) at the site.
     *
     * @return User email
     */
    public String getUserEmail() {
        return mUserEmail;
    }

    /**
     * Get the password of the user who authorized (or tried to do it) at the site.
     *
     * @return User password
     */
    public String getUserPassword() {
        return mUserPassword;
    }

    /**
     * Get info about which website database we are using, main of test.
     *
     * @return True if we are using test database
     */
    public int getTestSite() {
        return mTestSite;
    }

    /**
     * Keep email and password of authorized user
     * and info about which website database we are using, main of test.
     *
     * @param userEmail    Email entered by user
     * @param userPassword Password entered by user
     * @param testSite     Test site selection (equal 1 if selected)
     */
    public void setAuthorizationParameters(final String userEmail, final String userPassword, final int testSite) {
        mUserEmail = userEmail;
        mUserPassword = userPassword;
        mTestSite = testSite;
    }

    /**
     * Get distance loaded to persistent memory.
     *
     * @return The distance
     */
    public Distance getDistance() {
        return mDistance;
    }

    /**
     * Save distance to persistent memory.
     *
     * @param distance The distance to save
     */
    public void setDistance(final Distance distance) {
        mDistance = distance;
    }

    /**
     * Get the list of previously discovered Bluetooth devices.
     *
     * @return List od Bluetooth devices
     */
    public List<BluetoothDevice> getBTDeviceList() {
        return mBTDeviceList;
    }

    /**
     * Save the list of previously discovered Bluetooth devices.
     *
     * @param deviceList List od Bluetooth devices
     */
    public void setBTDeviceList(final List<BluetoothDevice> deviceList) {
        mBTDeviceList = deviceList;
    }

    /**
     * Get the connected Bluetooth station.
     *
     * @return Bluetooth device
     */
    public Station getStation() {
        return mStation;
    }

    /**
     * Save the connected Bluetooth station.
     *
     * @param station Bluetooth device
     */
    public void setStation(final Station station) {
        mStation = station;
    }

    /**
     * Get the current team number during chip initialization.
     *
     * @return Team number
     */
    public int getTeamNumber() {
        return mTeamNumber;
    }

    /**
     * Save the current team number during chip initialization.
     *
     * @param teamNumber Team number
     */
    public void setTeamNumber(final int teamNumber) {
        mTeamNumber = teamNumber;
    }

    /**
     * Get the current team mask during chip initialization.
     *
     * @return Team number as string
     */
    public int getTeamMask() {
        return mTeamMask;
    }

    /**
     * Save the current team mask during chip initialization.
     *
     * @param teamMask Team mask
     */
    public void setTeamMask(final int teamMask) {
        mTeamMask = teamMask;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        final Context context = getApplicationContext();
        mContext = context;
        // Set russian locale for debug build type
        if (BuildConfig.DEBUG) {
            switchToRussian(context);
        }
        // Get default path to application databases
        // and create databases folder if not exist
        final File databasePath = context.getDatabasePath(DB_NAME);
        final File folder = databasePath.getParentFile();
        if (folder != null && !folder.exists()) {
            final boolean success = folder.mkdir();
            if (!success) {
                mStartupError = getResources()
                        .getString(R.string.err_db_cant_create_dir).concat(folder.getAbsolutePath());
                return;
            }
        }
        // Try to open database (create it if it does not exist)
        try {
            final SQLiteDatabase database = SQLiteDatabase.openDatabase(
                    databasePath.getAbsolutePath(), null, SQLiteDatabase.CREATE_IF_NECESSARY);
            database.setLocale(new Locale("ru_RU"));
            mDb = database;
        } catch (SQLiteException e) {
            mStartupError = e.getMessage();
        }
        // Check database status
        updateDbStatus();
        if (mDbStatus == Distance.DB_STATE_OK) {
            // Try to load distance from database
            final Distance distance = new Distance(mDb);
            if (distance.hasErrors()) {
                mDbStatus = Distance.DB_STATE_FAILED;

            } else {
                mDistance = distance;
                // Get user email, password and test/main database flag from loaded distance
                mUserEmail = mDistance.getUserEmail();
                mUserPassword = mDistance.getUserPassword();
                mTestSite = mDistance.getTestSite();
            }
        }
    }


    /**
     * Switch to russian locale in debug version.
     *
     * @param context Application context
     */
    @SuppressWarnings("deprecation")
    private void switchToRussian(final Context context) {
        final Locale locale = new Locale("ru");
        Locale.setDefault(locale);
        final Resources res = context.getResources();
        final Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    @Override
    public void onTerminate() {
        // Close database on app termination
        if (mDb != null) {
            mDb.close();
        }
        super.onTerminate();
    }

    @Override
    protected void attachBaseContext(final Context base) {
        super.attachBaseContext(base);
        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }
}
