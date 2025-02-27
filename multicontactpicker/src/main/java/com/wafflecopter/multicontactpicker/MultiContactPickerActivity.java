package com.wafflecopter.multicontactpicker;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.l4digital.fastscroll.FastScrollRecyclerView;
import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.wafflecopter.multicontactpicker.RxContacts.Contact;
import com.wafflecopter.multicontactpicker.RxContacts.RxContacts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

public class MultiContactPickerActivity extends AppCompatActivity implements MaterialSearchView.OnQueryTextListener {

    public static final String EXTRA_RESULT_SELECTION = "extra_result_selection";
    private FastScrollRecyclerView recyclerView;
    private List<Contact> contactList = new ArrayList<>();
    private TextView tvSelectAll;
    private TextView tvSelectBtn;
    private TextView tvNoContacts;
    private LinearLayout controlPanel;
    private MultiContactPickerAdapter adapter;
    private Toolbar toolbar;
    private MaterialSearchView searchView;
    private ProgressBar progressBar;
    private MenuItem searchMenuItem;
    private MultiContactPicker.Builder builder;
    private boolean allSelected = false;
    private CompositeDisposable disposables;
    private Integer animationCloseEnter, animationCloseExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent == null) return;

        builder = (MultiContactPicker.Builder) intent.getSerializableExtra("builder");

        disposables = new CompositeDisposable();

        setTheme(builder.theme);

        setContentView(R.layout.activity_multi_contact_picker);

        toolbar = findViewById(R.id.toolbar);
        searchView = findViewById(R.id.search_view);
        controlPanel = findViewById(R.id.controlPanel);
        progressBar = findViewById(R.id.progressBar);
        tvSelectAll = findViewById(R.id.tvSelectAll);
        tvSelectBtn = findViewById(R.id.tvSelect);
        tvNoContacts = findViewById(R.id.tvNoContacts);
        recyclerView = findViewById(R.id.recyclerView);

        initialiseUI(builder);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        adapter = new MultiContactPickerAdapter(contactList, (contact, totalSelectedContacts) -> {
            updateSelectBarContents(totalSelectedContacts);
            if (builder.selectionMode == MultiContactPicker.CHOICE_MODE_SINGLE) {
                finishPicking();
            }
        });

        loadContacts();

        recyclerView.setAdapter(adapter);

        tvSelectBtn.setOnClickListener(view -> finishPicking());

        tvSelectAll.setOnClickListener(view -> {
            allSelected = !allSelected;
            if (adapter != null)
                adapter.setAllSelected(allSelected);
            if (allSelected)
                tvSelectAll.setText(getString(R.string.tv_unselect_all_btn_text));
            else
                tvSelectAll.setText(getString(R.string.tv_select_all_btn_text));
        });

        TextView tvSkip = findViewById(R.id.skip);
        tvSkip.setOnClickListener(v -> {

            Intent result = new Intent();
            result.putExtra(EXTRA_RESULT_SELECTION, MultiContactPicker.buildResult(new ArrayList<>()));
            setResult(RESULT_OK, result);
            finish();
            overrideAnimation();

//                setResult(RESULT_CANCELED);
//                finish();
//                overrideAnimation();
        });

    }

    private void finishPicking() {
        Intent result = new Intent();
        result.putExtra(EXTRA_RESULT_SELECTION, MultiContactPicker.buildResult(adapter.getSelectedContacts()));
        setResult(RESULT_OK, result);
        finish();
        overrideAnimation();
    }

    private void overrideAnimation() {
        if (animationCloseEnter != null && animationCloseExit != null) {
            overridePendingTransition(animationCloseEnter, animationCloseExit);
        }
    }

    private void updateSelectBarContents(int totalSelectedContacts) {
        tvSelectBtn.setEnabled(totalSelectedContacts > 0);
        if (totalSelectedContacts > 0) {
            tvSelectBtn.setText(getString(R.string.tv_select_btn_text_enabled, String.valueOf(totalSelectedContacts)));
        } else {
            tvSelectBtn.setText(getString(R.string.tv_select_btn_text_disabled));
        }
    }

    private void initialiseUI(MultiContactPicker.Builder builder) {
        setSupportActionBar(toolbar);
        searchView.setOnQueryTextListener(this);

        this.animationCloseEnter = builder.animationCloseEnter;
        this.animationCloseExit = builder.animationCloseExit;

        if (builder.bubbleColor != 0)
            recyclerView.setBubbleColor(builder.bubbleColor);
        if (builder.handleColor != 0)
            recyclerView.setHandleColor(builder.handleColor);
        if (builder.bubbleTextColor != 0)
            recyclerView.setBubbleTextColor(builder.bubbleTextColor);
        if (builder.trackColor != 0)
            recyclerView.setTrackColor(builder.trackColor);
        recyclerView.setHideScrollbar(builder.hideScrollbar);
        recyclerView.setTrackVisible(builder.showTrack);
        if (builder.selectionMode == MultiContactPicker.CHOICE_MODE_SINGLE) {
            controlPanel.setVisibility(View.GONE);
        } else {
            controlPanel.setVisibility(View.VISIBLE);
        }

        tvSelectAll.setVisibility(builder.hideSelectAllButton ? View.INVISIBLE : View.VISIBLE);

        if (builder.selectionMode == MultiContactPicker.CHOICE_MODE_SINGLE && builder.selectedItems.size() > 0) {
            throw new RuntimeException("You must be using MultiContactPicker.CHOICE_MODE_MULTIPLE in order to use setSelectedContacts()");
        }

        if (builder.titleText != null) {
            setTitle(builder.titleText);
        }

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
            overrideAnimation();
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadContacts() {
        tvSelectAll.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        RxContacts.fetch(builder.columnLimit, this)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> disposables.add(disposable))
                .filter(contact -> contact.getDisplayName() != null)
                .subscribe(new Observer<Contact>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull Contact value) {
                        contactList.add(value);
                        if (builder.selectedItems.contains(value.getId())) {
                            adapter.setContactSelected(value.getId());
                        }
                        Collections.sort(contactList, (contact, t1) -> contact.getDisplayName().compareToIgnoreCase(t1.getDisplayName()));
                        if (builder.loadingMode == MultiContactPicker.LOAD_ASYNC) {
                            if (adapter != null) {
                                adapter.notifyDataSetChanged();
                            }
                            progressBar.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        progressBar.setVisibility(View.GONE);
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        if (contactList.size() == 0) {
                            tvNoContacts.setVisibility(View.VISIBLE);
                        }
                        if (adapter != null && builder.loadingMode == MultiContactPicker.LOAD_SYNC) {
                            adapter.notifyDataSetChanged();
                        }
                        if (adapter != null) {
                            updateSelectBarContents(adapter.getSelectedContactsCount());
                        }
                        progressBar.setVisibility(View.GONE);
                        tvSelectAll.setEnabled(true);
                    }
                });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mcp_menu_main, menu);
        searchMenuItem = menu.findItem(R.id.mcp_action_search);
        setSearchIconColor(searchMenuItem, builder.searchIconColor);
        searchView.setMenuItem(searchMenuItem);
        return true;
    }

    private void setSearchIconColor(MenuItem menuItem, final Integer color) {
        if (color != null) {
            Drawable drawable = menuItem.getIcon();
            if (drawable != null) {
                drawable = DrawableCompat.wrap(drawable);
                DrawableCompat.setTint(drawable.mutate(), color);
                menuItem.setIcon(drawable);
            }
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (adapter != null) {
            adapter.filterOnText(query);
        }
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (adapter != null) {
            adapter.filterOnText(newText);
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (searchView.isSearchOpen()) {
            searchView.closeSearch();
        } else {
            super.onBackPressed();
            overrideAnimation();
        }
    }

    @Override
    public void onDestroy() {
        disposables.clear();
        super.onDestroy();
    }
}
