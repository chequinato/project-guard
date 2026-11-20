package com.guard.ui.contacts;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.guard.data.SettingsStore;
import com.google.android.material.textfield.TextInputLayout;
import com.guard.model.BrazilianPhone;
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
    private TextInputLayout layoutName, layoutPhone;              // a "moldura" do campo (mostra o erro)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        dao = ((GuardApplication) getApplication()).getContactDao();

        list = findViewById(R.id.list_contacts);
        inputName = findViewById(R.id.input_name);
        inputPhone = findViewById(R.id.input_phone);
        inputRelationship = findViewById(R.id.input_relationship);
        layoutName = findViewById(R.id.layout_name);
        layoutPhone = findViewById(R.id.layout_phone);
        clearErrorOnType(inputName, layoutName);
        clearErrorOnType(inputPhone, layoutPhone);

        // Seu nome (vai no SMS). Começa com o que está salvo e salva a cada letra digitada.
        SettingsStore settings = ((GuardApplication) getApplication()).getSettings();
        EditText inputUserName = findViewById(R.id.input_user_name);
        inputUserName.setText(settings.getUserName());
        inputUserName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                settings.setUserName(s.toString().trim());
            }
        });

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
        String name = inputName.getText().toString();
        String phone = inputPhone.getText().toString();

        // 1) Valida ANTES de criar. Cada erro aparece embaixo do próprio campo.
        boolean nameOk = !name.trim().isEmpty();
        layoutName.setError(nameOk ? null : getString(R.string.error_name));
        String phoneError = phoneError(phone);
        layoutPhone.setError(phoneError);   // null = sem erro (volta a mostrar a dica)
        if (!nameOk || phoneError != null) {
            Press.strong(nameOk ? layoutPhone : layoutName);   // vibração curta de "não deu"
            (nameOk ? inputPhone : inputName).requestFocus();
            return;
        }

        try {
            // 2) Tudo certo: salva o número já no padrão (+5511912345678).
            EmergencyContact contact = new EmergencyContact(
                    name,
                    BrazilianPhone.normalize(phone),
                    inputRelationship.getText().toString(),
                    dao.nextPriority());
            dao.insert(contact);
            Press.tick(list);

            // Limpa o formulário, esconde o teclado e mostra a lista atualizada.
            inputName.setText("");
            inputPhone.setText("");
            inputRelationship.setText("");
            layoutName.setError(null);
            layoutPhone.setError(null);
            inputName.clearFocus();
            InputMethodManager keyboard = getSystemService(InputMethodManager.class);
            keyboard.hideSoftInputFromWindow(list.getWindowToken(), 0);
            render();
        } catch (IllegalArgumentException e) {
            // Segurança extra: o modelo também valida. Não deve acontecer depois das checagens acima.
            Toast.makeText(this, R.string.error_phone_invalid, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Texto do erro do telefone (no idioma do app), ou null se estiver tudo certo.
     * A REGRA mora no modelo (BrazilianPhone); aqui só escolhemos a frase para cada problema.
     */
    private String phoneError(String phone) {
        BrazilianPhone.Problem problem = BrazilianPhone.check(phone);
        if (problem == null) {
            // Formato certo: falta só conferir se o número já não está na lista.
            String normalized = BrazilianPhone.normalize(phone);
            for (EmergencyContact c : dao.findAllByPriority()) {
                if (c.getPhone().equals(normalized)) return getString(R.string.error_phone_duplicate);
            }
            return null;
        }
        switch (problem) {
            case EMPTY:              return getString(R.string.error_phone_required);
            case INVALID_CHARACTERS: return getString(R.string.error_phone_chars);
            case NOT_BRAZIL:         return getString(R.string.error_phone_country);
            case INVALID_DDD:        return getString(R.string.error_phone_ddd, BrazilianPhone.ddd(phone));
            case NOT_MOBILE:         return getString(R.string.error_phone_mobile);
            default:                 return getString(R.string.error_phone_invalid);   // WRONG_LENGTH
        }
    }

    /** Assim que a pessoa volta a digitar, o erro some (para não ficar "brigando" com ela). */
    private static void clearErrorOnType(EditText input, TextInputLayout layout) {
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (layout.getError() != null) layout.setError(null);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
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
