package com.guard.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.guard.model.EmergencyContact;

import java.util.ArrayList;
import java.util.List;

/**
 * PADRÃO DAO (Data Access Object): a ÚNICA classe que sabe o SQL dos contatos.
 *
 * As telas trabalham com objetos EmergencyContact e nunca veem SQL nem Cursor.
 * Se um dia o banco mudar, só esta classe precisa ser alterada.
 */
public class ContactDao {

    private final GuardDatabaseHelper helper;

    public ContactDao(GuardDatabaseHelper helper) {
        this.helper = helper;
    }

    /** Salva um contato novo e devolve o id que o banco gerou. */
    public long insert(EmergencyContact contact) {
        SQLiteDatabase db = helper.getWritableDatabase();   // abre o banco para escrita
        long id = db.insertOrThrow(GuardDatabaseHelper.TABLE_CONTACTS, null, toValues(contact));
        contact.setId(id);
        return id;
    }

    /**
     * Apaga um contato e reorganiza as prioridades (1, 2, 3... sem buracos).
     *
     * Usa uma TRANSAÇÃO: ou TODOS os comandos funcionam, ou NENHUM vale.
     * Assim o banco nunca fica "pela metade" se algo der errado no meio.
     */
    public void delete(EmergencyContact contact) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        // TRY/FINALLY: o bloco "finally" roda SEMPRE, com erro ou sem erro.
        // Garante que a transação seja fechada.
        try {
            db.delete(GuardDatabaseHelper.TABLE_CONTACTS, "id = ?", new String[]{String.valueOf(contact.getId())});

            // A coluna priority é UNIQUE: se renumerarmos direto, dois contatos teriam o mesmo número
            // por um instante e o banco recusaria. Truque: primeiro deixamos todas negativas...
            db.execSQL("UPDATE " + GuardDatabaseHelper.TABLE_CONTACTS + " SET priority = -priority");

            // ...depois percorremos em ordem e numeramos 1, 2, 3...
            // (ORDER BY priority DESC: -1 vem antes de -2, ou seja, a ordem original.)
            int priority = 1;
            // TRY-WITH-RESOURCES: o Cursor declarado nos parênteses é fechado sozinho no final.
            try (Cursor c = db.rawQuery("SELECT id FROM " + GuardDatabaseHelper.TABLE_CONTACTS
                    + " ORDER BY priority DESC", null)) {
                while (c.moveToNext()) {   // anda linha por linha do resultado
                    // priority++ usa o valor atual e SÓ DEPOIS soma 1.
                    db.execSQL("UPDATE " + GuardDatabaseHelper.TABLE_CONTACTS + " SET priority = ? WHERE id = ?",
                            new Object[]{priority++, c.getLong(0)});
                }
            }
            db.setTransactionSuccessful();   // "pode confirmar tudo"
        } finally {
            db.endTransaction();   // confirma (se deu certo) ou desfaz (se deu erro)
        }
    }

    /** Todos os contatos, já em ordem de prioridade. */
    public List<EmergencyContact> findAllByPriority() {
        List<EmergencyContact> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT id, name, phone, relationship, priority FROM "
                + GuardDatabaseHelper.TABLE_CONTACTS + " ORDER BY priority", null)) {
            while (c.moveToNext()) {
                // Os números (0, 1, 2...) são as posições das colunas no SELECT acima.
                EmergencyContact contact = new EmergencyContact(
                        c.getString(1), c.getString(2), c.getString(3), c.getInt(4));
                contact.setId(c.getLong(0));
                list.add(contact);
            }
        }
        return list;
    }

    /** Próxima prioridade livre: a maior que existe + 1 (ou 1, se não houver nenhum contato). */
    public int nextPriority() {
        SQLiteDatabase db = helper.getReadableDatabase();
        // COALESCE(x, 0) = "use x; se for nulo (tabela vazia), use 0".
        try (Cursor c = db.rawQuery("SELECT COALESCE(MAX(priority), 0) + 1 FROM "
                + GuardDatabaseHelper.TABLE_CONTACTS, null)) {
            c.moveToFirst();
            return c.getInt(0);
        }
    }

    /** Converte o objeto em "coluna -> valor", o formato que o Android usa para inserir. */
    private static ContentValues toValues(EmergencyContact contact) {
        ContentValues v = new ContentValues();
        v.put("name", contact.getName());
        v.put("phone", contact.getPhone());
        v.put("relationship", contact.getRelationship());
        v.put("priority", contact.getPriority());
        return v;
    }
}
