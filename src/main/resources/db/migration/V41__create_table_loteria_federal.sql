CREATE TABLE loteria_federal (
    id BIGSERIAL PRIMARY KEY,
    concurso VARCHAR(20) NOT NULL UNIQUE,
    data_sorteio DATE NOT NULL,
    premio_1 VARCHAR(10) NOT NULL,
    premio_2 VARCHAR(10) NOT NULL,
    premio_3 VARCHAR(10) NOT NULL,
    premio_4 VARCHAR(10) NOT NULL,
    premio_5 VARCHAR(10) NOT NULL
);
