# ADR 0007: Entrega com contêiner e deploy simulado

- **Status:** aceita
- **Data:** 2026-10-20

## Contexto

Até aqui, a API só rodava com o Java instalado na máquina. O fluxo de branches (`develop` → `homolog` → `main`) e as tags de versão já existiam, mas uma tag não entregava nada: não havia artefato versionado nem ambiente onde ele fosse implantado.

O projeto não tem servidor nem nuvem paga. O objetivo é mostrar um pipeline de entrega completo e verificável, com promoção entre ambientes e aprovação para produção, sem depender de infraestrutura externa.

## Decisões

1. **Imagem Docker multi-stage.** O primeiro estágio compila com o JDK. O segundo roda só com o JRE (Alpine), sem Maven nem código-fonte. As dependências do Maven são baixadas numa camada separada do código, então mudar uma classe não baixa tudo de novo. O jar é extraído nas camadas do Spring Boot (`dependencies`, `spring-boot-loader`, `snapshot-dependencies`, `application`): numa versão nova, só a camada `application` muda.

2. **Contêiner seguro por padrão.** O processo roda com um usuário sem privilégios (UID 10001). O `.dockerignore` é uma lista de permissões: só entram no build o `pom.xml`, o Maven Wrapper e `src/main`, então `.env`, `.git` e `target` nunca vão parar na imagem. Não há segredo na imagem: `JWT_SECRET` e as credenciais do banco chegam por variável de ambiente, e a API não sobe sem um `JWT_SECRET` de pelo menos 32 caracteres. A memória da JVM acompanha o limite do contêiner (`MaxRAMPercentage=75`), e o `HEALTHCHECK` usa o `/actuator/health`.

3. **A API no `compose.yaml` fica atrás do perfil `api`.** `docker compose up` continua subindo só o PostgreSQL, para quem roda a API pela IDE. `docker compose --profile api up` sobe a API e o banco, e a API só inicia quando o banco está saudável. O CI e o deploy usam o mesmo arquivo, então o que se testa é o que se executa localmente.

4. **Um teste de fumaça reaproveitado em todo lugar.** O `scripts/smoke-test.sh` percorre o fluxo principal pela API real: cadastro, login, `401` sem token, depósito, reenvio idempotente (`Idempotent-Replayed: true`), transferência, extrato e saldo final. Ele roda no CI de cada PR e depois de cada deploy.

5. **Deploy disparado por tag, com ambientes do GitHub.**
   - `vX.Y.Z-rc.N` na `homolog`: constrói a imagem, publica no GitHub Container Registry (GHCR) e implanta no ambiente `homolog`.
   - `vX.Y.Z` na `main`: implanta no ambiente `production`, que exige **aprovação manual**.
   - Os dois ambientes só aceitam tags `v*` e têm o próprio `JWT_SECRET`.
   - O workflow recusa tags que não batem com a versão do `pom.xml` ou que não apontam para a branch certa.

6. **Construir uma vez, implantar várias.** Produção não constrói uma imagem nova. O workflow exige que o código da tag final seja idêntico (mesma árvore do Git) ao da última `rc` e promove **a mesma imagem**, identificada pelo digest `sha256`, que passou na homologação. Promover é só criar novas tags no registro (`producao`, `X.Y.Z`, `X.Y`, `latest`); nada é reconstruído.

7. **"Implantar" é simulado.** O job de deploy sobe a imagem publicada com um PostgreSQL no próprio runner do GitHub Actions e roda o teste de fumaça. Num ambiente real, só esse passo mudaria (por exemplo, para atualizar um serviço na nuvem ou um cluster); a construção, a promoção e a aprovação continuariam iguais.

8. **A GitHub Release continua manual.** Ela é criada com `gh release create` depois do deploy em produção, para que o autor da release seja uma pessoa e as notas sejam revisadas.

## Consequências

- ✅ Toda versão vira uma imagem versionada no GHCR, que roda em qualquer máquina com Docker.
- ✅ Um Dockerfile quebrado é detectado no PR, antes do merge, e não na hora da release.
- ✅ A imagem que chega à produção é byte a byte a que foi homologada.
- ✅ Produção exige aprovação e só aceita código que passou pela homologação.
- ⚠️ Não existe um servidor no ar: o ambiente vive enquanto o job roda. Para testar a imagem, é preciso rodá-la localmente.
- ⚠️ Os testes do Maven não rodam no build da imagem (já rodaram no CI). Uma imagem construída fora do fluxo de PRs não passou por eles.
- ⚠️ No primeiro push, o pacote do GHCR nasce privado e precisa ser tornado público nas configurações do pacote.
