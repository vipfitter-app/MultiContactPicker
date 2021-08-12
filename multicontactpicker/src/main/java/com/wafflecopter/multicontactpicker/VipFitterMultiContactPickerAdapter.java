package com.wafflecopter.multicontactpicker;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.l4digital.fastscroll.FastScroller;
import com.wafflecopter.multicontactpicker.RxContacts.Contact;

import java.util.ArrayList;
import java.util.List;

public class VipFitterMultiContactPickerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements FastScroller.SectionIndexer, Filterable {

    public static final int VIEW_TYPE_HEADER = 0;
    public static final int VIEW_TYPE_ITEM = 1;

    private List<Contact> contactItemList;
    private List<Contact> contactItemListOriginal;
    private VipFitterMultiContactPickerAdapter.ContactSelectListener listener;
    private String currentFilterQuery;
    private Context context;
    private boolean choiceModeSingle;

    interface ContactSelectListener {
        void onContactSelected(Contact contact, int totalSelectedContacts);
    }

    @Override
    public int getItemViewType(int position) {

        super.getItemViewType(position);
        Contact item = contactItemList.get(position);

        return item.getType();
    }

    VipFitterMultiContactPickerAdapter(Context context, List<Contact> contactItemList, VipFitterMultiContactPickerAdapter.ContactSelectListener listener) {
        this.contactItemList = contactItemList;
        this.contactItemListOriginal = contactItemList;
        this.listener = listener;
        this.context = context;
    }

    public void setChoiceModeSingle(boolean choiceModeSingle) {
        this.choiceModeSingle = choiceModeSingle;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cell_contact_header, viewGroup, false);
            return new VipFitterMultiContactPickerAdapter.VHHeader(view);
        } else {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_row_contact_pick_item_two, viewGroup, false);
            return new VipFitterMultiContactPickerAdapter.ContactViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int i) {

        final Contact contactItem = getItem(i);
        if (contactItem != null) {

            switch (holder.getItemViewType()) {

                case VIEW_TYPE_HEADER:

                    VHHeader holder1 = ((VipFitterMultiContactPickerAdapter.VHHeader) holder);

                    if (contactItem.getDisplayName() != null) {
                        holder1.mTvTitle.setText(contactItem.getDisplayName().substring(0, 1));
                        Typeface typeface = Typeface.createFromAsset(context.getAssets(), "fonts/Montserrat-Bold.otf");
                        if (typeface != null) {
                            holder1.mTvTitle.setTypeface(typeface);
                        }
                    }

                    break;

                case VIEW_TYPE_ITEM:

                    VipFitterMultiContactPickerAdapter.ContactViewHolder contactViewHolder = (VipFitterMultiContactPickerAdapter.ContactViewHolder) holder;

                    contactViewHolder.tvContactName.setText(contactItem.getDisplayName());

                    if (contactItem.getPhoneNumbers().size() > 0) {
                        String phoneNumber = contactItem.getPhoneNumbers().get(0).getNumber().replaceAll("\\s+", "");
                        String displayName = contactItem.getDisplayName().replaceAll("\\s+", "");
                        if (!phoneNumber.equals(displayName)) {
                            contactViewHolder.tvNumber.setVisibility(View.VISIBLE);
                            contactViewHolder.tvNumber.setText(phoneNumber);
                        } else {
                            contactViewHolder.tvNumber.setVisibility(View.GONE);
                        }
                    } else {
                        if (contactItem.getEmails().size() > 0) {
                            String email = contactItem.getEmails().get(0).replaceAll("\\s+", "");
                            String displayName = contactItem.getDisplayName().replaceAll("\\s+", "");
                            if (!email.equals(displayName)) {
                                contactViewHolder.tvNumber.setVisibility(View.VISIBLE);
                                contactViewHolder.tvNumber.setText(email);
                            } else {
                                contactViewHolder.tvNumber.setVisibility(View.GONE);
                            }
                        } else {
                            contactViewHolder.tvNumber.setVisibility(View.GONE);
                        }
                    }

                    Typeface typeface = Typeface.createFromAsset(context.getAssets(), "fonts/Montserrat-Regular.otf");
                    if (typeface != null) {
                        contactViewHolder.tvContactName.setTypeface(typeface);
                        contactViewHolder.tvNumber.setTypeface(typeface);
                    }

                    highlightTerm(contactViewHolder.tvContactName, currentFilterQuery, contactViewHolder.tvContactName.getText().toString());

                    if (choiceModeSingle) {
                        contactViewHolder.ivSelectedState.setVisibility(View.GONE);
                        contactViewHolder.ivArrow.setVisibility(View.VISIBLE);
                    } else {
                        contactViewHolder.ivSelectedState.setVisibility(View.VISIBLE);
                        contactViewHolder.ivArrow.setVisibility(View.GONE);

                        if (contactItem.isSelected()) {
                            contactViewHolder.ivSelectedState.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.check_on));
                        } else {
                            contactViewHolder.ivSelectedState.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.check_off));
                        }
                    }

                    contactViewHolder.mView.setOnClickListener(view -> {
                        setContactSelected(contactItem.getId());
                        if (listener != null) {
                            listener.onContactSelected(getItem(i), getSelectedContactsCount());
                        }
                        notifyDataSetChanged();
                    });
            }
        }
    }

    private void highlightTerm(TextView tv, String query, String originalString) {
        if (query != null && !query.isEmpty()) {
            int startPos = originalString.toLowerCase().indexOf(query.toLowerCase());
            int endPos = startPos + query.length();
            if (startPos != -1) {
                Spannable spannable = new SpannableString(originalString);
                ColorStateList blackColor = new ColorStateList(new int[][]{new int[]{}}, new int[]{Color.BLACK});
                TextAppearanceSpan highlightSpan = new TextAppearanceSpan(null, Typeface.BOLD, -1, blackColor, null);
                spannable.setSpan(highlightSpan, startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                tv.setText(spannable);
            } else {
                tv.setText(originalString);
            }
        } else {
            tv.setText(originalString);
        }
    }

    protected void setAllSelected(boolean isAll) {
        for (Contact c : contactItemList) {
            c.setSelected(isAll);
            if (listener != null) {
                listener.onContactSelected(c, getSelectedContactsCount());
            }
        }
        notifyDataSetChanged();
    }

    protected void setContactSelected(long id) {
        if (contactItemList.size() == 0) {
            return;
        }

        int pos = getItemPosition(contactItemList, id);
        contactItemList.get(pos).setSelected(!contactItemList.get(pos).isSelected());
    }

    private int getItemPosition(List<Contact> list, long mid) {
        int i = 0;
        for (Contact contact : list) {
            if (contact.getId() == mid && contact.getType() == VIEW_TYPE_ITEM) {
                return i;
            }
            i++;
        }
        return -1;
    }

    protected int getSelectedContactsCount() {
        return ((getSelectedContacts() != null) ? getSelectedContacts().size() : 0);
    }

    List<Contact> getSelectedContacts() {
        List<Contact> selectedContacts = new ArrayList<>();
        for (Contact contact : contactItemListOriginal) {
            if (contact.isSelected()) {
                selectedContacts.add(contact);
            }
        }
        return selectedContacts;
    }

    @Override
    public int getItemCount() {
        return (null != contactItemList ? contactItemList.size() : 0);
    }

    private Contact getItem(int pos) {
        return contactItemList.get(pos);
    }

    @Override
    public String getSectionText(int position) {

        try {
            return String.valueOf(contactItemList.get(position).getDisplayName().charAt(0));
        } catch (NullPointerException | IndexOutOfBoundsException ex) {
            ex.printStackTrace();
            return "";
        }
    }

    private class ContactViewHolder extends RecyclerView.ViewHolder {
        private final View mView;
        private TextView tvContactName;
        private TextView tvNumber;
        private ImageView ivSelectedState, ivArrow;

        ContactViewHolder(View view) {
            super(view);
            this.mView = view;
            this.tvContactName = view.findViewById(R.id.tvContactName);
            this.tvNumber = view.findViewById(R.id.tvNumber);
            this.ivSelectedState = view.findViewById(R.id.ivSelectedState);
            this.ivArrow = view.findViewById(R.id.ivArrow);
        }
    }

    public void filterOnText(String query) {
        this.currentFilterQuery = query;
        getFilter().filter(currentFilterQuery);
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                contactItemList = (List<Contact>) results.values;
                notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<Contact> filteredResults;
                if (constraint.length() == 0) {
                    filteredResults = contactItemListOriginal;
                    currentFilterQuery = null;
                } else {
                    filteredResults = getFilteredResults(constraint.toString().toLowerCase());
                }
                FilterResults results = new FilterResults();
                results.values = filteredResults;
                return results;
            }
        };
    }

    private List<Contact> getFilteredResults(String constraint) {
        List<Contact> results = new ArrayList<>();
        for (Contact item : contactItemListOriginal) {
            if (item.getDisplayName().toLowerCase().contains(constraint)) {
                results.add(item);
            }
        }
        return results;
    }

    private static class VHHeader extends RecyclerView.ViewHolder {

        private final TextView mTvTitle;

        private VHHeader(View itemView) {

            super(itemView);
            mTvTitle = itemView.findViewById(R.id.tv_title);

        }
    }

}
