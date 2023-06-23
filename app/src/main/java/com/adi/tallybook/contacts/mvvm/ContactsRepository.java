package com.adi.tallybook.contacts.mvvm;

import static com.adi.tallybook.apputilities.FireStoreConstants.PHONE_NUMBER;
import static com.adi.tallybook.apputilities.FireStoreConstants.USERS;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.adi.tallybook.contacts.utils.LoadUserContacts;
import com.adi.tallybook.models.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/*
    @ContactsRepository which access user's device contacts and saves it to the room database
    updating if the user/contact is an existing user of Tally Book or not
 */
public class ContactsRepository {

    private static final String TAG = "aditya";

    // @ContactDao object for accessing contacts_table
    private final ContactDao contactDao;

    private final Context context;

    // flags to notify if data from respective databases are loaded or not
    private boolean userContactsLoaded = false, contactDatabaseLoaded = false;

    // user devices contacts as a map
    private HashMap<String, String> userContactMap;

    // users map from Room ContactDatabase
    private HashMap<String, User> contactDatabaseMap;

    // refresh callback to the fragment
    private RefreshCallBack refreshCallBack;

    // both database refresh flags
    private boolean userContactsRefreshed = false, roomContactDatabaseRefreshed = false;

    public ContactsRepository(Context context) {
        this.context = context;
        this.contactDao = ContactDatabase.getContactDatabase(context).getContactDao();
    }

    // set call backs to both databases refresh
    public void setRefreshCallBack(RefreshCallBack refreshCallBack) {
        this.refreshCallBack = refreshCallBack;
    }

    // Refreshes the whole @ContactDatabase by following the whole refreshing database flowchart
    public void refreshContacts() {
        Log.d(TAG, "Refreshing contacts");
        /*
            To refresh the @ContactDatabase <- RoomDatabase

            We need both the User Contacts database and also @ContactDatabase
         */

        // loading both databases
        contactDatabaseLoaded = userContactsLoaded = false;

        // refreshing both databases
        userContactsRefreshed = roomContactDatabaseRefreshed = false;

        // Loading User device's contacts on background thread as a HashMap
        // map as { "phone_number" : "contact_display_name"}
        LoadUserContacts.fromDevice(context, userContactMap -> {
            this.userContactMap = userContactMap;

            // user contacts loaded
            userContactsLoaded = true;

            if (contactDatabaseLoaded) {
                // meaning both the databases are loaded
                updateDatabases();
            }
        });

        // loading Room ContactDatabase
        LoadUserContacts.fromRoom(contactDao, contactDatabaseMap -> {
            this.contactDatabaseMap = contactDatabaseMap;

            // room contact database loaded
            contactDatabaseLoaded = true;

            if (userContactsLoaded) {
                // meaning both the databases are loaded
                updateDatabases();
            }
        });

    }

    /*
        Once both databases are loaded,
        There are two tasks to be accomplished

        1. Check through every Contact of user if the contact is a registered user of Tally book.
            thus, updating it in the room database.
        2. Also, checking through the room database if any existing user has unregistered.
            thus, deleting the contact from the room database.

        Doing both the operations simultaneously on respective threads
     */
    private void updateDatabases() {
        Log.d(TAG, "Both user contact's and Room databases loaded");
        Log.d(TAG, "user contact base : " + userContactMap.size());
        Log.d(TAG, "room database contacts : " + contactDatabaseMap.size());

        // Doing 1st operation
        new Thread(this::checkThroughUserContactMap).start();

        // Doing 2nd operation
        new Thread(this::checkThroughRoomUserContacts).start();
    }

    /*
        Doing above mentioned 1st operation.
     */
    private void checkThroughUserContactMap() {
        Log.d(TAG, "checking Through UserContactMap for updates");

        for (String phone_number : userContactMap.keySet()) {

            Log.d(TAG, "checkThroughUserContactMap: " + Thread.currentThread().getName());

            FirebaseFirestore.getInstance().collection(USERS)
                    .whereIn(PHONE_NUMBER, Collections.singletonList(phone_number))
                    .get().addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {

                            User user;

                            if (task.getResult().getDocuments().size() > 0) {
                                // @phone_number found on tally books remote database
                                // thus, this is a registered user

                                user = task.getResult().getDocuments().get(0).toObject(User.class);

                                if (user != null) {

                                    // Setting display name of the user as per the name in the user's contact directory
                                    user.setDisplay_name(userContactMap.get(phone_number));

                                    // Setting flag as true, indicating this user is an registered user
                                    user.setOnTallyBook(true);

                                }
                            } else {
                                // @phone_number thus is not an registered user
                                user = new User(null, phone_number);

                                // thus, setting up display name as per user's contact directory
                                user.setDisplay_name(userContactMap.get(phone_number));

                                // also, flag as false indicating not an tally book user
                                user.setOnTallyBook(false);

                            }

                            // finally, adding or updating this user to the room database
                            if (user != null) addOrUpdate(user);

                        } else {
                            // something went wrong while querying Fire Store database
                            Log.e(TAG, "checkThroughUserContactMap: ", task.getException());
                        }

                    });

        }

        // user contacts refreshed
        userContactsRefreshed = true;

        if (roomContactDatabaseRefreshed){
            // both the databases are refreshed
            // sending call back

            if (refreshCallBack != null) {
                new Handler(Looper.getMainLooper()).post(() -> refreshCallBack.onRefresh());
            }
        }

    }

    /*
        This function adds or updates the respective user to the room database
     */
    private void addOrUpdate(@NonNull User user) {

        new Thread(() -> {

            if (contactDatabaseMap.containsKey(user.getPhone_number())) {
                // This user is already present in the room contact database
                User roomUser = contactDatabaseMap.get(user.getPhone_number());

                if (roomUser == null) roomUser = new User(null, "");

                //thus, checking if the content of them matches.
                if (!user.contentMatches(roomUser)) {
                    // User in the room database doesn't match the content with this user details
                    // thus, updating the user in room

                    contactDao.updateContact(user);
                    Log.d(TAG, "updating contact: from " + roomUser);
                    Log.d(TAG, "updating contact : to  " + user);
                }
            } else {
                // This user is not in the local room database
                // thus, adding it to the database

                contactDao.insertContact(user);
                Log.d(TAG, "adding contact: " + user);
            }

        }).start();

    }

    public void delete(User user){
        contactDao.deleteContact(user);
    }

    /*
        Checking through all the contacts in room database and deleting that contact which is not there in user contact base
        meaning, user has deleted that contact from their device
     */
    private void checkThroughRoomUserContacts() {
        Log.d(TAG, "checking Through RoomUserContacts for any deletions of contact ");

        for (String phone_number: contactDatabaseMap.keySet()){

            if (!userContactMap.containsKey(phone_number)){
                // this phone number is not there in user contact base
                // that means, user has deleted this phone number

                // thus, deleting it from room database also
                delete(contactDatabaseMap.get(phone_number));
                Log.d(TAG, "deleting contact: " + contactDatabaseMap.get(phone_number));
            }

        }

        // room contact database refreshed
        roomContactDatabaseRefreshed = true;

        if (userContactsRefreshed){
            // both databases refreshed
            // sending call back on Main Thread

            if (refreshCallBack != null) {
                new Handler(Looper.getMainLooper()).post(() -> refreshCallBack.onRefresh());
            }
        }
    }

    public LiveData<List<User>> getContactList(){
        return contactDao.getContactListLive();
    }

    @FunctionalInterface
    public interface RefreshCallBack{
        void onRefresh();
    }

}
