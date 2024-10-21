package project2;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.ArrayList;

/*
    The StudentFakebookOracle class is derived from the FakebookOracle class and implements
    the abstract query functions that investigate the database provided via the <connection>
    parameter of the constructor to discover specific information.
*/
public final class StudentFakebookOracle extends FakebookOracle {
    // [Constructor]
    // REQUIRES: <connection> is a valid JDBC connection
    public StudentFakebookOracle(Connection connection) {
        oracle = connection;
    }

    @Override
    // Query 0
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the total number of users for which a birth month is listed
    //        (B) Find the birth month in which the most users were born
    //        (C) Find the birth month in which the fewest users (at least one) were born
    //        (D) Find the IDs, first names, and last names of users born in the month
    //            identified in (B)
    //        (E) Find the IDs, first names, and last name of users born in the month
    //            identified in (C)
    //
    // This query is provided to you completed for reference. Below you will find the appropriate
    // mechanisms for opening up a statement, executing a query, walking through results, extracting
    // data, and more things that you will need to do for the remaining nine queries
    public BirthMonthInfo findMonthOfBirthInfo() throws SQLException {
        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            // Step 1
            // ------------
            // * Find the total number of users with birth month info
            // * Find the month in which the most users were born
            // * Find the month in which the fewest (but at least 1) users were born
            ResultSet rst = stmt.executeQuery(
                    "SELECT COUNT(*) AS Birthed, Month_of_Birth " + // select birth months and number of uses with that birth month
                            "FROM " + UsersTable + " " + // from all users
                            "WHERE Month_of_Birth IS NOT NULL " + // for which a birth month is available
                            "GROUP BY Month_of_Birth " + // group into buckets by birth month
                            "ORDER BY Birthed DESC, Month_of_Birth ASC"); // sort by users born in that month, descending; break ties by birth month

            int mostMonth = 0;
            int leastMonth = 0;
            int total = 0;
            while (rst.next()) { // step through result rows/records one by one
                if (rst.isFirst()) { // if first record
                    mostMonth = rst.getInt(2); //   it is the month with the most
                }
                if (rst.isLast()) { // if last record
                    leastMonth = rst.getInt(2); //   it is the month with the least
                }
                total += rst.getInt(1); // get the first field's value as an integer
            }
            BirthMonthInfo info = new BirthMonthInfo(total, mostMonth, leastMonth);

            // Step 2
            // ------------
            // * Get the names of users born in the most popular birth month
            rst = stmt.executeQuery(
                    "SELECT User_ID, First_Name, Last_Name " + // select ID, first name, and last name
                            "FROM " + UsersTable + " " + // from all users
                            "WHERE Month_of_Birth = " + mostMonth + " " + // born in the most popular birth month
                            "ORDER BY User_ID"); // sort smaller IDs first

            while (rst.next()) {
                info.addMostPopularBirthMonthUser(new UserInfo(rst.getLong(1), rst.getString(2), rst.getString(3)));
            }

            // Step 3
            // ------------
            // * Get the names of users born in the least popular birth month
            rst = stmt.executeQuery(
                    "SELECT User_ID, First_Name, Last_Name " + // select ID, first name, and last name
                            "FROM " + UsersTable + " " + // from all users
                            "WHERE Month_of_Birth = " + leastMonth + " " + // born in the least popular birth month
                            "ORDER BY User_ID"); // sort smaller IDs first

            while (rst.next()) {
                info.addLeastPopularBirthMonthUser(new UserInfo(rst.getLong(1), rst.getString(2), rst.getString(3)));
            }

            // Step 4
            // ------------
            // * Close resources being used
            rst.close();
            stmt.close(); // if you close the statement first, the result set gets closed automatically

            return info;

        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return new BirthMonthInfo(-1, -1, -1);
        }
    }

    @Override
    // Query 1
    // -----------------------------------------------------------------------------------
    // GOALS: (A) The first name(s) with the most letters
    //        (B) The first name(s) with the fewest letters
    //        (C) The first name held by the most users
    //        (D) The number of users whose first name is that identified in (C)
    public FirstNameInfo findNameInfo() throws SQLException {
        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                FirstNameInfo info = new FirstNameInfo();
                info.addLongName("Aristophanes");
                info.addLongName("Michelangelo");
                info.addLongName("Peisistratos");
                info.addShortName("Bob");
                info.addShortName("Sue");
                info.addCommonName("Harold");
                info.addCommonName("Jessica");
                info.setCommonNameCount(42);
                return info;
            */
            FirstNameInfo info = new FirstNameInfo();
            ResultSet rs = null;

            // max len of first names
            String sqlMaxLen = "SELECT MAX(LENGTH(first_name)) FROM " + UsersTable;
            rs = stmt.executeQuery(sqlMaxLen);
            int maxLen = 0;
            if (rs.next()){
                maxLen = rs.getInt(1);
            }
            rs.close();

            // find first names w max len
            String sqlMaxNames = "SELECT DISTINCT first_name FROM " + UsersTable + " WHERE LENGTH(first_name) = " + maxLen + " ORDER BY first_name";
            rs = stmt.executeQuery(sqlMaxNames);
            while (rs.next()){
                String name = rs.getString(1);
                info.addLongName(name);
            }
            rs.close();

            // min len of first names
            String sqlMinLen = "SELECT MIN(LENGTH(first_name)) FROM " + UsersTable;
            rs = stmt.executeQuery(sqlMinLen);
            int minLen = 0;
            if (rs.next()){
                minLen = rs.getInt(1);
            }
            rs.close();

            // find first names w min len
            String sqlMinNames = "SELECT DISTINCT first_name FROM " + UsersTable + " WHERE LENGTH(first_name) = " + minLen + " ORDER BY first_name";
            rs = stmt.executeQuery(sqlMinNames);
            while (rs.next()){
                String name = rs.getString(1);
                info.addShortName(name);
            }
            rs.close();

            // Max Count of individual first names
            String sqlMaxCount = "SELECT MAX(cnt) FROM (" + "SELECT first_name, COUNT(*) AS cnt FROM " + UsersTable + " GROUP BY first_name)";
            rs = stmt.executeQuery(sqlMaxCount);
            int maxCount = 0;
            if (rs.next()) {
                maxCount = rs.getInt(1);
            }
            rs.close();
            info.setCommonNameCount(maxCount);

            // Find most common first names
            String sqlMostCommonNames = "SELECT first_name FROM (" + "SELECT first_name, COUNT(*) AS cnt FROM " + UsersTable + " GROUP BY first_name) WHERE cnt = " + maxCount + " ORDER BY first_name";
            rs = stmt.executeQuery(sqlMostCommonNames);
            while (rs.next()){
                String name = rs.getString(1);
                info.addCommonName(name);
            }
            rs.close();
            stmt.close();

            return info;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return new FirstNameInfo();
        }
    }

    @Override
    // Query 2
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the IDs, first names, and last names of users without any friends
    //
    // Be careful! Remember that if two users are friends, the Friends table only contains
    // the one entry (U1, U2) where U1 < U2.
    public FakebookArrayList<UserInfo> lonelyUsers() throws SQLException {
        FakebookArrayList<UserInfo> results = new FakebookArrayList<UserInfo>(", ");

        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                UserInfo u1 = new UserInfo(15, "Abraham", "Lincoln");
                UserInfo u2 = new UserInfo(39, "Margaret", "Thatcher");
                results.add(u1);
                results.add(u2);
            */

           String sql = "SELECT u.user_id, u.first_name, u.last_name " +
                     "FROM " + UsersTable + " u " +
                     "WHERE NOT EXISTS ( " +
                     "SELECT 1 FROM " + FriendsTable + " f " +
                     "WHERE f.user1_id = u.user_id OR f.user2_id = u.user_id ) " +
                     "ORDER BY u.user_id";
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()){
                long userId = rs.getLong("user_id");
                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");
                UserInfo user = new UserInfo(userId, firstName, lastName);
                results.add(user);
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return results;
    }

    @Override
    // Query 3
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the IDs, first names, and last names of users who no longer live
    //            in their hometown (i.e. their current city and their hometown are different)
    public FakebookArrayList<UserInfo> liveAwayFromHome() throws SQLException {
        FakebookArrayList<UserInfo> results = new FakebookArrayList<UserInfo>(", ");
        /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                UserInfo u1 = new UserInfo(9, "Meryl", "Streep");
                UserInfo u2 = new UserInfo(104, "Tom", "Hanks");
                results.add(u1);
                results.add(u2);
        */

        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            String sql = "SELECT U.user_id, U.first_name, U.last_name " +
                     "FROM " + UsersTable + " U " +
                     "JOIN " + CurrentCitiesTable + " C ON U.user_id = C.user_id " +
                     "JOIN " + HometownCitiesTable + " H ON U.user_id = H.user_id " +
                     "WHERE C.current_city_id <> H.hometown_city_id " +
                     "ORDER BY U.user_id";
            
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                long userId = rs.getLong("user_id");
                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");
                UserInfo user = new UserInfo(userId, firstName, lastName);
                results.add(user);
            }
            rs.close();

        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return results;
    }

    @Override
    // Query 4
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the IDs, links, and IDs and names of the containing album of the top
    //            <num> photos with the most tagged users
    //        (B) For each photo identified in (A), find the IDs, first names, and last names
    //            of the users therein tagged
    public FakebookArrayList<TaggedPhotoInfo> findPhotosWithMostTags(int num) throws SQLException {
        FakebookArrayList<TaggedPhotoInfo> results = new FakebookArrayList<TaggedPhotoInfo>("\n");

        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                PhotoInfo p = new PhotoInfo(80, 5, "www.photolink.net", "Winterfell S1");
                UserInfo u1 = new UserInfo(3901, "Jon", "Snow");
                UserInfo u2 = new UserInfo(3902, "Arya", "Stark");
                UserInfo u3 = new UserInfo(3903, "Sansa", "Stark");
                TaggedPhotoInfo tp = new TaggedPhotoInfo(p);
                tp.addTaggedUser(u1);
                tp.addTaggedUser(u2);
                tp.addTaggedUser(u3);
                results.add(tp);
            */
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return results;
    }

    @Override
    // Query 5
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the IDs, first names, last names, and birth years of each of the two
    //            users in the top <num> pairs of users that meet each of the following
    //            criteria:
    //              (i) same gender
    //              (ii) tagged in at least one common photo
    //              (iii) difference in birth years is no more than <yearDiff>
    //              (iv) not friends
    //        (B) For each pair identified in (A), find the IDs, links, and IDs and names of
    //            the containing album of each photo in which they are tagged together
    public FakebookArrayList<MatchPair> matchMaker(int num, int yearDiff) throws SQLException {
        FakebookArrayList<MatchPair> results = new FakebookArrayList<MatchPair>("\n");

        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                UserInfo u1 = new UserInfo(93103, "Romeo", "Montague");
                UserInfo u2 = new UserInfo(93113, "Juliet", "Capulet");
                MatchPair mp = new MatchPair(u1, 1597, u2, 1597);
                PhotoInfo p = new PhotoInfo(167, 309, "www.photolink.net", "Tragedy");
                mp.addSharedPhoto(p);
                results.add(mp);
            */
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return results;
    }

    @Override
    // Query 6
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the IDs, first names, and last names of each of the two users in
    //            the top <num> pairs of users who are not friends but have a lot of
    //            common friends
    //        (B) For each pair identified in (A), find the IDs, first names, and last names
    //            of all the two users' common friends
    public FakebookArrayList<UsersPair> suggestFriends(int num) throws SQLException {
        FakebookArrayList<UsersPair> results = new FakebookArrayList<UsersPair>("\n");

        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                UserInfo u1 = new UserInfo(16, "The", "Hacker");
                UserInfo u2 = new UserInfo(80, "Dr.", "Marbles");
                UserInfo u3 = new UserInfo(192, "Digit", "Le Boid");
                UsersPair up = new UsersPair(u1, u2);
                up.addSharedFriend(u3);
                results.add(up);
            */

         //bidirectional friendship view
        String allFriendsQuery = "SELECT user1_id AS user_id1, user2_id AS user_id2 FROM " + FriendsTable +
                                 " UNION ALL SELECT user2_id AS user_id1, user1_id AS user_id2 FROM " + FriendsTable;

        //pairs of users who share mutual friends and are not friends themselves
        String mutualFriendsQuery =
                "SELECT mf.user_id1, mf.user_id2, COUNT(*) AS mutual_friends_count FROM (" +
                    "SELECT af1.user_id1 AS user_id1, af2.user_id1 AS user_id2 " +
                    "FROM (" + allFriendsQuery + ") af1 " +
                    "JOIN (" + allFriendsQuery + ") af2 ON af1.user_id2 = af2.user_id2 " +
                    "WHERE af1.user_id1 < af2.user_id1 " +
                ") mf " +
                "LEFT JOIN " + FriendsTable + " f ON f.user1_id = mf.user_id1 AND f.user2_id = mf.user_id2 " +
                "WHERE f.user1_id IS NULL " +
                "GROUP BY mf.user_id1, mf.user_id2 " +
                "ORDER BY mutual_friends_count DESC, mf.user_id1 ASC, mf.user_id2 ASC";

        // Limit to top num pairs
        String topPairsQuery = "SELECT * FROM (" + mutualFriendsQuery + ") WHERE ROWNUM <= " + num;

        ResultSet rsTopPairs = stmt.executeQuery(topPairsQuery);

        int numPairs = 0;
        long[] user1Ids = new long[num];
        long[] user2Ids = new long[num];

        FakebookArrayList<Long> userIds = new FakebookArrayList<Long>(", ");

        while (rsTopPairs.next() && numPairs < num) {
            long userId1 = rsTopPairs.getLong("user_id1");
            long userId2 = rsTopPairs.getLong("user_id2");
            int mutualFriendsCount = rsTopPairs.getInt("mutual_friends_count");

            user1Ids[numPairs] = userId1;
            user2Ids[numPairs] = userId2;
            numPairs++;

            if (!userIds.contains(userId1)) {
                userIds.add(userId1);
            }
            if (!userIds.contains(userId2)) {
                userIds.add(userId2);
            }
        }
        rsTopPairs.close();

        int numUsers = userIds.size();
        long[] userIdsArray = new long[numUsers];
        String[] firstNames = new String[numUsers];
        String[] lastNames = new String[numUsers];
        for (int i = 0; i < numUsers; i++) {
            userIdsArray[i] = userIds.get(i);
        }

        if (numUsers > 0) {
            //comma-separated list of user IDs
            StringBuilder userIdsStrBuilder = new StringBuilder();
            for (int i = 0; i < numUsers; i++) {
                if (i > 0) {
                    userIdsStrBuilder.append(",");
                }
                userIdsStrBuilder.append(userIdsArray[i]);
            }
            String userIdsStr = userIdsStrBuilder.toString();

            String getUsersQuery = "SELECT user_id, first_name, last_name FROM " + UsersTable +
                                   " WHERE user_id IN (" + userIdsStr + ")";

            ResultSet rsUsers = stmt.executeQuery(getUsersQuery);

            while (rsUsers.next()) {
                long userId = rsUsers.getLong("user_id");
                String firstName = rsUsers.getString("first_name");
                String lastName = rsUsers.getString("last_name");

                for (int i = 0; i < numUsers; i++) {
                    if (userIdsArray[i] == userId) {
                        firstNames[i] = firstName;
                        lastNames[i] = lastName;
                        break;
                    }
                }
            }
            rsUsers.close();
        }

        // Now, create UsersPair objects with full UserInfo
        for (int i = 0; i < numPairs; i++) {
            long userId1 = user1Ids[i];
            long userId2 = user2Ids[i];

            // Find user1 info
            UserInfo user1 = null;
            UserInfo user2 = null;
            for (int j = 0; j < numUsers; j++) {
                if (userIdsArray[j] == userId1) {
                    user1 = new UserInfo(userId1, firstNames[j], lastNames[j]);
                }
                if (userIdsArray[j] == userId2) {
                    user2 = new UserInfo(userId2, firstNames[j], lastNames[j]);
                }
                if (user1 != null && user2 != null) {
                    break;
                }
            }

            UsersPair pair = new UsersPair(user1, user2);

            // Query to get mutual friend IDs
            String mutualFriendIdsQuery =
                    "SELECT DISTINCT af1.user_id2 AS mutual_friend_id " +
                    "FROM (" + allFriendsQuery + ") af1 " +
                    "JOIN (" + allFriendsQuery + ") af2 ON af1.user_id2 = af2.user_id2 " +
                    "WHERE af1.user_id1 = " + userId1 +
                    " AND af2.user_id1 = " + userId2;

            ResultSet rsMutualFriends = stmt.executeQuery(mutualFriendIdsQuery);

            // Collect mutual friend IDs
            FakebookArrayList<Long> mutualFriendIds = new FakebookArrayList<Long>(", ");
            while (rsMutualFriends.next()) {
                long mutualFriendId = rsMutualFriends.getLong("mutual_friend_id");
                if (!mutualFriendIds.contains(mutualFriendId)) {
                    mutualFriendIds.add(mutualFriendId);
                }
            }
            rsMutualFriends.close();

            if (!mutualFriendIds.isEmpty()) {
                //comma-separated list of mutual friend IDs
                StringBuilder mutualFriendIdsStrBuilder = new StringBuilder();
                for (int j = 0; j < mutualFriendIds.size(); j++) {
                    if (j > 0) {
                        mutualFriendIdsStrBuilder.append(",");
                    }
                    mutualFriendIdsStrBuilder.append(mutualFriendIds.get(j));
                }
                String mutualFriendIdsStr = mutualFriendIdsStrBuilder.toString();

                String getMutualFriendsQuery = "SELECT user_id, first_name, last_name FROM " + UsersTable +
                                               " WHERE user_id IN (" + mutualFriendIdsStr + ") ORDER BY user_id";

                ResultSet rsMutualFriendDetails = stmt.executeQuery(getMutualFriendsQuery);
                while (rsMutualFriendDetails.next()) {
                    long mfId = rsMutualFriendDetails.getLong("user_id");
                    String firstName = rsMutualFriendDetails.getString("first_name");
                    String lastName = rsMutualFriendDetails.getString("last_name");
                    UserInfo mutualFriend = new UserInfo(mfId, firstName, lastName);
                    pair.addSharedFriend(mutualFriend);
                }
                rsMutualFriendDetails.close();
            }

            results.add(pair);
        }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return results;
    }

    @Override
    // Query 7
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the name of the state or states in which the most events are held
    //        (B) Find the number of events held in the states identified in (A)
    public EventStateInfo findEventStates() throws SQLException {
        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                EventStateInfo info = new EventStateInfo(50);
                info.addState("Kentucky");
                info.addState("Hawaii");
                info.addState("New Hampshire");
                return info;
            */
            return new EventStateInfo(-1); // placeholder for compilation
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return new EventStateInfo(-1);
        }
    }

    @Override
    // Query 8
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the ID, first name, and last name of the oldest friend of the user
    //            with User ID <userID>
    //        (B) Find the ID, first name, and last name of the youngest friend of the user
    //            with User ID <userID>
    public AgeInfo findAgeInfo(long userID) throws SQLException {
        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                UserInfo old = new UserInfo(12000000, "Galileo", "Galilei");
                UserInfo young = new UserInfo(80000000, "Neil", "deGrasse Tyson");
                return new AgeInfo(old, young);
            */
            return new AgeInfo(new UserInfo(-1, "UNWRITTEN", "UNWRITTEN"), new UserInfo(-1, "UNWRITTEN", "UNWRITTEN")); // placeholder for compilation
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return new AgeInfo(new UserInfo(-1, "ERROR", "ERROR"), new UserInfo(-1, "ERROR", "ERROR"));
        }
    }

    @Override
    // Query 9
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find all pairs of users that meet each of the following criteria
    //              (i) same last name
    //              (ii) same hometown
    //              (iii) are friends
    //              (iv) less than 10 birth years apart
    public FakebookArrayList<SiblingInfo> findPotentialSiblings() throws SQLException {
        FakebookArrayList<SiblingInfo> results = new FakebookArrayList<SiblingInfo>("\n");

        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                UserInfo u1 = new UserInfo(81023, "Kim", "Kardashian");
                UserInfo u2 = new UserInfo(17231, "Kourtney", "Kardashian");
                SiblingInfo si = new SiblingInfo(u1, u2);
                results.add(si);
            */
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return results;
    }

    // Member Variables
    private Connection oracle;
    private final String UsersTable = FakebookOracleConstants.UsersTable;
    private final String CitiesTable = FakebookOracleConstants.CitiesTable;
    private final String FriendsTable = FakebookOracleConstants.FriendsTable;
    private final String CurrentCitiesTable = FakebookOracleConstants.CurrentCitiesTable;
    private final String HometownCitiesTable = FakebookOracleConstants.HometownCitiesTable;
    private final String ProgramsTable = FakebookOracleConstants.ProgramsTable;
    private final String EducationTable = FakebookOracleConstants.EducationTable;
    private final String EventsTable = FakebookOracleConstants.EventsTable;
    private final String AlbumsTable = FakebookOracleConstants.AlbumsTable;
    private final String PhotosTable = FakebookOracleConstants.PhotosTable;
    private final String TagsTable = FakebookOracleConstants.TagsTable;
}
