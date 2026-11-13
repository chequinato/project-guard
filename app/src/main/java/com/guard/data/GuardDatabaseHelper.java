package com.guard.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Banco SQLite local do Guard (o arquivo guard.db fica dentro do celular/emulador).
 * Não precisa instalar nada: o SQLite já vem no Android.
 *
 * Tabelas (modelo simples, de propósito):
 *   emergency_contacts -> tela de contatos
 *   trips              -> tela de histórico (um resumo por viagem; as leituras dos sensores NÃO são salvas)
 *
 * Detalhes e próximos passos possíveis: docs/database.md
 */
public class GuardDatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "guard.db";
    public static final int DATABASE_VERSION = 2;

    public static final String TABLE_CONTACTS = "emergency_contacts";
    public static final String TABLE_TRIPS = "trips";

    private static final String CREATE_CONTACTS =
            "CREATE TABLE " + TABLE_CONTACTS + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name TEXT NOT NULL, "
                    + "phone TEXT NOT NULL, "
                    + "relationship TEXT NOT NULL DEFAULT '', "
                    + "priority INTEGER NOT NULL UNIQUE CHECK (priority >= 1)"
                    + ")";

    private static final String CREATE_TRIPS =
            "CREATE TABLE " + TABLE_TRIPS + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "vehicle_type TEXT NOT NULL, "          // CAR | MOTORCYCLE | BICYCLE
                    + "start_millis INTEGER NOT NULL, "
                    + "end_millis INTEGER NOT NULL, "
                    + "max_speed_kmh REAL NOT NULL DEFAULT 0, "
                    + "alert_count INTEGER NOT NULL DEFAULT 0"
                    + ")";

    public GuardDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true); // o SQLite vem com FK desligada por padrão
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_CONTACTS);
        db.execSQL(CREATE_TRIPS);
        seedDemoContacts(db);
    }

    /**
     * Contatos de demonstração (os mesmos do site), para a apresentação já abrir pronta.
     * Os números são fictícios. Para começar vazio, apague esta chamada no onCreate.
     */
    private static void seedDemoContacts(SQLiteDatabase db) {
        String sql = "INSERT INTO " + TABLE_CONTACTS + " (name, phone, relationship, priority) VALUES (?, ?, ?, ?)";
        db.execSQL(sql, new Object[]{"Ana", "+5511900000001", "Sister", 1});
        db.execSQL(sql, new Object[]{"Dad", "+5511900000002", "Father", 2});
        db.execSQL(sql, new Object[]{"Bruno", "+5511900000003", "Friend", 3});
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Projeto de faculdade: na dúvida, recria tudo (perde os dados locais).
        // Num app de verdade, aqui iriam os ALTER TABLE de cada versão.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRIPS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);
        onCreate(db);
    }
}
