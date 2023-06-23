package com.adi.tallybook.contacts.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.adi.tallybook.R;
import com.adi.tallybook.apputilities.AppUtils;
import com.adi.tallybook.contacts.adapters.ContactsAdapter;
import com.adi.tallybook.contacts.mvvm.ContactsRepository;
import com.adi.tallybook.contacts.mvvm.ContactsViewModel;
import com.adi.tallybook.databinding.ContactsFragmentBinding;
import com.adi.tallybook.models.User;

import java.util.Collections;
import java.util.List;

public class ContactsFragment extends Fragment implements ContactsRepository.RefreshCallBack, MenuProvider,
        SearchView.OnQueryTextListener, ContactsAdapter.FilterCallBack, ContactsAdapter.ContactItemClickListener {

    // data binding
    private ContactsFragmentBinding binding;

    // view model
    private ContactsViewModel viewModel;

    // livedata of both contact lists
    protected LiveData<List<User>> contactListLive;

    public ContactsFragment() {
        // required
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ContactsFragmentBinding.inflate(inflater, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(ContactsViewModel.class);

        // registering refresh call back
        viewModel.setRefreshCallBack(this);

        // registering search result call back
        viewModel.getContactsAdapter().setFilterCallBack(this);

        // registering contact item click listener
        viewModel.getContactsAdapter().setContactItemClickListener(this);

        if (savedInstanceState == null) {
            // refreshing contacts
            viewModel.refreshContacts();
        }

        initViews();

        observeContacts(savedInstanceState == null);

        clickEvents();

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // removing contact list observer
        contactListLive.removeObservers(requireActivity());

        // setting all attributes to default
        viewModel.setAllToDefault();
    }

    /*
       Observe LiveData from Room database of Contacts
     */
    private void observeContacts(boolean isNewInstance) {

        contactListLive = viewModel.getContactList();

        /*
            observing all contacts in room database
         */
        contactListLive.observe(requireActivity(), contactList -> {

            // sorting and submitting contact list to RV adapter on another thread
            if (contactList.size() > 0) {
                // setting up the count of contacts as sub title
                binding.toolbar.setSubtitle(contactList.size() + " contacts");

                // checking if user has search input in the last instance
                if (viewModel.searchInput == null || isNewInstance){
                    // user had no search input in last instance or this is new instance of this fragment
                    // thus, submitting the whole contact list after sorting

                    viewModel.getContactsAdapter().sortAndSubmit(contactList);

                    // updating no search result alert tv
                    binding.noSearchResult.setVisibility(View.GONE);
                }else {
                    // user had some search input
                    // thus, filtering that search input with new contact list

                    viewModel.getContactsAdapter().sortAndFilter(contactList, viewModel.searchInput);
                }

                binding.noContacts.setVisibility(View.GONE);
            }else{
                // no contacts in room
                binding.noContacts.setVisibility(View.VISIBLE);
            }
        });

    }

    private void clickEvents() {
        // back arrow
        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        // scroll to top btn
        binding.scrollToTop.setOnClickListener(v -> binding.contactsRv.smoothScrollToPosition(0));
    }

    private void initViews() {
        // initializing recycler view of contacts
        binding.contactsRv.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.contactsRv.setAdapter(viewModel.getContactsAdapter());

        // if contacts are being refreshed, showing progress to user
        if (viewModel.isRefreshing()) binding.refreshProgress.setVisibility(View.VISIBLE);

        // registering menu provider to current fragment activity
        binding.toolbar.addMenuProvider(this, requireActivity(), Lifecycle.State.RESUMED);

        // hiding scroll to top fab
        binding.scrollToTop.hide();

        // implementing scroll to top functionality to contact rv
        binding.contactsRv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // getting position of first visible element in the contact list
                if (recyclerView.getLayoutManager() != null){
                    LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    int firstVisiblePos = linearLayoutManager.findFirstVisibleItemPosition();

                    if (firstVisiblePos > 3){
                        // show scroll to top btn
                        binding.scrollToTop.show();
                    }else{
                        // hiding it
                        binding.scrollToTop.hide();
                    }
                }

            }
        });
    }

    /*
        Contact refresh call back
     */
    @Override
    public void onRefresh() {
        viewModel.setRefreshing(false);
        Toast.makeText(requireContext(), "Contacts updates successfully", Toast.LENGTH_SHORT).show();
        binding.refreshProgress.setVisibility(View.GONE);
    }

    // search call back
    @Override
    public void onSearchResult(List<User> filteredList) {

        if (filteredList.size() > 0) {
            binding.noSearchResult.setVisibility(View.GONE);
            viewModel.getContactsAdapter().submitList(filteredList);
        }else{
            // no result found

            viewModel.getContactsAdapter().submitList(Collections.emptyList());
            binding.noSearchResult.setVisibility(View.VISIBLE);
        }
    }

    // menu inflation
    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        MenuItem menuItem = menu.findItem(R.id.contact_search_bar);
        if (menuItem != null){
            SearchView searchView = (SearchView) menuItem.getActionView();
            if (searchView != null){
                searchView.setQueryHint("Type a name or number...");

                searchView.setOnQueryTextListener(this);
            }
        }
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.contact_refresh) {
            // refresh contacts
            if (!viewModel.isRefreshing()) viewModel.refreshContacts();

            binding.refreshProgress.setVisibility(View.VISIBLE);
        }

        return true;
    }

    /*
        contact recyclerview item click listener
     */
    @Override
    public void onContactItemClick(@NonNull User user) {
        if (user.getOnTallyBook()){
            // user is on tally book

            Toast.makeText(requireContext(), user.getDisplay_name(), Toast.LENGTH_SHORT).show();
        }else {
            // user is not on tally book
            // thus, starting intent to invite that particular user

            String whats_app_url = "https://api.whatsapp.com/send?phone="+ user.getPhone_number()
                    +"&text=" + getString(R.string.invite_msg);

            Intent whatsAppIntent = new Intent(Intent.ACTION_VIEW);
            whatsAppIntent.setData(Uri.parse(whats_app_url));

            try {
                startActivity(whatsAppIntent);
            }catch (ActivityNotFoundException e){
                // chances are there is no WhatsApp installed on device
                // thus, sending sms to the phone number

                Uri uri = Uri.parse("smsto:" + user.getPhone_number());

                Intent smsIntent = new Intent(Intent.ACTION_SENDTO, uri);
                smsIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.invite_msg));

                startActivity(Intent.createChooser(smsIntent, "Choose an app"));
            }

        }
    }

    // searchView query listeners
    @Override
    public boolean onQueryTextSubmit(String query) {
        AppUtils.closeKeyboard(requireActivity());
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (newText != null){

            viewModel.searchInput = newText;

            if (newText.trim().isEmpty()) {
                // no search input from user
                // loading whole contact list

                if (viewModel.getContactsAdapter().getFullContactList() != null){
                    viewModel.getContactsAdapter().submitList(viewModel.getContactsAdapter().getFullContactList());
                    // updating @searchInput to null
                    // indicating whole list is being shown

                    viewModel.searchInput = null;
                }
            }

            new Thread(() -> viewModel.getContactsAdapter().filterContacts(newText)).start();
        }

        return true;
    }
}