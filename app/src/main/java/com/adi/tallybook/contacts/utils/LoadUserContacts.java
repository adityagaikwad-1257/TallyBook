package com.adi.tallybook.contacts.utils;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import com.adi.tallybook.contacts.mvvm.ContactDao;
import com.adi.tallybook.models.User;

import java.util.HashMap;
import java.util.List;

public abstract class LoadUserContacts {
    private static final String TAG = "aditya";

    @SuppressLint("Range")
    public static void fromDevice(Context context, FromDeviceLoadSuccess listener){
        new Thread(() -> {
            HashMap<String, String> contactMap = new HashMap<>();

            Log.d(TAG, "loading user contacts");

            ContentResolver cr = context.getContentResolver();
            Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                    null, null, null, null);

            if ((cur != null ? cur.getCount() : 0) > 0) {
                while (cur.moveToNext()) {
                    String id = cur.getString(
                            cur.getColumnIndex(ContactsContract.Contacts._ID));
                    String name = cur.getString(cur.getColumnIndex(
                            ContactsContract.Contacts.DISPLAY_NAME));

                    if (cur.getInt(cur.getColumnIndex(
                            ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                        Cursor pCur = cr.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                new String[]{id}, null);
                        while (pCur.moveToNext()) {
                            String phoneNo = pCur.getString(pCur.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER));

                            contactMap.put(modify(phoneNo), name);
                        }
                        pCur.close();
                    }
                }

                cur.close();
            }

            Log.d(TAG, "loading user contacts completed");
            // sending callback
            listener.onContactLoaded(contactMap);
        }).start();
    }

    /*
        Some phone numbers doesn't have +91 at prefix
        Thus, this function adds
     */
    private static String modify(String phoneNo) {
        if (phoneNo == null || phoneNo.isEmpty()) return "";

        // removing spaces
        phoneNo = phoneNo.replace(" ", "");

        if (phoneNo.length() < 3) return phoneNo;

        if (phoneNo.startsWith("+91")) return phoneNo;
        else return "+91"+phoneNo;
    }

    // loads contacts from room database
    public static void fromRoom(ContactDao contactDao, FromRoomLoadSuccess listener){
        new Thread(() -> {

            List<User> contactList = contactDao.getContactsList();

            // storing contact list as map of phone number and contact itself

            HashMap<String, User> contactMap = new HashMap<>();
            for (User user: contactList) contactMap.put(user.getPhone_number(), user);

            listener.onContactLoaded(contactMap);

        }).start();
    }

    @FunctionalInterface
    public interface FromDeviceLoadSuccess{
        void onContactLoaded(HashMap<String, String> contactMap);
    }

    @FunctionalInterface
    public interface FromRoomLoadSuccess{
        void onContactLoaded(HashMap<String, User> userHashMap);
    }

}
