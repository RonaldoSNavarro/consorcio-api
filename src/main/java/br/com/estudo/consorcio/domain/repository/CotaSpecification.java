package br.com.estudo.consorcio.domain.repository;

import br.com.estudo.consorcio.domain.model.Cota;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public class CotaSpecification {

    private CotaSpecification() {}

    public static Specification<Cota> porGrupoId(Long grupoId) {
        return (root, query, cb) -> grupoId == null ? null : cb.equal(root.get("grupo").get("id"), grupoId);
    }

    public static Specification<Cota> porCodigoCota(Integer codigoCota) {
        return (root, query, cb) -> codigoCota == null ? null : cb.equal(root.get("codigoCota"), codigoCota);
    }

    @Deprecated
    public static Specification<Cota> porNumeroCota(Integer numeroCota) {
        return porCodigoCota(numeroCota);
    }

    public static Specification<Cota> porVersaoHistorico(Integer versaoHistorico) {
        return (root, query, cb) -> versaoHistorico == null ? null : cb.equal(root.get("versaoHistorico"), versaoHistorico);
    }

    @Deprecated
    public static Specification<Cota> porVersao(Integer versao) {
        return porVersaoHistorico(versao);
    }

    public static Specification<Cota> porCpfCnpj(String cpfCnpj) {
        if (cpfCnpj == null || cpfCnpj.isBlank()) return (root, query, cb) -> null;
        return (root, query, cb) -> {
            var clienteJoin = root.join("cliente", JoinType.INNER);
            return cb.equal(clienteJoin.get("cpfCnpj"), cpfCnpj.replaceAll("[^0-9]", ""));
        };
    }

    public static Specification<Cota> porStatusDiferenteDe(br.com.estudo.consorcio.domain.model.StatusCota status) {
        return (root, query, cb) -> status == null ? null : cb.notEqual(root.get("status"), status);
    }
}
