# Project Proposal: Guard

**Course:** Object-Oriented Programming (OOP) and Professional English · **Institution:** UniAnchieta · **Year:** 2026

## 1. Title
**Guard: Crash Detection for Any Smartphone**
*Help arrives even when you can't ask for it.*

## 2. Executive Summary
Guard is a mobile app that turns the sensors already in a smartphone into crash detection. If it detects a possible accident and the user does not answer within 30 seconds, it sends the user's live location to their emergency contacts. It works for cars, motorcycles and bicycles, with no extra device and no subscription. We also built an interactive website that explains the idea and lets visitors test the detector.

## 3. Problem or Need
After a crash, the minutes before anyone knows are the ones that matter most, but the victim may be unconscious or unable to call for help. According to the WHO (Global status report on road safety 2023), about 1.19 million people die on the world's roads every year, and over half of them are pedestrians, cyclists and motorcyclists. Today's crash detection usually needs a specific phone, a paid plan or extra hardware, so many riders are left without protection.

## 4. Proposed Solution
We built an Android app in Java using Object-Oriented Programming. Guard reads four signals together:
- **Speed drop (GPS):** at least 20 km/h lost in 2 seconds (motorcycle profile)
- **Impact (accelerometer):** a violent jolt, stronger than braking
- **Rotation (gyroscope):** the phone spun or tumbled
- **Stillness:** no movement for 10 seconds

**The rule:** impact **and** speed drop **and** (rotation **or** stillness). Because one sensor can be fooled but four signals together cannot, Guard avoids false alarms like a dropped phone, a hard brake or a speed bump.

When the signals agree, Guard starts a 30-second countdown with an "I'm OK" button. If nobody answers, contact 1 receives an SMS with a live map link. If there is no reply in 2 minutes, Guard alerts the next contact. Each vehicle (car, motorcycle, bicycle) has its own sensitivity limits.

## 5. Objectives and Expected Benefits
**Objectives:**
- Detect possible crashes using only the sensors of a common smartphone
- Reduce the time until someone learns about the accident
- Keep false alarms low with combined signals

**Expected benefits:**
- Faster help for people who cannot call
- Free and available to anyone with a phone
- Privacy: the location is shared only when an alert fires, and only with the user's own list
- A practical project that applies OOP: encapsulation, inheritance, abstraction and polymorphism

## 6. Implementation Plan
| Phase | Main steps | Time |
| --- | --- | --- |
| 1. Planning | Project proposal, classes, GitHub repository | Week 1 |
| 2. Website prototype | Interactive site with ride simulation and detector demo | Week 1 |
| 3. Core development | Main Java classes and SQLite database | Weeks 2–3 |
| 4. Detection and alerts | Detectors, countdown, alerts, contact escalation | Weeks 4–5 |
| 5. App interface | Android Studio screens | Week 6 |
| 6. Testing | Unit tests with simulated sensor data | Week 7 |
| 7. Presentation | Final presentation | Week 8 |

*(Adjust the weeks to the real course calendar.)*

## 7. Resources and Risks
**Resources:** a team of five students; Java, Android Studio and SQLite; GitHub for version control; an Android phone or emulator.

**Risks and solutions:**
- *Gyroscope data may be too complex.* → Simulated (mock) sensor data for tests and the demo.
- *False alarms or missed crashes.* → Four combined signals and per-vehicle limits.
- *Short deadline.* → Tasks divided in GitHub branches.
- *Privacy and permissions.* → Ask for consent; share location only when an alert fires.
- *Real-life limits.* → Guard is a concept project, not an emergency service.

## 8. Conclusion
Guard offers crash detection to the people others leave out: anyone with a phone, in a car, on a motorcycle or on a bicycle. It is simple, free and designed to avoid false alarms. We have a working website prototype and a Java app with a clear structure and tests. For these reasons, we believe this project should be approved.
