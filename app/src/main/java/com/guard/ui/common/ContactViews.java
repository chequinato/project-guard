package com.guard.ui.common;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.guard.R;
import com.guard.model.BrazilianPhone;
import com.guard.model.EmergencyContact;

/**
 * Monta a "linha" visual de um contato (item_contact.xml).
 * Usada na tela inicial e na de contatos, por isso fica em "common".
 */
public final class ContactViews {

    /**
     * Cores dos avatares (fundo, texto), as mesmas três do site: azul, laranja e verde.
     * int[][] = array de arrays: cada posição guarda um par {fundo, texto}.
     */
    private static final int[][] AVATAR_COLORS = {
            {R.color.guard_ice, R.color.guard_accent},
            {R.color.avatar_orange_bg, R.color.guard_alert_text},
            {R.color.avatar_green_bg, R.color.guard_ok_text},
    };

    private ContactViews() { }

    /**
     * Cria a linha de um contato.
     *
     * @param parent      onde a linha vai entrar
     * @param contact     o contato
     * @param showDivider false na última linha (não desenha a linha divisória)
     * @param onRemove    ação do botão × (null = sem botão, como na tela inicial)
     */
    public static View inflate(ViewGroup parent, EmergencyContact contact, boolean showDivider, Runnable onRemove) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);

        ((TextView) row.findViewById(R.id.txt_priority)).setText(String.valueOf(contact.getPriority()));
        ((TextView) row.findViewById(R.id.txt_name)).setText(contact.getName());
        // Sem parentesco, mostramos o telefone já formatado: "(11) 91234-5678".
        String subtitle = contact.getRelationship().isEmpty()
                ? BrazilianPhone.format(contact.getPhone())
                : contact.getRelationship();
        ((TextView) row.findViewById(R.id.txt_relationship)).setText(subtitle);

        // Avatar: primeira letra + cor que depende da prioridade (1 azul, 2 laranja, 3 verde, 4 azul...).
        TextView avatar = row.findViewById(R.id.txt_avatar);
        int[] colors = AVATAR_COLORS[(contact.getPriority() - 1) % AVATAR_COLORS.length];
        avatar.setText(contact.getName().substring(0, 1).toUpperCase());
        avatar.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(parent.getContext(), colors[0])));
        avatar.setTextColor(ContextCompat.getColor(parent.getContext(), colors[1]));

        row.findViewById(R.id.divider).setVisibility(showDivider ? View.VISIBLE : View.GONE);

        if (onRemove != null) {
            View action = row.findViewById(R.id.btn_action);
            action.setVisibility(View.VISIBLE);
            // Runnable é uma interface com um método só (run): perfeita para "guardar uma ação".
            action.setOnClickListener(v -> onRemove.run());
        }
        return row;
    }
}
