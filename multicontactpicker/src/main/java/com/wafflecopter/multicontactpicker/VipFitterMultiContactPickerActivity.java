package com.wafflecopter.multicontactpicker;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wafflecopter.multicontactpicker.RxContacts.Contact;
import com.wafflecopter.multicontactpicker.RxContacts.RxContacts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

public class VipFitterMultiContactPickerActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    public static final String EXTRA_RESULT_SELECTION = "extra_result_selection";

    private SearchView searchView;
    private MultiContactPicker.Builder builder;
    private List<Contact> contactList = new ArrayList<>();
    ArrayList<Contact> mListItems = new ArrayList<>();
    private CompositeDisposable disposables;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private TextView tvNoContacts;
    private TextView tvTitle;
    private VipFitterMultiContactPickerAdapter adapter;
    private TextView tvBtnDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent == null) return;

        builder = (MultiContactPicker.Builder) intent.getSerializableExtra("builder");

        disposables = new CompositeDisposable();

        setTheme(builder.theme);

        setContentView(R.layout.activity_vipfitter_multi_contact_picker);

        ImageView mImgClose = findViewById(R.id.img_close);
        mImgClose.setVisibility(View.VISIBLE);
        mImgClose.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            View view = getCurrentFocus();

            if (view == null) {
                view = new View(VipFitterMultiContactPickerActivity.this);
            }

            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }

            Intent result = new Intent();
            result.putExtra(EXTRA_RESULT_SELECTION, MultiContactPicker.buildResult(new ArrayList<>()));
            setResult(RESULT_OK, result);
            finish();
        });

        searchView = findViewById(R.id.searchview);
        recyclerView = findViewById(R.id.listview);
        progressBar = findViewById(R.id.progressBar);
        tvNoContacts = findViewById(R.id.tvNoContacts);
        tvTitle = findViewById(R.id.tv_det_title);
        tvBtnDone = findViewById(R.id.tv_done);
        tvBtnDone.setOnClickListener(v -> finishPicking());

        initialiseUI(builder);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        adapter = new VipFitterMultiContactPickerAdapter(this, mListItems, (contact, totalSelectedContacts) -> {
            updateSelectBarContents(totalSelectedContacts);
            if (builder.selectionMode == MultiContactPicker.CHOICE_MODE_SINGLE) {
                finishPicking();
            }
        });

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadContacts();
    }

    private void updateSelectBarContents(int totalSelectedContacts) {
        tvBtnDone.setEnabled(totalSelectedContacts > 0);
        tvBtnDone.setTextColor(totalSelectedContacts > 0 ?
                ResourcesCompat.getColor(getResources(), R.color.text_color, null) :
                ResourcesCompat.getColor(getResources(), R.color.text_color_alpha, null));
    }

    private void finishPicking() {
        Intent result = new Intent();
        result.putExtra(EXTRA_RESULT_SELECTION, MultiContactPicker.buildResult(adapter.getSelectedContacts()));
        setResult(RESULT_OK, result);
        finish();
    }

    private void loadContacts() {
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
//                            progressBar.setVisibility(View.GONE);
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

                        reloadAdapter();
                    }
                });
    }

    private void reloadAdapter() {

        String prevName = "";
        String newName;
        String name;

        mListItems = new ArrayList<>();

        for (int i = 0; i < contactList.size(); i++) {

            Contact model = contactList.get(i);

            if (model != null) {

                newName = model.getDisplayName() != null ? model.getDisplayName().substring(0, 1).toUpperCase() : "";
                name = model.getDisplayName();

                if (newName != null) {

                    ArrayList<HashMap<String, String>> d = lettersList();

                    for (HashMap<String, String> hashMap : d) {

                        String firstLetter = hashMap.get("title");
                        String firstLetter2 = hashMap.get("title2");
                        String firstLetterId = hashMap.get("id");

                        if (newName != null) {
                            if (newName.toUpperCase().contains(Objects.requireNonNull(firstLetter)) ||
                                    newName.toUpperCase().contains(Objects.requireNonNull(firstLetter2))) {
                                newName = firstLetterId;
                            }
                        }
                    }

                    if (i == 0) {

                        Contact contact = new Contact(model.getId(), VipFitterMultiContactPickerAdapter.VIEW_TYPE_HEADER);
                        contact.setDisplayName(model.getDisplayName());
                        contact.setNameEn(newName);
                        mListItems.add(contact);
                        prevName = newName;

                        Contact contactItem = new Contact(model.getId(), VipFitterMultiContactPickerAdapter.VIEW_TYPE_ITEM);
                        contactItem.setDisplayName(model.getDisplayName());
                        contactItem.setPhoneNumbers(model.getPhoneNumbers());

                        for (HashMap<String, String> hashMap : d) {

                            String firstLetter = hashMap.get("title");
                            String firstLetter2 = hashMap.get("title2");
                            String firstLetterId = hashMap.get("id");

                            if (name.contains(Objects.requireNonNull(firstLetter))) {
                                name = name.replaceAll(firstLetter, Objects.requireNonNull(firstLetterId));
                                contactItem.setNameEn(name);
                            } else if (name.contains(Objects.requireNonNull(firstLetter2))) {
                                name = name.replaceAll(firstLetter2, Objects.requireNonNull(firstLetterId));
                                contactItem.setNameEn(name);
                            }
                        }

                        if ((contactItem.getNameEn() == null || contactItem.getNameEn().equals("")) && name != null && !name.equals("")) {
                            contactItem.setNameEn(name);
                        }

                        mListItems.add(contactItem);

                    } else {

                        boolean newHeader = true;
                        for (HashMap<String, String> hashMap : d) {
                            String firstLetter = hashMap.get("title");
                            String firstLetter2 = hashMap.get("title2");
                            String firstLetterId = hashMap.get("id");
                            if ((Objects.equals(firstLetter, newName) ||
                                    Objects.equals(firstLetter2, newName)) &&
                                    Objects.equals(firstLetterId, prevName)) {
                                newHeader = false;
                                break;
                            }
                        }

                        assert newName != null;
                        if (!newName.equals(prevName) && newHeader) {

                            Contact contact = new Contact(model.getId(), VipFitterMultiContactPickerAdapter.VIEW_TYPE_HEADER);
                            contact.setDisplayName(model.getDisplayName());
                            contact.setNameEn(newName);
                            mListItems.add(contact);
                            prevName = newName;
                        }

                        Contact contactItem = new Contact(model.getId(), VipFitterMultiContactPickerAdapter.VIEW_TYPE_ITEM);
                        contactItem.setDisplayName(model.getDisplayName());
                        contactItem.setPhoneNumbers(model.getPhoneNumbers());

                        for (HashMap<String, String> hashMap : d) {

                            String firstLetter = hashMap.get("title");
                            String firstLetter2 = hashMap.get("title2");
                            String firstLetterId = hashMap.get("id");

                            if (name.contains(Objects.requireNonNull(firstLetter))) {
                                name = name.replaceAll(firstLetter, Objects.requireNonNull(firstLetterId));
                                contactItem.setNameEn(name);
                            } else if (name.contains(Objects.requireNonNull(firstLetter2))) {
                                name = name.replaceAll(firstLetter2, Objects.requireNonNull(firstLetterId));
                                contactItem.setNameEn(name);
                            }
                        }

                        if ((contactItem.getNameEn() == null || contactItem.getNameEn().equals("")) && name != null && !name.equals("")) {
                            contactItem.setNameEn(name);
                        }

                        mListItems.add(model);
                    }
                }
            }
        }

        adapter = new VipFitterMultiContactPickerAdapter(this, mListItems, (contact, totalSelectedContacts) -> {
            updateSelectBarContents(totalSelectedContacts);
            if (builder.selectionMode == MultiContactPicker.CHOICE_MODE_SINGLE) {
                finishPicking();
            }
        });
        adapter.setChoiceModeSingle(builder.selectionMode == MultiContactPicker.CHOICE_MODE_SINGLE);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void initialiseUI(MultiContactPicker.Builder builder) {
        searchView.setOnQueryTextListener(this);

        if (builder.titleText != null) {
            tvTitle.setText(builder.titleText);
        }

        Typeface typefaceBold = Typeface.createFromAsset(getAssets(), "fonts/Montserrat-Bold.otf");
        if (typefaceBold != null) {
            tvTitle.setTypeface(typefaceBold);
            tvBtnDone.setTypeface(typefaceBold);
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

    public ArrayList<HashMap<String, String>> lettersList() {

        ArrayList<HashMap<String, String>> arrayHashMap = new ArrayList<>();

        HashMap<String, String> map1 = new HashMap<String, String>() {{
            put("id", "A");
            put("title", "Á");
            put("title2", "À");
        }};
        arrayHashMap.add(map1);

        HashMap<String, String> map1_1 = new HashMap<String, String>() {{
            put("id", "a");
            put("title", "á");
            put("title2", "à");
        }};
        arrayHashMap.add(map1_1);

        HashMap<String, String> map2 = new HashMap<String, String>() {{
            put("id", "E");
            put("title", "É");
            put("title2", "È");
        }};
        arrayHashMap.add(map2);

        HashMap<String, String> map2_2 = new HashMap<String, String>() {{
            put("id", "e");
            put("title", "é");
            put("title2", "è");
        }};
        arrayHashMap.add(map2_2);

        HashMap<String, String> map3 = new HashMap<String, String>() {{
            put("id", "I");
            put("title", "Í");
            put("title2", "Ì");
        }};
        arrayHashMap.add(map3);

        HashMap<String, String> map3_3 = new HashMap<String, String>() {{
            put("id", "i");
            put("title", "í");
            put("title2", "ì");
        }};
        arrayHashMap.add(map3_3);

        HashMap<String, String> map4 = new HashMap<String, String>() {{
            put("id", "O");
            put("title", "Ó");
            put("title2", "Ò");
        }};
        arrayHashMap.add(map4);

        HashMap<String, String> map4_4 = new HashMap<String, String>() {{
            put("id", "o");
            put("title", "ó");
            put("title2", "ò");
        }};
        arrayHashMap.add(map4_4);

        HashMap<String, String> map5 = new HashMap<String, String>() {{
            put("id", "U");
            put("title", "Ú");
            put("title2", "Ù");
        }};
        arrayHashMap.add(map5);

        HashMap<String, String> map5_5 = new HashMap<String, String>() {{
            put("id", "u");
            put("title", "ú");
            put("title2", "ù");
        }};
        arrayHashMap.add(map5_5);

        return arrayHashMap;
    }
}
