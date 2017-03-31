package com.example.looser43.testnupuit;


import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;


public class MainActivity extends AppCompatActivity {

    public static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 99;
    public Handler h;
    Database myDB;
    Boolean permission = false;
    int exist;
    @BindView(R.id.contact_photo)
    CircleImageView contactPhoto;
    @BindView(R.id.contact_name)
    TextView contactName;
    @BindView(R.id.tick_mark)
    ImageView tickMark;
    @BindView(R.id.contact_list_recyclerview)
    RecyclerView contactListRecyclerview;
    private List<Contact> contactList = new ArrayList<>();
    private ContactListAdapter contactListAdapter;
    private String mOrderBy = ContactsContract.Contacts.DISPLAY_NAME_PRIMARY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        showAllContacts();
        contactListRecyclerview.setLayoutManager(new LinearLayoutManager(this));
        contactListAdapter = new ContactListAdapter(contactList, getApplicationContext());
        contactListRecyclerview.setAdapter(contactListAdapter);

        contactListAdapter.setOnLoadMore(new OnLoadMore() {
            @Override
            public void onLoadMore() {
                contactList.add(null);
                contactListAdapter.notifyItemInserted(contactList.size() - 1);

                //Handler will Load more data into the recyclerview list
                h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Removes the loading item
                        contactList.remove(contactList.size() - 1);
                        contactListAdapter.notifyItemRemoved(contactList.size());
                        //Load data

                        showAllContacts();
                        contactListAdapter.notifyDataSetChanged();
                        contactListAdapter.setLoaded();
                    }
                }, 5000);
            }
        });

    }

    private List<Contact> showAllContacts() {
        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        } else {
            // Android version is lesser than 6.0 or the permission is already granted.
            //List<Contact> contacts = new ArrayList<>();
            getAllContacts();
            //ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.co, contacts);
            //rvContacts.setAdapter(contacts);
            //getAllContacts();
            //return contacts;
        }
        return null;
    }


    private void getAllContacts() {
        List<Contact> contactList = new ArrayList<>();
        Contact contact;
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {

                int hasPhoneNumber = Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)));
                if (hasPhoneNumber > 0) {
                    String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                    String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                    contact = new Contact();
                    contact.setContactName(name);

                    Cursor phoneCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id},
                            null);
                    if (phoneCursor.moveToNext()) {
                        String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        contact.setContactNumber(phoneNumber);
                    }

                    phoneCursor.close();
                    contactList.add(contact);
                }
            }

            displayAllContacts();
        }

        ContactListAdapter contactAdapter = new ContactListAdapter(contactList, getApplicationContext());
        contactListRecyclerview.setLayoutManager(new LinearLayoutManager(this));
        contactListRecyclerview.setAdapter(contactAdapter);

    }

    private void displayAllContacts() {

        //it will call constructor of databae class.
        // so the db will get created and also a table
        List<Contact> contactList = new ArrayList<>();
        Contact contactListItem;

        Cursor c = myDB.getAllData();
        if (c != null && c.getCount() > 0)

        {
            while (c.moveToNext()) {

                String name = c.getString(1);
                String phoneNo = c.getString(0);
                contactListItem = new Contact();
                contactListItem.setContactName(name);
                contactListItem.setContactNumber(phoneNo);
                contactList.add(contactListItem);
            }

        }

        ContactListAdapter contactAdapter = new ContactListAdapter(contactList, getApplicationContext());
        contactListRecyclerview.setLayoutManager(new LinearLayoutManager(this));
        contactListRecyclerview.setAdapter(contactAdapter);
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {

        public Button loadmore;
        public ProgressBar progressBar;

        public LoadingViewHolder(View itemView) {
            super(itemView);
            loadmore = (Button) itemView.findViewById(R.id.load_more);
            progressBar = (ProgressBar) itemView.findViewById(R.id.progressBar);
        }
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {

        public TextView contactName, contactNumber;

        public ContactViewHolder(View itemView) {
            super(itemView);
            contactName = (TextView) itemView.findViewById(R.id.person_name);
            contactNumber = (TextView) itemView.findViewById(R.id.person_number);
        }
    }

    class ContactListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


        private final int TYPE_CONTACT = 0;
        private final int TYPE_LOAD = 1;
        private OnLoadMore onLoadMore;
        private boolean isLoading;
        private int visibleContacts = 10;
        private int lastVisibleItem, totalItemCount;

        public ContactListAdapter(List<Contact> contactList, Context applicationContext) {
            final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) contactListRecyclerview.getLayoutManager();
            contactListRecyclerview.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    totalItemCount = linearLayoutManager.getItemCount();
                    lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();

                    if (!isLoading && totalItemCount <= (lastVisibleItem + visibleContacts)) {
                        if (onLoadMore != null) {
                            onLoadMore.onLoadMore();
                        }
                        isLoading = true;
                    }
                }
            });
        }

        public void setOnLoadMore(OnLoadMore onLoadMore) {
            this.onLoadMore = onLoadMore;
        }

        @Override
        public int getItemViewType(int position) {
            return contactList.get(position) == null ? TYPE_LOAD : TYPE_CONTACT;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_CONTACT) {
                View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.contact_list_item, parent, false);
                return new ContactViewHolder(view);
            } else if (viewType == TYPE_LOAD) {
                View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.loading_button, parent, false);
                return new LoadingViewHolder(view);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

            if (holder instanceof ContactViewHolder) {
                Contact contact = contactList.get(position);
                ContactViewHolder contactViewHolder = (ContactViewHolder) holder;
                contactViewHolder.contactName.setText(contact.getContactName());
                contactViewHolder.contactNumber.setText(contact.getContactNumber());
            } else if (holder instanceof LoadingViewHolder) {
                final LoadingViewHolder loadingViewHolder = (LoadingViewHolder) holder;
                loadingViewHolder.loadmore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // code here
                        loadingViewHolder.loadmore.setVisibility(View.VISIBLE);
                        loadingViewHolder.progressBar.setVisibility(View.GONE);
                        loadingViewHolder.progressBar.setIndeterminate(true);

                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return contactList == null ? 0 : contactList.size();
        }

        public void setLoaded() {
            isLoading = false;
        }
    }
}
