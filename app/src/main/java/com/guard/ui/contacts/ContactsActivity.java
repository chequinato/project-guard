package com.guard.ui.contacts;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.guard.GuardApplication;
import com.guard.R;
import com.guard.data.ContactDao;
import com.guard.model.EmergencyContact;
import com.guard.ui.common.ContactViews;
import com.guard.ui.common.Press;

import java.util.List;

/**
 * Tela 4 · "Who should we call?" (layout: activity_contacts.xml).
 * Lista, adiciona e remove contatos de emergência.
 */
public class ContactsActivity extends AppCompatActivity {

    private ContactDao dao;
    private LinearLayout list;
    private EditText inputName, inputPhone, inputRelationship;   // EditText = campo de digitação

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        dao = ((GuardApplication) getApplication()).getContactDao();

        list = findViewById(R.id.list_contacts);
        inputName = findViewById(R.id.input_name);
        inputPhone = findViewById(R.id.input_phone);
        inputRelationship = findViewById(R.id.input_relationship);

        View btnBack = findViewById(R.id.btn_back);
        View btnAdd = findViewById(R.id.btn_add);
        Press.attach(btnBack, btnAdd);
        btnBack.setOnClickListener(v -> finish());
        btnAdd.setOnClickListener(v -> addContact());
        render();
    }

    /** Redesenha a lista a partir do banco. Cada linha tem um × para remover. */
    private void render() {
        list.removeAllViews();
        List<EmergencyContact> contacts = dao.findAllByPriority();
        for (int i = 0; i < contacts.size(); i++) {
            EmergencyContact c = contacts.get(i);
            boolean isLast = i == contacts.size() - 1;
            list.addView(ContactViews.inflate(list, c, !isLast, () -> confirmRemove(c)));
        }
        list.setVisibility(contacts.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void addContact() {
        try {
            // A VALIDAÇÃO mora no modelo (encapsulamento): se algo estiver errado,
            // o construtor de EmergencyContact lança uma exceção e caímos no catch.
            EmergencyContact contact = new EmergencyContact(
                    inputName.getText().toString(),
                    inputPhone.getText().toString(),
                    inputRelationship.getText().toString(),
                    dao.nextPriority());
            dao.insert(contact);
            Press.tick(list);

            // Limpa o formulário, esconde o teclado e mostra a lista atualizada.
            inputName.setText("");
            inputPhone.setText("");
            inputRelationship.setText("");
            inputName.clearFocus();
            InputMethodManager keyboard = getSystemService(InputMethodManager.class);
            keyboard.hideSoftInputFromWindow(list.getWindowToken(), 0);
            render();
        } catch (IllegalArgumentException e) {
            // A mensagem da exceção vem do modelo, em inglês. Para a tela, usamos o texto
            // traduzido do strings.xml, escolhido de acordo com o campo que está errado.
            Toast.makeText(this, validationMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /** Qual campo está errado? Devolve o id do texto (strings.xml) para mostrar ao usuário. */
    private int validationMessage() {
        if (inputName.getText().toString().trim().isEmpty()) return R.string.error_name;
        if (inputPhone.getText().toString().trim().isEmpty()) return R.string.error_phone_required;
        return R.string.error_phone_invalid;
    }

    /** Caixa de diálogo "Remove Ana?" com os botões Remove / Cancel. */
    private void confirmRemove(EmergencyContact contact) {
        // BUILDER: monta o objeto passo a passo; cada método devolve o próprio builder.
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.remove_contact_title, contact.getName()))
                .setMessage(R.string.remove_contact_text)
                .setPositiveButton(R.string.remove, (dialog, which) -> {
                    // Se o banco recusar algo, avisamos em vez de deixar o app fechar.
                    try {
                        dao.delete(contact);
                        Press.tick(list);
                    } catch (RuntimeException e) {
                        Toast.makeText(this, R.string.error_remove, Toast.LENGTH_SHORT).show();
                    }
                    render();
                })
                .setNegativeButton(R.string.cancel, null)   // null = só fecha
                .show();
    }
}
