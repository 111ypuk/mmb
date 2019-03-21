package ru.mmb.sportiduinomanager.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Support of distance parameters, points lists and discounts.
 */
public final class Distance {
    /**
     * Raid_id from website database.
     */
    private final int mRaidId;
    /**
     * Raid_name from website database.
     */
    private final String mRaidName;
    /**
     * Unixtime when distance has been downloaded from site.
     */
    private final long mTimeDownloaded;
    /**
     * Unixtime when website database will(was) become readonly.
     */
    private final long mTimeReadonly;
    /**
     * Unixtime of last active point closing time.
     */
    private final long mTimeFinish;
    /**
     * Email of authorized user who performs all interaction with website.
     */
    private final String mUserEmail;
    /**
     * Password of authorized user who performs all interaction with website.
     */
    private final String mUserPassword;
    /**
     * Which website database is used, test or main.
     */
    private final int mTestSite;
    /**
     * Sparse array of active points, array index == point number.
     */
    private Point[] mPoints;
    /**
     * List of discounts.
     */
    private Discount[] mDiscounts;

    /**
     * Construct distance from imported data.
     *
     * @param userEmail      Email of the user downloading the raid
     * @param userPassword   Password of the user downloading the raid
     * @param testSite       Download raid test site instead of main site
     * @param raidId         ID of the raid
     * @param timeDownloaded Time when the distance was download from site
     * @param timeReadonly   Time when the raid becomes readonly
     * @param timeFinish     Time when the last active point is closed
     * @param raidName       ASCII raid name
     */
    public Distance(final String userEmail, final String userPassword, final int testSite,
                    final int raidId, final String raidName, final long timeDownloaded,
                    final long timeReadonly, final long timeFinish) {
        mUserEmail = userEmail;
        mUserPassword = userPassword;
        mTestSite = testSite;
        mRaidId = raidId;
        mRaidName = raidName;
        mTimeDownloaded = timeDownloaded;
        mTimeReadonly = timeReadonly;
        mTimeFinish = timeFinish;
    }

    /**
     * Get email of the authorized user who downloaded the distance from website.
     *
     * @return User email
     */
    public String getUserEmail() {
        return mUserEmail;
    }

    /**
     * Get password of the authorized user who downloaded the distance from website.
     *
     * @return User password
     */
    public String getUserPassword() {
        return mUserPassword;
    }

    /**
     * Get info about which website database we are using, main of test.
     *
     * @return Test site flag
     */
    public int getTestSite() {
        return mTestSite;
    }

    /**
     * Get the time when the distance was downloaded from site.
     *
     * @return Unixtime
     */
    long getTimeDownloaded() {
        return mTimeDownloaded;
    }

    /**
     * Get raid id.
     *
     * @return Raid id
     */
    int getRaidId() {
        return mRaidId;
    }

    /**
     * Get name of current raid.
     *
     * @return Raid name
     */
    public String getRaidName() {
        return mRaidName;
    }

    /**
     * Get time when the distance will become readonly at the site.
     *
     * @return Unixtime
     */
    long getTimeReadonly() {
        return mTimeReadonly;
    }

    /**
     * Get time when the last raid point will be closed.
     *
     * @return Unixtime
     */
    long getTimeFinish() {
        return mTimeFinish;
    }

    /**
     * Get time of downloading distance from site as a string.
     *
     * @return Formatted datetime
     */
    public String getDownloadDate() {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(mTimeDownloaded * 1000);
        final DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        return format.format(calendar.getTime());
    }

    /**
     * Get list of active points names.
     *
     * @return List of names
     */
    public List<String> getPointNames() {
        final List<String> names = new ArrayList<>();
        for (final Point point : mPoints) {
            if (point != null) names.add(point.mName);
        }
        return names;
    }

    /**
     * Fill lists with all points parameters for fast saving in local database.
     *
     * @param numbers    Points numbers
     * @param types      Points types
     * @param penalties  Points penalties
     * @param startTimes Points opening times
     * @param endTimes   Points closing times
     * @param names      Points names
     */
    void fillPointsLists(final List<Integer> numbers, final List<Integer> types,
                                final List<Integer> penalties, final List<Long> startTimes,
                                final List<Long> endTimes, final List<String> names) {
        for (int i = 1; i < mPoints.length; i++) {
            if (mPoints[i] == null) continue;
            numbers.add(i);
            types.add(mPoints[i].mType);
            penalties.add(mPoints[i].mPenalty);
            startTimes.add(mPoints[i].mStart);
            endTimes.add(mPoints[i].mEnd);
            names.add(mPoints[i].mName);
        }
    }

    /**
     * Fill lists with all discounts parameters for fast saving in local database.
     *
     * @param minutes Discounts in minutes
     * @param fromN   Discount intervals starting points
     * @param toN     Discount intervals ending points
     */
    void fillDiscountsLists(final List<Integer> minutes, final List<Integer> fromN,
                                   final List<Integer> toN) {
        for (final Discount discount : mDiscounts) {
            minutes.add(discount.mMinutes);
            fromN.add(discount.mFrom);
            toN.add(discount.mTo);
        }
    }

    /**
     * Get the point type.
     *
     * @param number Point number
     * @return Point type
     */
    public int getPointType(final int number) {
        if (number < 0 || number >= mPoints.length || mPoints[number] == null) return -1;
        return mPoints[number].mType;
    }

    /**
     * Check if the database can be reloaded from server loosing all current data.
     *
     * @return True if it can be reloaded
     */
    public boolean canBeReloaded() {
        // Allow data loss if it was not initialized correctly
        if (mTimeReadonly == 0 || mTimeFinish == 0) return true;
        // Get current time
        final long now = System.currentTimeMillis() / 1000L;
        // Allow data loss if distance was not set readonly yet
        // (we can and should reload it)
        if (now < mTimeReadonly) return true;
        // Allow data loss if race was finished more then 1 month ago
        // (race was finalized anyway)
        return now > (mTimeFinish + 3600 * 24 * 30);
        // TODO: Add check for team results which were not uploaded to server
    }

    /**
     * Allocate point array with maxIndex as max array index.
     *
     * @param maxIndex       Max index in point array
     * @param initChipsPoint Name of pseudo point for chip initialization
     */
    void initPointArray(final int maxIndex, final String initChipsPoint) {
        mPoints = new Point[maxIndex + 1];
        mPoints[0] = new Point(0, 0, 0, 0, initChipsPoint);
    }

    /**
     * Allocate discount array with nDiscount as array size.
     *
     * @param numberOfDiscounts Number of discounts
     */
    void initDiscountArray(final int numberOfDiscounts) {
        mDiscounts = new Discount[numberOfDiscounts];
    }

    /**
     * Construct point and save it to appropriate position in point array.
     *
     * @param index   Position in point array
     * @param type    Point type (start, finish, etc)
     * @param penalty Penalty for missing the point
     * @param start   Unixtime when point starts registering of teams
     * @param end     Unixtime when point ends registering of teams
     * @param name    Point name
     * @return True in case of valid index value
     */
    boolean addPoint(final int index, final int type, final int penalty, final long start, final long end,
                            final String name) {
        // Check if point array was initialized
        if (mPoints == null) return false;
        // Check if point index is valid
        if (index <= 0 || index >= mPoints.length) return false;
        // Check if the point was already set
        if (mPoints[index] == null) {
            // set the point
            mPoints[index] = new Point(type, penalty, start, end, name);
            return true;
        }
        return false;
    }

    /**
     * Add new discount to the list of discounts.
     *
     * @param minutes   The discount
     * @param fromPoint First point of discount interval
     * @param toPoint   Last point of discount interval
     * @return True in case of success
     */
    boolean addDiscount(final int minutes, final int fromPoint, final int toPoint) {
        // Check if discount array was initialized
        if (mDiscounts == null) return false;
        for (int i = 0; i < mDiscounts.length; i++) {
            if (mDiscounts[i] == null) {
                mDiscounts[i] = new Discount(minutes, fromPoint, toPoint);
                return true;
            }
        }
        return false;
    }

    /**
     * Check the distance (loaded from site or from local db) for various errors.
     *
     * @return True if some errors were found
     */
    public boolean hasErrors() {
        // Check distance parameters
        if (mRaidId <= 0) return true;
        if (mTimeReadonly <= 0) return true;
        if (mTimeFinish <= 0) return true;
        if (mTimeFinish <= mTimeReadonly) return true;
        if ("".equals(mRaidName)) return true;

        // Check if some points were loaded
        if (mPoints == null) return true;
        if (mPoints.length <= 1) return true;
        // Check if all points were loaded
        if (mPoints[0] == null) return true;
        if (mPoints[mPoints.length - 1] == null) return true;
        // check point data
        for (int i = 1; i < mPoints.length; i++) {
            if (mPoints[i] != null) {
                if (mPoints[i].mType <= 0 || mPoints[i].mType > 5) return true;
                if (mPoints[i].mPenalty < 0) return true;
                if (mPoints[i].mStart > 0 && mPoints[i].mEnd < mPoints[i].mStart) return true;
                if ("".equals(mPoints[i].mName)) return true;
            }
        }

        // Check if some discounts were loaded
        if (mDiscounts == null) return true;
        if (mDiscounts.length == 0) return false;
        // Check discounts data
        for (final Discount discount : mDiscounts) {
            // Check if all discounts were loaded
            if (discount == null) return true;
            // Check discount value
            if (discount.mMinutes <= 0) return true;
            // Check discount interval
            if (discount.mFrom <= 0 || discount.mFrom >= mPoints.length) return true;
            if (discount.mTo <= 0 || discount.mTo >= mPoints.length) return true;
            if (discount.mFrom >= discount.mTo) return true;
            if (mPoints[discount.mFrom] == null) return true;
            if (mPoints[discount.mTo] == null) return true;
        }

        // No errors were detected
        return false;
    }

    /*
    public String getPointName(final int index) {
        if (index < 0 || index >= mPoints.size()) return UNKNOWN_POINT;
        Point point = mPoints.get(index);
        if (point == null) return UNKNOWN_POINT;
        return point.mName;
    }*/

    /**
     * An active point parameters.
     */
    private class Point {
        /**
         * Point type (start, finish, etc).
         */
        final int mType;
        /**
         * Penalty in minutes for missing the point.
         */
        final int mPenalty;
        /**
         * Unixtime when the point starts to work.
         */
        final long mStart;
        /**
         * Unixtime when the point ends working.
         */
        final long mEnd;
        /**
         * Point name.
         */
        final String mName;

        /**
         * Constructor for Point class.
         *
         * @param type    Point type
         * @param penalty Penalty for missing this point
         * @param start   Time at which this point start working
         * @param end     Time at which this point stop working
         * @param name    Point name
         */
        Point(final int type, final int penalty, final long start, final long end, final String name) {
            mType = type;
            mPenalty = penalty;
            mStart = start;
            mEnd = end;
            mName = name;
        }
    }

    /**
     * Discount for missing some points.
     */
    private class Discount {
        /**
         * The discount in minutes.
         */
        final int mMinutes;
        /**
         * First point of the distance part where discount is active.
         */
        final int mFrom;
        /**
         * Last point of the interval.
         */
        final int mTo;

        /**
         * Constructor for Discount class.
         *
         * @param minutes   Value of discount in minutes
         * @param fromPoint Starting point for discount interval
         * @param toPoint   Ending point for discount interval
         */
        Discount(final int minutes, final int fromPoint, final int toPoint) {
            mMinutes = minutes;
            mFrom = fromPoint;
            mTo = toPoint;
        }
    }
}
