# Guard

> *Help arrives even when you can't ask for it.*

Guard is an Android app (Java, Object-Oriented Programming) that turns the sensors already in a smartphone into crash detection. If it detects a possible accident and the user does not answer within 30 seconds, it sends the user's location to their emergency contacts, one by one.

Concept project for **OOP & Professional English**, UniAnchieta, 2026.
Website: [`docs/index.html`](docs/index.html) (can be published with GitHub Pages, see below).

| Team | RA |
| --- | --- |
| Arthur Marques da Silva | 2600519 |
| Caio Alves | 2600531 |
| Gabriel Ribeiro | 2603036 |
| Gustavo Gonçalves Negri | 2641128 |
| Miguel Chequinato | 2602503 |

---

## Como abrir no Android Studio

1. `git clone https://github.com/chequinato/project-guard.git`
2. Android Studio → **Open** → selecione a pasta `project-guard` (a raiz, não a pasta `app`).
3. Espere o **Gradle Sync** terminar. Se o Android Studio sugerir atualizar o Android Gradle Plugin, pode aceitar.
4. Crie um emulador (Device Manager → um Pixel com Android 13 ou mais novo) e clique em **Run ▶**.

Não precisa instalar banco de dados: o SQLite já vem no Android e o arquivo `guard.db` é criado sozinho na primeira execução.

### Roteiro da demo
1. O app já abre com 3 contatos de demonstração (Ana, Dad, Bruno). Em **Edit** dá para adicionar ou remover.
2. Escolha o veículo e toque em **Start trip**.
3. Na tela escura, toque num cenário: `Real crash`, `Phone dropped`, `Hard brake`, `Speed bump`...
4. No `Real crash` aparece **Are you OK?** com 30 s. Deixe zerar para ver o alerta e o escalonamento, ou toque **I'm OK**.
5. Depois toque em **End trip** e abra **History** para ver a viagem salva.

Durante a contagem, o alerta usa o canal `DemoAlert` (não envia SMS de verdade). Na demo o escalonamento espera 20 s em vez de 2 min.

---

## Estrutura

```
project-guard/
├─ app/src/main/java/com/guard/
│  ├─ GuardApplication.java   Objeto do app inteiro: banco, preferências e a viagem em andamento
│  ├─ model/                  O "domínio": os dados e suas regras
│  │  ├─ vehicle/             HERANÇA: Vehicle (abstrata) → Car, Motorcycle, Bicycle + VehicleType
│  │  ├─ User, EmergencyContact, Trip, SensorReading
│  │  └─ Incident, IncidentStatus
│  ├─ sensor/                 OBSERVER: SensorMonitor, SensorListener,
│  │                          MockSensorSource (simulado) + Scenario, AndroidSensorSource (real)
│  ├─ detection/              POLIMORFISMO + STRATEGY: IncidentDetector e os 4 detectores,
│  │                          IncidentManager (a regra combinada)
│  ├─ alert/                  AlertChannel (DemoAlert, SmsAlert, MessagingAppAlert) e AlertService
│  ├─ session/                TripSession: liga sensor + detecção + viagem durante uma viagem
│  ├─ data/                   SQLite (DAO): GuardDatabaseHelper, ContactDao, TripDao, TripRecord,
│  │                          SettingsStore
│  └─ ui/                     Uma pasta por tela
│     ├─ splash/              Tela 0 · Abertura animada  (SplashActivity)
│     ├─ home/                Tela 1 · New trip          (MainActivity)
│     ├─ trip/                Tela 2 · Trip mode         (TripActivity)
│     ├─ alert/               Tela 3 · Are you OK?       (AlertActivity)
│     ├─ contacts/            Tela 4 · Contatos          (ContactsActivity)
│     ├─ history/             Tela 5 · Histórico         (HistoryActivity)
│     └─ common/              Peças usadas por mais de uma tela (ContactViews)
├─ app/src/main/res/
│  ├─ layout/                 O visual de cada tela (XML)
│  └─ values/                 Textos (strings.xml), cores (colors.xml) e temas (themes.xml)
├─ app/src/test/              Testes JUnit, na mesma divisão de pastas (rodam sem emulador)
└─ docs/                      Site, proposta, arquitetura e banco
```

**Regra de ouro da organização:** `model`, `sensor`, `detection`, `alert` e `session` são **Java puro** (sem `import android`). Por isso os 27 testes rodam no computador, sem emulador. Só `data/`, `ui/`, `AndroidSensorSource`, `SmsAlert` e `MessagingAppAlert` dependem do Android.

**O código é comentado para estudo:** cada classe explica o conceito de POO que usa, e as linhas com algum recurso novo de Java (lambda, generics, `final`, try-with-resources...) têm um comentário explicando.

### Caminho de uma leitura de sensor

```
MockSensorSource ──► IncidentManager ──► 4 detectores ──► regra ──► Incident
       │                                                                │
       ├──► Trip (guarda)                                    TripSession guarda o incidente ativo
       └──► TripSession ──► TripActivity (desenha)                      │
                                                     AlertActivity usa ESSE MESMO objeto
```

## Conceitos de POO (onde estão)

| Conceito | Onde |
| --- | --- |
| Encapsulamento | `EmergencyContact`, `User`, `Trip`, `Incident`: campos privados e setters que validam |
| Herança | `Vehicle` → `Car`, `Motorcycle`, `Bicycle` (cada um com seus limites) |
| Abstração | `Vehicle` e `SensorMonitor` abstratas; interfaces `IncidentDetector` e `AlertChannel` |
| Polimorfismo | `IncidentManager` percorre uma lista de `IncidentDetector`; `AlertService` usa qualquer `AlertChannel` |
| Composição | `User` tem `EmergencyContact`s; `Trip` tem `SensorReading`s e `Incident`s |
| Observer | `SensorMonitor` avisa os `SensorListener` (`IncidentManager`, `Trip`, `TripSession`) |
| Factory | `Vehicle.of(tipo)` cria o veículo certo sem quem chama conhecer as subclasses |
| DAO | `ContactDao` e `TripDao` são os únicos que conhecem SQL |
| Strategy | Detectores e canais trocáveis; o `Vehicle` escolhido muda os limites |

Mais detalhes e o diagrama de classes: [`docs/architecture.md`](docs/architecture.md).

---

## Testes

`app/src/test` tem 27 testes JUnit que rodam sem emulador (botão direito na pasta → *Run 'Tests in...'*).
O `IncidentManagerTest` toca todos os cenários do site (batida, celular caindo, freada, lombada, parado) e confere se o Guard responde certo em cada um. O `TripSessionTest` confere a viagem inteira e prova que a viagem e o alerta usam o mesmo `Incident`.

## Para estudar antes da apresentação

Cada um deve saber explicar a parte que vai apresentar. Perguntas para treinar:

1. Por que `Vehicle` é `abstract`? O que quebraria se desse para fazer `new Vehicle(...)`?
2. O `IncidentManager` não sabe quais detectores existem. Como ele chama `analyze()` em todos? Que conceito é esse?
3. Por que o `DecelerationDetector` guarda uma fila e o `StillnessDetector` guarda só um número?
4. Pela regra (Impact **and** Speed drop **and** (Rotation **or** Stillness)), por que o celular caindo no chão não gera alerta?
5. Como trocar o `MockSensorSource` pelos sensores reais sem mexer no `IncidentManager`?
6. Como trocar o `DemoAlert` por SMS de verdade? Quantas classes você precisa alterar?
7. Por que a prioridade dos contatos é `UNIQUE` no banco e também é validada no `User`?

## Próximos passos possíveis

- Usar `AndroidSensorSource` (sensores reais) com pedido de permissão de localização.
- Trocar `DemoAlert` por `SmsAlert` e pedir a permissão `SEND_SMS`.
- Tela de ajustes de sensibilidade (feature 9 da proposta).
- Salvar cada incidente com o status final (tabela `incidents`, ver `docs/database.md`).

## Como trabalhar em grupo

Veja [`CONTRIBUTING.md`](CONTRIBUTING.md). Resumo: **ninguém faz commit direto na `main`**; cada um trabalha numa branch e abre Pull Request.

## Publicar o site (GitHub Pages)

Settings → Pages → *Deploy from a branch* → branch `main`, pasta `/docs` → Save. Em alguns minutos o site fica em `https://chequinato.github.io/project-guard/`.

---

*Guard is a concept project, not an emergency service.*
