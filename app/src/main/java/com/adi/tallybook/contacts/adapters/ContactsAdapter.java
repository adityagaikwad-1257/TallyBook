package com.adi.tallybook.contacts.adapters;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.adi.tallybook.databinding.ContactListTitleViewholderBinding;
import com.adi.tallybook.databinding.ContactViewholderBinding;
import com.adi.tallybook.models.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ContactsAdapter extends ListAdapter<User, RecyclerView.ViewHolder> {

    /*
        place holder for titles
     */
    public static final String CONTACTS_ON_TALLY_BOOK = "Contacts on Tally Book";
    public static final String INVITE_TO_TALLY_BOOK = "Invite to Tally Book";

    /*
        ViewType for view holder
     */
    public static final int TITLE_VIEW_TYPE = 100;
    public static final int CONTACT_VIEW_TYPE = 200;

    // search result
    private List<User> fullContactList;

    // search result call back
    private FilterCallBack filterCallBack;

    // interface to click events
    private ContactItemClickListener contactItemClickListener;

    /*
        comparing content of the new list add with the content of old list
     */
    private static final DiffUtil.ItemCallback<User> DIFF_UTIL = new DiffUtil.ItemCallback<User>() {
        @Override
        public boolean areItemsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return oldItem.getPhone_number().equals(newItem.getPhone_number());
        }

        @Override
        public boolean areContentsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return oldItem.contentMatches(newItem);
        }
    };

    public ContactsAdapter(){
        super(DIFF_UTIL);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == CONTACT_VIEW_TYPE){
            ContactViewholderBinding binding = ContactViewholderBinding.inflate(LayoutInflater.from(parent.getContext()));
            return new ContactViewHolder(binding);
        }else {
            ContactListTitleViewholderBinding binding = ContactListTitleViewholderBinding.inflate(LayoutInflater.from(parent.getContext()));
            return new TitleViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof TitleViewHolder){
            // title view holder

            TitleViewHolder titleViewHolder = (TitleViewHolder) holder;

            titleViewHolder.binding.title.setText(getItem(position).getPhone_number());

        }
        else if (holder instanceof ContactViewHolder){
            // contact view holder

            ContactViewHolder contactViewHolder = (ContactViewHolder) holder;

            contactViewHolder.setData(getItem(position), contactItemClickListener);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position).getPhone_number().equals(CONTACTS_ON_TALLY_BOOK) ||
            getItem(position).getPhone_number().equals(INVITE_TO_TALLY_BOOK)){
            // View type is of title

            return TITLE_VIEW_TYPE;
        }else{
            // View type is of Contact

            return CONTACT_VIEW_TYPE;
        }
    }

    // adapter util functions

    public List<User> getFullContactList() {
        return fullContactList;
    }

    /*
            sorting the contact list in a manner that all the contacts registered on tally book will be first
            and overall there be a ascending order of display names
         */
    public void sortAndSubmit(List<User> contactList) {

        new Thread(() -> {

            if (contactList == null) return;

            // sorting contact list
            sortAndSave(contactList);

            // submitting list to adapter
            // this should happen on Main thread
            // thus, posting this to Main thread Handler
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> submitList(contactList));

        }).start();
    }

    public synchronized void sortAndSave(List<User> contactList){

        Collections.sort(contactList, (c1, c2) -> {
            if (c1.getOnTallyBook() && c2.getOnTallyBook()) {
                // both contacts c1 and c2 are on
                // thus, comparing their @display_name
                return c1.getDisplay_name().compareTo(c2.getDisplay_name());
            }

            // thus, one of the contact is not on tally book or both of them not on tally book
            // checking which one is a registered user of tally book and ordering likewise

            if (c1.getOnTallyBook() && !c2.getOnTallyBook()) {
                // c1 is on tally book and c2 is not on tally book
                // thus, c1 must be before c2

                return -1;
            } else if (!c1.getOnTallyBook() && c2.getOnTallyBook()) {
                // c1 is not on tally book and c2 is on tally book
                // thus, c2 must be before c1

                return 1;
            } else {
                // both of the contacts are not on tally book
                // thus, comparing their @display_name

                return c1.getDisplay_name().compareTo(c2.getDisplay_name());
            }
        });

        // also, updating the @fullContactList
        this.fullContactList = contactList;

        addContactTitlePlaceHolders(contactList);
    }

    /*
           Adding place holder contact for titles to the single recyclerview with two type of contacts
           1. Contacts on Tally Book
           2. Invite to Tally Book

           Thus, adding empty @User object with phone number "Contacts on Tally Book" at 0th index
           and "Invite to Tally Book" at ith index
           where contactList[i-1] is on tally book and contactList[i+1] is not on Tally Book
    */
    private void addContactTitlePlaceHolders(List<User> filteredList) {

        if (filteredList.get(0).getOnTallyBook()) {
            // There are some contacts on Tally book
            filteredList.add(0, new User(null, ContactsAdapter.CONTACTS_ON_TALLY_BOOK));

            // finding ith index
            for (int i = 2; i < filteredList.size(); i++) {
                if (filteredList.get(i - 1).getOnTallyBook() && !filteredList.get(i).getOnTallyBook()) {
                    // go the ith index
                    filteredList.add(i, new User(null, ContactsAdapter.INVITE_TO_TALLY_BOOK));
                    break;
                }
            }

        } else {
            // There are no contacts on Tally book
            filteredList.add(0, new User(null, ContactsAdapter.INVITE_TO_TALLY_BOOK));
        }

    }

    // search in contact list
    // matching user input with every contact's display name and phone number in contact list
    public synchronized void filterContacts(String searchInput) {

        if (fullContactList == null) return;

        List<User> filteredList = new ArrayList<>();

        // index of title place holders of "INVITE TO TALLY BOOK"
        int inviteIndex = -1;

        // index of title place holders of "CONTACTS ON TALLY BOOK"
        int onTallyIndex = -1;

        // index counter of filtered list
        int k = 0;

        for (int i = 0; i<fullContactList.size(); i++) {

            User user = fullContactList.get(i);

            if (user.getPhone_number().equals(ContactsAdapter.CONTACTS_ON_TALLY_BOOK)){
                // title found, capturing it
                onTallyIndex = k++;

                filteredList.add(user);
                continue;
            }

            if (user.getPhone_number().equals(ContactsAdapter.INVITE_TO_TALLY_BOOK)){
                // title found, capturing it
                inviteIndex = k++;

                filteredList.add(user);
                continue;
            }

            if (user.getPhone_number().contains(searchInput)) {
                // search input matches with current user's phone number
                // thus, adding this user to the filtered list

                k++;
                filteredList.add(user);
            } else if (user.getDisplay_name() != null && user.getDisplay_name().trim().toLowerCase().contains(searchInput)) {
                // search input matches with current user's display name
                // thus, adding this user to the filtered list

                k++;
                filteredList.add(user);
            }
        }

        if (onTallyIndex != -1){
            // title place holder found
            if (onTallyIndex + 1 >= filteredList.size() || !filteredList.get(onTallyIndex+1).getOnTallyBook()){
                // after title place holder, there are no contacts on tally book
                // thus, removing the title

                filteredList.remove(onTallyIndex);
                inviteIndex--; // as filter list size decreases dynamically
            }
        }

        if (inviteIndex != -1){
            // title place holder found
            if (inviteIndex == filteredList.size()-1){
                // title is the last entry in the list
                // thus, removing the title

                filteredList.remove(inviteIndex);
            }
        }

        if (filterCallBack != null) {
            new Handler(Looper.getMainLooper()).post(() -> filterCallBack.onSearchResult(filteredList));
        }
    }


    // sorts the contact list, saves and filters by searchInput
    public void sortAndFilter(List<User> contactList, String searchInput) {

        new Thread(() -> {

            // firstly sorting and saving the new contact list
            sortAndSave(contactList);

            // filtering by search input
            filterContacts(searchInput);

        }).start();

    }

    // contact filter call back
    @FunctionalInterface
    public interface FilterCallBack{
        void onSearchResult(List<User> filteredList);
    }

    public void setFilterCallBack(FilterCallBack filterCallBack) {
        this.filterCallBack = filterCallBack;
    }

    // contact item click listener
    @FunctionalInterface
    public interface ContactItemClickListener{
        void onContactItemClick(@NonNull User user);
    }

    public void setContactItemClickListener(ContactItemClickListener contactItemClickListener) {
        this.contactItemClickListener = contactItemClickListener;
    }

    // view holders

    public static class ContactViewHolder extends RecyclerView.ViewHolder{
        private final ContactViewholderBinding binding;

        public ContactViewHolder(ContactViewholderBinding binding){
            super(binding.getRoot());
            this.binding = binding;
        }

        public void setData(User user, ContactItemClickListener contactItemClickListener){
            binding.setUser(user); // data binding to the views

            // click listener
            binding.getRoot().setOnClickListener(v -> {
                if (contactItemClickListener != null) contactItemClickListener.onContactItemClick(user);
            });

            // **************** loading contact image afterwards ******************
        }
    }

    public static class TitleViewHolder extends RecyclerView.ViewHolder{

        public ContactListTitleViewholderBinding binding;

        public TitleViewHolder(@NonNull ContactListTitleViewholderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
