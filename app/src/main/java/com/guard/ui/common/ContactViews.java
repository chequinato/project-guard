package com.guard.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.guard.R;
import com.guard.model.EmergencyContact;

/**
 * Monta a "linha" visual de um contato (o layout item_contact.xml).
 * Fica em "common" porque duas telas usam: a inicial (home) e a de contatos.
 *
 * É uma classe UTILITÁRIA: só tem métodos static e ninguém precisa criar um objeto dela.
 * - "final" na classe: ninguém herda.
 * - construtor "private": ninguém consegue fazer new ContactViews().
 */
public final class ContactViews {

    private ContactViews() { }

    /**
     * Cria a View de um contato preenchida com os dados dele.
     *
     * @param parent  onde a linha vai ser colocada (usado para medir o tamanho)
     * @param contact o contato a mostrar
     */
    public static View inflate(ViewGroup parent, EmergencyContact contact) {
        // "Inflar" = transformar o XML do layout em objetos View de verdade.
        // O "false" no fim diz: "ainda não coloque dentro do parent, eu coloco depois".
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);

        // findViewById acha um componente pelo id definido no XML (android:id="@+id/txt_name").
        // O (TextView) na frente é um CAST, para podermos chamar setText.
        ((TextView) row.findViewById(R.id.txt_priority)).setText(String.valueOf(contact.getPriority()));
        // substring(0, 1) = só a primeira letra do nome, para o "avatar".
        ((TextView) row.findViewById(R.id.txt_avatar)).setText(contact.getName().substring(0, 1).toUpperCase());
        ((TextView) row.findViewById(R.id.txt_name)).setText(contact.getName());

        // Sem relação cadastrada? Mostra o telefone no lugar.
        String subtitle = contact.getRelationship().isEmpty() ? contact.getPhone() : contact.getRelationship();
        ((TextView) row.findViewById(R.id.txt_relationship)).setText(subtitle);
        return row;
    }
}
