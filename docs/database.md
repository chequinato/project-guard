# Banco de dados (SQLite)

O Guard usa o **SQLite que já vem no Android**. Não precisa instalar servidor nem baixar nada: o arquivo `guard.db` é criado no próprio celular/emulador na primeira execução (classe `GuardDatabaseHelper`).

Para ver o banco enquanto o app roda: Android Studio → **App Inspection** → **Database Inspector**.

## O que já existe

### `emergency_contacts`
| Coluna | Tipo | Regra |
| --- | --- | --- |
| id | INTEGER | PK, autoincremento |
| name | TEXT | obrigatório |
| phone | TEXT | obrigatório (só dígitos e `+`) |
| relationship | TEXT | opcional, padrão `''` |
| priority | INTEGER | obrigatório, único, `>= 1` (1 = avisado primeiro) |

### `trips`
| Coluna | Tipo | Regra |
| --- | --- | --- |
| id | INTEGER | PK, autoincremento |
| vehicle_type | TEXT | `CAR`, `MOTORCYCLE` ou `BICYCLE` (`VehicleType.name()`) |
| start_millis | INTEGER | início da viagem (ms desde 1970, fácil de ordenar) |
| end_millis | INTEGER | fim da viagem |
| max_speed_kmh | REAL | velocidade máxima |
| alert_count | INTEGER | quantos possíveis acidentes foram detectados |

Nome do usuário e veículo escolhido ficam em `SettingsStore` (SharedPreferences), porque são só dois valores e não precisam de tabela.

## Por que tão simples?

- **Leituras dos sensores não são salvas.** Uma viagem de 30 min gera 3.600 leituras (uma a cada 500 ms). O histórico só mostra o resumo.
- **Incidentes viram um contador** em `trips`. Se quiserem guardar o detalhe de cada um (horário, local, status final), o próximo passo seria:

```sql
CREATE TABLE incidents (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    trip_id       INTEGER NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    time_millis   INTEGER NOT NULL,
    latitude      REAL NOT NULL,
    longitude     REAL NOT NULL,
    status        TEXT NOT NULL   -- IncidentStatus.name()
);
```
Relação: uma viagem tem 0..N incidentes, por isso a chave estrangeira fica em `incidents`.
Lembrem de subir `DATABASE_VERSION` ao mudar o esquema, senão o `onCreate` não roda de novo em quem já instalou.
