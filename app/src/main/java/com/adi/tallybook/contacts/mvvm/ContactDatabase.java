package com.adi.tallybook.contacts.mvvm;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.adi.tallybook.models.User;

/*
    Contacts Database

    Database where we store all contacts from user's device
 */
@Database(entities = User.class, version = 1, exportSchema = false)
public abstract class ContactDatabase extends RoomDatabase {

    private static final String CONTACTS_DB = "contacts_db";

    // Data Access Object @ContactDao to access tables and run queries on @ContactDatabase
    public abstract ContactDao getContactDao();

    private static ContactDatabase contactDatabase;

    // Return an singleton object of @ContactDatabase
    public synchronized static ContactDatabase getContactDatabase(Context context){
        if (contactDatabase == null){
            // initializing @ContactDatabase object

            contactDatabase = Room.databaseBuilder(context.getApplicationContext(),
                    ContactDatabase.class, CONTACTS_DB)
                    .fallbackToDestructiveMigration()
                    .build();
        }

        return contactDatabase;
    }
}
