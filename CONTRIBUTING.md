# Como contribuir (fluxo do grupo)

Cinco pessoas no mesmo repositório: se todo mundo mexer direto na `main`, um sobrescreve o trabalho do outro. Então:

## 1. Pegue uma tarefa
Combine no grupo quem faz o quê (README → *Próximos passos*). Uma pessoa por tarefa.

## 2. Crie uma branch
```bash
git checkout main
git pull
git checkout -b sensores-reais
```
Nome da branch: o que você vai fazer, curto e sem espaço.

## 3. Trabalhe e faça commits pequenos
```bash
git add app/src/main/java/com/guard/ui/trip/TripActivity.java
git commit -m "Usa AndroidSensorSource quando há permissão de localização"
```
Antes de subir: rode os testes no Android Studio (botão direito em `app/src/test` → *Run 'Tests in ...'*).

## 4. Suba e abra um Pull Request
```bash
git push -u origin sensores-reais
```
No GitHub aparece o botão **Compare & pull request**. Peça para alguém do grupo revisar antes de fazer o merge.

## Regras rápidas
- Não suba `local.properties`, `.idea/` nem `build/` (o `.gitignore` já cuida disso).
- Código e nomes de classes em inglês; comentários podem ser em português.
- Mudou algum limite de detecção? Atualize também o site (`docs/index.html`) para os dois contarem a mesma história.
