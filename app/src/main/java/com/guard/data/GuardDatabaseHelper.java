package com.guard.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Cria e abre o banco SQLite do Guard (o arquivo "guard.db", dentro do celular/emulador).
 * Não precisa instalar nada: o SQLite já vem no Android.
 *
 * "extends SQLiteOpenHelper" = herdamos uma classe pronta do Android, que já sabe
 * abrir o arquivo, criar na primeira vez (onCreate) e atualizar quando a versão muda (onUpgrade).
 *
 * Tabelas:
 *   emergency_contacts -> tela de contatos
 *   trips              -> tela de histórico (um resumo por viagem)
 *
 * Modelo completo e próximos passos: docs/database.md
 */
public class GuardDatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "guard.db";

    /**
     * Versão do ESQUEMA (estrutura das tabelas). Toda vez que mudar uma tabela, some 1:
     * o Android percebe a diferença e chama onUpgrade() em quem já tinha o app instalado.
     */
    public static final int DATABASE_VERSION = 2;

    public static final String TABLE_CONTACTS = "emergency_contacts";
    public static final String TABLE_TRIPS = "trips";

    // SQL guardado em constantes. O "+" junta pedaços de texto (concatenação).
    private static final String CREATE_CONTACTS =
            "CREATE TABLE " + TABLE_CONTACTS + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "          // número gerado sozinho
                    + "name TEXT NOT NULL, "                            // NOT NULL = obrigatório
                    + "phone TEXT NOT NULL, "
                    + "relationship TEXT NOT NULL DEFAULT '', "        // DEFAULT = valor se ninguém passar
                    + "priority INTEGER NOT NULL UNIQUE CHECK (priority >= 1)"  // UNIQUE = sem repetir
                    + ")";

    private static final String CREATE_TRIPS =
            "CREATE TABLE " + TABLE_TRIPS + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "vehicle_type TEXT NOT NULL, "          // "CAR", "MOTORCYCLE" ou "BICYCLE"
                    + "start_millis INTEGER NOT NULL, "       // data como número: fácil de ordenar
                    + "end_millis INTEGER NOT NULL, "
                    + "max_speed_kmh REAL NOT NULL DEFAULT 0, " // REAL = número com vírgula
                    + "alert_count INTEGER NOT NULL DEFAULT 0"
                    + ")";

    public GuardDatabaseHelper(Context context) {
        // null = usar o "cursor" padrão do Android (não precisamos de um personalizado).
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /** Roda UMA vez: quando o arquivo guard.db ainda não existe. */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_CONTACTS);   // execSQL = executa um comando SQL que não devolve linhas
        db.execSQL(CREATE_TRIPS);
        seedDemoContacts(db);
    }

    /**
     * Roda quando DATABASE_VERSION aumenta.
     * Projeto de faculdade: apagamos e recriamos tudo (perde os dados locais).
     * Num app de verdade, aqui iriam comandos ALTER TABLE para não perder nada.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRIPS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);
        onCreate(db);
    }

    /**
     * Contatos de demonstração (os mesmos do site), para a apresentação já abrir pronta.
     * Os números são fictícios. Para começar com a lista vazia, apague a chamada no onCreate().
     */
    private static void seedDemoContacts(SQLiteDatabase db) {
        // Os "?" são PARÂMETROS: o banco encaixa os valores com segurança.
        // Nunca monte SQL juntando texto digitado pelo usuário (risco de "SQL injection").
        String sql = "INSERT INTO " + TABLE_CONTACTS + " (name, phone, relationship, priority) VALUES (?, ?, ?, ?)";
        // new Object[]{...} = um array criado na hora, com um valor para cada "?".
        db.execSQL(sql, new Object[]{"Ana", "+5511900000001", "Sister", 1});
        db.execSQL(sql, new Object[]{"Dad", "+5511900000002", "Father", 2});
        db.execSQL(sql, new Object[]{"Bruno", "+5511900000003", "Friend", 3});
    }
}
