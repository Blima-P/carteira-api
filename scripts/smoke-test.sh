#!/usr/bin/env bash
# Teste de fumaça: confirma que uma instância recém-implantada atende o fluxo principal.
# Uso: scripts/smoke-test.sh [url-base]   (padrão: http://localhost:8080)
# Requer: curl e jq. Cria usuários com e-mails únicos, então pode rodar mais de uma vez no mesmo banco.
set -euo pipefail

URL="${1:-http://localhost:8080}"
EXECUCAO="$(date +%s)-$RANDOM"
SENHA="senha-de-fumaca-123"
CORPO="$(mktemp)"
CABECALHOS="$(mktemp)"
trap 'rm -f "$CORPO" "$CABECALHOS"' EXIT

falhar() {
  echo "FALHOU: $*" >&2
  echo "Resposta: $(cat "$CORPO")" >&2
  exit 1
}

# chamar <método> <caminho> <status esperado> [token] [corpo JSON] [Idempotency-Key]
chamar() {
  local metodo="$1" caminho="$2" esperado="$3" token="${4:-}" dados="${5:-}" chave="${6:-}"
  local argumentos=(-sS -X "$metodo" -o "$CORPO" -D "$CABECALHOS" -w '%{http_code}')
  if [ -n "$token" ]; then argumentos+=(-H "Authorization: Bearer $token"); fi
  if [ -n "$chave" ]; then argumentos+=(-H "Idempotency-Key: $chave"); fi
  if [ -n "$dados" ]; then argumentos+=(-H "Content-Type: application/json" -d "$dados"); fi
  local status
  status="$(curl "${argumentos[@]}" "$URL$caminho")"
  if [ "$status" != "$esperado" ]; then
    falhar "$metodo $caminho retornou $status (esperado $esperado)"
  fi
  echo "ok  $metodo $caminho -> $status"
}

# confere <expressão jq que deve ser verdadeira> <descrição>
confere() {
  jq -e "$1" "$CORPO" >/dev/null || falhar "$2"
}

echo "Aguardando $URL/actuator/health..."
for tentativa in $(seq 1 60); do
  if curl -fsS "$URL/actuator/health" 2>/dev/null | grep -q '"UP"'; then
    break
  fi
  if [ "$tentativa" = 60 ]; then
    echo "FALHOU: a API não ficou saudável em 2 minutos" >&2
    exit 1
  fi
  sleep 2
done

ANA="ana-$EXECUCAO@fumaca.test"
BRUNO="bruno-$EXECUCAO@fumaca.test"
chamar POST /api/auth/cadastro 201 "" "{\"nome\":\"Ana\",\"email\":\"$ANA\",\"senha\":\"$SENHA\"}"
chamar POST /api/auth/cadastro 201 "" "{\"nome\":\"Bruno\",\"email\":\"$BRUNO\",\"senha\":\"$SENHA\"}"

chamar POST /api/auth/login 200 "" "{\"email\":\"$ANA\",\"senha\":\"$SENHA\"}"
TOKEN="$(jq -r .token "$CORPO")"

chamar GET /api/carteiras/minha 401
chamar GET /api/carteiras/minha 200 "$TOKEN"
confere '.saldo == 0' "carteira nova deveria ter saldo 0"

CHAVE="deposito-$EXECUCAO"
chamar POST /api/transacoes/depositos 201 "$TOKEN" '{"valor": 100.00}' "$CHAVE"
confere '.saldoAtual == 100' "depósito deveria resultar em saldo 100"

# Reenvio com a mesma chave: mesma resposta, sem depositar de novo
chamar POST /api/transacoes/depositos 201 "$TOKEN" '{"valor": 100.00}' "$CHAVE"
grep -qi '^Idempotent-Replayed: true' "$CABECALHOS" || falhar "reenvio deveria ter o cabeçalho Idempotent-Replayed"

chamar POST /api/transacoes/transferencias 201 "$TOKEN" "{\"emailDestinatario\":\"$BRUNO\",\"valor\":30.00}" "transferencia-$EXECUCAO"
confere '.saldoAtual == 70' "transferência deveria deixar saldo 70"

chamar GET "/api/transacoes/extrato?tamanho=10" 200 "$TOKEN"
confere '[.itens[].natureza] == ["DEBITO", "CREDITO"]' "extrato deveria ter a transferência e um único depósito"

chamar GET /api/carteiras/minha 200 "$TOKEN"
confere '.saldo == 70' "saldo final deveria ser 70"

echo "Teste de fumaça concluído com sucesso em $URL"
