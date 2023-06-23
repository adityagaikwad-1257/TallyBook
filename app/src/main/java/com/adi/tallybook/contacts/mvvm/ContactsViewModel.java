package com.adi.tallybook.contacts.mvvm;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.adi.tallybook.contacts.adapters.ContactsAdapter;
import com.adi.tallybook.models.User;

import java.util.List;

public class ContactsViewModel extends AndroidViewModel {

    private final ContactsRepository contactsRepository;

    // flag if contact list is being refreshed
    private boolean refreshing = false;

    // RV adapter for contact list
    private final ContactsAdapter contactsAdapter;

    // search input
    public String searchInput = null;

    public ContactsViewModel(@NonNull Application application) {
        super(application);

        this.contactsAdapter = new ContactsAdapter();

        this.contactsRepository = new ContactsRepository(application.getApplicationContext());
    }

    public boolean isRefreshing() {
        return refreshing;
    }

    public void setRefreshing(boolean refreshing) {
        this.refreshing = refreshing;
    }

    public LiveData<List<User>> getContactList(){
        return contactsRepository.getContactList();
    }

    public void refreshContacts(){
        setRefreshing(true);
        contactsRepository.refreshContacts();
    }

    public void setRefreshCallBack(ContactsRepository.RefreshCallBack refreshCallBack){
        contactsRepository.setRefreshCallBack(refreshCallBack);
    }

    public ContactsAdapter getContactsAdapter() {
        return contactsAdapter;
    }

    public void setAllToDefault() {

        // removing refresh call
        setRefreshCallBack(null);

        // removing search call back
        getContactsAdapter().setFilterCallBack(null);

    }
}
