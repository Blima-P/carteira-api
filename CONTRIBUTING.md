# Guia de contribuição

Este projeto segue um fluxo Git inspirado em **Git Flow**, com ambientes simulados de **homologação** e **produção**.

## Branches

| Branch      | Papel                           | Recebe código de                        |
|-------------|---------------------------------|-----------------------------------------|
| `main`      | Produção                        | `homolog` (PR de release)               |
| `homolog`   | Homologação (pré-produção)      | `develop` (PR de release)               |
| `develop`   | Integração (branch padrão)      | branches de trabalho (PR)               |
| `feat/*`, `fix/*`, `test/*`, `ci/*`, `docs/*`, `refactor/*`, `chore/*` | Trabalho do dia a dia | — |
| `hotfix/*`  | Correção urgente em produção    | sai da `main`, volta para `main` e `develop` |

Ninguém faz push direto em `develop`, `homolog` ou `main`: tudo entra via Pull Request com CI verde.

```text
feat/* ──squash──► develop ──merge──► homolog ──merge──► main
                                         │                 │
                                    tag vX.Y.Z-rc.N   tag vX.Y.Z + Release
```

## Commits — Conventional Commits

```text
<tipo>(<escopo opcional>): <descrição no imperativo, minúscula>
```

| Tipo       | Quando usar                                   |
|------------|-----------------------------------------------|
| `feat`     | Nova funcionalidade                           |
| `fix`      | Correção de bug                               |
| `test`     | Adição/ajuste de testes                       |
| `refactor` | Mudança de código sem alterar comportamento   |
| `docs`     | Documentação                                  |
| `ci`       | Pipelines (GitHub Actions)                    |
| `build`    | Build/dependências (Maven, Docker)            |
| `chore`    | Tarefas de manutenção                         |

Exemplos: `feat(auth): adiciona login com JWT`, `fix(transfer): impede saldo negativo`.

Mudanças incompatíveis usam `!` (`feat(api)!: ...`) e geram versão **major**.

## Estratégia de merge

| PR                      | Estratégia       | Motivo                                                   |
|-------------------------|------------------|----------------------------------------------------------|
| `feat/*` → `develop`    | **Squash**       | 1 PR = 1 commit limpo; o título do PR vira a mensagem    |
| `develop` → `homolog`   | **Merge commit** | Preserva o histórico e evita divergência entre branches  |
| `homolog` → `main`      | **Merge commit** | Idem                                                     |

## Versionamento — SemVer

`MAJOR.MINOR.PATCH` — ex.: `v0.2.1`

- **MAJOR**: quebra de compatibilidade da API
- **MINOR**: nova funcionalidade compatível
- **PATCH**: correção compatível

Tags:

- `vX.Y.Z-rc.N` — *release candidate*, criada na `homolog`
- `vX.Y.Z` — versão final, criada na `main` (gera uma GitHub Release)

## Passo a passo de uma feature

```bash
git switch develop
git pull
git switch -c feat/minha-feature
# ... código + testes ...
git add .
git commit -m "feat(escopo): descreve a mudança"
git push -u origin feat/minha-feature
gh pr create --base develop --fill
```

## Passo a passo de uma release

```bash
# 0. preparar a versão (branch chore/release-vX.Y.Z -> develop, squash):
#    tirar o -SNAPSHOT do pom.xml e mover "Não lançado" do CHANGELOG.md para a nova versão

# 1. develop -> homolog
gh pr create --base homolog --head develop --title "release: v0.1.0"
# após merge:
git switch homolog && git pull
git tag -a v0.1.0-rc.1 -m "v0.1.0-rc.1"
git push origin v0.1.0-rc.1

# 2. homolog -> main (após validar em homologação)
gh pr create --base main --head homolog --title "release: v0.1.0"
# após merge:
git switch main && git pull
git tag -a v0.1.0 -m "v0.1.0"
git push origin v0.1.0
gh release create v0.1.0 --generate-notes
```
