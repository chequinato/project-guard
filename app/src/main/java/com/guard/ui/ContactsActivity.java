package com.guard.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.guard.GuardApplication;
import com.guard.R;
import com.guard.data.ContactDao;
import com.guard.model.EmergencyContact;

/** Tela 4 · "Who should we call?": lista, adiciona e remove contatos de emergência. */
public class ContactsActivity extends AppCompatActivity {

    private ContactDao dao;
    private LinearLayout list;
    private EditText inputName, inputPhone, inputRelationship;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        dao = ((GuardApplication) getApplication()).getContactDao();

        list = findViewById(R.id.list_contacts);
        inputName = findViewById(R.id.input_name);
        inputPhone = findViewById(R.id.input_phone);
        inputRelationship = findViewById(R.id.input_relationship);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_add).setOnClickListener(v -> addContact());
        render();
    }

    private void render() {
        list.removeAllViews();
        for (EmergencyContact c : dao.findAllByPriority()) {
            View row = ContactViews.inflate(list, c);
            row.setOnClickListener(v -> confirmRemove(c));
            list.addView(row);
        }
    }

    private void addContact() {
        try {
            // A validação mora no modelo (encapsulamento): se algo estiver errado, o construtor reclama.
            EmergencyContact contact = new EmergencyContact(
                    inputName.getText().toString(),
                    inputPhone.getText().toString(),
                    inputRelationship.getText().toString(),
                    dao.nextPriority());
            dao.insert(contact);
            inputName.setText("");
            inputPhone.setText("");
            inputRelationship.setText("");
            render();
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmRemove(EmergencyContact contact) {
        new AlertDialog.Builder(this)
                .setTitle("Remove " + contact.getName() + "?")
                .setMessage("The others move up in the alert order.")
                .setPositiveButton("Remove", (d, w) -> {
                    dao.delete(contact);
                    render();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
