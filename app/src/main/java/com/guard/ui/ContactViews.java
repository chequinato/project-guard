package com.guard.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.guard.R;
import com.guard.model.EmergencyContact;

/** Monta a linha de um contato (item_contact.xml). Usado na tela inicial e na de contatos. */
final class ContactViews {

    private ContactViews() { }

    static View inflate(ViewGroup parent, EmergencyContact contact) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        ((TextView) row.findViewById(R.id.txt_priority)).setText(String.valueOf(contact.getPriority()));
        ((TextView) row.findViewById(R.id.txt_avatar)).setText(contact.getName().substring(0, 1).toUpperCase());
        ((TextView) row.findViewById(R.id.txt_name)).setText(contact.getName());
        String rel = contact.getRelationship().isEmpty() ? contact.getPhone() : contact.getRelationship();
        ((TextView) row.findViewById(R.id.txt_relationship)).setText(rel);
        return row;
    }
}
