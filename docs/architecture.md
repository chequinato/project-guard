# Arquitetura do Guard

## Fluxo de uma leitura

```
MockSensorSource / AndroidSensorSource      (SensorMonitor, padrão Observer)
        │ publish(SensorReading)
        ├──► Trip.addReading()               guarda velocidade máxima
        ├──► TripActivity.render()           atualiza a tela
        └──► IncidentManager.onReading()
                 │ para cada IncidentDetector (polimorfismo):
                 │   ImpactDetector · DecelerationDetector · RolloverDetector · StillnessDetector
                 │ regra: Impact AND SpeedDrop AND (Rotation OR Stillness)
                 ▼
             IncidentListener ──► CountdownActivity (30 s, "I'm OK")
                                         │ tempo acabou
                                         ▼
                                  AlertService.start() ──► AlertChannel.send()
                                         │ sem resposta
                                         ▼
                                  AlertService.escalate() ──► próximo contato
```

## Diagrama de classes

```mermaid
classDiagram
    class Vehicle {
        <<abstract>>
        -double impactThresholdG
        -double speedDropThresholdKmh
        -double rotationThresholdDegPerSec
        +of(VehicleType) Vehicle
    }
    Vehicle <|-- Car
    Vehicle <|-- Motorcycle
    Vehicle <|-- Bicycle

    class User {
        -String name
        -Vehicle selectedVehicle
        -List~EmergencyContact~ contacts
        +addContact(EmergencyContact)
        +getContactsByPriority()
    }
    User o-- Vehicle
    User *-- EmergencyContact

    class Trip {
        -List~SensorReading~ readings
        -List~Incident~ incidents
        +addReading(SensorReading)
        +end(long)
    }
    Trip *-- SensorReading
    Trip *-- Incident

    class SensorMonitor {
        <<abstract>>
        -List~SensorListener~ listeners
        #publish(SensorReading)
        +start()
        +stop()
    }
    SensorMonitor <|-- MockSensorSource
    SensorMonitor <|-- AndroidSensorSource
    SensorMonitor --> SensorListener

    class SensorListener { <<interface>> +onReading(SensorReading) }
    SensorListener <|.. IncidentManager

    class IncidentDetector {
        <<interface>>
        +analyze(SensorReading, Vehicle) boolean
        +reset()
    }
    IncidentDetector <|.. ImpactDetector
    IncidentDetector <|.. DecelerationDetector
    IncidentDetector <|.. RolloverDetector
    IncidentDetector <|.. StillnessDetector
    IncidentManager o-- IncidentDetector

    class AlertChannel {
        <<interface>>
        +send(EmergencyContact, String) boolean
    }
    AlertChannel <|.. SmsAlert
    AlertChannel <|.. MessagingAppAlert
    AlertChannel <|.. DemoAlert
    AlertService --> AlertChannel
    AlertService --> Incident
```

## Por que o domínio não usa Android

`model`, `sensor` (exceto `AndroidSensorSource`), `detection` e `alert` (exceto `SmsAlert` e `MessagingAppAlert`) são Java puro. Vantagens:

- os testes rodam no computador, sem emulador;
- trocar sensor real por simulado (ou SMS por demo) é trocar **uma** classe, graças às abstrações.
