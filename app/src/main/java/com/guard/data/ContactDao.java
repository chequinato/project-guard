package com.guard.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.guard.model.EmergencyContact;

import java.util.ArrayList;
import java.util.List;

/**
 * DAO (Data Access Object): a ÚNICA classe que sabe o SQL dos contatos.
 * As telas trabalham com objetos EmergencyContact e nunca veem um Cursor.
 * O TripDao segue o mesmo modelo.
 */
public class ContactDao {

    private final GuardDatabaseHelper helper;

    public ContactDao(GuardDatabaseHelper helper) {
        this.helper = helper;
    }

    public long insert(EmergencyContact contact) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long id = db.insertOrThrow(GuardDatabaseHelper.TABLE_CONTACTS, null, toValues(contact));
        contact.setId(id);
        return id;
    }

    public void delete(EmergencyContact contact) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(GuardDatabaseHelper.TABLE_CONTACTS, "id = ?", new String[]{String.valueOf(contact.getId())});
            // Reorganiza as prioridades: 1, 2, 3... sem buracos.
            // Primeiro joga todo mundo para negativo para não bater na restrição UNIQUE no meio do caminho.
            db.execSQL("UPDATE " + GuardDatabaseHelper.TABLE_CONTACTS + " SET priority = -priority");
            int priority = 1;
            try (Cursor c = db.rawQuery("SELECT id FROM " + GuardDatabaseHelper.TABLE_CONTACTS
                    + " ORDER BY priority DESC", null)) {
                while (c.moveToNext()) {
                    db.execSQL("UPDATE " + GuardDatabaseHelper.TABLE_CONTACTS + " SET priority = ? WHERE id = ?",
                            new Object[]{priority++, c.getLong(0)});
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<EmergencyContact> findAllByPriority() {
        List<EmergencyContact> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT id, name, phone, relationship, priority FROM "
                + GuardDatabaseHelper.TABLE_CONTACTS + " ORDER BY priority", null)) {
            while (c.moveToNext()) {
                EmergencyContact contact = new EmergencyContact(
                        c.getString(1), c.getString(2), c.getString(3), c.getInt(4));
                contact.setId(c.getLong(0));
                list.add(contact);
            }
        }
        return list;
    }

    public int nextPriority() {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT COALESCE(MAX(priority), 0) + 1 FROM "
                + GuardDatabaseHelper.TABLE_CONTACTS, null)) {
            c.moveToFirst();
            return c.getInt(0);
        }
    }

    private static ContentValues toValues(EmergencyContact contact) {
        ContentValues v = new ContentValues();
        v.put("name", contact.getName());
        v.put("phone", contact.getPhone());
        v.put("relationship", contact.getRelationship());
        v.put("priority", contact.getPriority());
        return v;
    }
}
