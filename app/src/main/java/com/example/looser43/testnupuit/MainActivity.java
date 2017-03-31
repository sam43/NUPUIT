package com.example.looser43.testnupuit;

import android.os.Bundle;
import android.os.Handler;
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

    private List<Contact> contactList = new ArrayList<>();
    private ContactListAdapter contactListAdapter;
    public Handler h;

    @BindView(R.id.contact_photo)
    CircleImageView contactPhoto;
    @BindView(R.id.contact_name)
    TextView contactName;
    @BindView(R.id.tick_mark)
    ImageView tickMark;
    @BindView(R.id.contact_list_recyclerview)
    RecyclerView contactListRecyclerview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        for (int i = 0; i < 10; i++) {
            Contact contact = new Contact();
            contact.setContactName(" "+i); // Displays the name
            contact.setContactNumber("+880"+i);
            contactList.add(contact);
        }

        contactListRecyclerview.setLayoutManager(new LinearLayoutManager(this));
        contactListAdapter = new ContactListAdapter();
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

                        int index = contactList.size();
                        int end = index + 10;

                        for (int i = index; i < end ; i++) {
                            Contact contact = new Contact();
                            contact.setContactName(" "+i); // Displays the name
                            contact.setContactNumber("+880"+i);
                            contactList.add(contact);
                        }

                        contactListAdapter.notifyDataSetChanged();
                        contactListAdapter.setLoaded();
                    }
                },5000);
            }
        });

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

        public ContactListAdapter() {
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
            }
            else if (holder instanceof LoadingViewHolder) {
                final LoadingViewHolder loadingViewHolder = (LoadingViewHolder) holder;
                loadingViewHolder.loadmore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // code here
                        loadingViewHolder.loadmore.setVisibility(View.GONE);
                        loadingViewHolder.progressBar.setVisibility(View.VISIBLE);
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
