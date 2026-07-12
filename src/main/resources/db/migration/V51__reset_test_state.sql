UPDATE grupos SET status = 'EM_ANDAMENTO' WHERE id = 1;
UPDATE assembleias SET status = 'AGENDADA', pedra_chave_calculada = null, numero_sorteado = null WHERE grupo_id = 1;
DELETE FROM contemplacoes;
DELETE FROM lancamentos_contabeis;
UPDATE cotas SET status = 'ATIVA' WHERE status = 'CONTEMPLADA';
