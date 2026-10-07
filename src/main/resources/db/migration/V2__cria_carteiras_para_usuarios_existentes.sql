-- =====================================================================
-- V2 — Carteira para usuários cadastrados antes da criação automática
--
-- A partir desta versão, toda carteira nasce junto com o usuário (mesma transação).
-- Esta migração de dados cobre os usuários que já existiam. É segura para rodar
-- em banco vazio e não cria carteira duplicada (uk_carteiras_usuario).
-- =====================================================================

INSERT INTO carteiras (id, usuario_id, saldo, versao, criado_em, atualizado_em)
SELECT gen_random_uuid(), u.id, 0, 0, now(), now()
FROM usuarios u
WHERE NOT EXISTS (SELECT 1 FROM carteiras c WHERE c.usuario_id = u.id);
