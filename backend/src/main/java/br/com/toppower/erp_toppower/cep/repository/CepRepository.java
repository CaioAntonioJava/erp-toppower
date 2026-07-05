package br.com.toppower.erp_toppower.cep.repository;

import br.com.toppower.erp_toppower.cep.entity.Cep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio da base local de CEPs.
 *
 * <p>A PK natural e {@code String} (o CEP em 8 digitos), logo
 * {@link JpaRepository#findById(Object)} ja e o lookup por CEP —
 * nao e necessario metodo derivado extra.</p>
 */
@Repository
public interface CepRepository extends JpaRepository<Cep, String>, JpaSpecificationExecutor<Cep> {

    boolean existsByCep(String cep);

    /**
     * Lista CEPs de uma cidade, ordenados por logradouro. Util para
     * autocomplete de enderecos quando o usuario ainda nao digitou o CEP.
     */
    List<Cep> findByUfAndCidadeOrderByLogradouro(String uf, String cidade);
}