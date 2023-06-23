package com.adi.tallybook.contacts.mvvm;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.adi.tallybook.models.User;

import java.util.LinkedList;
import java.util.List;

@Dao
public interface ContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertContact(User user);

    @Update
    void updateContact(User user);

    @Delete
    void deleteContact(User user);

    @Query("select * from contacts_table")
    List<User> getContactsList();

    @Query("select * from contacts_table")
    LiveData<List<User>> getContactListLive();

}